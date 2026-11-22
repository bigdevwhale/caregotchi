package app.caregotchi.ui.components

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.caregotchi.domain.hhmm
import app.caregotchi.domain.tr
import app.caregotchi.ui.theme.C
import app.caregotchi.ui.theme.T
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** A drawn choice: a picture on a soft tile, the label under it; the chosen one is lifted and ringed in honey. */
@Composable
fun PicTile(icon: String, label: String, on: Boolean, modifier: Modifier = Modifier, pic: Dp = 52.dp, dark: Boolean = false, onClick: () -> Unit) {
    val lift by animateFloatAsState(if (on) 1f else 0f, label = "lift")
    val base = if (dark) Color(0xFF2A1915) else C.card2
    Column(modifier.graphicsLayer { translationY = -4.dp.toPx() * lift }
        .shadow((1 + 5 * lift).dp, RoundedCornerShape(16.dp), ambientColor = C.shadow, spotColor = C.shadow)
        .clip(RoundedCornerShape(16.dp))
        .background(if (on) (if (dark) Color(0xFF5E211A) else Color(0xFFFFF3D6)) else base)
        .border(if (on) 2.5.dp else 1.dp, if (on) (if (dark) Color(0xFF9A3A2C) else C.honey) else (if (dark) Color(0xFF3A221D) else C.line), RoundedCornerShape(16.dp))
        .clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            if (on) Box(Modifier.size(pic + 8.dp).background(Brush.radialGradient(listOf(Color(0x66FFFFFF), Color.Transparent)), CircleShape))
            Art(icon, Modifier.size(pic), alpha = if (dark && !on) .75f else 1f)
        }
        Text(label, style = T.tiny.copy(color = if (dark) (if (on) Color(0xFFF0C1B6) else Color(0xFFC98E83)) else C.ink), textAlign = TextAlign.Center, maxLines = 2,
            modifier = Modifier.padding(top = 4.dp))
    }
}

/** Tiles in rows of [cols], each row's tiles as tall as its tallest. */
@Composable
fun <T> PicGrid(items: List<T>, cols: Int, modifier: Modifier = Modifier, gap: Dp = 8.dp, tile: @Composable (T, Modifier) -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
        items.chunked(cols).forEach { row ->
            Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { tile(it, Modifier.weight(1f).fillMaxHeight()) }
                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** A small round token that toggles, picture only, its name under it. */
@Composable
fun Token(icon: String, label: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(58.dp).shadow(if (on) 4.dp else 0.dp, CircleShape).clip(CircleShape).background(if (on) Color(0xFFFFF3D6) else C.card2)
            .border(if (on) 2.5.dp else 1.dp, if (on) C.sage else C.line, CircleShape), contentAlignment = Alignment.Center) {
            Art(icon, Modifier.size(40.dp), alpha = if (on) 1f else .6f)
            if (on) Box(Modifier.align(Alignment.TopEnd).size(18.dp).clip(CircleShape).background(C.sage), contentAlignment = Alignment.Center) { Text("✓", style = T.tiny.copy(color = Color.White)) }
        }
        Text(label, style = T.tiny.copy(color = if (on) C.ink else C.mute), modifier = Modifier.padding(top = 3.dp), maxLines = 1)
    }
}

/** How strong, 1..10: ten segments from honey to ember; tap or slide. */
@Composable
fun StrengthBar(value: Int, dark: Boolean = false, onChange: (Int) -> Unit) {
    val words = listOf(tr("слабо", "mild"), tr("терпимо", "bearable"), tr("сильно", "strong"), tr("очень сильно", "very strong"), tr("невыносимо", "unbearable"))
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(if (value > 0) "$value" else "–", style = T.title.copy(color = if (dark) Color(0xFFF0C1B6) else C.ink))
            Text(if (value > 0) "  " + words[((value - 1) / 2).coerceIn(0, 4)] else "  " + tr("коснитесь шкалы", "tap the scale"), style = T.smallB.copy(color = if (dark) Color(0xFF8D5F57) else C.mute), modifier = Modifier.padding(bottom = 4.dp))
        }
        Box(Modifier.fillMaxWidth().height(44.dp).padding(top = 6.dp)
            .pointerInput(Unit) { detectTapGestures { o -> onChange(((o.x / size.width) * 10).toInt().coerceIn(0, 9) + 1) } }
            .pointerInput(Unit) { detectHorizontalDragGestures { ch, _ -> onChange(((ch.position.x / size.width) * 10).toInt().coerceIn(0, 9) + 1) } }) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width / 10; val gap = 3.dp.toPx()
                for (i in 0 until 10) {
                    val col = lerp(Color(0xFFD9A441), Color(0xFFA33B2C), i / 9f)
                    val h = size.height * (.45f + .55f * i / 9f)
                    drawRoundRect(if (i < value) col else (if (dark) Color(0xFF4A2E28) else Color(0xFFEDE3D1)), Offset(i * w + gap / 2, size.height - h), Size(w - gap, h), CornerRadius(6.dp.toPx()))
                }
            }
        }
    }
}

