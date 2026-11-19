package app.caregotchi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.caregotchi.R

/** Colour tokens: oatmeal paper, cocoa ink, honey, sage, dusty rose; at night the hollow by candlelight. */
class Pal(
    val dark: Boolean, val bg: Color, val card: Color, val card2: Color, val ink: Color, val mute: Color, val line: Color,
    val acc: Color, val accInk: Color, val honey: Color, val sage: Color, val rose: Color, val bad: Color, val badSoft: Color,
    val water: Color, val sleep: Color, val wood: Color, val woodDark: Color, val woodLight: Color, val shadow: Color, val okSoft: Color,
)

private fun c(hex: Long) = Color(0xFF000000 or hex)

val Light = Pal(false, c(0xFBF6EC), c(0xFFFCF5), c(0xF3EAD9), c(0x3B2E26), c(0x7A6656), c(0xE3D6BF),
    c(0x6B4A3A), c(0xFFFFFF), c(0xD9A441), c(0x8FA98A), c(0xC98B86), c(0x9A3A2C), c(0xEAD2CD),
    c(0x7FB0C9), c(0x8F88B8), c(0xA8743F), c(0x6F4520), c(0xD4A066), Color(0x33583A20), c(0xE7EFE2))

val Dark = Pal(true, c(0x1F1714), c(0x2A201B), c(0x352820), c(0xF2E6D6), c(0xB39E8A), c(0x45352B),
    c(0xE0B070), c(0x2A1C14), c(0xE0B070), c(0x9DBB97), c(0xD79C96), c(0xE07A6A), c(0x4A2A24),
    c(0x8DBDD6), c(0xA59FD0), c(0x7A5330), c(0x4A2E14), c(0xA07A4C), Color(0x66000000), c(0x2C3A2A))

object C {
    @Volatile var p: Pal = Light
    val dark get() = p.dark
    val bg get() = p.bg; val card get() = p.card; val card2 get() = p.card2; val ink get() = p.ink; val mute get() = p.mute; val line get() = p.line
    val acc get() = p.acc; val accInk get() = p.accInk; val honey get() = p.honey; val sage get() = p.sage; val rose get() = p.rose
    val bad get() = p.bad; val badSoft get() = p.badSoft; val water get() = p.water; val sleep get() = p.sleep
    val wood get() = p.wood; val woodDark get() = p.woodDark; val woodLight get() = p.woodLight; val shadow get() = p.shadow; val okSoft get() = p.okSoft
    val paper = Color(0xFFFFFAF0)
    val inkPaper = Color(0xFF3B2E26)
}

val Nunito = FontFamily(Font(R.font.nunito_regular, FontWeight.Normal), Font(R.font.nunito_semibold, FontWeight.SemiBold), Font(R.font.nunito_bold, FontWeight.Bold), Font(R.font.nunito_bold, FontWeight.ExtraBold))
val Lora = FontFamily(Font(R.font.lora_regular, FontWeight.Normal), Font(R.font.lora_medium, FontWeight.Medium), Font(R.font.lora_medium, FontWeight.SemiBold), Font(R.font.lora_medium, FontWeight.Bold))

object T {
    val title get() = TextStyle(fontFamily = Lora, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 28.sp, color = C.ink)
    val h2 get() = TextStyle(fontFamily = Lora, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 25.sp, color = C.ink)
    val h3 get() = TextStyle(fontFamily = Lora, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 21.sp, color = C.ink)
    val body get() = TextStyle(fontFamily = Nunito, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink)
    val bodyB get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink)
    val small get() = TextStyle(fontFamily = Nunito, fontSize = 13.sp, lineHeight = 17.sp, color = C.mute)
    val smallB get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 17.sp, color = C.ink)
    val tiny get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp, color = C.mute)
    val sec get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 1.sp, color = C.mute)
    val num get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = C.ink)
    val btn get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    val note get() = TextStyle(fontFamily = Lora, fontSize = 15.sp, lineHeight = 21.sp, color = C.inkPaper)
}

@Composable
fun MendTheme(dark: Boolean, content: @Composable () -> Unit) {
    C.p = if (dark) Dark else Light
    val p = C.p
    val scheme = if (dark) darkColorScheme(primary = p.acc, background = p.bg, surface = p.card, onSurface = p.ink, onBackground = p.ink)
    else lightColorScheme(primary = p.acc, background = p.bg, surface = p.card, onSurface = p.ink, onBackground = p.ink)
    MaterialTheme(colorScheme = scheme, content = content)
}
