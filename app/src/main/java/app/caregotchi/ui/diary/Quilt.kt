package app.caregotchi.ui.diary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.caregotchi.domain.Day
import app.caregotchi.domain.Letter
import app.caregotchi.domain.dayShort
import app.caregotchi.domain.fmtDay
import app.caregotchi.domain.hhmm
import app.caregotchi.domain.minuteOf
import app.caregotchi.domain.monthName
import app.caregotchi.domain.today
import app.caregotchi.domain.tr
import app.caregotchi.domain.weekStart
import app.caregotchi.ui.Data
import app.caregotchi.ui.Nav
import app.caregotchi.ui.Screen
import app.caregotchi.ui.components.Art
import app.caregotchi.ui.theme.C
import app.caregotchi.ui.theme.T
import java.time.LocalDate

/** A patch's fabric: its colour and pattern say how the day went. */
internal enum class Cloth(val base: Color, val ink: Color) {
    FULL(Color(0xFF9FBD92), Color(0xFF7FA071)),     // all done: sage with little flowers
    GOOD(Color(0xFFE2C27E), Color(0xFFC9A45C)),     // nearly: honey gingham
    LOW(Color(0xFFD9B0A4), Color(0xFFC08F82)),      // little care: dusty rose stripes
    PLAID(Color(0xFFC9B9E0), Color(0xFFAB98C8)),    // under the blanket: lilac knit
    EMPTY(Color(0xFFEFE4CF), Color(0xFFE2D4BA)),    // no marks: plain linen
}

internal fun clothOf(x: Day?): Cloth = when {
    x == null || !x.any && !x.plaid -> Cloth.EMPTY
    x.plaid -> Cloth.PLAID
    x.score >= .8 -> Cloth.FULL
    x.good -> Cloth.GOOD
    else -> Cloth.LOW
}

internal fun DrawScope.fabric(c: Cloth) {
    drawRect(c.base)
    val w = size.width; val h = size.height; val s = 2.dp.toPx()
    clipRect {
        when (c) {
            Cloth.FULL -> { // tiny four-petal flowers
                var y = h * .25f; var row = 0
                while (y < h) { var x = if (row % 2 == 0) w * .25f else w * .6f
                    while (x < w) { for (k in 0..3) { val a = k * Math.PI / 2; drawCircle(Color(0x99FFFFFF), s * 1.1f, Offset(x + (Math.cos(a) * s * 1.4).toFloat(), y + (Math.sin(a) * s * 1.4).toFloat())) }
                        drawCircle(Color(0xFFE2C27E), s * .9f, Offset(x, y)); x += w * .5f }
                    y += h * .5f; row++ }
            }
            Cloth.GOOD -> { var x = 0f; while (x < w) { drawRect(c.ink.copy(alpha = .45f), Offset(x, 0f), Size(w / 8, h)); x += w / 4 }
                var y = 0f; while (y < h) { drawRect(c.ink.copy(alpha = .35f), Offset(0f, y), Size(w, h / 8)); y += h / 4 } }
            Cloth.LOW -> { var x = -h; while (x < w) { drawLine(c.ink.copy(alpha = .6f), Offset(x, h), Offset(x + h, 0f), s); x += s * 4 } }
            Cloth.PLAID -> { var y = s * 2; while (y < h) { var x = 0f; while (x < w) { drawLine(c.ink, Offset(x, y), Offset(x + s * 1.5f, y - s * 1.5f), s * .8f); drawLine(c.ink, Offset(x + s * 1.5f, y - s * 1.5f), Offset(x + s * 3, y), s * .8f); x += s * 3 }; y += s * 3 } }
            Cloth.EMPTY -> { var y = 0f; while (y < h) { drawLine(c.ink, Offset(0f, y), Offset(w, y), 1f); y += s * 1.5f } }
        }
    }
}

