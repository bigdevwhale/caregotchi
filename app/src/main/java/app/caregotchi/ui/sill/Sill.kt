package app.caregotchi.ui.sill

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.caregotchi.container
import app.caregotchi.data.Med
import app.caregotchi.domain.dayShort
import app.caregotchi.domain.fmtDay
import app.caregotchi.domain.halves
import app.caregotchi.domain.fmtDayShort
import app.caregotchi.domain.Plan
import app.caregotchi.ui.home.MEALS
import app.caregotchi.ui.home.word
import app.caregotchi.domain.g
import app.caregotchi.domain.hhmm
import app.caregotchi.domain.plural
import app.caregotchi.domain.today
import app.caregotchi.domain.tr
import app.caregotchi.domain.weekStart
import app.caregotchi.ui.Data
import app.caregotchi.ui.Nav
import app.caregotchi.ui.Screen
import app.caregotchi.ui.components.Art
import app.caregotchi.ui.components.Banner
import app.caregotchi.ui.components.Btn
import app.caregotchi.ui.components.Chip
import app.caregotchi.ui.components.Field
import app.caregotchi.ui.components.Ghost
import app.caregotchi.ui.components.Page
import app.caregotchi.ui.components.Section
import app.caregotchi.ui.components.Stepper
import app.caregotchi.ui.components.ClockDial
import app.caregotchi.ui.components.Head
import app.caregotchi.ui.components.Paper
import app.caregotchi.ui.components.PaperField
import app.caregotchi.ui.components.PicGrid
import app.caregotchi.ui.components.PicTile
import app.caregotchi.ui.components.Toggle
import app.caregotchi.ui.components.tap
import app.caregotchi.ui.theme.C
import app.caregotchi.ui.theme.T
import kotlinx.coroutines.launch

val HERBS = listOf("mint", "chamomile", "lavender", "rosemary", "sage", "aloe")
fun herbName(h: String) = when (h) { "mint" -> tr("Мята", "Mint"); "chamomile" -> tr("Ромашка", "Chamomile"); "lavender" -> tr("Лаванда", "Lavender")
    "rosemary" -> tr("Розмарин", "Rosemary"); "sage" -> tr("Шалфей", "Sage"); else -> tr("Алоэ", "Aloe") }

@Composable
fun SillScreen(d: Data, nav: Nav) {
    val meds = d.activeMeds.sortedBy { it.asNeeded }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Banner("banner_sill", tr("Подоконник", "Windowsill"), tr("Лекарство – горшок. Вовремя – цветёт.", "Each medicine is a pot. On time, it blooms."))
        Column(Modifier.padding(horizontal = 14.dp)) {
            Spacer(Modifier.height(8.dp))
            Plank(d, meds, nav)
            if (meds.any { !it.asNeeded }) Pillbox(d)
            Slips(d, nav)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Plank(d: Data, meds: List<Med>, nav: Nav) {
    Box(Modifier.fillMaxWidth().height(138.dp)) {
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(24.dp).shadow(3.dp, RoundedCornerShape(4.dp)).clip(RoundedCornerShape(4.dp))
            .background(Brush.verticalGradient(listOf(C.woodLight, C.wood))))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 16.dp).align(Alignment.BottomStart), verticalAlignment = Alignment.Bottom) {
            for (m in meds) {
                val st = d.book.potStage(m)
                val later = m.from > today() && !m.asNeeded
                Box(Modifier.width(98.dp).clickable { nav.push(Screen.MedEdit(m.id)) }, contentAlignment = Alignment.BottomCenter) {
                    Art("pot_${m.herb}_$st", Modifier.padding(bottom = 10.dp).size(88.dp), alpha = if (later) .45f else 1f)
                    if (later) Text(tr("с ${fmtDayShort(m.from)}", "from ${fmtDayShort(m.from)}"), style = T.tiny.copy(color = Color.White), modifier = Modifier.align(Alignment.TopEnd).clip(RoundedCornerShape(50)).background(C.sage).padding(horizontal = 6.dp))
                    val run = d.book.medRun(m)
                    Text(m.name.take(14) + if (!m.asNeeded && run > 0) " · $run" else "", style = T.tiny.copy(color = Color(0xFF3B2E26)), maxLines = 1,
                        modifier = Modifier.offset(y = 8.dp).rotate(-2f).clip(RoundedCornerShape(4.dp)).background(C.paper).border(1.dp, Color(0xFFD9C7A6), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp))
                    if (later) {} else if (st == 3) Text(tr("вянет", "wilting"), style = T.tiny.copy(color = Color.White), modifier = Modifier.align(Alignment.TopEnd).clip(RoundedCornerShape(50)).background(C.rose).padding(horizontal = 6.dp))
                    else if (st == 2) Text(tr("цветёт", "blooms"), style = T.tiny.copy(color = Color.White), modifier = Modifier.align(Alignment.TopEnd).clip(RoundedCornerShape(50)).background(C.honey).padding(horizontal = 6.dp))
                }
            }
            Box(Modifier.width(90.dp).clickable { nav.push(Screen.MedEdit(0)) }, contentAlignment = Alignment.BottomCenter) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Art("icon_seedling", Modifier.size(54.dp))
                    Text(tr("+ Лекарство", "+ Medicine"), style = T.smallB.copy(color = Color(0xFF3B2E26)), modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(C.paper).padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }
    }
    if (meds.isEmpty()) Text(tr("Добавьте лекарства – каждое вырастет здесь горшком. Питомец будет напоминать о них и радоваться каждой таблетке.",
        "Add your medicine – each grows here as a pot. The pet will remind you and cheer every pill."), style = T.small, modifier = Modifier.padding(top = 10.dp))
}

