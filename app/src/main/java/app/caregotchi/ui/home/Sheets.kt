package app.caregotchi.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.caregotchi.container
import app.caregotchi.data.Attack
import app.caregotchi.domain.Badges
import app.caregotchi.domain.Loot
import app.caregotchi.domain.Shop
import app.caregotchi.domain.Stage
import app.caregotchi.domain.Troubles
import app.caregotchi.domain.Zoo
import app.caregotchi.domain.dayOf
import app.caregotchi.domain.fmtDay
import app.caregotchi.domain.g
import app.caregotchi.domain.hhmm
import app.caregotchi.domain.minuteOf
import app.caregotchi.domain.today
import app.caregotchi.domain.tr
import app.caregotchi.domain.weekStart
import app.caregotchi.ui.Data
import app.caregotchi.ui.Nav
import app.caregotchi.ui.Screen
import app.caregotchi.ui.components.Art
import app.caregotchi.ui.components.Btn
import app.caregotchi.ui.components.Chip
import app.caregotchi.ui.components.Ghost
import app.caregotchi.ui.components.Leaves
import app.caregotchi.ui.components.Popup
import app.caregotchi.ui.components.Sheet
import app.caregotchi.ui.components.Thread
import app.caregotchi.ui.components.PicGrid
import app.caregotchi.ui.components.PicTile
import app.caregotchi.ui.components.Token
import app.caregotchi.ui.components.StrengthBar
import app.caregotchi.ui.components.HeadPicker
import app.caregotchi.ui.components.tap
import app.caregotchi.ui.theme.C
import app.caregotchi.ui.theme.T
import kotlinx.coroutines.launch

fun moveIcon(id: String) = when (id) { "home" -> "icon_rope"; "other" -> "icon_ball"; else -> "icon_$id" }
fun qualityIcon(q: Int) = when (q) { 3 -> "icon_wholesome"; 2 -> "icon_ordinary"; else -> "icon_fastfood" }

/** − and + as two round buttons. */
@Composable
fun Stepper2(onMinus: () -> Unit, onPlus: () -> Unit) = Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    listOf("−" to onMinus, "+" to onPlus).forEach { (t, f) -> Box(Modifier.size(42.dp).clip(androidx.compose.foundation.shape.CircleShape).background(C.card2).clickable(onClick = f), contentAlignment = Alignment.Center) { Text(t, style = T.btn.copy(color = C.ink)) } }
}

val MOVES = listOf("walk" to ("Прогулка" to "Walk"), "run" to ("Бег" to "Run"), "gym" to ("Зал" to "Gym"), "yoga" to ("Йога, растяжка" to "Yoga, stretching"),
    "bike" to ("Велосипед" to "Bike"), "swim" to ("Плавание" to "Swim"), "dance" to ("Танцы" to "Dance"), "home" to ("Зарядка дома" to "Home workout"), "other" to ("Другое" to "Other"))
