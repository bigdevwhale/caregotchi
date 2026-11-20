package app.caregotchi.data

import app.caregotchi.AppContainer
import app.caregotchi.domain.Lang
import app.caregotchi.domain.Level
import app.caregotchi.domain.Pay
import app.caregotchi.domain.Quests
import app.caregotchi.domain.dayOf
import app.caregotchi.domain.minuteOf
import app.caregotchi.domain.startOf
import app.caregotchi.domain.today
import app.caregotchi.domain.weekStart
import kotlin.random.Random

/**
 * Two months of a life with migraines for the demo build and the screen renders: three medicines, meals, water,
 * walks and sleep, bad spells that come more often after short nights and pressure swings, three pets and an egg.
 */
object DemoSeed {
    /** [awake]: screenshots want the pet up, so a wake-up is added this morning if the night isn't over yet. */
    suspend fun seedIfEmpty(c: AppContainer, now: Long = System.currentTimeMillis(), awake: Boolean = app.caregotchi.BuildConfig.SHOTS) {
        if (c.settings.now().onboarded || c.dao.medList().isNotEmpty()) return
        val r = Random(7)
        val t = dayOf(now)
        val from = t - 62
        val ru = Lang.ru
        val topa = c.dao.insert(Med(name = if (ru) "Топирамат" else "Topiramate", dose = if (ru) "25 мг" else "25 mg", times = "480,1200", herb = "mint", from = from + 4, created = startOf(from + 4)))
        val mag = c.dao.insert(Med(name = if (ru) "Магний B6" else "Magnesium B6", dose = "", times = "780", herb = "chamomile", stock = 24, from = from + 10, created = startOf(from + 10)))
        val suma = c.dao.insert(Med(name = if (ru) "Суматриптан" else "Sumatriptan", dose = if (ru) "50 мг" else "50 mg", pain = 2, herb = "lavender", created = startOf(from)))
        c.dao.insert(Med(name = if (ru) "Витамин D" else "Vitamin D", dose = if (ru) "2000 МЕ" else "2000 IU", times = "${15 * 60}", food = 3, gap = 60, meals = "1", herb = "sage",
            from = weekStart(t) + 7, until = weekStart(t) + 7 + 59, created = startOf(t)))
        val ibu = c.dao.insert(Med(name = if (ru) "Ибупрофен" else "Ibuprofen", dose = if (ru) "400 мг" else "400 mg", pain = 1, herb = "rosemary", created = startOf(from)))
        val marks = mutableListOf<Mark>(); val attacks = mutableListOf<Attack>(); val air = mutableListOf<Air>()
        var p = 1014.0
        fun at(day: Long, min: Int) = startOf(day) + min * 60_000L
        for (day in from..t) {
            val swing = if (r.nextFloat() < .2f) 6 + r.nextDouble() * 6 else r.nextDouble() * 4
            val lo = p - swing * r.nextDouble(); val hi = lo + swing
            air += Air(day, lo, hi, lo + swing * r.nextDouble()); p = (p + r.nextDouble(-4.0, 4.0)).coerceIn(996.0, 1030.0)
            val shortNight = r.nextFloat() < .22f
            val bedMin = if (shortNight) 24 * 60 + 60 + r.nextInt(60) else 22 * 60 + 40 + r.nextInt(90)
            val wakeMin = if (shortNight) 6 * 60 + 40 + r.nextInt(30) else 7 * 60 + r.nextInt(70)
            fun add(m: Mark) { if (m.at <= now) marks += m }
            if (day > from) add(Mark(at = at(day - 1, bedMin), kind = "sleep"))
            add(Mark(at = at(day, wakeMin), kind = "wake"))
            val lazy = r.nextFloat() < .12f
            if (!lazy && r.nextFloat() < .8f) add(Mark(at = at(day, wakeMin + 30 + r.nextInt(40)), kind = "eat", sub = "breakfast", q = listOf(3, 3, 2, 2, 0).random(r), tags = if (r.nextFloat() < .4f) "fruit" else ""))
            if (r.nextFloat() < .85f) add(Mark(at = at(day, 13 * 60 + r.nextInt(80)), kind = "eat", sub = "lunch", q = listOf(3, 2, 2, 1, 0).random(r), tags = if (r.nextFloat() < .5f) "veg,protein" else "protein"))
            if (!lazy && r.nextFloat() < .9f) add(Mark(at = at(day, 19 * 60 + r.nextInt(90)), kind = "eat", sub = "dinner", q = listOf(3, 2, 2, 1).random(r), tags = if (r.nextFloat() < .5f) "veg" else ""))
            if (r.nextFloat() < .35f) add(Mark(at = at(day, 16 * 60 + r.nextInt(60)), kind = "eat", sub = "snack", q = listOf(1, 1, 2, 3).random(r), tags = if (r.nextFloat() < .5f) "sweet" else "fruit"))
            repeat(if (lazy) 2 else 3 + r.nextInt(5)) { add(Mark(at = at(day, 9 * 60 + it * 110 + r.nextInt(50)), kind = "water")) }
            // coffee in the morning, sometimes a second cup in the afternoon; a skipped coffee day now and then
            if (r.nextFloat() < .88f) add(Mark(at = at(day, wakeMin + 20 + r.nextInt(30)), kind = "caf", sub = listOf("coffee", "latte", "cappuccino", "espresso").random(r), mins = listOf(95, 75, 75, 65).random(r)))
            if (r.nextFloat() < .45f) add(Mark(at = at(day, 15 * 60 + r.nextInt(150)), kind = "caf", sub = listOf("coffee", "tea", "green", "cola").random(r), mins = listOf(95, 45, 30, 35).random(r)))
            if (day > t - 14) { // the last fortnight: blood pressure morning and evening
                add(Mark(at = at(day, 8 * 60 + r.nextInt(30)), kind = "bp", sys = 118 + r.nextInt(18), dia = 74 + r.nextInt(12), pulse = 64 + r.nextInt(14)))
                if (r.nextFloat() < .8f) add(Mark(at = at(day, 20 * 60 + r.nextInt(40)), kind = "bp", sys = 122 + r.nextInt(20), dia = 76 + r.nextInt(12), pulse = 68 + r.nextInt(14)))
            }
            if (r.nextFloat() < .6f) add(Mark(at = at(day, 17 * 60 + r.nextInt(120)), kind = "move", sub = listOf("walk", "walk", "walk", "yoga", "run", "bike", "gym").random(r), mins = listOf(15, 20, 30, 30, 40, 45, 60).random(r)))
            if (day >= from + 4) for (slot in listOf(480, 1200)) if (r.nextFloat() < .92f) add(Mark(at = at(day, slot + r.nextInt(if (r.nextFloat() < .85f) 40 else 140)), kind = "pill", med = topa, slot = slot))
            if (day >= from + 10 && r.nextFloat() < .85f) add(Mark(at = at(day, 780 + r.nextInt(50)), kind = "pill", med = mag, slot = 780))
            // a spell: more likely after a short night, a pressure swing or a skipped breakfast
            val chance = .11f + (if (shortNight) .25f else 0f) + (if (swing >= 6) .2f else 0f) + (if (lazy) .12f else 0f)
            if (day < t && r.nextFloat() < chance) {
                val s = at(day, 10 * 60 + r.nextInt(8 * 60)); val len = (2 + r.nextInt(10)) * 3600_000L / 2
                val took = if (r.nextFloat() < .7f) "$suma" else "$ibu"
                attacks += Attack(start = s, end = s + len, kind = if (r.nextFloat() < .8f) "migraine" else "headache", strength = 4 + r.nextInt(5),
                    side = listOf("left", "left", "right", "both").random(r), symptoms = listOf("light", "nausea,light", "aura,light,sound", "nausea").random(r),
                    triggers = listOfNotNull(if (shortNight) "sleep" else null, if (swing >= 6) "weather" else null, if (lazy) "meal" else null).joinToString(","),
                    took = took, relief = if (took == "$suma") listOf(3, 3, 3, 2, 1).random(r) else listOf(2, 1, 3).random(r))
                add(Mark(at = s + 20 * 60_000L, kind = "pill", med = took.toLong()))
            }
        }
        // the cycle: three periods four weeks apart, the last one going on; a sleepless night two days before each
        val rc = Random(11)
        for (st in listOf(t - 55, t - 27, t - 1)) {
            attacks += Attack(start = at(st, 8 * 60 + rc.nextInt(120)), end = if (st < t - 5) at(st + 4, 21 * 60) else 0, kind = app.caregotchi.domain.PERIOD)
            attacks += Attack(start = at(st - 2, 60 + rc.nextInt(60)), end = at(st - 2, 5 * 60 + rc.nextInt(60)), kind = "insomnia", strength = 4 + rc.nextInt(3))
        }
        if (awake && marks.filter { it.kind == "sleep" || it.kind == "wake" }.maxByOrNull { it.at }?.kind == "sleep")
            marks += Mark(at = now - 40 * 60_000L, kind = "wake")
        // pay for every mark as the app would have
        val priced = marks.sortedBy { it.at }.let { all ->
            val seen = mutableMapOf<Long, MutableList<Mark>>()
            val s0 = Settings()
            all.map { m -> val same = seen.getOrPut(dayOf(m.at)) { mutableListOf() }; val (xp, lv) = Pay.of(m, same, s0); same += m; m.copy(late = if (m.slot >= 0) (minuteOf(m.at) - m.slot).coerceAtLeast(0) else 0, xp = xp, leaves = lv) }
        }
        c.dao.insertMarks(priced); c.dao.insertAttacks(attacks); c.dao.upsertAir(air)
        val xp = 1320   // level 8: the history paid more, but a demo keeper is mid-way
        c.dao.insert(Pet(species = "fox", name = if (ru) "Рыжик" else "Ginger", found = from, hatched = from, xp = 1450, home = true))
        c.dao.insert(Pet(species = "hedgehog", name = if (ru) "Колючка" else "Prickles", found = from + 30, hatched = from + 32, xp = 420))
        c.dao.insert(Pet(species = "bunny", name = if (ru) "Овсянка" else "Oatmeal", found = from + 40, hatched = from + 42, xp = 130))
        c.dao.insert(Pet(species = "owl", name = if (ru) "Угу" else "Hoot", found = t, hatched = 0))
        val level = Level.of(xp).first
        c.settings.update { it.copy(onboarded = false, xp = xp, leaves = 248, leavesEver = 1240, items = setOf("lights", "painting", "plush", "lantern", "books"),
            badges = setOf("0", "4", "2", "14", "12", "5", "3"), eggsSeen = app.caregotchi.domain.Zoo.all.map { it.id }.toSet(), plaids = 2, plaidDays = setOf(t - 17),
            questDay = t, quests = Quests.deal(t, true), questsDone = emptySet(), chestWeek = weekStart(t) - 7,
            remindBp = true, cycle = true, town = if (ru) "Москва" else "Moscow", lat = 55.75, lon = 37.62, guessed = true, clues = mapOf("f:sleep" to 1),
            outside = Outside(Weather.CLOUDY, 6.0, 1009.0, List(24) { 1009.0 - it * .4 }, now).json(), inbox = emptyList(), seen = setOf("level:$level")) }
        // what the history earned is already on the blanket: no windows on the first look
        repeat(4) { c.tick() }
        c.settings.update { it.copy(onboarded = true, inbox = emptyList(), questsDone = emptySet()) }
    }
}
