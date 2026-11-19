package app.caregotchi

import android.app.Application
import android.content.Context
import app.caregotchi.data.Air
import app.caregotchi.data.Attack
import app.caregotchi.data.Db
import app.caregotchi.data.Mark
import app.caregotchi.data.Med
import app.caregotchi.data.Outside
import app.caregotchi.data.OutsideSource
import app.caregotchi.data.Pet
import app.caregotchi.data.Settings
import app.caregotchi.data.SettingsStore
import app.caregotchi.domain.Badges
import app.caregotchi.domain.Book
import app.caregotchi.domain.Chest
import app.caregotchi.domain.Gender
import app.caregotchi.domain.Lang
import app.caregotchi.domain.Level
import app.caregotchi.domain.Loot
import app.caregotchi.domain.Pay
import app.caregotchi.domain.Quests
import app.caregotchi.domain.Shop
import app.caregotchi.domain.Stage
import app.caregotchi.domain.Troubles
import app.caregotchi.domain.startOf
import app.caregotchi.domain.Zoo
import app.caregotchi.domain.dayOf
import app.caregotchi.domain.minuteOf
import app.caregotchi.domain.stats
import app.caregotchi.domain.today
import app.caregotchi.domain.weekStart
import app.caregotchi.work.Reminders
import app.caregotchi.work.Widget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Something to show for a moment: what a mark brought. */
data class Reward(val xp: Int, val leaves: Int, val id: Long = 0)