/** Where it hurts, on a drawn head seen from the front: forehead, left, right; the back of the head and «all over» below. */
@Composable
fun HeadPicker(side: String, dark: Boolean = false, onChange: (String) -> Unit) {
    val skin = if (dark) Color(0xFF3A2520) else Color(0xFFF3DCC4)
    val line = if (dark) Color(0xFF6A3A30) else Color(0xFFB98A6A)
    val hot = Color(0xFFC0392B)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(150.dp).pointerInput(Unit) {
            detectTapGestures { o ->
                val x = o.x / size.width; val y = o.y / size.height
                onChange(when { y < .36f -> "front"; x < .5f -> "left"; else -> "right" }.let { if (it == side) "" else it })
            }
        }) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                val head = Rect(w * .2f, h * .06f, w * .8f, h * .86f)
                fun glow(c: Offset, r: Float) = drawCircle(Brush.radialGradient(listOf(hot.copy(alpha = .75f), Color.Transparent), c, r), r, c)
                // ears and neck
                drawOval(skin, Offset(w * .13f, h * .4f), Size(w * .12f, h * .18f)); drawOval(skin, Offset(w * .75f, h * .4f), Size(w * .12f, h * .18f))
                drawRect(skin, Offset(w * .4f, h * .8f), Size(w * .2f, h * .16f))
                drawOval(skin, head.topLeft, head.size)
                if (side == "front" || side == "both") glow(Offset(w * .5f, h * .2f), w * .26f)
                if (side == "left" || side == "both") glow(Offset(w * .3f, h * .42f), w * .24f)
                if (side == "right" || side == "both") glow(Offset(w * .7f, h * .42f), w * .24f)
                if (side == "back") drawOval(hot.copy(alpha = .3f), head.topLeft, head.size)
                drawOval(line, head.topLeft, head.size, style = Stroke(2.5.dp.toPx()))
                // a calm face: closed eyes and a small mouth
                val p = Path().apply { moveTo(w * .36f, h * .47f); quadraticTo(w * .4f, h * .5f, w * .44f, h * .47f); moveTo(w * .56f, h * .47f); quadraticTo(w * .6f, h * .5f, w * .64f, h * .47f)
                    moveTo(w * .45f, h * .66f); quadraticTo(w * .5f, h * .69f, w * .55f, h * .66f) }
                drawPath(p, line, style = Stroke(2.dp.toPx()))
                // the dividing dashes
                var y = h * .1f; while (y < h * .82f) { drawLine(line.copy(alpha = .35f), Offset(w * .5f, y), Offset(w * .5f, y + h * .03f), 1.5.dp.toPx()); y += h * .06f }
                drawLine(line.copy(alpha = .35f), Offset(w * .26f, h * .34f), Offset(w * .74f, h * .34f), 1.5.dp.toPx())
            }
            Text(tr("как в зеркале", "as in a mirror"), style = T.tiny.copy(color = line), modifier = Modifier.align(Alignment.BottomCenter))
        }
        Spacer(Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(when (side) { "front" -> tr("Лоб", "Forehead"); "left" -> tr("Слева", "Left"); "right" -> tr("Справа", "Right"); "back" -> tr("Затылок", "Back of the head"); "both" -> tr("С обеих сторон", "Both sides"); else -> tr("Коснитесь,\nгде болит", "Tap where\nit hurts") },
                style = T.h3.copy(color = if (dark) Color(0xFFD9A196) else C.ink))
            listOf("back" to tr("Затылок", "Back"), "both" to tr("Везде", "All over")).forEach { (id, l) ->
                Box(Modifier.clip(RoundedCornerShape(50)).background(if (side == id) (if (dark) Color(0xFF5E211A) else C.acc) else (if (dark) Color(0xFF2A1915) else C.card2))
                    .clickable { onChange(if (side == id) "" else id) }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Text(l, style = T.smallB.copy(color = if (side == id) (if (dark) Color(0xFFF0C1B6) else C.accInk) else (if (dark) Color(0xFFC98E83) else C.ink)))
                }
            }
        }
    }
}