val MEALS = listOf("breakfast" to ("Завтрак" to "Breakfast"), "lunch" to ("Обед" to "Lunch"), "dinner" to ("Ужин" to "Dinner"), "snack" to ("Перекус" to "Snack"))
val QUALITY = listOf(3 to ("Полезно" to "Wholesome"), 2 to ("Обычно" to "Ordinary"), 1 to ("Фастфуд, сладкое" to "Fast food, sweets"))
val TAGS = listOf("veg" to ("Овощи" to "Vegetables"), "fruit" to ("Фрукты" to "Fruit"), "protein" to ("Белок" to "Protein"), "sweet" to ("Сладкое" to "Sweets"))
fun word(l: List<Pair<String, Pair<String, String>>>, id: String) = l.firstOrNull { it.first == id }?.second?.let { tr(it.first, it.second) } ?: id

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Sheets(d: Data, nav: Nav) {
    val sheet = nav.sheet ?: return
    val c = LocalContext.current.container
    val scope = c.scope
    val close = { nav.sheet = null }
    when {
        sheet.startsWith("rate:") -> {
            val m = d.marks.firstOrNull { it.id == sheet.drop(5).toLongOrNull() } ?: return
            var q by remember { mutableIntStateOf(m.q) }
            var tags by remember { mutableStateOf(m.tags.split(',').filter { it.isNotBlank() }.toSet()) }
            var sub by remember { mutableStateOf(m.sub) }
            Sheet(close) {
                Text(tr("Как поел{|а}?", "How did you eat?").let(::g), style = T.h2)
                Text(tr("Можно не отвечать – еда уже записана.", "You can skip this – the meal is saved."), style = T.small)
                Spacer(Modifier.height(12.dp))
                PicGrid(MEALS, 4) { (id, w), mod -> PicTile("icon_$id", tr(w.first, w.second), sub == id, mod, pic = 46.dp) { sub = id } }
                Text(tr("Насколько полезно", "How wholesome"), style = T.sec, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                PicGrid(QUALITY, 3) { (v, w), mod -> PicTile(qualityIcon(v), tr(w.first, w.second), q == v, mod, pic = 60.dp) { q = if (q == v) 0 else v } }
                d.pet?.let { pet -> if (q > 0) Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Art("pet_${pet.species}_${if (q == 3) 6 else 3}", Modifier.size(44.dp)); Spacer(Modifier.width(8.dp))
                    Text(when (q) { 3 -> tr("Витамины! Я прямо чувствую, как расту.", "Vitamins! I can feel myself growing."); 2 -> tr("Сытно и спокойно.", "Filling and calm.")
                        else -> tr("Вкуснятина! Но давай в следующий раз с овощами?", "Yummy! Vegetables next time?") }, style = T.smallB) } }
                Text(tr("Что было на тарелке", "What was on the plate"), style = T.sec, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TAGS.forEach { (id, w) -> Token("icon_$id", tr(w.first, w.second), id in tags) { tags = if (id in tags) tags - id else tags + id } }
                }
                Btn(tr("Готово", "Done"), Modifier.fillMaxWidth().padding(top = 18.dp)) {
                    scope.launch { c.dao.update(m.copy(sub = sub)); c.rateMeal(m.copy(sub = sub), q, tags.joinToString(",")) }; close()
                }
            }
        }
        sheet == "bp" -> BpSheet(d, nav)
        sheet == "period" -> PeriodSheet(d, nav)
        sheet.startsWith("mark:") -> MarkSheet(sheet.drop(5).toLongOrNull() ?: 0, d, nav)
        sheet.startsWith("add:") -> AddSheet(sheet.drop(4).toLongOrNull() ?: today(), d, nav)
        sheet == "caf" -> CafSheet(d, nav)
        sheet == "move" -> {
            var kind by remember { mutableStateOf("walk") }
            var mins by remember { mutableIntStateOf(30) }
            Sheet(close) {
                Text(tr("Движение", "Moving"), style = T.h2)
                Text(tr("Всё считается: прогулка до магазина тоже.", "Everything counts: a walk to the shop too."), style = T.small)
                Spacer(Modifier.height(12.dp))
                PicGrid(MOVES, 3) { (id, w), mod -> PicTile(moveIcon(id), tr(w.first, w.second), kind == id, mod, pic = 50.dp) { kind = id } }
                Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Art("icon_hourglass", Modifier.size(54.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr("$mins мин", "$mins min"), style = T.title)
                        Text(tr("Сегодня уже ${d.book.todayCare.moveMins} из ${d.s.moveGoal}", "${d.book.todayCare.moveMins} of ${d.s.moveGoal} today"), style = T.tiny)
                    }
                    Stepper2({ mins = (mins - 5).coerceAtLeast(5) }, { mins = (mins + 5).coerceAtMost(240) })
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(10, 20, 30, 45, 60, 90).forEach { m -> Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (mins == m) C.acc else C.card2).clickable { mins = m }.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                        Text("$m", style = T.smallB.copy(color = if (mins == m) C.accInk else C.ink)) } }
                }
                Btn(tr("Записать", "Save"), Modifier.fillMaxWidth().padding(top = 16.dp), icon = moveIcon(kind)) {
                    scope.launch { c.log("move", sub = kind, mins = mins) }; close()
                }
            }
        }
        sheet == "pill" -> Sheet(close) {
            Text(tr("Лекарства", "Medicine"), style = T.h2)
            // the doses due now first (#20); the list is kept as it opened, so a dose just taken stays in it with a tick
            // and several medicines can be noted one after another before «Done» (#22)
            val left = remember { d.book.dosesLeft().sortedBy { kotlin.math.abs(it.second - d.book.nowMin) > 120 } }
            val todays = d.book.marksOn(d.book.today).filter { it.kind == "pill" }
            if (left.isEmpty() && d.activeMeds.none { it.asNeeded }) Text(tr("Лекарств пока нет. Добавьте их на подоконнике.", "No medicine yet. Add it on the windowsill."), style = T.small)
            left.forEach { (m, slot) ->
                val done = todays.firstOrNull { it.med == m.id && it.slot == slot }
                Row(Modifier.fillMaxWidth().clickable(enabled = done == null) { scope.launch { c.log("pill", med = m.id, slot = slot) } }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Art("pot_${m.herb}_1", Modifier.size(36.dp)); Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) { Text(m.name + if (m.dose.isNotBlank()) " · ${m.dose}" else "", style = T.bodyB.copy(color = if (done != null) C.mute else C.ink)); Text(hhmm(slot) + app.caregotchi.domain.doseNote(m, d.book.today).let { if (it.isEmpty()) "" else " · $it" } + app.caregotchi.domain.Plan.foodWord(m).let { if (it.isEmpty()) "" else " · $it" }, style = T.small) }
                    Text(if (done != null) "✓ " + hhmm(minuteOf(done.at)) else tr("Принял{|а}", "Taken").let(::g), style = T.smallB.copy(color = if (done != null) C.mute else C.acc))
                }
            }
            d.activeMeds.filter { it.asNeeded }.forEach { m ->
                // taken as needed can be taken again, but not by a double tap
                val last = todays.filter { it.med == m.id }.maxByOrNull { it.at }
                Row(Modifier.fillMaxWidth().clickable { if (last == null || System.currentTimeMillis() - last.at > 3_000) scope.launch { c.log("pill", med = m.id) } }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Art("pot_${m.herb}_1", Modifier.size(36.dp)); Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) { Text(m.name, style = T.bodyB); Text(tr("по необходимости", "as needed") + if (last != null) tr(" · сегодня в ${hhmm(minuteOf(last.at))}", " · today at ${hhmm(minuteOf(last.at))}") else "", style = T.small) }
                    Text(tr("Принял{|а}", "Taken").let(::g), style = T.smallB.copy(color = C.acc))
                }
            }
            if (left.isNotEmpty() || d.activeMeds.any { it.asNeeded }) Btn(tr("Готово", "Done"), Modifier.fillMaxWidth().padding(top = 10.dp)) { close() }
            Ghost(tr("Добавить лекарство", "Add medicine"), Modifier.fillMaxWidth().padding(top = 10.dp)) { close(); nav.push(Screen.MedEdit(0)) }
        }
        sheet == "bad" -> Sheet(close) {
            Text(tr("Что случилось?", "What's wrong?"), style = T.h2)
            Text(tr("Запись – тоже забота. Питомец ничего не теряет.", "Writing it down is care too. The pet loses nothing."), style = T.small)
            Spacer(Modifier.height(12.dp))
            val kinds = Troubles.kinds(d.s)
            PicGrid(kinds, 3) { k, mod -> PicTile("icon_${k.icon}", k.title, false, mod, pic = 54.dp) {
                if (k.id == "pressure") nav.sheet = "bp" else scope.launch { val id = c.startAttack(k.id); close(); if (k.dark) nav.dark = id else nav.push(Screen.AttackEdit(id)) } } }
            if (d.s.cycle) {
                val p = d.book.periodNow
                Row(Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(16.dp)).background(Color(0x22C98B86)).border(1.dp, Color(0x55C98B86), RoundedCornerShape(16.dp))
                    .clickable { nav.sheet = "period" }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Art("icon_period", Modifier.size(40.dp)); Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (p != null) tr("Месячные · день ${today() - app.caregotchi.domain.dayOf(p.start) + 1}", "Period · day ${today() - app.caregotchi.domain.dayOf(p.start) + 1}") else tr("Месячные", "Period"), style = T.bodyB)
                        Text(if (p != null) tr("Закончились? И что беспокоит сегодня", "Over? And what bothers you today") else tr("Не приступ, а состояние: начало, конец и симптомы отдельно", "Not a spell but a state: the start, the end and symptoms apart"), style = T.small)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            val plaid = today() in d.s.plaidDays
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0x22C9B9E0)).border(1.dp, Color(0x55C9B9E0), RoundedCornerShape(16.dp))
                .clickable { scope.launch { c.plaidToday(!plaid) }; close() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Art("icon_plaid", Modifier.size(40.dp)); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (plaid) tr("Снять плед", "Take the blanket off") else tr("Я болею – плед на сегодня", "I'm ill – a blanket day"), style = T.bodyB)
                    Text(tr("Потребности не убывают, день засчитан хорошим.", "Needs stand still; the day counts as good."), style = T.small)
                }
            }
        }
        sheet == "quests" -> Sheet(close, banner = "banner_quests") {
            Text(tr("Задания на сегодня", "Today's tasks"), style = T.h2)
            val done = if (d.s.questDay == today()) d.s.questsDone else emptySet()
            d.quests.forEach { q ->
                val p = q.progress(d.book.todayCare, d.s).coerceAtMost(q.of)
                val ok = q.id in done || p >= q.of
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Art("icon_${q.icon}", Modifier.size(36.dp)); Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(q.text, style = T.bodyB.copy(color = if (ok) C.mute else C.ink, textDecoration = if (ok) androidx.compose.ui.text.style.TextDecoration.LineThrough else null))
                        Text(if (ok) tr("готово", "done") else "$p / ${q.of}", style = T.small)
                    }
                    Text("+${q.leaves}", style = T.num); Art("icon_leaf", Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            ChestBlock(d, nav)
        }
        sheet == "chest" -> Sheet(close, banner = "banner_quests") { ChestBlock(d, nav) }
        sheet.startsWith("need:") -> NeedSheet(sheet.drop(5), d, nav)
        else -> close()
    }
}