/** The week's pill box: a cell a scheduled time a day; open cells were taken. Today's cells can be tapped. */
@Composable
private fun Pillbox(d: Data) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val monday = weekStart(today())
    val times = d.activeMeds.filter { !it.asNeeded && it.from <= monday + 6 }.flatMap { m -> (0..6).flatMap { m.timesOn(monday + it) } }.distinct().sorted().take(4)
    var due = 0; var took = 0
    val lid = Brush.verticalGradient(listOf(Color(0xFFC9A8DC), Color(0xFFB38FCA)))
    Column(Modifier.padding(top = 18.dp).fillMaxWidth().shadow(3.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp))
        .background(Brush.verticalGradient(listOf(Color(0xFFE7D3F0), Color(0xFFD6BFE3)))).padding(10.dp)) {
        val rows = times.map { t ->
            t to (0..6).map { i ->
                val day = monday + i
                val ms = d.activeMeds.filter { m -> !m.asNeeded && t in m.timesOn(day) && day >= m.from && (m.until == 0L || day <= m.until) && m.halvesOn(day) > 0 }
                val done = ms.count { m -> d.book.marksOn(day).any { it.kind == "pill" && it.med == m.id && it.slot == t } }
                if (day < today() || (day == today() && t <= d.book.nowMin)) { due += ms.size; took += done }
                Triple(day, ms, done)
            }
        }
        Row { Text(tr("Таблетница недели", "The week's pill box"), style = T.smallB.copy(color = Color(0xFF5B3F6E)), modifier = Modifier.weight(1f))
            Text(tr("$took из $due вовремя", "$took of $due"), style = T.tiny.copy(color = Color(0xFF5B3F6E))) }
        Row(Modifier.padding(top = 6.dp)) { Spacer(Modifier.width(40.dp)); (0..6).forEach { Text(dayShort(it), style = T.tiny.copy(color = Color(0xFF8B6C9E)), modifier = Modifier.weight(1f), textAlign = TextAlign.Center) } }
        for ((t, cells) in rows) Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(hhmm(t), style = T.tiny.copy(color = Color(0xFF8B6C9E)), modifier = Modifier.width(40.dp))
            for ((day, ms, done) in cells) {
                val all = ms.isNotEmpty() && done >= ms.size
                val past = day < today() || (day == today() && t + 60 < d.book.nowMin)
                val closed = !all && !past
                Box(Modifier.weight(1f).padding(2.dp).aspectRatio(1.4f).clip(RoundedCornerShape(7.dp))
                    .then(if (closed && ms.isNotEmpty()) Modifier.background(lid) else Modifier.background(if (!all && past && ms.isNotEmpty()) Color(0xFFF3D9D5) else Color(0xFFF6ECFA)))
                    .then(if (day == today()) Modifier.border(2.dp, C.honey, RoundedCornerShape(7.dp)) else Modifier)
                    .clickable(enabled = day == today() && !all && ms.isNotEmpty()) { scope.launch { ms.forEach { m -> if (d.book.marksOn(day).none { it.kind == "pill" && it.med == m.id && it.slot == t }) c.log("pill", med = m.id, slot = t) } } },
                    contentAlignment = Alignment.Center) {
                    when {
                        ms.isEmpty() -> {}
                        all -> Text("✓", style = T.smallB.copy(color = Color(0xFF6E9A62)))
                        closed -> Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp).width(12.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color(0x77FFFFFF)))
                        else -> Text("·", style = T.smallB.copy(color = Color(0xFFB0706A)))
                    }
                }
            }
        }
    }
}