/** The month as a patchwork quilt; a stitched cross marks a bad spell, a boot a day of moving; tap a patch to open the day. */
@Composable
fun QuiltPane(d: Data, nav: Nav) {
    var shift by remember { mutableIntStateOf(0) }
    val m = LocalDate.ofEpochDay(today()).withDayOfMonth(1).plusMonths(shift.toLong())
    val first = m.toEpochDay(); val len = m.lengthOfMonth()
    val days = (first until first + len).mapNotNull { d.book.care(it) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("‹", style = T.title, modifier = Modifier.clickable { shift-- }.padding(horizontal = 14.dp))
        Text(monthName(m.monthValue).replaceFirstChar { it.uppercase() } + if (m.year != LocalDate.now().year) " ${m.year}" else "", style = T.h2, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("›", style = T.title.copy(color = if (shift < 0) C.ink else C.line), modifier = Modifier.clickable(enabled = shift < 0) { shift++ }.padding(horizontal = 14.dp))
    }
    Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) { (0..6).forEach { Text(dayShort(it), style = T.tiny, modifier = Modifier.weight(1f), textAlign = TextAlign.Center) } }
    val lead = m.dayOfWeek.value - 1
    Box {
        Column(Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp)).background(Color(0xFFB8845A)).padding(5.dp)
            .clip(RoundedCornerShape(6.dp)).background(Color(0xFFF3E6CC)).padding(3.dp)) {
            (0 until (lead + len + 6) / 7).forEach { w ->
                Row {
                    (0..6).forEach { i ->
                        val n = w * 7 + i - lead
                        val day = first + n
                        val inMonth = n in 0 until len
                        val x = if (inMonth && day <= today()) d.book.care(day) else null
                        val cloth = if (!inMonth) null else clothOf(x)
                        Box(Modifier.weight(1f).aspectRatio(1f).padding(1.dp)
                            .drawBehind {
                                if (cloth == null) return@drawBehind
                                fabric(if (day > today()) Cloth.EMPTY else cloth)
                                drawRoundRect(Color(0x99FFFFFF), Offset(3f, 3f), Size(size.width - 6f, size.height - 6f), CornerRadius(2f),
                                    style = Stroke(1.6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))))
                                if ((x?.attacks ?: 0) > 0) { // a red cross-stitch
                                    val c = Offset(size.width / 2, size.height / 2); val r = size.minDimension * .2f; val red = Color(0xFFB0342A)
                                    drawLine(red, c - Offset(r, r), c + Offset(r, r), 4f); drawLine(red, c + Offset(-r, r), c + Offset(r, -r), 4f)
                                }
                                if (day == today()) drawRect(Color(0xFF6B4A3A), style = Stroke(5f))
                            }
                            .clickable(enabled = inMonth && day <= today()) { nav.push(Screen.DayView(day)) }) {
                            if (inMonth) {
                                Text("${n + 1}", style = T.tiny.copy(color = Color(0xAA3B2E26)), modifier = Modifier.padding(start = 3.dp))
                            }
                        }
                    }
                }
            }
        }
        // a sewn-on label with the month's tally
        if (days.isNotEmpty()) {
            val spells = days.sumOf { it.attacks }; val good = days.count { it.good }
            Column(Modifier.align(Alignment.BottomEnd).offset(x = (-8).dp, y = 14.dp).rotate(-3f).shadow(3.dp, RoundedCornerShape(3.dp)).clip(RoundedCornerShape(3.dp))
                .background(C.paper).border(1.5.dp, Color(0xFFC98B86), RoundedCornerShape(3.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text(tr("$good хороших · $spells ${if (spells == 1) "приступ" else if (spells in 2..4) "приступа" else "приступов"}", "$good good · $spells spells"), style = T.tiny.copy(color = Color(0xFF3B2E26)))
            }
        }
    }
    Spacer(Modifier.height(18.dp))
    Legend()
    if (days.isNotEmpty()) Jars(d, days)
    Clothesline(d, nav)
}

@Composable
private fun Legend() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf(Cloth.FULL to tr("всё", "all"), Cloth.GOOD to tr("почти", "nearly"), Cloth.LOW to tr("мало", "little"), Cloth.PLAID to tr("плед", "blanket")).forEach { (c, l) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).drawBehind { fabric(c) }); Text(" $l", style = T.tiny)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) { Text("✕", style = T.smallB.copy(color = Color(0xFFB0342A))); Text(" " + tr("приступ", "spell"), style = T.tiny) }
    }
}