@Composable
private fun ChestBlock(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val scope = c.scope
    val monday = weekStart(today())
    val good = (monday..today()).count { d.book.care(it)?.good == true }
    var loot by remember { mutableStateOf<Loot?>(null) }
    Text(tr("Сундук недели", "The week's chest"), style = T.h2)
    Text(tr("Пять хороших дней за неделю – и сундук откроется. День под пледом и день с приступом тоже считаются.",
        "Five good days in a week and the chest opens. Blanket days and days with a spell count too."), style = T.small)
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        (0..6).forEach { i -> val day = monday + i; val g0 = d.book.care(day)?.good == true
            Box(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(50)).background(if (g0) C.sage else C.card2).border(1.dp, if (day == today()) C.honey else C.line, RoundedCornerShape(50))) }
        Art("icon_chest", Modifier.size(44.dp))
    }
    when {
        d.s.chestWeek == monday -> Text(tr("Сундук этой недели уже открыт. Новый – с понедельника.", "This week's chest is open. A new one on Monday."), style = T.smallB)
        good >= 5 -> Btn(tr("Открыть сундук", "Open the chest"), Modifier.fillMaxWidth(), color = C.honey, ink = Color(0xFF3B2E26), icon = "icon_chest") { scope.launch { loot = c.openChest() } }
        else -> Text(tr("Ещё ${5 - good} – и откроется.", "${5 - good} more to go."), style = T.smallB)
    }
    loot?.let { l -> LootPopup(l) { loot = null; nav.sheet = null } }
}

