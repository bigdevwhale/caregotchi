package app.caregotchi.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.caregotchi.ui.theme.C
import app.caregotchi.ui.theme.T

fun Modifier.tap(onClick: () -> Unit) = this.clickable(MutableInteractionSource(), null, onClick = onClick)
fun Modifier.card(r: Dp = 18.dp, color: Color? = null) = this.shadow(4.dp, RoundedCornerShape(r), ambientColor = C.shadow, spotColor = C.shadow)
    .clip(RoundedCornerShape(r)).background(color ?: C.card)
/** A soft raised button look: a darker lip under the face. */
fun Modifier.lip(color: Color, under: Color, r: Dp = 16.dp) = this.clip(RoundedCornerShape(r)).background(under).padding(bottom = 3.dp).clip(RoundedCornerShape(r)).background(color)

private val ids = HashMap<String, Int>()

/** A drawable by its name (pets, items, icons are looked up at run time); 0 when missing. */
@Composable
fun resId(name: String): Int {
    val ctx = LocalContext.current
    return ids.getOrPut(name) { ctx.resources.getIdentifier(name, "drawable", ctx.packageName) }
}

/** Codex art by name, drawn crisp. */
@Composable
fun Art(name: String, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.Fit, alpha: Float = 1f, tint: androidx.compose.ui.graphics.ColorFilter? = null) {
    val id = resId(name)
    if (id == 0) { Box(modifier); return }
    Image(ImageBitmap.imageResource(id), null, modifier, contentScale = scale, alpha = alpha, colorFilter = tint, filterQuality = FilterQuality.None)
}

val grey = androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) })

@Composable
fun Btn(text: String, modifier: Modifier = Modifier, color: Color = C.acc, ink: Color = C.accInk, enabled: Boolean = true, small: Boolean = false, icon: String? = null, onClick: () -> Unit) {
    Row(modifier.lip(if (enabled) color else C.line, if (enabled) lerp(color, Color.Black, .25f) else C.line).clickable(enabled = enabled, onClick = onClick)
        .padding(horizontal = if (small) 14.dp else 18.dp, vertical = if (small) 8.dp else 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        if (icon != null) { Art(icon, Modifier.size(if (small) 18.dp else 22.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, style = T.btn.copy(color = if (enabled) ink else C.mute, fontSize = if (small) T.smallB.fontSize else T.btn.fontSize), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun Ghost(text: String, modifier: Modifier = Modifier, color: Color = C.ink, small: Boolean = false, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(14.dp)).border(1.5.dp, C.line, RoundedCornerShape(14.dp)).clickable(onClick = onClick)
        .padding(horizontal = if (small) 12.dp else 16.dp, vertical = if (small) 7.dp else 12.dp), contentAlignment = Alignment.Center) {
        Text(text, style = (if (small) T.smallB else T.btn).copy(color = color), maxLines = 1)
    }
}

@Composable
fun Chip(text: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(50)).background(if (on) C.acc else C.card2).clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(text, style = T.smallB.copy(color = if (on) C.accInk else C.ink), maxLines = 1)
    }
}

@Composable
fun Segments(items: List<String>, sel: Int, modifier: Modifier = Modifier, onSel: (Int) -> Unit) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(C.card2).padding(3.dp)) {
        items.forEachIndexed { i, s ->
            Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (i == sel) C.card else Color.Transparent).clickable { onSel(i) }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Text(s, style = T.smallB.copy(color = if (i == sel) C.ink else C.mute), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun Section(text: String, modifier: Modifier = Modifier, aside: String? = null) {
    Row(modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
        Text(text, style = T.h3, modifier = Modifier.weight(1f))
        if (aside != null) Text(aside, style = T.tiny)
    }
}

/** A thread-like progress line, as on the pet's blanket. */
@Composable
fun Thread(v: Float, modifier: Modifier = Modifier, color: Color = C.rose, h: Dp = 10.dp) {
    Box(modifier.fillMaxWidth().height(h).clip(RoundedCornerShape(50)).background(C.paper).border(1.dp, C.honey.copy(alpha = .6f), RoundedCornerShape(50))) {
        Canvas(Modifier.fillMaxWidth(v.coerceIn(0f, 1f)).height(h)) {
            var x = 2f
            while (x < size.width - 4) { drawLine(color, Offset(x, size.height / 2), Offset(minOf(x + 7.dp.toPx(), size.width - 2), size.height / 2), 4.dp.toPx(), StrokeCap.Round); x += 11.dp.toPx() }
        }
    }
}

/** A round gauge with a picture inside. */
@Composable
fun Ring(v: Float, color: Color, icon: String, modifier: Modifier = Modifier, size: Dp = 58.dp) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = 5.dp.toPx(); val r = Size(this.size.width - w, this.size.height - w)
            drawCircle(C.card2, radius = this.size.minDimension / 2 - w / 2)
            drawArc(C.line, 0f, 360f, false, Offset(w / 2, w / 2), r, style = Stroke(w))
            drawArc(color, -90f, 360f * v.coerceIn(0.001f, 1f), false, Offset(w / 2, w / 2), r, style = Stroke(w, cap = StrokeCap.Round))
        }
        Art(icon, Modifier.size(size * .58f))
    }
}