/** A 24-hour clock face: tap the rim to add a time (to the half hour), tap a marker to take it off. */
@Composable
fun ClockDial(times: List<Int>, modifier: Modifier = Modifier, onChange: (List<Int>) -> Unit) {
    val face = C.paper; val ink = Color(0xFF3B2E26)
    Box(modifier.aspectRatio(1f).pointerInput(times) {
        detectTapGestures { o ->
            val cx = size.width / 2f; val cy = size.height / 2f
            val a = atan2(o.y - cy, o.x - cx) + PI / 2
            val frac = ((a / (2 * PI)) + 1) % 1
            val min = ((frac * 1440 / 30).roundToInt() * 30) % 1440
            val near = times.firstOrNull { kotlin.math.abs(((it - min + 720 + 1440) % 1440) - 720) <= 45 }
            onChange(if (near != null) times - near else (times + min).sorted())
        }
    }) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2; val c = Offset(size.width / 2, size.height / 2)
            drawCircle(Color(0xFF8A5A2E), r); drawCircle(face, r * .92f)
            // night half shaded
            drawArc(Color(0x22283C6A), 180f, 180f, true, Offset(c.x - r * .92f, c.y - r * .92f), Size(r * 1.84f, r * 1.84f))
            for (hh in 0 until 24) {
                val ang = hh / 24.0 * 2 * PI - PI / 2
                val l = if (hh % 6 == 0) .16f else .07f
                drawLine(ink.copy(alpha = .6f), Offset((c.x + cos(ang) * r * .9f).toFloat(), (c.y + sin(ang) * r * .9f).toFloat()),
                    Offset((c.x + cos(ang) * r * (.9f - l)).toFloat(), (c.y + sin(ang) * r * (.9f - l)).toFloat()), if (hh % 6 == 0) 3f else 1.5f)
            }
            for (t in times) {
                val ang = t / 1440.0 * 2 * PI - PI / 2
                val p = Offset((c.x + cos(ang) * r * .66f).toFloat(), (c.y + sin(ang) * r * .66f).toFloat())
                drawLine(Color(0xFFC98B86), c, p, 4f)
                drawCircle(Color(0xFFC98B86), r * .1f, p); drawCircle(Color.White, r * .045f, p)
            }
            drawCircle(ink, r * .04f, c)
        }
        listOf(0 to "0", 360 to "6", 720 to "12", 1080 to "18").forEach { (m, l) ->
            val ang = m / 1440.0 * 2 * PI - PI / 2
            Text(l, style = T.tiny.copy(color = ink), modifier = Modifier.align(Alignment.Center).offset((cos(ang) * 64).dp, (sin(ang) * 64).dp))
        }
        Column(Modifier.align(Alignment.Center).offset(y = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (times.isEmpty()) tr("коснитесь\nкраешка", "tap\nthe rim") else times.joinToString(" · ") { hhmm(it) }, style = T.tiny.copy(color = ink), textAlign = TextAlign.Center, maxLines = 3)
        }
    }
}

/** A sheet of lined paper with a coloured margin, like a prescription pad. */
@Composable
fun Paper(modifier: Modifier = Modifier, tilt: Float = 0f, stamp: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.rotate(tilt)) {
        Column(Modifier.fillMaxWidth().shadow(5.dp, RoundedCornerShape(4.dp)).clip(RoundedCornerShape(4.dp)).background(C.paper)
            .drawBehind {
                var y = 30.dp.toPx(); while (y < size.height) { drawLine(Color(0xFFEADFCB), Offset(0f, y), Offset(size.width, y), 1.5f); y += 28.dp.toPx() }
                drawLine(Color(0x55C98B86), Offset(28.dp.toPx(), 0f), Offset(28.dp.toPx(), size.height), 2f)
            }.padding(start = 38.dp, end = 16.dp, top = 14.dp, bottom = 16.dp), content = content)
        if (stamp != null) Art(stamp, Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-12).dp).size(44.dp).rotate(12f))
    }
}

/** A field written on paper: a hand-drawn underline instead of a box. */
@Composable
fun PaperField(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 4.dp).drawBehind { drawLine(Color(0xFFB98A6A), Offset(0f, size.height), Offset(size.width, size.height), 2f) }.padding(bottom = 4.dp)) {
        if (value.isEmpty()) Text(hint, style = T.note.copy(color = Color(0xFFB0A090)))
        androidx.compose.foundation.text.BasicTextField(value, onChange, singleLine = true, textStyle = T.note.copy(fontSize = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp)),
            modifier = Modifier.fillMaxWidth(), cursorBrush = androidx.compose.ui.graphics.SolidColor(C.rose))
    }
}