@Composable
fun LootPopup(l: Loot, onClose: () -> Unit) = Popup(onClose) {
    Art("icon_chest", Modifier.size(96.dp))
    Text(tr("Что в сундуке", "In the chest"), style = T.title)
    Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Reward("icon_leaf", "+${l.leaves}")
        l.item?.let { Reward("item_$it", Shop.byId[it]?.title ?: it) }
        if (l.plaid) Reward("icon_plaid", tr("Плед", "Blanket"))
    }
    Btn(tr("Забрать", "Take it"), Modifier.fillMaxWidth(), onClick = onClose)
}

@Composable
private fun Reward(icon: String, label: String) {
    Column(Modifier.clip(RoundedCornerShape(12.dp)).background(C.card2).padding(8.dp).width(78.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Art(icon, Modifier.size(44.dp)); Text(label, style = T.tiny.copy(color = C.ink), textAlign = TextAlign.Center, maxLines = 2)
    }
}

/** Today's marks of one need, to undo or add. */
@Composable
private fun NeedSheet(k: String, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val scope = c.scope
    val close = { nav.sheet = null }
    val kinds = when (k) { "eat" -> listOf("eat"); "water" -> listOf("water"); "pill" -> listOf("pill"); "walk" -> listOf("move"); else -> listOf("sleep", "wake") }
    val ms = d.book.marksOn(today()).filter { it.kind in kinds }.sortedBy { it.at }
    val t = d.book.todayCare
    Sheet(close) {
        val (title, sub) = when (k) {
            "eat" -> tr("Еда сегодня", "Food today") to tr("${t.meals} раз · полезных ${t.wholesome}" + (if (t.veg) " · были овощи или фрукты" else ""), "${t.meals} times · ${t.wholesome} wholesome" + (if (t.veg) " · with vegetables or fruit" else ""))
            "water" -> tr("Вода сегодня", "Water today") to tr("${t.water} из ${d.s.waterGoal} стаканов", "${t.water} of ${d.s.waterGoal} glasses")
            "pill" -> tr("Лекарства сегодня", "Medicine today") to tr("${t.taken} из ${t.due} по расписанию", "${t.taken} of ${t.due} scheduled")
            "walk" -> tr("Движение сегодня", "Moving today") to tr("${t.moveMins} из ${d.s.moveGoal} минут", "${t.moveMins} of ${d.s.moveGoal} minutes")
            else -> tr("Сон", "Sleep") to (if (t.slept >= 0) tr("Этой ночью – ${app.caregotchi.domain.fmt1(t.slept)} ч", "Last night – ${app.caregotchi.domain.fmt1(t.slept)} h") else tr("Отметьте «Отбой» вечером и «Подъём» утром.", "Mark «Bed» at night and «Up» in the morning."))
        }
        Text(title, style = T.h2); Text(sub, style = T.small)
        Spacer(Modifier.height(8.dp))
        if (ms.isEmpty()) Text(tr("Пока ничего.", "Nothing yet."), style = T.small, modifier = Modifier.padding(vertical = 8.dp))
        ms.forEach { m ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(hhmm(minuteOf(m.at)), style = T.smallB.copy(color = C.mute), modifier = Modifier.width(48.dp))
                Text(markText(m, d), style = T.body, modifier = Modifier.weight(1f).clickable { nav.sheet = "mark:${m.id}" })
                Text(tr("убрать", "undo"), style = T.smallB.copy(color = C.bad), modifier = Modifier.clickable { scope.launch { c.undo(m) } }.padding(6.dp))
            }
        }
        if (ms.isNotEmpty()) Text(tr("Нажмите на запись, чтобы поправить время или подробности.", "Tap a record to change its time or details."), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
        Ghost(tr("Записать задним числом", "Add something earlier"), Modifier.fillMaxWidth().padding(top = 10.dp)) { nav.sheet = "add:${today()}" }
        Btn(tr("Открыть день в дневнике", "Open the day in the diary"), Modifier.fillMaxWidth().padding(top = 12.dp), color = C.card2, ink = C.ink) { close(); nav.push(Screen.DayView(today())) }
    }
}

fun markText(m: app.caregotchi.data.Mark, d: Data): String = when (m.kind) {
    "eat" -> word(MEALS, m.sub) + when (m.q) { 3 -> tr(" · полезно", " · wholesome"); 1 -> tr(" · фастфуд/сладкое", " · fast food/sweets"); 2 -> tr(" · обычно", " · ordinary"); else -> "" } +
        m.tags.split(',').filter { it.isNotBlank() }.joinToString("") { " · " + word(TAGS, it).lowercase() }
    "water" -> tr("Стакан воды", "A glass of water")
    "pill" -> (d.meds.firstOrNull { it.id == m.med }?.name ?: tr("Лекарство", "Medicine")) + if (m.slot >= 0) (if (m.late <= 60) tr(" · вовремя", " · on time") else tr(" · с опозданием", " · late")) else ""
    "move" -> word(MOVES, m.sub) + " · ${m.mins} " + tr("мин", "min")
    "caf" -> drinkName(m.sub) + " · ${m.mins} " + tr("мг", "mg")
    "bp" -> tr("Давление ${m.sys}/${m.dia}", "Blood pressure ${m.sys}/${m.dia}") + if (m.pulse > 0) tr(" · пульс ${m.pulse}", " · pulse ${m.pulse}") else ""
    "sleep" -> tr("Отбой", "Went to bed")
    "wake" -> tr("Подъём", "Got up")
    else -> m.kind
}

/** The dark room: a bad spell written down almost blind, with big buttons in a dim red. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DarkRoom(id: Long, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val scope = c.scope
    val a0 = d.attacks.firstOrNull { it.id == id } ?: return
    var a by remember(id) { mutableStateOf(a0) }
    fun save(x: Attack) { a = x; scope.launch { c.saveAttack(x) } }
    BackHandler { nav.dark = 0 }
    val dim = Color(0xFF1C1210); val txt = Color(0xFFC98E83); val off = Color(0xFF2A1915); val on = Color(0xFF5E211A)
    @Composable fun Big(label: String, sel: Boolean, mod: Modifier = Modifier, onClick: () -> Unit) {
        Box(mod.clip(RoundedCornerShape(12.dp)).background(if (sel) on else off).border(1.dp, Color(0xFF3A221D), RoundedCornerShape(12.dp)).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp), contentAlignment = Alignment.Center) { Text(label, style = T.bodyB.copy(color = if (sel) Color(0xFFF0C1B6) else txt)) }
    }
    @Composable fun Lbl(s: String) = Text(s.uppercase(), style = T.sec.copy(color = Color(0xFF7A4C44)), modifier = Modifier.padding(top = 14.dp, bottom = 7.dp))
    Column(Modifier.fillMaxSize().background(dim).tap {}.statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 14.dp)) {
        d.pet?.let { Art("pet_${it.species}_8", Modifier.align(Alignment.CenterHorizontally).size(84.dp), alpha = .35f) }
        Text(tr("Тише, я рядом", "Hush, I'm here"), style = T.h2.copy(color = Color(0xFFD9A196)))
        Text(Troubles.title(a.kind) + " · " + tr("с ", "since ") + hhmm(minuteOf(a.start)) + tr(". Отметьте, что можете, – остальное потом.", ". Mark what you can – the rest later."), style = T.small.copy(color = Color(0xFF8D5F57)))
        Lbl(tr("Сила", "How strong"))
        StrengthBar(a.now, dark = true) { save(if (a.end == 0L) a.felt(it) else a.copy(strength = it)) }
        // how it went: the time of each change (#11)
        if (a.points.size > 1) Text(app.caregotchi.ui.components.curveText(a), style = T.small.copy(color = Color(0xFF8D5F57)), modifier = Modifier.padding(top = 4.dp))
        if (a.kind == "migraine" || a.kind == "headache") {
            Lbl(tr("Где", "Where"))
            HeadPicker(a.side, dark = true) { save(a.copy(side = it)) }
        }
        Lbl(tr("Ещё", "Also"))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Troubles.symptoms.forEach { (sid, w) -> val l = a.list(a.symptoms); Big(tr(w.first, w.second), sid in l) { save(a.copy(symptoms = (if (sid in l) l - sid else l + sid).joinToString(","))) } } }
        Lbl(tr("Принял{|а}", "Took").let(::g))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val l = a.list(a.took)
            d.activeMeds.filter { it.pain > 0 || it.asNeeded }.forEach { m -> Big(m.name, "${m.id}" in l) {
                val took = "${m.id}" !in l
                save(a.copy(took = (if (took) l + "${m.id}" else l - "${m.id}").joinToString(",")))
                if (took) scope.launch { c.log("pill", med = m.id) }
            } }
            // what isn't on the sill, in your own words (#11)
            l.filter { it.toLongOrNull() == null }.forEach { name -> Big(name, true) { save(a.copy(took = (l - name).joinToString(","))) } }
            var other by remember { mutableStateOf(false) }
            Big(tr("+ Другое", "+ Other"), false) { other = true }
            if (other) app.caregotchi.ui.components.TookOther({ other = false }, dark = true) { name -> save(a.copy(took = (l + name).joinToString(","))) }
            Big(tr("Ничего", "Nothing"), l.isEmpty() && a.relief == -1) { save(a.copy(took = "")) }
        }
        Lbl(tr("Похоже, из-за", "Maybe because of"))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Troubles.triggers.forEach { (sid, w) -> val l = a.list(a.triggers); Big(g(tr(w.first, w.second)), sid in l) { save(a.copy(triggers = (if (sid in l) l - sid else l + sid).joinToString(","))) } } }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color(0xFF2A1915)).clickable { nav.dark = 0 }.padding(16.dp), contentAlignment = Alignment.Center) {
                Text(tr("Свернуть", "Hide"), style = T.btn.copy(color = txt)) }
            Box(Modifier.weight(1.4f).clip(RoundedCornerShape(16.dp)).background(Color(0xFF3A1D18)).clickable { scope.launch { c.endAttack(a) }; nav.dark = 0 }.padding(16.dp), contentAlignment = Alignment.Center) {
                Text(tr("Отпустило", "It eased"), style = T.btn.copy(color = Color(0xFFE8B0A5))) }
        }
    }
}

/** News kept for a window: a level, an egg, a hatching, a new stage, a badge, a blanket over a missed day, a chest. */
@Composable
fun Inbox(d: Data) {
    val c = LocalContext.current.container
    val scope = c.scope
    val key = d.s.inbox.firstOrNull { !it.startsWith("quest:") } ?: return
    val close: () -> Unit = { scope.launch { c.seen(key) } }
    val parts = key.split(':')
    when (parts[0]) {
        "level" -> Popup(close) {
            d.pet?.let { Art("pet_${it.species}_6", Modifier.size(110.dp)) }
            Text(tr("Уровень ${parts[1]}!", "Level ${parts[1]}!"), style = T.title)
            Text(tr("+50 листиков. В лавке могли открыться новые вещи.", "+50 leaves. New things may be open in the shop."), style = T.small, textAlign = TextAlign.Center)
            val opened = Shop.cozy.filter { it.level == parts[1].toIntOrNull() }
            if (opened.isNotEmpty()) Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { opened.forEach { Reward("item_${it.id}", it.title) } }
            Btn(tr("Ура", "Hooray"), Modifier.fillMaxWidth().padding(top = 14.dp), onClick = close)
        }
        "egg" -> Popup(close) {
            val sp = Zoo.of(parts[1])
            Art("egg_${sp.id}", Modifier.size(110.dp))
            Text(tr("Новое яйцо!", "A new egg!"), style = T.title)
            Text(tr("За «${sp.how}». Два хороших дня – и оно вылупится.", "For «${sp.how}». Two good days and it hatches."), style = T.small, textAlign = TextAlign.Center)
            Btn(tr("В гнездо", "To the nest"), Modifier.fillMaxWidth().padding(top = 14.dp), onClick = close)
        }
        "hatch" -> Popup(close) {
            val p = d.pets.firstOrNull { it.id == parts[1].toLongOrNull() }
            if (p != null) { val t = Zoo.of(p.species).title; Art("pet_${p.species}_0", Modifier.size(120.dp))
                Text(tr((if (t.endsWith("а")) "Вылупилась " else "Вылупился ") + t.lowercase() + "!", "A ${t.lowercase()} hatched!"), style = T.title, textAlign = TextAlign.Center)
                Text(tr("Имя – ${p.name}. Поселить в дупле можно в «Жильцах».", "Its name is ${p.name}. You can move it into the hollow in «Pets»."), style = T.small, textAlign = TextAlign.Center) }
            Btn(tr("Привет!", "Hello!"), Modifier.fillMaxWidth().padding(top = 14.dp), onClick = close)
        }
        "stage" -> Popup(close) {
            val p = d.pets.firstOrNull { it.id == parts[1].toLongOrNull() }
            val st = parts.getOrNull(2)?.toIntOrNull() ?: 1
            if (p != null) Art("pet_${p.species}_${minOf(st, 2)}", Modifier.size(120.dp))
            Text(tr("Новая ступень!", "A new stage!"), style = T.title)
            Text(tr("${p?.name ?: ""} – теперь ${Stage.name(st).lowercase()}.", "${p?.name ?: ""} is now ${Stage.name(st).lowercase()}."), style = T.small)
            Btn(tr("Как вырос!", "So big!"), Modifier.fillMaxWidth().padding(top = 14.dp), onClick = close)
        }
        "badge" -> Popup(close) {
            val b = Badges.all.getOrNull(parts[1].toIntOrNull() ?: 0) ?: Badges.all[0]
            Art("badge_${b.id}", Modifier.size(100.dp))
            Text(tr("Новая нашивка", "A new badge"), style = T.title)
            Text("«${b.title}» – ${b.why}", style = T.small, textAlign = TextAlign.Center)
            Btn(tr("Пришить на одеяло", "Sew it on"), Modifier.fillMaxWidth().padding(top = 14.dp), onClick = close)
        }
        "plaid" -> Popup(close) {
            Art("icon_plaid", Modifier.size(90.dp))
            Text(tr("Плед спас серию", "A blanket saved the streak"), style = T.title, textAlign = TextAlign.Center)
            Text(tr("${fmtDay(parts[1].toLongOrNull() ?: today())} не было отметок – этот день укрыт пледом. Пледов осталось: ${d.s.plaids}.",
                "${fmtDay(parts[1].toLongOrNull() ?: today())} had no marks – I covered it with a blanket. Blankets left: ${d.s.plaids}."), style = T.small, textAlign = TextAlign.Center)
            Btn(tr("Спасибо", "Thanks"), Modifier.fillMaxWidth().padding(top = 14.dp), onClick = close)
        }
        "loot" -> LootPopup(Loot(parts.getOrNull(1)?.toIntOrNull() ?: 0, parts.getOrNull(2)?.takeIf { it.isNotEmpty() }, parts.getOrNull(3) == "true"), close)
        else -> LaunchedEffect(key) { c.seen(key) }
    }
}


/** A blood pressure reading: three big number fields filled with the last values, and the reading before to compare. */
@Composable
fun BpSheet(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val scope = c.scope
    val close = { nav.sheet = null }
    val last = d.marks.filter { it.kind == "bp" }.maxByOrNull { it.at }
    var sys by remember { mutableStateOf("${last?.sys ?: 120}") }
    var dia by remember { mutableStateOf("${last?.dia ?: 80}") }
    var pulse by remember { mutableStateOf(last?.pulse?.takeIf { it > 0 }?.let { "$it" } ?: "") }
    val ok = (sys.toIntOrNull() ?: 0) in 60..260 && (dia.toIntOrNull() ?: 0) in 30..180 && (pulse.isEmpty() || (pulse.toIntOrNull() ?: 0) in 30..220)
    Sheet(close) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Art("icon_tonometer", Modifier.size(56.dp)); Spacer(Modifier.width(10.dp))
            Column {
                Text(tr("Давление", "Blood pressure"), style = T.h2)
                Text(if (last != null) tr("В прошлый раз ${last.sys}/${last.dia}", "Last time ${last.sys}/${last.dia}") + (if (last.pulse > 0) tr(", пульс ${last.pulse}", ", pulse ${last.pulse}") else "") +
                    " · " + app.caregotchi.domain.relDay(app.caregotchi.domain.dayOf(last.at)) + " " + hhmm(minuteOf(last.at)) else tr("Первое измерение", "The first reading"), style = T.small)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BpField(tr("Верхнее", "Systolic"), sys, Modifier.weight(1f)) { sys = it }
            BpField(tr("Нижнее", "Diastolic"), dia, Modifier.weight(1f)) { dia = it }
            BpField(tr("Пульс", "Pulse"), pulse, Modifier.weight(1f)) { pulse = it }
        }
        Text(tr("Пульс можно не заполнять.", "The pulse is optional."), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
        Btn(tr("Записать", "Save"), Modifier.fillMaxWidth().padding(top = 14.dp), enabled = ok, icon = "icon_tonometer") {
            scope.launch { c.log("bp", sys = sys.toInt(), dia = dia.toInt(), pulse = pulse.toIntOrNull() ?: 0) }; close()
        }
        Ghost(tr("Мне плохо – записать приступ", "I feel bad – write down a spell"), Modifier.fillMaxWidth().padding(top = 8.dp), color = C.bad) {
            scope.launch { val id = c.startAttack("pressure"); close(); nav.push(Screen.AttackEdit(id)) }
        }
    }
}

@Composable
internal fun BpField(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(C.card2).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = T.tiny)
        androidx.compose.foundation.text.BasicTextField(value, { v -> onChange(v.filter { it.isDigit() }.take(3)) }, singleLine = true,
            textStyle = T.title.copy(textAlign = TextAlign.Center, color = C.ink),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(C.acc), modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp))
    }
}