@Composable
private fun Slips(d: Data, nav: Nav) {
    val slips = mutableListOf<Triple<String, String, String>>()
    for (m in d.activeMeds) {
        val left = d.book.daysLeft(m)
        if (left != null && left <= 7) slips += Triple("icon_shop", tr("Пора в аптеку", "Time for the pharmacy"), tr("«${m.name}» – осталось на $left ${plural(left.toLong(), "день", "дня", "дней")}.", "«${m.name}» – $left days left."))
        if (m.from - today() in 1..14 && !m.asNeeded) slips += Triple("icon_calendar", tr("Скоро начнём", "Starting soon"), tr("«${m.name}» – с ${fmtDay(m.from)}. Накануне напомню.", "«${m.name}» – from ${fmtDay(m.from)}. A reminder comes the evening before."))
        if (m.until > 0 && m.until - today() in 0..3) slips += Triple("icon_calendar", tr("Курс кончается", "The course ends"), tr("«${m.name}» – до ${fmtDay(m.until)}.", "«${m.name}» – until ${fmtDay(m.until)}."))
    }
    if (d.activeMeds.any { it.pain > 0 }) {
        val (n, line) = d.book.painDays()
        slips += Triple("icon_pill", tr("$n из $line", "$n of $line"), tr("дней с обезболивающими в этом месяце. Чаще – и голова может болеть уже от самих таблеток.", "days with painkillers this month. More often and the pills themselves can cause headaches."))
    }
    if (slips.isEmpty()) return
    Spacer(Modifier.height(14.dp))
    slips.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEachIndexed { i, (icon, title, text) ->
                Box(Modifier.weight(1f).rotate(if (i == 0) -.8f else .8f)) {
                    Column(Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(4.dp)).clip(RoundedCornerShape(4.dp)).background(C.paper).padding(10.dp)) {
                        Text(title, style = T.note.copy(fontSize = androidx.compose.ui.unit.TextUnit(15f, androidx.compose.ui.unit.TextUnitType.Sp)))
                        Text(text, style = T.small.copy(color = Color(0xFF5A4636)))
                    }
                    Art(icon, Modifier.align(Alignment.TopEnd).offset(6.dp, (-10).dp).size(30.dp))
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

/** The cycle as kept, in the user's order, with «@k» when the course's first day [from] was day k of it – so that today is day [todayIdx]. */
private fun stored(cycle: List<Int>, todayIdx: Int, from: Long): String {
    // the same dose every day has no cycle: nothing to count, and a running course would divide by zero (#21)
    if (from > today() || cycle.isEmpty()) return cycle.joinToString(",")
    val k = Math.floorMod(todayIdx - (today() - from), cycle.size.toLong()).toInt()
    return cycle.joinToString(",") + if (k != 0) ",@$k" else ""
}

/** A medicine as a prescription slip: the name and dose in ink, the times on a clock face, the course, the stock, the pot it grows in. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedPage(id: Long, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    if (id != 0L && d.meds.none { it.id == id }) return
    val m0 = d.meds.firstOrNull { it.id == id } ?: Med(name = "", times = "480", herb = HERBS[d.meds.size % HERBS.size], from = today())
    var name by remember { mutableStateOf(m0.name) }
    var dose by remember { mutableStateOf(m0.dose) }
    var times by remember { mutableStateOf(m0.timeList()) }
    var asNeeded by remember { mutableStateOf(m0.asNeeded && id != 0L) }
    var pain by remember { mutableIntStateOf(m0.pain) }
    var stock by remember { mutableIntStateOf(m0.stock) }
    var per by remember { mutableIntStateOf(m0.perDose) }
    // a dose that changes by the day of a repeating cycle, in halves of a pill (#9)
    var cycle by remember { mutableStateOf(m0.patternList().takeIf { it.size >= 2 } ?: emptyList()) }
    // which day of the cycle today is, for a course already running; the stored cycle counts from the first day
    var todayIdx by remember { mutableIntStateOf(if (cycle.isEmpty() || m0.from > today()) 0 else Math.floorMod(today() - m0.from + m0.cycleStart(), cycle.size.toLong()).toInt()) }
    var until by remember { mutableLongStateOf(m0.until) }
    var from by remember { mutableLongStateOf(if (id == 0L) today() else m0.from) }
    var herb by remember { mutableStateOf(m0.herb) }
    var food by remember { mutableIntStateOf(m0.food) }
    var gap by remember { mutableIntStateOf(m0.gap) }
    var meals by remember { mutableStateOf(m0.mealList()) }
    val mealTimes = d.s.mealTimes
    val linked = food > 0 && meals.isNotEmpty()
    val shown = if (linked) m0.copy(food = food, gap = gap, meals = meals.joinToString(",")).timesFor(mealTimes) else times
    // the course can be planned ahead while it hasn't begun
    val planning = id == 0L || m0.from > today()
    Page(if (id == 0L) tr("Новое лекарство", "New medicine") else m0.name, { nav.pop() }) {
        Row(verticalAlignment = Alignment.Bottom) {
            Art("pot_${herb}_${if (id == 0L) 0 else d.book.potStage(m0)}", Modifier.size(96.dp))
            Column(Modifier.padding(start = 10.dp, bottom = 8.dp)) {
                Text(herbName(herb), style = T.h3)
                Text(if (id != 0L && !m0.asNeeded) tr("без пропусков ${d.book.medRun(m0)} ${plural(d.book.medRun(m0).toLong(), "день", "дня", "дней")}", "${d.book.medRun(m0)} days without a miss")
                    else tr("вырастет на подоконнике", "will grow on the windowsill"), style = T.small)
            }
        }
        Paper(Modifier.padding(top = 10.dp), tilt = -.6f, stamp = "icon_rx") {
            Text(tr("Рецепт", "Prescription"), style = T.sec.copy(color = Color(0xFFB0706A)))
            PaperField(name, { name = it }, tr("Название", "Name"))
            PaperField(dose, { dose = it }, tr("Доза, например «25 мг»", "Dose, e.g. «25 mg»"))
        }
        Head("icon_alarm", tr("Когда принимать", "When to take it"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PicTile("icon_alarm", tr("По часам", "On a schedule"), !asNeeded, Modifier.weight(1f), pic = 40.dp) { asNeeded = false }
            PicTile("icon_migraine", tr("Когда нужно", "When needed"), asNeeded, Modifier.weight(1f), pic = 40.dp) { asNeeded = true }
        }
        if (!asNeeded) {
            Head("icon_eat", tr("Привязать к еде", "Tied to meals"), tr("для тех, кто пьёт до еды или после", "for medicine taken before or after food"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to tr("Нет", "No"), 1 to tr("До еды", "Before"), 2 to tr("Во время еды", "With"), 3 to tr("После еды", "After")).forEach { (v, l) ->
                    Chip(l, food == v) { food = v; if (v > 0 && meals.isEmpty()) meals = mealTimes.indices.toList() }
                }
            }
            if (food > 0) {
                if (food != 2) Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (food == 1) tr("за", "by") else tr("через", "after"), style = T.smallB)
                    listOf(15, 30, 60, 120).forEach { n -> Chip(if (n >= 60) tr("${n / 60} ч", "${n / 60} h") else tr("$n мин", "$n min"), gap == n, Modifier.weight(1f)) { gap = n } }
                }
                Text(tr("Какие приёмы пищи", "Which meals"), style = T.sec, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    mealTimes.forEachIndexed { i, t -> Chip(word(MEALS, Plan.names(mealTimes)[i]) + " " + hhmm(t), i in meals) { meals = if (i in meals) meals - i else (meals + i).sorted() } }
                }
                Text(if (linked) tr("Напомню в ${shown.joinToString(", ") { hhmm(it) }}", "Reminders at ${shown.joinToString(", ") { hhmm(it) }}") +
                    (if (food == 3) tr(" – а если поедите позже, напоминание сдвинется следом.", " – and if you eat later, the reminder follows.") else ".")
                    else tr("Выберите хотя бы один приём пищи.", "Pick at least one meal."), style = T.smallB, modifier = Modifier.padding(top = 8.dp))
                Text(tr("Своё время еды – в Настройках → Напоминания.", "Your own meal times are in Settings → Reminders."), style = T.tiny)
            } else {
                Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) { ClockDial(times, Modifier.size(220.dp)) { times = it } }
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                    listOf(8 * 60, 13 * 60, 20 * 60, 22 * 60).forEach { t -> Chip(hhmm(t), t in times) { times = if (t in times) times - t else (times + t).sorted() } }
                }
            }
            if (planning) {
                Head("icon_calendar", tr("Начать", "Start"), if (from > today()) tr("с ${fmtDay(from)} – до тех пор только напомню накануне", "from ${fmtDay(from)} – till then just a word the evening before") else tr("сегодня", "today"))
                val monday = weekStart(today()) + 7
                val firstNext = java.time.LocalDate.ofEpochDay(today()).withDayOfMonth(1).plusMonths(1).toEpochDay()
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(today() to tr("Сегодня", "Today"), today() + 1 to tr("Завтра", "Tomorrow"), monday to tr("С понедельника", "Next Monday"), firstNext to tr("С ${fmtDay(firstNext)}", "From ${fmtDay(firstNext)}")).distinctBy { it.first }.forEach { (dd, l) ->
                        Chip(l, from == dd) { if (until > 0) until += dd - from; from = dd }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(14.dp)).background(C.card2).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("‹", style = T.title.copy(color = if (from > today()) C.ink else C.line), modifier = Modifier.clickable(enabled = from > today()) { from--; if (until > 0) until-- }.padding(horizontal = 14.dp))
                    Text(fmtDay(from), style = T.bodyB, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("›", style = T.title, modifier = Modifier.clickable { from++; if (until > 0) until++ }.padding(horizontal = 14.dp))
                }
            }
            Head("icon_calendar", tr("Курс", "Course"), if (until > 0) tr("до ${fmtDay(until)} включительно", "until ${fmtDay(until)}") else tr("без конца", "no end"))
            // a running course counts from its first day, so «20 days» stays 20 days when it's opened later
            val start = if (planning) from else if (m0.until > 0) minOf(m0.from.takeIf { it > 0 } ?: today(), today()) else today()
            val presets = listOf(7, 14, 30, 60, 90)
            val len = if (until > 0) (until - start + 1).toInt() else 0
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(tr("Без конца", "No end"), until == 0L) { until = 0 }
                presets.forEach { n -> Chip(tr("$n дн.", "$n d"), len == n) { until = start + n - 1 } }
                Chip(tr("Своё", "Other"), until > 0 && len !in presets) { if (until == 0L) until = start + 19 else if (len in presets) until = start + len }
            }
            // any length: 20 days, 45 days (#9)
            if (until > 0) Stepper(tr("Дней в курсе", "Days in the course"), "$len",
                { until = maxOf(start, until - 1).coerceAtLeast(today()) }, { until += 1 })
        }
        Head("icon_headache", tr("Это обезболивающее?", "Is it a painkiller?"), tr("для счётчика: частые обезболивающие сами могут вызывать головную боль", "for the count: painkillers taken too often can cause headaches"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Triple(0, tr("Нет", "No"), "icon_pill"), Triple(1, tr("Обычное", "Simple"), "icon_compress"), Triple(2, tr("Триптан", "Triptan"), "icon_migraine")).forEach { (v, l, ic) ->
                PicTile(ic, l, pain == v, Modifier.weight(1f), pic = 40.dp) { pain = v } }
        }
        Head("icon_bottle", tr("Запас", "Stock"), if (stock > 0 && !asNeeded && shown.isNotEmpty()) Med.daysOf(Med(name = "", times = shown.joinToString(","), perDose = per, from = if (planning) from else m0.from, pattern = stored(cycle, todayIdx, if (planning) from else m0.from)), stock, today())
            ?.let { tr("хватит на $it дн.", "lasts $it d") } else null)
        Stepper(tr("Таблеток в пачке", "Pills left"), if (stock == 0) tr("не считать", "not counted") else halves(stock), { stock = (stock - (if (stock > 40) 10 else 2)).coerceAtLeast(0) }, { stock += if (stock >= 40) 10 else 2 })
        if (!asNeeded) {
            Head("icon_pill", tr("Сколько за приём", "How much a dose"), if (cycle.size >= 2) tr("по кругу, начиная с первого дня курса", "in turn, from the course's first day") else null)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PicTile("icon_pill", tr("Каждый день одинаково", "The same every day"), cycle.isEmpty(), Modifier.weight(1f), pic = 40.dp) { cycle = emptyList() }
                PicTile("icon_calendar", tr("По дням по-разному", "Different by day"), cycle.isNotEmpty(), Modifier.weight(1f), pic = 40.dp) { if (cycle.isEmpty()) { cycle = listOf(per, per, per); todayIdx = 0 } }
            }
        }
        if (asNeeded || cycle.isEmpty()) Stepper(tr("Таблеток за приём", "Pills a dose"), halves(per), { per = (per - 1).coerceAtLeast(1) }, { per = (per + 1).coerceAtMost(12) })
        else {
            val running = (if (planning) from else m0.from) <= today()
            cycle.forEachIndexed { i, h ->
                Stepper(tr("День ${i + 1}", "Day ${i + 1}") + if (running && i == todayIdx) tr(" · сегодня", " · today") else "", if (h == 0) tr("перерыв", "off") else halves(h),
                    { cycle = cycle.toMutableList().also { it[i] = (h - 1).coerceAtLeast(0) } }, { cycle = cycle.toMutableList().also { it[i] = (h + 1).coerceAtMost(12) } })
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (cycle.size < 14) Chip(tr("+ ещё день", "+ one more day"), false, Modifier.weight(1f)) { cycle = cycle + cycle.last() }
                if (cycle.size > 2) Chip(tr("− убрать день", "− drop a day"), false, Modifier.weight(1f)) { cycle = cycle.dropLast(1) }
            }
            if (todayIdx >= cycle.size) todayIdx = 0
            if (running) FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(tr("Сегодня –", "Today is"), style = T.smallB)
                cycle.indices.forEach { i -> Chip(tr("день ${i + 1}", "day ${i + 1}"), todayIdx == i) { todayIdx = i } }
            }
            Text(tr("Таблеток за приём, «½» – половинка. Потом всё повторяется с первого дня.", "Pills a dose, «½» is half a pill. Then it starts again from day one."), style = T.tiny, modifier = Modifier.padding(top = 6.dp))
        }
        Head("icon_seedling", tr("Горшок на подоконнике", "Pot on the windowsill"))
        PicGrid(HERBS, 3, gap = 8.dp) { h, mod -> PicTile("pot_${h}_2", herbName(h), h == herb, mod, pic = 48.dp) { herb = h } }
        Btn(tr("Сохранить", "Save"), Modifier.fillMaxWidth().padding(top = 20.dp), enabled = name.isNotBlank() && (asNeeded || (if (food > 0) linked else times.isNotEmpty())), icon = "icon_seedling") {
            scope.launch {
                c.saveMed(m0.copy(name = name.trim(), dose = dose.trim(), times = if (asNeeded) "" else shown.joinToString(","), pain = pain, stock = stock, perDose = per, pattern = if (asNeeded || cycle.isEmpty()) "" else stored(cycle, todayIdx, if (planning && !asNeeded) from else if (id == 0L) today() else m0.from),
                    until = if (asNeeded) 0 else until, herb = herb, from = if (planning && !asNeeded) from else if (id == 0L) today() else m0.from,
                    food = if (asNeeded) 0 else food, gap = gap, meals = if (asNeeded || food == 0) "" else meals.joinToString(",")))
                nav.pop()
            }
        }
        if (id != 0L) Ghost(tr("Больше не принимаю", "I no longer take it"), Modifier.fillMaxWidth().padding(top = 10.dp), color = C.bad) {
            scope.launch { c.saveMed(m0.copy(archived = true)); nav.pop() }
        }
        Text(tr("Приложение не даёт медицинских советов – дозы и схемы назначает врач.", "The app gives no medical advice – your doctor sets doses and schedules."), style = T.tiny, modifier = Modifier.padding(top = 14.dp))
    }
}