class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val db = Db.create(app)
    val dao = db.dao()
    val settings = SettingsStore(app)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    /** Little rewards floating up over the pet. */
    val rewards = MutableSharedFlow<Reward>(extraBufferCapacity = 8)
    private val lock = Mutex()

    fun write(block: suspend AppContainer.() -> Unit) { scope.launch { block() } }

    suspend fun book(s: Settings? = null, now: Long = System.currentTimeMillis()) =
        Book(dao.markList(), dao.medList(), dao.attackList(), dao.airList(), s ?: settings.now(), now)

    /** Marks something done: works out how late a dose is, pays for it, feeds the pet, checks tasks, badges and eggs. */
    suspend fun log(kind: String, sub: String = "", med: Long = 0, slot: Int = -1, at: Long = System.currentTimeMillis(), q: Int = 0, tags: String = "", mins: Int = 0, sys: Int = 0, dia: Int = 0, pulse: Int = 0): Reward = lock.withLock {
        val s = settings.now()
        val day = dayOf(at)
        val same = dao.marksSince(startOf(day)).filter { dayOf(it.at) == day }
        val late = if (kind == "pill" && slot >= 0) (minuteOf(at) - slot).coerceAtLeast(0) else 0
        val m0 = Mark(at = at, kind = kind, sub = sub, med = med, slot = slot, late = late, q = q, tags = tags, mins = mins, sys = sys, dia = dia, pulse = pulse)
        val (xp, leaves) = Pay.of(m0, same, s)
        val id = dao.insert(m0.copy(xp = xp, leaves = leaves))
        if (kind == "pill" && med > 0) dao.medList().firstOrNull { it.id == med }?.let { m -> if (m.stock > 0) dao.update(m.copy(stock = (m.stock - m.perDose).coerceAtLeast(0))) }
        val extra = gain(xp, leaves)
        Widget.refresh(app); Reminders.schedule(app)
        Reward(xp, leaves + extra, id).also { rewards.tryEmit(it) }
    }

    /** Adds experience and leaves, feeds the pet at home, then looks at tasks, levels, badges and eggs. Returns leaves from finished tasks. */
    private suspend fun gain(xp: Int, leaves: Int): Int {
        if (xp > 0) dao.petList().firstOrNull { it.home && it.hatched > 0 }?.let { p ->
            val before = Stage.of(p.xp); val after = Stage.of(p.xp + xp)
            dao.update(p.copy(xp = p.xp + xp))
            if (after > before) settings.update { it.copy(inbox = it.inbox + "stage:${p.id}:$after") }
        }
        val lvl0 = Level.of(settings.now().xp).first
        settings.update { it.copy(xp = it.xp + xp, leaves = it.leaves + leaves, leavesEver = it.leavesEver + leaves) }
        val lvl1 = Level.of(settings.now().xp).first
        if (lvl1 > lvl0) settings.update { it.copy(leaves = it.leaves + 50, leavesEver = it.leavesEver + 50, inbox = it.inbox + "level:$lvl1") }
        return settle()
    }

    /** Tasks done, badges sewn on and eggs found since the last look. Returns the leaves the tasks brought. */
    private suspend fun settle(): Int {
        val s = settings.now()
        val b = book(s)
        val t = today()
        val quests = if (s.questDay == t) s.quests else Quests.deal(t, b.meds.any { !it.asNeeded && !it.archived })
        val done0 = if (s.questDay == t) s.questsDone else emptySet()
        val newly = quests.mapNotNull { Quests.byId[it] }.filter { it.id !in done0 && it.progress(b.todayCare, s) >= it.of }
        val questLeaves = newly.sumOf { it.leaves }
        val st = b.stats()
        val level = Level.of(s.xp).first
        val badges = Badges.all.filter { "${it.id}" !in s.badges && it.progress(st, s, level) >= it.need }.map { "${it.id}" }
        val pets = dao.petList()
        val owned = pets.map { it.species }.toSet()
        val eggs = Zoo.all.filter { it.id !in owned && it.id !in s.eggsSeen && it.progress(st) >= it.need }.take(1)
        for (e in eggs) dao.insert(Pet(species = e.id, name = Zoo.randomName(e.id), found = t))
        settings.update { it.copy(questDay = t, quests = quests, questsDone = done0 + newly.map { q -> q.id },
            leaves = it.leaves + questLeaves, leavesEver = it.leavesEver + questLeaves, badges = it.badges + badges,
            eggsSeen = it.eggsSeen + eggs.map { e -> e.id },
            inbox = it.inbox + newly.map { q -> "quest:${q.id}" } + badges.map { x -> "badge:$x" } + eggs.map { e -> "egg:${e.id}" }) }
        return questLeaves
    }

    /** Says afterwards how wholesome a meal was (a little bonus for a wholesome one). */
    suspend fun rateMeal(m: Mark, q: Int, tags: String) = lock.withLock {
        val bonus = if (q == 3 && m.q != 3) 2 else 0
        dao.update(m.copy(q = q, tags = tags, leaves = m.leaves + bonus))
        if (bonus > 0) settings.update { it.copy(leaves = it.leaves + bonus, leavesEver = it.leavesEver + bonus) }
        settle()
    }

    suspend fun undo(m: Mark) = lock.withLock {
        dao.deleteMark(m.id)
        settings.update { it.copy(xp = (it.xp - m.xp).coerceAtLeast(0), leaves = (it.leaves - m.leaves).coerceAtLeast(0)) }
        if (m.kind == "pill" && m.med > 0) dao.medList().firstOrNull { it.id == m.med }?.let { md -> if (md.stock > 0 || md.times.isNotBlank()) dao.update(md.copy(stock = md.stock + md.perDose)) }
        Widget.refresh(app)
    }

    /** A bad spell begins. Writing it down is care too: it pays, and never harms the pet. */
    suspend fun startAttack(kind: String, at: Long = System.currentTimeMillis()): Long = lock.withLock {
        val b = book()
        val a = Attack(start = at, kind = kind, triggers = Troubles.guess(b, at).joinToString(","))
        val id = dao.insert(a)
        val spellsToday = b.attacksOn(dayOf(at)).size
        if (spellsToday < 2) { gain(Pay.ATTACK_XP, Pay.ATTACK_LEAVES); rewards.tryEmit(Reward(Pay.ATTACK_XP, Pay.ATTACK_LEAVES)) } else settle()
        Widget.refresh(app)
        id
    }

    suspend fun saveAttack(a: Attack) { if (a.id == 0L) dao.insert(a) else dao.update(a); if (a.took.isNotBlank() && a.relief == 0) Reminders.relief(app, a) }
    suspend fun endAttack(a: Attack, at: Long = System.currentTimeMillis()) { dao.update(a.copy(end = at)); Widget.refresh(app) }
    suspend fun deleteAttack(a: Attack) = dao.deleteAttack(a.id)

    /** A day under the blanket: the needs stand still and the day counts as good. */
    suspend fun plaidToday(on: Boolean) {
        val t = today()
        settings.update { it.copy(plaidDays = if (on) it.plaidDays + t else it.plaidDays - t) }
        Widget.refresh(app)
    }

    suspend fun saveMed(m: Med): Long {
        val id = if (m.id == 0L) dao.insert(m.copy(created = System.currentTimeMillis())) else { dao.update(m); m.id }
        Reminders.schedule(app); return id
    }

    suspend fun buy(id: String): Boolean = lock.withLock {
        val it = Shop.byId[id] ?: return false
        val s = settings.now()
        if (s.leaves < it.price || (!it.special && id in s.items) || Level.of(s.xp).first < it.level) return false
        when (id) {
            "plaid" -> settings.update { st -> st.copy(leaves = st.leaves - it.price, plaids = st.plaids + 1) }
            "egg" -> {
                val owned = dao.petList().map { p -> p.species }.toSet()
                val sp = Zoo.all.filter { z -> z.id !in owned }.randomOrNull() ?: return false
                dao.insert(Pet(species = sp.id, name = Zoo.randomName(sp.id), found = today()))
                settings.update { st -> st.copy(leaves = st.leaves - it.price, eggsSeen = st.eggsSeen + sp.id, inbox = st.inbox + "egg:${sp.id}") }
            }
            "chest" -> { settings.update { st -> st.copy(leaves = st.leaves - it.price) }; val l = Chest.roll(settings.now(), Level.of(s.xp).first); give(l); settings.update { st -> st.copy(inbox = st.inbox + "loot:${l.leaves}:${l.item.orEmpty()}:${l.plaid}") } }
            else -> settings.update { st -> st.copy(leaves = st.leaves - it.price, items = st.items + id) }
        }
        settle(); true
    }

    private suspend fun give(l: Loot) = settings.update { st -> st.copy(leaves = st.leaves + l.leaves, leavesEver = st.leavesEver + l.leaves,
        items = if (l.item != null) st.items + l.item else st.items, plaids = st.plaids + if (l.plaid) 1 else 0) }

    /** Opens this week's chest, once five good days are in. */
    suspend fun openChest(): Loot? = lock.withLock {
        val s = settings.now(); val b = book(s)
        val monday = weekStart(today())
        if (s.chestWeek == monday) return null
        val good = (monday..today()).count { d -> b.care(d)?.good == true }
        if (good < 5) return null
        val l = Chest.roll(s, Level.of(s.xp).first)
        give(l); settings.update { it.copy(chestWeek = monday) }; settle(); l
    }

    suspend fun toggleItem(id: String) = settings.update { it.copy(hidden = if (id in it.hidden) it.hidden - id else it.hidden + id) }
    suspend fun clue(id: String, v: Int) { settings.update { it.copy(clues = it.clues + (id to v)) }; if (v > 0) { gain(5, 5); rewards.tryEmit(Reward(5, 5)) } }
    suspend fun setHome(p: Pet) { dao.updatePets(dao.petList().map { it.copy(home = it.id == p.id) }); Widget.refresh(app) }
    suspend fun rename(p: Pet, name: String) = dao.update(p.copy(name = name.trim().ifEmpty { p.name }))
    suspend fun seen(key: String) = settings.update { it.copy(inbox = it.inbox - key, seen = it.seen + key) }

    /** Daily housekeeping: the day's tasks, eggs that hatch after two good days, a blanket over a missed day, reminders, the weather. */
    suspend fun tick() = lock.withLock {
        val s = settings.now(); val t = today()
        val b = book(s)
        // a missed yesterday is covered by a blanket if one is left, so the streak holds
        val y = b.care(t - 1)
        if (y != null && !y.any && !y.plaid && s.plaids > 0 && (b.care(t - 2)?.any == true)) {
            settings.update { it.copy(plaids = it.plaids - 1, plaidDays = it.plaidDays + (t - 1), inbox = it.inbox + "plaid:${t - 1}") }
        }
        // eggs hatch after two good days
        val b2 = book()
        for (p in dao.petList().filter { it.hatched == 0L }) {
            val good = (p.found..t).count { d -> b2.care(d)?.good == true }
            if (good >= 2) {
                val anyHome = dao.petList().any { it.home }
                dao.update(p.copy(hatched = t, home = !anyHome))
                settings.update { it.copy(inbox = it.inbox + "hatch:${p.id}") }
            }
        }
        settle()
        Reminders.schedule(app)
        Widget.refresh(app)
    }

    /** Fetches the weather for the window when older than half an hour; keeps the day's pressure for the detective. */
    suspend fun refreshOutside(force: Boolean = false) = withContext(Dispatchers.IO) { runCatching {
        var s = settings.now()
        if (s.lat.isNaN() && !s.guessed) {
            val t = OutsideSource.guess(Lang.ru)
            settings.update { it.copy(guessed = true, town = t?.name ?: it.town, lat = t?.lat ?: it.lat, lon = t?.lon ?: it.lon) }
            s = settings.now()
        }
        if (s.lat.isNaN()) return@runCatching
        val old = Outside.of(s.outside)
        if (!force && old != null && System.currentTimeMillis() - old.at < 30 * 60_000L) return@runCatching
        val o = OutsideSource.fetch(s.lat, s.lon)
        settings.update { it.copy(outside = o.json()) }
        if (!o.pressure.isNaN()) {
            val d = today(); val a = dao.air(d)
            dao.upsert(Air(d, minOf(a?.min ?: o.pressure, o.pressure), maxOf(a?.max ?: o.pressure, o.pressure), o.pressure))
        }
    } }
}

class CaregotchiApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        Lang.ru = resources.configuration.locales[0].language == "ru"
        container = AppContainer(this)
        container.scope.launch(Dispatchers.Default) { Gender.fem = container.settings.now().fem }
        Reminders.channels(this)
    }
}

val Context.container: AppContainer get() = (applicationContext as CaregotchiApp).container
