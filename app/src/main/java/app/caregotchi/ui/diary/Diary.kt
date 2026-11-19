package app.caregotchi.ui.diary

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.caregotchi.container
import app.caregotchi.data.Attack
import app.caregotchi.domain.Day
import app.caregotchi.domain.Detective
import app.caregotchi.domain.Letter
import app.caregotchi.domain.Troubles
import app.caregotchi.domain.dayShort
import app.caregotchi.domain.dur
import app.caregotchi.domain.fmt1
import app.caregotchi.domain.fmtDay
import app.caregotchi.domain.g
import app.caregotchi.domain.hhmm
import app.caregotchi.domain.minuteOf
import app.caregotchi.domain.monthName
import app.caregotchi.domain.startOf
import app.caregotchi.domain.today
import app.caregotchi.domain.tr
import app.caregotchi.domain.weekStart
import app.caregotchi.domain.weekday
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
import app.caregotchi.ui.components.Segments
import app.caregotchi.ui.components.Stepper
import app.caregotchi.ui.components.Head
import app.caregotchi.ui.components.HeadPicker
import app.caregotchi.ui.components.Paper
import app.caregotchi.ui.components.PaperField
import app.caregotchi.ui.components.PicGrid
import app.caregotchi.ui.components.PicTile
import app.caregotchi.ui.components.StrengthBar
import app.caregotchi.ui.components.Token
import app.caregotchi.ui.home.markText
import app.caregotchi.ui.theme.C
import app.caregotchi.ui.theme.T
import kotlinx.coroutines.launch
import java.time.LocalDate

object DiaryTab { var tab by mutableIntStateOf(0) }

@Composable
fun DiaryScreen(d: Data, nav: Nav) {
    val now = LocalDate.ofEpochDay(today())
    val spells = d.attacks.count { LocalDate.ofEpochDay(app.caregotchi.domain.dayOf(it.start)).let { x -> x.month == now.month && x.year == now.year } }
    val good = d.book.days.count { LocalDate.ofEpochDay(it.day).let { x -> x.month == now.month && x.year == now.year } && it.good }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Banner("banner_diary", tr("Дневник", "Diary"), tr("${monthName(now.monthValue).replaceFirstChar { it.uppercase() }} · приступов $spells · хороших дней $good",
            "${monthName(now.monthValue)} · $spells spells · $good good days"))
        Column(Modifier.padding(horizontal = 14.dp)) {
            Spacer(Modifier.height(6.dp))
            Segments(listOf(tr("Одеяло", "Quilt"), tr("Сыщик", "Detective"), tr("Письмо", "Letter")), DiaryTab.tab) { DiaryTab.tab = it }
            Spacer(Modifier.height(10.dp))
            when (DiaryTab.tab) { 0 -> QuiltPane(d, nav); 1 -> Board(d, nav); else -> Letters(d) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

fun icon(kind: String) = when (kind) { "eat" -> "icon_eat"; "water" -> "icon_water"; "pill" -> "icon_pill"; "move" -> "icon_walk"; "sleep" -> "icon_sleep"; "wake" -> "icon_sunrise"; else -> "icon_star" }

@Composable
private fun Line(time: String, icon: String, text: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(time, style = T.smallB.copy(color = C.mute), modifier = Modifier.width(46.dp))
        Art(icon, Modifier.size(24.dp)); Spacer(Modifier.width(8.dp))
        Text(text, style = T.body, modifier = Modifier.weight(1f))
    }
}

/** The detective's cork board: clue notes to keep or strike, the doctor's report, the latest spells. */
@Composable
private fun Board(d: Data, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val scope = rememberCoroutineScope()
    val clues = remember(d.book) { Detective.clues(d.book) }.filter { (d.s.clues[it.id] ?: 0) >= 0 }
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(5.dp, C.wood, RoundedCornerShape(12.dp))) {
        Art("banner_detective", Modifier.matchParentSize(), ContentScale.Crop)
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 14.dp)) {
            if (clues.isEmpty()) Note(tr("Пока улик нет. Записывайте приступы и заботу – после трёх приступов сыщик начнёт искать связи: со сном, едой, водой, движением, лекарствами и погодой.",
                "No clues yet. Write down your spells and your care – after three spells the detective starts looking for links: sleep, food, water, moving, medicine and weather."), 0)
            clues.take(5).forEachIndexed { i, cl ->
                val kept = (d.s.clues[cl.id] ?: 0) > 0
                Note(cl.text, i, kept) {
                    if (!kept) Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Ghost(tr("Похоже на правду", "Sounds right"), small = true) { scope.launch { c.clue(cl.id, 1) } }
                        Ghost(tr("Зачеркнуть", "Strike out"), small = true) { scope.launch { c.clue(cl.id, -1) } }
                    } else Text(tr("В деле ✓", "In the file ✓"), style = T.tiny.copy(color = Color(0xFF4C6A46)), modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
    Btn(tr("Отчёт для врача (PDF)", "Report for the doctor (PDF)"), Modifier.fillMaxWidth().padding(top = 12.dp), icon = "icon_doctor") { scope.launch { Report.share(ctx, d) } }
    Section(tr("Последние приступы", "Latest spells"), aside = tr("всего ${d.attacks.size}", "${d.attacks.size} in all"))
    if (d.attacks.isEmpty()) Text(tr("Ни одного – и хорошо.", "None – good."), style = T.small)
    d.attacks.take(6).forEach { a ->
        Line(fmtDay(app.caregotchi.domain.dayOf(a.start)), "icon_heart", Troubles.title(a.kind) + (if (a.strength > 0) " · ${a.strength}/10" else "") +
            (if (a.end > 0) " · " + dur(((a.end - a.start) / 1000).toInt()) else "") + when (a.relief) { 3 -> tr(" · помогло", " · helped"); 2 -> tr(" · немного", " · a little"); 1 -> tr(" · не помогло", " · didn't help"); else -> "" }) { nav.push(Screen.AttackEdit(a.id)) }
    }
}

@Composable
private fun Note(text: String, i: Int, kept: Boolean = false, extra: @Composable () -> Unit = {}) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 7.dp).rotate(if (i % 2 == 0) -1.2f else 1f)) {
        Column(Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(3.dp)).clip(RoundedCornerShape(3.dp)).background(if (kept) Color(0xFFEEF4E8) else C.paper).padding(12.dp)) {
            Text(text, style = T.note); extra()
        }
        Box(Modifier.align(Alignment.TopCenter).offset(y = (-6).dp).size(13.dp).shadow(2.dp, CircleShape).clip(CircleShape).background(Color(0xFFC0392B)))
    }
}