/** A heading on a page: a drawn picture and a serif title. */
@Composable
fun Head(icon: String, title: String, sub: String? = null, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Art(icon, Modifier.size(34.dp)); Spacer(Modifier.width(10.dp))
        Column { Text(title, style = T.h3); if (sub != null) Text(sub, style = T.tiny) }
    }
}

/** A themed group: a paper card with a drawn picture in the corner. */
@Composable
fun Group(icon: String, title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().padding(top = 14.dp).shadow(3.dp, RoundedCornerShape(18.dp), ambientColor = C.shadow, spotColor = C.shadow)
        .clip(RoundedCornerShape(18.dp)).background(C.card).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(C.card2), contentAlignment = Alignment.Center) { Art(icon, Modifier.size(32.dp)) }
            Spacer(Modifier.width(10.dp)); Text(title, style = T.h3)
        }
        Spacer(Modifier.height(6.dp)); content()
    }
}

/**
 * When something happened, to put a record right or add one afterwards: the day (‹ ›, never past today),
 * the hours and the minutes (by five), each with its own − and +. It never goes into the future.
 */
@Composable
fun WhenPicker(at: Long, modifier: Modifier = Modifier, dayOnly: Boolean = false, onChange: (Long) -> Unit) {
    val now = System.currentTimeMillis()
    val day = app.caregotchi.domain.dayOf(at); val min = app.caregotchi.domain.minuteOf(at)
    fun put(d: Long, m: Int) = onChange((app.caregotchi.domain.startOf(d) + ((m % 1440 + 1440) % 1440) * 60_000L).coerceAtMost(now))
    @Composable fun Box3(label: String, value: String, mod: Modifier, minus: () -> Unit, plus: () -> Unit, plusOn: Boolean = true) {
        Column(mod.clip(RoundedCornerShape(14.dp)).background(C.card2).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = T.tiny)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("‹", style = T.title.copy(color = C.ink), modifier = Modifier.clip(CircleShape).clickable(onClick = minus).padding(horizontal = 10.dp))
                Text(value, style = T.bodyB, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1)
                Text("›", style = T.title.copy(color = if (plusOn) C.ink else C.line), modifier = Modifier.clip(CircleShape).clickable(enabled = plusOn, onClick = plus).padding(horizontal = 10.dp))
            }
        }
    }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box3(tr("день", "day"), app.caregotchi.domain.relDay(day), Modifier.weight(if (dayOnly) 1f else 1.5f), { put(day - 1, min) }, { put(day + 1, min) }, day < app.caregotchi.domain.today())
        if (!dayOnly) {
            Box3(tr("часы", "hours"), "%d".format(min / 60), Modifier.weight(1f), { put(day, min - 60) }, { put(day, min + 60) })
            Box3(tr("минуты", "minutes"), "%02d".format(min % 60), Modifier.weight(1f), { put(day, min - min % 5 - if (min % 5 == 0) 5 else 0) }, { put(day, min - min % 5 + 5) })
        }
    }
}

/** How a spell went (#11): «8:00 – 3 → 9:10 – 5 → 10:00 – 3 · отпустило в 11:00». */
fun curveText(a: app.caregotchi.data.Attack): String =
    a.points.joinToString(" → ") { (t, v) -> "${app.caregotchi.domain.hhmm(app.caregotchi.domain.minuteOf(t))} – $v" } +
        if (a.end > 0) tr(" · отпустило в ", " · eased at ") + app.caregotchi.domain.hhmm(app.caregotchi.domain.minuteOf(a.end)) else ""

/** Something taken that isn't on the sill, in a few words (#11). */
@Composable
fun TookOther(onClose: () -> Unit, dark: Boolean = false, onAdd: (String) -> Unit) {
    var t by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onClose,
        title = { Text(tr("Что ещё приняли?", "What else did you take?"), style = T.h3.copy(color = if (dark) Color(0xFFD9A196) else T.h3.color)) },
        text = { androidx.compose.material3.OutlinedTextField(t, { t = it.replace(",", " ").take(30) }, singleLine = true, placeholder = { Text(tr("Например: кофе, цитрамон", "E.g. coffee, aspirin")) }) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { if (t.isNotBlank()) onAdd(t.trim()); onClose() }) { Text(tr("Добавить", "Add")) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onClose) { Text(tr("Отмена", "Cancel")) } },
        // in the dark room it stays dim too
        containerColor = if (dark) Color(0xFF2A1915) else androidx.compose.material3.AlertDialogDefaults.containerColor,
        titleContentColor = if (dark) Color(0xFFD9A196) else androidx.compose.material3.AlertDialogDefaults.titleContentColor,
        textContentColor = if (dark) Color(0xFFC98E83) else androidx.compose.material3.AlertDialogDefaults.textContentColor,
    )
}
