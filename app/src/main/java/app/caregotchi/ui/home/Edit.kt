package app.caregotchi.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.caregotchi.container
import app.caregotchi.data.Mark
import app.caregotchi.domain.PERIOD
import app.caregotchi.domain.Plan
import app.caregotchi.domain.Troubles
import app.caregotchi.domain.dayOf
import app.caregotchi.domain.fmtDay
import app.caregotchi.domain.g
import app.caregotchi.domain.hhmm
import app.caregotchi.domain.minuteOf
import app.caregotchi.domain.plural
import app.caregotchi.domain.startOf
import app.caregotchi.domain.today
import app.caregotchi.domain.tr
import app.caregotchi.ui.Data
import app.caregotchi.ui.Nav
import app.caregotchi.ui.Screen
import app.caregotchi.ui.components.Art
import app.caregotchi.ui.components.Btn
import app.caregotchi.ui.components.Chip
import app.caregotchi.ui.components.Ghost
import app.caregotchi.ui.components.PicGrid
import app.caregotchi.ui.components.PicTile
import app.caregotchi.ui.components.Sheet
import app.caregotchi.ui.components.Token
import app.caregotchi.ui.components.WhenPicker
import app.caregotchi.ui.diary.markIcon
import app.caregotchi.ui.theme.C
import app.caregotchi.ui.theme.T
import kotlinx.coroutines.launch
import kotlin.math.abs

private fun kindTitle(kind: String) = when (kind) {
    "eat" -> tr("Еда", "A meal"); "water" -> tr("Вода", "Water"); "pill" -> tr("Лекарство", "Medicine"); "move" -> tr("Движение", "Moving")
    "caf" -> tr("Кофеин", "Caffeine"); "bp" -> tr("Давление", "Blood pressure"); "sleep" -> tr("Отбой", "Went to bed"); "wake" -> tr("Подъём", "Got up"); else -> kind
}

/** The scheduled time of [m]'s that fits [min] best (within three hours), or -1 for a dose outside the schedule. */
private fun slotFor(m: app.caregotchi.data.Med, min: Int): Int =
    m.timeList().minByOrNull { abs(it - min) }?.takeIf { abs(it - min) <= 180 } ?: -1