/** The month's care as five glass jars on a shelf, filled to the month's average. */
@Composable
private fun Jars(d: Data, days: List<Day>) {
    val s = d.s
    val eat = days.map { (it.meals.coerceAtMost(3) / 3.0) }.average()
    val water = days.map { (it.water / s.waterGoal.toDouble()).coerceAtMost(1.0) }.average()
    val pillDays = days.filter { it.due > 0 }
    val pill = if (pillDays.isEmpty()) 1.0 else pillDays.map { it.taken / it.due.toDouble() }.average()
    val move = days.map { (it.moveMins / s.moveGoal.toDouble()).coerceAtMost(1.0) }.average()
    val nights = days.filter { it.slept >= 0 }
    val sleep = if (nights.isEmpty()) 0.0 else nights.map { (it.slept / s.sleepGoal).coerceAtMost(1.0) }.average()
    val jars = listOf(Triple("icon_eat", tr("еда", "food"), eat to Color(0xFFE2B85C)), Triple("icon_water", tr("вода", "water"), water to Color(0xFF8DBDD6)),
        Triple("icon_pill", tr("лекарства", "medicine"), pill to Color(0xFFD99A92)), Triple("icon_walk", tr("движение", "moving"), move to Color(0xFF9DBB97)),
        Triple("icon_sleep", tr("сон", "sleep"), sleep to Color(0xFFA59FD0)))
    Text(tr("Банки месяца", "The month's jars"), style = T.h3, modifier = Modifier.padding(top = 14.dp))
    Text(tr("Насколько в среднем наполнялся каждый день", "How full an average day was"), style = T.tiny)
    Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(Modifier.fillMaxWidth().drawBehind {
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFD4A066), Color(0xFFA8743F)), 70.dp.toPx(), 80.dp.toPx()), Offset(0f, 70.dp.toPx()), Size(size.width, 9.dp.toPx()), CornerRadius(3.dp.toPx()))
        }.padding(horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            jars.forEach { (icon, label, v) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.width(54.dp).height(72.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize()) {
                            val w = size.width; val h = size.height; val r = 10.dp.toPx()
                            val body = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(w * .06f, h * .16f, w * .94f, h, CornerRadius(r))) }
                            drawPath(body, Color(0x33FFFFFF))
                            clipRect(top = h * .16f + (h * .84f) * (1 - v.first.toFloat())) { drawPath(body, v.second.copy(alpha = .85f)) }
                            drawPath(body, Color(0x885A4636), style = Stroke(2.dp.toPx()))
                            drawLine(Color(0x99FFFFFF), Offset(w * .2f, h * .3f), Offset(w * .2f, h * .8f), 3.dp.toPx())
                            drawRoundRect(Color(0xFFA8743F), Offset(w * .02f, 0f), Size(w * .96f, h * .16f), CornerRadius(4.dp.toPx()))
                        }
                        Art(icon, Modifier.size(26.dp).offset(y = 6.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("${(v.first * 100).toInt()}%", style = T.smallB)
                    Text(label, style = T.tiny)
                }
            }
        }
    }
}

