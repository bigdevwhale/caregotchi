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
                Box(Modifier.width(98.dp).clickable { nav.push(Screen.MedEdit(m.id)) }, contentAlignment = Alignment.BottomCenter) {
                    Art("pot_${m.herb}_$st", Modifier.padding(bottom = 10.dp).size(88.dp))
                    val run = d.book.medRun(m)
                    Text(m.name.take(14) + if (!m.asNeeded && run > 0) " · $run" else "", style = T.tiny.copy(color = Color(0xFF3B2E26)), maxLines = 1,
                        modifier = Modifier.offset(y = 8.dp).rotate(-2f).clip(RoundedCornerShape(4.dp)).background(C.paper).border(1.dp, Color(0xFFD9C7A6), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp))
                    if (st == 3) Text(tr("вянет", "wilting"), style = T.tiny.copy(color = Color.White), modifier = Modifier.align(Alignment.TopEnd).clip(RoundedCornerShape(50)).background(C.rose).padding(horizontal = 6.dp))
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
    val times = d.activeMeds.filter { !it.asNeeded }.flatMap { it.timeList() }.distinct().sorted().take(4)
    var due = 0; var took = 0
    val lid = Brush.verticalGradient(listOf(Color(0xFFC9A8DC), Color(0xFFB38FCA)))
    Column(Modifier.padding(top = 18.dp).fillMaxWidth().shadow(3.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp))
        .background(Brush.verticalGradient(listOf(Color(0xFFE7D3F0), Color(0xFFD6BFE3)))).padding(10.dp)) {
        val rows = times.map { t ->
            t to (0..6).map { i ->
                val day = monday + i
                val ms = d.activeMeds.filter { m -> !m.asNeeded && t in m.timeList() && day >= m.from && (m.until == 0L || day <= m.until) }
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
    var until by remember { mutableLongStateOf(m0.until) }
    var herb by remember { mutableStateOf(m0.herb) }
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
            Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) { ClockDial(times, Modifier.size(220.dp)) { times = it } }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                listOf(8 * 60, 13 * 60, 20 * 60, 22 * 60).forEach { t -> Chip(hhmm(t), t in times) { times = if (t in times) times - t else (times + t).sorted() } }
            }
            Head("icon_calendar", tr("Курс", "Course"), if (until > 0) tr("до ${fmtDay(until)} включительно", "until ${fmtDay(until)}") else tr("без конца", "no end"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 7, 14, 30, 60, 90).forEach { n -> Chip(if (n == 0) tr("Без конца", "No end") else tr("$n дн.", "$n d"), if (n == 0) until == 0L else until == today() + n - 1) { until = if (n == 0) 0 else today() + n - 1 } }
            }
        }
        Head("icon_headache", tr("Это обезболивающее?", "Is it a painkiller?"), tr("для счётчика: частые обезболивающие сами могут вызывать головную боль", "for the count: painkillers taken too often can cause headaches"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Triple(0, tr("Нет", "No"), "icon_pill"), Triple(1, tr("Обычное", "Simple"), "icon_compress"), Triple(2, tr("Триптан", "Triptan"), "icon_migraine")).forEach { (v, l, ic) ->
                PicTile(ic, l, pain == v, Modifier.weight(1f), pic = 40.dp) { pain = v } }
        }
        Head("icon_bottle", tr("Запас", "Stock"), if (stock > 0 && !asNeeded && times.isNotEmpty()) tr("хватит на ${stock / (times.size * per).coerceAtLeast(1)} дн.", "lasts ${stock / (times.size * per).coerceAtLeast(1)} d") else null)
        Stepper(tr("Таблеток в пачке", "Pills left"), if (stock == 0) tr("не считать", "not counted") else "$stock", { stock = (stock - (if (stock > 20) 5 else 1)).coerceAtLeast(0) }, { stock += if (stock >= 20) 5 else 1 })
        Stepper(tr("За один приём", "Per dose"), "$per", { per = (per - 1).coerceAtLeast(1) }, { per = (per + 1).coerceAtMost(6) })
        Head("icon_seedling", tr("Горшок на подоконнике", "Pot on the windowsill"))
        PicGrid(HERBS, 6, gap = 4.dp) { h, mod -> PicTile("pot_${h}_2", herbName(h), h == herb, mod, pic = 40.dp) { herb = h } }
        Btn(tr("Сохранить", "Save"), Modifier.fillMaxWidth().padding(top = 20.dp), enabled = name.isNotBlank() && (asNeeded || times.isNotEmpty()), icon = "icon_seedling") {
            scope.launch {
                c.saveMed(m0.copy(name = name.trim(), dose = dose.trim(), times = if (asNeeded) "" else times.joinToString(","), pain = pain, stock = stock, perDose = per,
                    until = if (asNeeded) 0 else until, herb = herb, from = if (id == 0L) today() else m0.from))
                nav.pop()
            }
        }
        if (id != 0L) Ghost(tr("Больше не принимаю", "I no longer take it"), Modifier.fillMaxWidth().padding(top = 10.dp), color = C.bad) {
            scope.launch { c.saveMed(m0.copy(archived = true)); nav.pop() }
        }
        Text(tr("Приложение не даёт медицинских советов – дозы и схемы назначает врач.", "The app gives no medical advice – your doctor sets doses and schedules."), style = T.tiny, modifier = Modifier.padding(top = 14.dp))
    }
}
