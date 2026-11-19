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
    val scope = rememberCoroutineScope()
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
            val left = d.book.dosesLeft()
            if (left.isEmpty() && d.activeMeds.none { it.asNeeded }) Text(tr("Лекарств пока нет. Добавьте их на подоконнике.", "No medicine yet. Add it on the windowsill."), style = T.small)
            left.forEach { (m, slot) ->
                Row(Modifier.fillMaxWidth().clickable { scope.launch { c.log("pill", med = m.id, slot = slot) }; close() }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Art("pot_${m.herb}_1", Modifier.size(36.dp)); Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) { Text(m.name + if (m.dose.isNotBlank()) " · ${m.dose}" else "", style = T.bodyB); Text(hhmm(slot), style = T.small) }
                    Text(tr("Принял{|а}", "Taken").let(::g), style = T.smallB.copy(color = C.acc))
                }
            }
            d.activeMeds.filter { it.asNeeded }.forEach { m ->
                Row(Modifier.fillMaxWidth().clickable { scope.launch { c.log("pill", med = m.id) }; close() }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Art("pot_${m.herb}_1", Modifier.size(36.dp)); Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) { Text(m.name, style = T.bodyB); Text(tr("по необходимости", "as needed"), style = T.small) }
                    Text(tr("Принял{|а}", "Taken").let(::g), style = T.smallB.copy(color = C.acc))
                }
            }
            Ghost(tr("Добавить лекарство", "Add medicine"), Modifier.fillMaxWidth().padding(top = 10.dp)) { close(); nav.push(Screen.MedEdit(0)) }
        }
        sheet == "bad" -> Sheet(close) {
            Text(tr("Что случилось?", "What's wrong?"), style = T.h2)
            Text(tr("Запись – тоже забота. Питомец ничего не теряет.", "Writing it down is care too. The pet loses nothing."), style = T.small)
            Spacer(Modifier.height(12.dp))
            val kinds = Troubles.all.filter { it.id != "period" || d.s.cycle } + d.s.kinds.map { Troubles.of(it) }
            PicGrid(kinds, 3) { k, mod -> PicTile("icon_${k.icon}", k.title, false, mod, pic = 54.dp) {
                scope.launch { val id = c.startAttack(k.id); close(); if (k.dark) nav.dark = id else nav.push(Screen.AttackEdit(id)) } } }
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
    val scope = rememberCoroutineScope()
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
    val scope = rememberCoroutineScope()
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
                Text(markText(m, d), style = T.body, modifier = Modifier.weight(1f).then(if (m.kind == "eat") Modifier.clickable { nav.sheet = "rate:${m.id}" } else Modifier))
                Text(tr("убрать", "undo"), style = T.smallB.copy(color = C.bad), modifier = Modifier.clickable { scope.launch { c.undo(m) } }.padding(6.dp))
            }
        }
        if (k == "eat") Text(tr("Нажмите на еду, чтобы сказать, насколько она была полезной.", "Tap a meal to say how wholesome it was."), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
        Btn(tr("Открыть день в дневнике", "Open the day in the diary"), Modifier.fillMaxWidth().padding(top = 12.dp), color = C.card2, ink = C.ink) { close(); nav.push(Screen.DayView(today())) }
    }
}

fun markText(m: app.caregotchi.data.Mark, d: Data): String = when (m.kind) {
    "eat" -> word(MEALS, m.sub) + when (m.q) { 3 -> tr(" · полезно", " · wholesome"); 1 -> tr(" · фастфуд/сладкое", " · fast food/sweets"); 2 -> tr(" · обычно", " · ordinary"); else -> "" } +
        m.tags.split(',').filter { it.isNotBlank() }.joinToString("") { " · " + word(TAGS, it).lowercase() }
    "water" -> tr("Стакан воды", "A glass of water")
    "pill" -> (d.meds.firstOrNull { it.id == m.med }?.name ?: tr("Лекарство", "Medicine")) + if (m.slot >= 0) (if (m.late <= 60) tr(" · вовремя", " · on time") else tr(" · с опозданием", " · late")) else ""
    "move" -> word(MOVES, m.sub) + " · ${m.mins} " + tr("мин", "min")
    "sleep" -> tr("Отбой", "Went to bed")
    "wake" -> tr("Подъём", "Got up")
    else -> m.kind
}

/** The dark room: a bad spell written down almost blind, with big buttons in a dim red. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DarkRoom(id: Long, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
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
        StrengthBar(a.strength, dark = true) { save(a.copy(strength = it)) }
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
    val scope = rememberCoroutineScope()
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