/** Today on a washing line: each mark a card under a peg, in the order of the day. */
@Composable
private fun Clothesline(d: Data, nav: Nav) {
    val items = d.book.marksOn(today()).sortedBy { it.at }.map { Triple(it.at, icon(it.kind), app.caregotchi.ui.home.markText(it, d)) } +
        d.book.attacksOn(today()).map { Triple(it.start, "icon_${app.caregotchi.domain.Troubles.of(it.kind).icon}", app.caregotchi.domain.Troubles.title(it.kind)) }
    Text(tr("Сегодня на верёвочке", "Today on the line"), style = T.h3)
    if (items.isEmpty()) { Text(tr("Пока пусто – отметки делаются в дупле.", "Nothing yet – marks are made in the hollow."), style = T.small); return }
    val sorted = items.sortedBy { it.first }
    Box(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).drawBehind {
            val p = Path().apply { moveTo(0f, 10.dp.toPx()); quadraticTo(size.width / 2, 26.dp.toPx(), size.width, 10.dp.toPx()) }
            drawPath(p, Color(0xFF8A6A4A), style = Stroke(2.dp.toPx()))
        }.padding(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            sorted.forEachIndexed { i, (at, icon, text) ->
                Column(Modifier.padding(top = 4.dp).rotate(if (i % 2 == 0) -3f else 3f).clickable { nav.push(Screen.DayView(today())) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.width(8.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFC9A16A)))
                    Column(Modifier.width(84.dp).shadow(3.dp, RoundedCornerShape(4.dp)).clip(RoundedCornerShape(4.dp)).background(C.paper).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Art(icon, Modifier.size(34.dp))
                        Text(hhmm(minuteOf(at)), style = T.tiny.copy(color = Color(0xFF7A6656)))
                        Text(text, style = T.tiny.copy(color = Color(0xFF3B2E26)), maxLines = 2, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

/** The pet's letters: this week's in a sealed envelope (tap to open), the earlier ones in a stack below. */
@Composable
fun Letters(d: Data) {
    val name = d.pet?.name ?: tr("Заботогочи", "Caregotchi")
    val last = weekStart(today()) - 7
    val weeks = (0..5).map { last - it * 7 }.filter { it + 6 >= d.book.first }
    var open by remember { mutableLongStateOf(-1L) }
    if (weeks.isEmpty()) {
        Envelope(tr("Первое письмо придёт в воскресенье", "The first letter comes on Sunday"), sealed = true) {}
        Text(tr("$name пишет по воскресеньям – о том, как прошла ваша неделя.", "$name writes on Sundays – about how your week went."), style = T.small, modifier = Modifier.padding(top = 10.dp))
        return
    }
    weeks.forEachIndexed { i, w ->
        val opened = open == w
        Envelope(if (i == 0) tr("Новое письмо · ${fmtDay(w)} – ${fmtDay(w + 6)}", "New letter · ${fmtDay(w)} – ${fmtDay(w + 6)}") else "${fmtDay(w)} – ${fmtDay(w + 6)}",
            sealed = !opened, fresh = i == 0, modifier = Modifier.padding(top = if (i == 0) 0.dp else 8.dp).rotate(if (i % 2 == 0) -.8f else .8f)) { open = if (opened) -1 else w }
        AnimatedVisibility(opened, enter = expandVertically() + fadeIn()) {
            Box(Modifier.fillMaxWidth().padding(top = 6.dp).shadow(6.dp, RoundedCornerShape(4.dp)).clip(RoundedCornerShape(4.dp)).background(C.paper)
                .drawBehind { var y = 34.dp.toPx(); while (y < size.height) { drawLine(Color(0xFFE9DCC4), Offset(12f, y), Offset(size.width - 12f, y), 2f); y += 26.dp.toPx() } }
                .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 18.dp)) {
                Column {
                    Text(Letter.of(d.book, name, w), style = T.note.copy(lineHeight = androidx.compose.ui.unit.TextUnit(26f, androidx.compose.ui.unit.TextUnitType.Sp)))
                    Spacer(Modifier.height(56.dp))
                }
                d.pet?.let { Art("pet_${it.species}_6", Modifier.align(Alignment.BottomEnd).offset(y = 10.dp).size(78.dp)) }
            }
        }
    }
}

@Composable
private fun Envelope(title: String, sealed: Boolean, fresh: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.fillMaxWidth().height(if (fresh) 120.dp else 64.dp).shadow(4.dp, RoundedCornerShape(6.dp)).clip(RoundedCornerShape(6.dp))
        .background(Color(0xFFF1E2C4)).clickable(onClick = onClick)
        .drawBehind {
            val w = size.width; val h = size.height
            drawPath(Path().apply { moveTo(0f, 0f); lineTo(w / 2, h * .55f); lineTo(w, 0f); close() }, Color(0xFFE6D2AE))
            drawLine(Color(0x55A8743F), Offset(0f, 0f), Offset(w / 2, h * .55f), 2f); drawLine(Color(0x55A8743F), Offset(w, 0f), Offset(w / 2, h * .55f), 2f)
        }) {
        if (sealed) Box(Modifier.align(Alignment.Center).offset(y = if (fresh) 6.dp else 2.dp).size(if (fresh) 46.dp else 30.dp).clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFFD0503F), Color(0xFF8E2A22)))), contentAlignment = Alignment.Center) {
            Art("icon_leaf", Modifier.size(if (fresh) 24.dp else 16.dp), alpha = .7f)
        }
        Text(title, style = (if (fresh) T.smallB else T.tiny).copy(color = Color(0xFF5A4636)), modifier = Modifier.align(Alignment.BottomStart).padding(10.dp))
        if (fresh && sealed) Text(tr("коснитесь, чтобы открыть", "tap to open"), style = T.tiny.copy(color = Color(0xFF8A6A4A)), modifier = Modifier.align(Alignment.TopEnd).padding(10.dp))
    }
}