/** A mark put right: when it was, and what exactly – the meal, the sport, the drink, the numbers, the medicine. Or taken away. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarkSheet(id: Long, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val scope = c.scope
    val close = { nav.sheet = null }
    val m0 = d.marks.firstOrNull { it.id == id } ?: return
    var m by remember(id) { mutableStateOf(m0) }
    Sheet(close) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Art(markIcon(m), Modifier.size(52.dp)); Spacer(Modifier.width(10.dp))
            Column {
                Text(kindTitle(m.kind), style = T.h2)
                Text(tr("Как было на самом деле: время и что именно.", "As it really was: the time and what exactly."), style = T.small)
            }
        }
        Text(tr("Когда", "When"), style = T.sec, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
        WhenPicker(m.at) { at -> m = m.copy(at = at, slot = if (m.kind == "pill" && m.slot >= 0) d.meds.firstOrNull { it.id == m.med }?.let { slotFor(it, minuteOf(at)) } ?: m.slot else m.slot) }
        when (m.kind) {
            "eat" -> {
                Spacer(Modifier.height(12.dp))
                PicGrid(MEALS, 4) { (sid, w), mod -> PicTile("icon_$sid", tr(w.first, w.second), m.sub == sid, mod, pic = 40.dp) { m = m.copy(sub = sid) } }
                Text(tr("Насколько полезно", "How wholesome"), style = T.sec, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                PicGrid(QUALITY, 3) { (v, w), mod -> PicTile(qualityIcon(v), tr(w.first, w.second), m.q == v, mod, pic = 44.dp) { m = m.copy(q = if (m.q == v) 0 else v) } }
                val tags = m.tags.split(',').filter { it.isNotBlank() }.toSet()
                FlowRow(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TAGS.forEach { (sid, w) -> Token("icon_$sid", tr(w.first, w.second), sid in tags) { m = m.copy(tags = (if (sid in tags) tags - sid else tags + sid).joinToString(",")) } }
                }
            }
            "move" -> {
                Spacer(Modifier.height(12.dp))
                PicGrid(MOVES, 3, gap = 6.dp) { (sid, w), mod -> PicTile(moveIcon(sid), tr(w.first, w.second), m.sub == sid, mod, pic = 36.dp) { m = m.copy(sub = sid) } }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("${m.mins} мин", "${m.mins} min"), style = T.title, modifier = Modifier.weight(1f))
                    Stepper2({ m = m.copy(mins = (m.mins - 5).coerceAtLeast(5)) }, { m = m.copy(mins = (m.mins + 5).coerceAtMost(300)) })
                }
            }
            "caf" -> {
                Spacer(Modifier.height(12.dp))
                PicGrid(DRINKS, 4, gap = 6.dp) { (sid, w, mg), mod -> PicTile("drink_$sid", tr(w.first, w.second), m.sub == sid, mod, pic = 36.dp) { m = m.copy(sub = sid, mins = mg) } }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("${m.mins} мг", "${m.mins} mg"), style = T.title, modifier = Modifier.weight(1f))
                    Stepper2({ m = m.copy(mins = (m.mins - 5).coerceAtLeast(5)) }, { m = m.copy(mins = (m.mins + 5).coerceAtMost(600)) })
                }
            }
            "bp" -> Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BpField(tr("Верхнее", "Systolic"), "${m.sys}", Modifier.weight(1f)) { m = m.copy(sys = it.toIntOrNull() ?: 0) }
                BpField(tr("Нижнее", "Diastolic"), "${m.dia}", Modifier.weight(1f)) { m = m.copy(dia = it.toIntOrNull() ?: 0) }
                BpField(tr("Пульс", "Pulse"), if (m.pulse > 0) "${m.pulse}" else "", Modifier.weight(1f)) { m = m.copy(pulse = it.toIntOrNull() ?: 0) }
            }
            "pill" -> {
                Text(tr("Что именно", "Which one"), style = T.sec, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    d.meds.filter { !it.archived || it.id == m.med }.forEach { md -> Chip(md.name, md.id == m.med) { m = m.copy(med = md.id, slot = slotFor(md, minuteOf(m.at))) } }
                }
                d.meds.firstOrNull { it.id == m.med }?.takeIf { !it.asNeeded }?.let { md ->
                    Text(tr("По расписанию", "On the schedule"), style = T.sec, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        md.timeList().forEach { t -> Chip(hhmm(t), m.slot == t) { m = m.copy(slot = t) } }
                        Chip(tr("Вне расписания", "Extra"), m.slot < 0) { m = m.copy(slot = -1) }
                    }
                }
            }
        }
        val ok = m.kind != "bp" || (m.sys in 60..260 && m.dia in 30..180 && (m.pulse == 0 || m.pulse in 30..220))
        Btn(tr("Сохранить", "Save"), Modifier.fillMaxWidth().padding(top = 18.dp), enabled = ok && m != m0) { scope.launch { c.editMark(m0, m) }; close() }
        Ghost(tr("Удалить запись", "Delete"), Modifier.fillMaxWidth().padding(top = 8.dp), color = C.bad) { scope.launch { c.undo(m0) }; close() }
    }
}

/**
 * Something done or felt earlier: first when, then what. The mark is saved at once and opens to be put right;
 * a bad spell opens its page; the period is started on that day.
 */