/** Drinks with caffeine and their usual milligrams. */
val DRINKS = listOf(Triple("espresso", "Эспрессо" to "Espresso", 65), Triple("coffee", "Кофе" to "Coffee", 95), Triple("latte", "Латте" to "Latte", 75),
    Triple("cappuccino", "Капучино" to "Cappuccino", 75), Triple("tea", "Чёрный чай" to "Black tea", 45), Triple("green", "Зелёный чай" to "Green tea", 30),
    Triple("energy", "Энергетик" to "Energy drink", 80), Triple("cola", "Кола" to "Cola", 35))
fun drinkName(id: String) = DRINKS.firstOrNull { it.first == id }?.second?.let { tr(it.first, it.second) } ?: id

/** A cup of something with caffeine: pick the drink, maybe a double, and see the day's milligrams against 400. */
@Composable
fun CafSheet(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val scope = c.scope
    val close = { nav.sheet = null }
    var double by remember { mutableStateOf(false) }
    val today = d.book.todayCare.caffeine
    val cups = d.book.marksOn(today()).filter { it.kind == "caf" }.sortedBy { it.at }
    Sheet(close) {
        Text(tr("Кофеин", "Caffeine"), style = T.h2)
        Text(tr("Сегодня $today мг из 400 – это примерно предел для взрослого.", "$today mg of 400 today – about an adult's limit."), style = T.small)
        Thread((today / 400f).coerceAtMost(1f), Modifier.padding(top = 8.dp), color = if (today > 400) C.bad else Color(0xFF8A5A2E))
        if (cups.isNotEmpty()) Text(cups.joinToString(" · ") { hhmm(minuteOf(it.at)) + " " + drinkName(it.sub).lowercase() }, style = T.tiny, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(12.dp))
        PicGrid(DRINKS, 4, gap = 6.dp) { (id, w, mg), mod ->
            PicTile("drink_$id", tr(w.first, w.second) + "\n" + tr("${if (double) mg * 2 else mg} мг", "${if (double) mg * 2 else mg} mg"), false, mod, pic = 42.dp) {
                scope.launch { c.log("caf", sub = id, mins = if (double) mg * 2 else mg) }; close()
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(14.dp)).clickable { double = !double }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Switch(double, { double = it }, colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = C.acc, checkedThumbColor = C.accInk, uncheckedTrackColor = C.card2, uncheckedBorderColor = C.line))
            Spacer(Modifier.width(10.dp)); Text(tr("Двойная порция или большая кружка", "A double or a big mug"), style = T.bodyB)
        }
        if (minuteOf(System.currentTimeMillis()) >= 16 * 60) Text(tr("После четырёх кофеин может помешать сну – питомец это заметит.", "After four, caffeine may get in the way of sleep – the pet will notice."), style = T.tiny, modifier = Modifier.padding(top = 4.dp))
    }
}