/** A page over the tabs with a back arrow. */
@Composable
fun Page(title: String, onBack: () -> Unit, scroll: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(C.bg).tap {}.statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = C.ink) }
            Text(title, style = T.h2, modifier = Modifier.weight(1f).padding(start = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(Modifier.weight(1f).fillMaxWidth().then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier).padding(horizontal = 18.dp).navigationBarsPadding().imePadding(),
            content = { content(); if (scroll) Spacer(Modifier.height(30.dp)) })
    }
}

/** A sheet from the bottom, optionally under a banner picture. */
@Composable
fun Sheet(onDismiss: () -> Unit, banner: String? = null, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onDismiss)
    Box(Modifier.fillMaxSize().background(Color(0x88000000)).tap(onDismiss), contentAlignment = Alignment.BottomCenter) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)).background(C.bg).tap {}.navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState())) {
            if (banner != null) Art(banner, Modifier.fillMaxWidth().height(120.dp), ContentScale.Crop)
            Column(Modifier.padding(20.dp)) {
                if (banner == null) { Box(Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(4.dp).clip(RoundedCornerShape(50)).background(C.line)); Spacer(Modifier.height(12.dp)) }
                content()
            }
        }
    }
}

/** A card in the middle of a dim screen, as its own window. */
@Composable
fun Popup(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.ui.window.Dialog(onDismiss, androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().tap(onDismiss).imePadding(), contentAlignment = Alignment.Center) {
            Column(Modifier.padding(24.dp).fillMaxWidth().card(24.dp).tap {}.verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, content = content)
        }
    }
}

@Composable
fun Field(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, number: Boolean = false, single: Boolean = true) {
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(C.card2).padding(horizontal = 12.dp, vertical = 11.dp)) {
        if (value.isEmpty()) Text(hint, style = T.body.copy(color = C.mute))
        BasicTextField(value, onChange, modifier = Modifier.fillMaxWidth(), textStyle = T.body.copy(color = C.ink), singleLine = single,
            keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default, cursorBrush = SolidColor(C.acc))
    }
}

@Composable
fun Toggle(title: String, sub: String? = null, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!on) }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, style = T.bodyB); if (sub != null) Text(sub, style = T.small) }
        Switch(on, onChange, colors = SwitchDefaults.colors(checkedTrackColor = C.acc, checkedThumbColor = C.accInk, uncheckedTrackColor = C.card2, uncheckedBorderColor = C.line))
    }
}

/** − value + */
@Composable
fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = T.bodyB, modifier = Modifier.weight(1f))
        Box(Modifier.size(36.dp).clip(CircleShape).background(C.card2).clickable(onClick = onMinus), contentAlignment = Alignment.Center) { Text("−", style = T.btn.copy(color = C.ink)) }
        Text(value, style = T.num, modifier = Modifier.width(76.dp), textAlign = TextAlign.Center)
        Box(Modifier.size(36.dp).clip(CircleShape).background(C.card2).clickable(onClick = onPlus), contentAlignment = Alignment.Center) { Text("+", style = T.btn.copy(color = C.ink)) }
    }
}

/** A leaf count with its coin. */
@Composable
fun Leaves(n: Int, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Art("icon_leaf", Modifier.size(size)); Spacer(Modifier.width(3.dp)); Text("$n", style = T.num)
    }
}

/** A banner picture with a paper ribbon for the page title. */
@Composable
fun Banner(name: String, title: String, sub: String, modifier: Modifier = Modifier, aside: @Composable BoxScope.() -> Unit = {}) {
    Box(modifier.fillMaxWidth().height(186.dp)) {
        Box(Modifier.fillMaxWidth().height(168.dp)) {
            Art(name, Modifier.fillMaxSize(), ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, .55f to Color.Transparent, 1f to Color(0x553B2E26))))
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp).background(C.woodDark))
            aside()
        }
        Column(Modifier.align(Alignment.BottomStart).padding(start = 14.dp).shadow(3.dp, RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp)).background(C.bg).padding(horizontal = 14.dp, vertical = 6.dp)) {
            Text(title, style = T.h2.copy(fontSize = androidx.compose.ui.unit.TextUnit(21f, androidx.compose.ui.unit.TextUnitType.Sp)))
            Text(sub, style = T.tiny)
        }
    }
}

@Composable
fun Empty(text: String, modifier: Modifier = Modifier) = Text(text, style = T.small, modifier = modifier.fillMaxWidth().padding(vertical = 18.dp), textAlign = TextAlign.Center)

fun lerp(a: Color, b: Color, t: Float) = Color(a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, a.alpha + (b.alpha - a.alpha) * t)

/** Remembers a toggled state set. */
@Composable
fun rememberSet(init: Set<String>): androidx.compose.runtime.MutableState<Set<String>> = remember { mutableStateOf(init) }

@Composable
fun Hint(text: String, modifier: Modifier = Modifier, onClose: () -> Unit) {
    var open by remember { mutableStateOf(true) }
    if (!open) return
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.card2).padding(12.dp), verticalAlignment = Alignment.Top) {
        Text(text, style = T.small.copy(color = C.ink), modifier = Modifier.weight(1f))
        Text("×", style = T.btn.copy(color = C.mute), modifier = Modifier.clickable { open = false; onClose() }.padding(start = 10.dp))
    }
}