@Composable
fun AddSheet(day: Long, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val scope = c.scope
    val close = { nav.sheet = null }
    var at by remember(day) { mutableLongStateOf(if (day >= today()) System.currentTimeMillis() else startOf(day) + 12 * 3600_000L) }
    var step by remember(day) { mutableStateOf("") }
    fun log(kind: String, sub: String = "", med: Long = 0, slot: Int = -1, mins: Int = 0, sys: Int = 0, dia: Int = 0) =
        scope.launch { val r = c.log(kind, sub = sub, med = med, slot = slot, at = at, mins = mins, sys = sys, dia = dia); nav.sheet = "mark:${r.id}" }
    Sheet(close) {
        Text(tr("Записать задним числом", "Add something earlier"), style = T.h2)
        Text(tr("Сначала когда, потом что. Подробности поправите сразу после.", "First when, then what. You can fill in the details right after."), style = T.small)
        Spacer(Modifier.height(10.dp))
        WhenPicker(at) { at = it }
        Spacer(Modifier.height(14.dp))
        val min = minuteOf(at)
        when (step) {
            "pill" -> {
                Text(tr("Какое лекарство", "Which medicine"), style = T.sec, modifier = Modifier.padding(bottom = 6.dp))
                d.activeMeds.forEach { md ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { log("pill", med = md.id, slot = slotFor(md, min)) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Art("pot_${md.herb}_1", Modifier.size(34.dp)); Spacer(Modifier.width(10.dp))
                        Text(md.name, style = T.bodyB, modifier = Modifier.weight(1f))
                        Text(slotFor(md, min).takeIf { it >= 0 }?.let { hhmm(it) } ?: tr("вне расписания", "extra"), style = T.small)
                    }
                }
                if (d.activeMeds.isEmpty()) Text(tr("Лекарств пока нет.", "No medicine yet."), style = T.small)
            }
            "bad" -> PicGrid(Troubles.kinds(d.s), 3) { k, mod -> PicTile("icon_${k.icon}", k.title, false, mod, pic = 44.dp) {
                if (k.id == "pressure") log("bp", sys = 120, dia = 80)
                else scope.launch { val id = c.startAttack(k.id, at); close(); nav.push(Screen.AttackEdit(id)) }
            } }
            else -> {
                val last = d.marks.firstOrNull { it.kind == "bp" }
                val items = listOf(Triple("eat", "icon_${Plan.mealAt(d.s.mealTimes, min)}", kindTitle("eat")), Triple("water", "icon_water", tr("Стакан воды", "Water")),
                    Triple("pill", "icon_pill", kindTitle("pill")), Triple("move", "icon_walk", kindTitle("move")), Triple("caf", "drink_coffee", kindTitle("caf")),
                    Triple("bp", "icon_tonometer", kindTitle("bp")), Triple("sleep", "icon_sleep", kindTitle("sleep")), Triple("wake", "icon_sunrise", kindTitle("wake")),
                    Triple("bad", "icon_thermo", tr("Было плохо", "Felt bad"))) +
                    (if (d.s.cycle && !d.book.onPeriod(dayOf(at))) listOf(Triple(PERIOD, "icon_period", tr("Начались месячные", "Period began"))) else emptyList())
                PicGrid(items, 3, gap = 6.dp) { (k, icon, label), mod -> PicTile(icon, label, false, mod, pic = 40.dp) {
                    when (k) {
                        "eat" -> log("eat", sub = Plan.mealAt(d.s.mealTimes, min))
                        "move" -> log("move", sub = "walk", mins = 30)
                        "caf" -> log("caf", sub = "coffee", mins = 95)
                        "bp" -> log("bp", sys = last?.sys ?: 120, dia = last?.dia ?: 80)
                        "pill", "bad" -> step = k
                        PERIOD -> scope.launch { val id = c.startPeriod(at); close(); nav.push(Screen.AttackEdit(id)) }
                        else -> log(k)
                    }
                } }
            }
        }
        if (step.isNotEmpty()) Ghost(tr("← Назад", "← Back"), Modifier.fillMaxWidth().padding(top = 10.dp)) { step = "" }
    }
}