/** A bad spell in full, to change afterwards: what it was, when, how strong, where, what helped. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttackPage(id: Long, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val a0 = d.attacks.firstOrNull { it.id == id } ?: return
    var a by remember(id) { mutableStateOf(a0) }
    fun upd(x: Attack) { a = x; scope.launch { c.saveAttack(x) } }
    val kinds = Troubles.all.filter { it.id != "period" || d.s.cycle } + d.s.kinds.map { Troubles.of(it) }
    Page(Troubles.title(a.kind) + " · " + fmtDay(app.caregotchi.domain.dayOf(a.start)), { nav.pop() }) {
        Head("icon_${Troubles.of(a.kind).icon}", tr("Что это было", "What it was"))
        PicGrid(kinds, 4, gap = 6.dp) { k, mod -> PicTile("icon_${k.icon}", k.title, a.kind == k.id, mod, pic = 40.dp) { upd(a.copy(kind = k.id)) } }
        Head("icon_alarm", tr("Когда", "When"), if (a.end > 0) tr("длился ", "lasted ") + dur(((a.end - a.start) / 1000).toInt()) else tr("ещё идёт", "still going"))
        Stepper(tr("Начало", "Start"), hhmm(minuteOf(a.start)), { upd(a.copy(start = a.start - 15 * 60_000L)) }, { upd(a.copy(start = a.start + 15 * 60_000L)) })
        if (a.end > 0) Stepper(tr("Конец", "End"), hhmm(minuteOf(a.end)) + if (app.caregotchi.domain.dayOf(a.end) != app.caregotchi.domain.dayOf(a.start)) "+1" else "",
            { upd(a.copy(end = (a.end - 15 * 60_000L).coerceAtLeast(a.start + 60_000))) }, { upd(a.copy(end = a.end + 15 * 60_000L)) })
        else Btn(tr("Отпустило сейчас", "It eased now"), small = true, color = C.sage, ink = Color.White) { scope.launch { c.endAttack(a); a = a.copy(end = System.currentTimeMillis()) } }
        Head("icon_thermo", tr("Сила", "Strength"))
        StrengthBar(a.strength) { upd(a.copy(strength = it)) }
        if (a.kind == "migraine" || a.kind == "headache") {
            Head("icon_headache", tr("Где болело", "Where it hurt"))
            HeadPicker(a.side) { upd(a.copy(side = it)) }
        }
        Head("icon_compress", tr("Что ещё было", "What else"))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Troubles.symptoms.forEach { (sid, w) -> val l = a.list(a.symptoms); Chip(tr(w.first, w.second), sid in l) { upd(a.copy(symptoms = (if (sid in l) l - sid else l + sid).joinToString(","))) } }
        }
        Head("icon_bottle", tr("Что принял{|а}", "What you took").let(::g))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            d.meds.filter { !it.archived || "${it.id}" in a.list(a.took) }.take(5).forEach { m -> val l = a.list(a.took)
                Token("pot_${m.herb}_2", m.name.take(10), "${m.id}" in l) { upd(a.copy(took = (if ("${m.id}" in l) l - "${m.id}" else l + "${m.id}").joinToString(","))) } }
        }
        if (a.took.isNotBlank()) {
            Head("icon_heart", tr("Помогло?", "Did it help?"))
            val sp = d.pet?.species ?: "fox"
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Triple(3, tr("Да", "Yes"), 6), Triple(2, tr("Немного", "A little"), 2), Triple(1, tr("Нет", "No"), 7)).forEach { (v, l, f) ->
                    PicTile("pet_${sp}_$f", l, a.relief == v, Modifier.weight(1f), pic = 58.dp) { upd(a.copy(relief = v)) } }
            }
        }
        Head("icon_detective", tr("Похоже, из-за", "Maybe because of"), tr("сыщик сравнит это с днями без приступов", "the detective compares it with good days"))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Troubles.triggers.forEach { (sid, w) -> val l = a.list(a.triggers); Chip(g(tr(w.first, w.second)), sid in l) { upd(a.copy(triggers = (if (sid in l) l - sid else l + sid).joinToString(","))) } } }
        Head("icon_letter", tr("Заметка", "Note"))
        Paper { PaperField(a.note, { upd(a.copy(note = it)) }, tr("Что ещё стоит запомнить", "Anything else worth remembering")) }
        Ghost(tr("Удалить запись", "Delete"), Modifier.fillMaxWidth().padding(top = 20.dp), color = C.bad) { scope.launch { c.deleteAttack(a); nav.pop() } }
    }
}

private fun lerpRed(v: Int) = app.caregotchi.ui.components.lerp(Color(0xFFD9A441), Color(0xFFA33B2C), (v - 1) / 9f)