/**
 * The cycle: the period on or off (with the day it began), the day of the cycle and the next one expected,
 * and what bothers now – each written as its own spell, so the detective can tie it to the days of the cycle.
 */
@Composable
fun PeriodSheet(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val scope = c.scope
    val close = { nav.sheet = null }
    val b = d.book
    val now = b.periodNow
    val day = b.cycleDay(today())
    val next = b.nextPeriod
    Sheet(close) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Art("icon_period", Modifier.size(56.dp)); Spacer(Modifier.width(10.dp))
            Column {
                Text(tr("Цикл", "The cycle"), style = T.h2)
                Text(when {
                    now != null -> tr("Месячные · день ${today() - dayOf(now.start) + 1}", "Period · day ${today() - dayOf(now.start) + 1}")
                    day > 0 -> tr("День цикла $day", "Cycle day $day") + (next?.let { n -> if (n >= today()) tr(" · следующие около ${fmtDay(n)}", " · the next about ${fmtDay(n)}") else tr(" · уже могли начаться", " · may have begun") } ?: "")
                    else -> tr("Отметьте начало – питомец начнёт считать дни цикла.", "Mark the start – the pet will count the cycle days.")
                }, style = T.small)
            }
        }
        Spacer(Modifier.height(12.dp))
        if (now != null) {
            Btn(tr("Закончились", "It ended"), Modifier.fillMaxWidth(), color = C.sage, ink = Color.White) { scope.launch { c.endAttack(now) }; close() }
            Ghost(tr("Поправить даты", "Change the dates"), Modifier.fillMaxWidth().padding(top = 6.dp)) { close(); nav.push(Screen.AttackEdit(now.id)) }
        } else {
            Text(tr("Начались", "It began"), style = T.sec, modifier = Modifier.padding(bottom = 6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to tr("Сегодня", "Today"), 1 to tr("Вчера", "Yesterday"), 2 to tr("Позавчера", "2 days ago")).forEach { (ago, l) ->
                    Btn(l, Modifier.weight(1f), color = if (ago == 0) C.acc else C.card2, ink = if (ago == 0) C.accInk else C.ink, small = true) {
                        scope.launch { c.startPeriod(if (ago == 0) System.currentTimeMillis() else startOf(today() - ago) + 9 * 3600_000L) }; close()
                    }
                }
            }
        }
        val cy = b.cycles
        if (cy.isNotEmpty()) {
            val lens = b.periods.filter { it.end > 0 }.map { dayOf(it.end) - dayOf(it.start) + 1 }
            Text(tr("Цикл в среднем ${cy.takeLast(6).average().toInt()} ${plural(cy.takeLast(6).average().toLong(), "день", "дня", "дней")}", "The cycle is ${cy.takeLast(6).average().toInt()} days on average") +
                (if (lens.isNotEmpty()) tr(", месячные – около ${lens.average().toInt()} дн.", ", the period about ${lens.average().toInt()} d.") else "."), style = T.smallB, modifier = Modifier.padding(top = 12.dp))
        }
        Text(tr("Что беспокоит", "What bothers you"), style = T.sec, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
        val kinds = listOf("headache", "migraine", "stomach", "back", "insomnia", "anxiety").map { Troubles.of(it) }
        PicGrid(kinds, 3, gap = 6.dp) { k, mod -> PicTile("icon_${k.icon}", k.title, false, mod, pic = 40.dp) {
            scope.launch { val id = c.startAttack(k.id); close(); if (k.dark) nav.dark = id else nav.push(Screen.AttackEdit(id)) }
        } }
        Text(tr("Каждый симптом пишется отдельно, со временем начала и конца. Через пару циклов сыщик покажет, в какие дни они приходят: до месячных, в первый день или во время.",
            "Each symptom is its own record, with a start and an end. After a couple of cycles the detective shows which days they come on: before the period, on its first day or during it."),
            style = T.tiny, modifier = Modifier.padding(top = 10.dp))
    }
}
