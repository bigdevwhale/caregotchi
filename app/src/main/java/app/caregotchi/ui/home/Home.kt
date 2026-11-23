package app.caregotchi.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.res.imageResource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.caregotchi.BuildConfig
import app.caregotchi.container
import app.caregotchi.data.Outside
import app.caregotchi.domain.Mood
import app.caregotchi.domain.Shop
import app.caregotchi.domain.Stage
import app.caregotchi.domain.dur
import app.caregotchi.domain.fmt1
import app.caregotchi.domain.g
import app.caregotchi.domain.hourOf
import app.caregotchi.domain.today
import app.caregotchi.domain.tr
import app.caregotchi.domain.weekStart
import app.caregotchi.ui.Data
import app.caregotchi.ui.Nav
import app.caregotchi.ui.Screen
import app.caregotchi.ui.components.Art
import app.caregotchi.ui.components.Leaves
import app.caregotchi.ui.components.Ring
import app.caregotchi.ui.components.lip
import app.caregotchi.ui.components.tap
import app.caregotchi.ui.theme.C
import app.caregotchi.ui.theme.T
import app.caregotchi.work.Widget
import app.caregotchi.work.mealNow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A frame to show for a moment after a mark: the pet eats, drinks, holds its pill, cheers. */
class Act { var frame by mutableIntStateOf(-1); var line by mutableStateOf<String?>(null) }

@Composable
fun HomeScreen(d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val act = remember { Act() }
    val floats = remember { mutableStateListOf<Pair<Long, app.caregotchi.Reward>>() }
    LaunchedEffect(Unit) { c.rewards.collect { r -> val k = System.nanoTime(); floats.add(k to r); scope.launch { delay(1500); floats.removeAll { it.first == k } } } }
    LaunchedEffect(act.frame) { if (act.frame >= 0) { delay(2200); act.frame = -1 } }
    fun mark(kind: String, frame: Int, line: String, sub: String = "", after: (Long) -> Unit = {}) = scope.launch {
        act.frame = frame; act.line = line
        val r = c.log(kind, sub = sub)
        after(r.id)
    }

    val onEat = { mark("eat", 3, tr("Ням! Каша – лучшая еда для нас обоих.", "Yum! Porridge is the best food for us both."), mealNow(d.s.mealTimes)) { id -> nav.sheet = "rate:$id" } }
    val onWater = { mark("water", 4, tr("Буль-буль. Вода – это хорошо.", "Glug glug. Water is good.")) }
    // food or water pressed on the widget: the app opens and it's done here, with the pet's word and the meal's sheet
    LaunchedEffect(app.caregotchi.ui.Start.act) {
        when (app.caregotchi.ui.Start.act) { "eat" -> onEat(); "water" -> onWater() }
        app.caregotchi.ui.Start.act = null
    }
    Column(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            Room(d, act, Modifier.fillMaxWidth().requiredHeight(maxWidth * 1.5f).align(Alignment.TopCenter), nav)
            Hud(d, nav, Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 6.dp))
            Corners(d, nav, Modifier.align(Alignment.BottomCenter))
            for ((k, r) in floats) androidx.compose.runtime.key(k) { Float(r, Modifier.align(Alignment.Center)) }
        }
        Panel(d, nav,
            onEat = { onEat() },
            onWater = { onWater() },
            // always the list from the sill, so you pick what you took (#20): with two medicines due, the button used to note the first
            onPill = { nav.sheet = "pill" },
            onMove = { nav.sheet = "move" },
            onSleep = {
                if (d.book.asleep) mark("wake", 6, tr("Доброе утро! Потягушки.", "Good morning! Big stretch."))
                else mark("sleep", 8, tr("Спокойной ночи. Сворачиваюсь клубком.", "Good night. Curling up."))
            })
    }
}

/** The hollow: the room picture with the bought things, the medicine pots on the sill, the pet and its words. */
@Composable
private fun Room(d: Data, act: Act, modifier: Modifier, nav: Nav) {
    val s = d.s
    BoxWithConstraints(modifier) {
        val w = maxWidth; val h = maxHeight
        Art("room_bg", Modifier.fillMaxSize(), ContentScale.FillBounds)
        // the bought things, back to front
        for (it in Shop.cozy.filter { it.id in s.items && it.id !in s.hidden }.sortedBy { it.y }) {
            // the height follows the picture's own shape: with the width alone a wide picture (the garland, #17) kept its
            // pixel height and was shrunk to a fifth of the room
            val id = app.caregotchi.ui.components.resId("item_${it.id}")
            val ratio = if (id == 0) 1f else androidx.compose.ui.graphics.ImageBitmap.imageResource(id).let { b -> b.width.toFloat() / b.height }
            Art("item_${it.id}", Modifier.offset(w * it.x, h * it.y).width(w * it.w).aspectRatio(ratio))
        }
        // one pot a regular medicine on the sill
        val pots = d.activeMeds.filter { !it.asNeeded }.take(4)
        if (pots.isNotEmpty()) Row(Modifier.align(Alignment.TopCenter).offset(y = h * .365f - h * .075f).height(h * .075f), horizontalArrangement = Arrangement.spacedBy(w * .015f), verticalAlignment = Alignment.Bottom) {
            for (m in pots) Art("pot_${m.herb}_${d.book.potStage(m)}", Modifier.height(h * .075f).width(w * .1f).clickable { nav.push(Screen.MedEdit(m.id)) })
        }
        // dusk and night through the round window
        val hr = hourOf(d.book.now)
        val night = when (hr) { in 21..23, in 0..5 -> .38f; 20, 6 -> .2f; else -> 0f }
        if (night > 0) Box(Modifier.fillMaxSize().background(Color(0xFF0D1430).copy(alpha = night)))
        // the pet on the rug
        val pet = d.pet
        val mood = d.book.health.mood
        if (pet != null && mood != Mood.AWAY) {
            val stage = Stage.of(pet.xp)
            val asleep = d.book.asleep
            val idle = Widget.frame(pet.species, stage, mood, asleep)
            val frame = if (act.frame >= 0) "pet_${pet.species}_${act.frame}" else idle
            val small = if (act.frame >= 0 || mood == Mood.SICK || mood == Mood.BED || asleep) when (stage) { 0 -> .7f; 1 -> .85f; else -> 1f } else 1f
            val size = w * (if (stage == 3) .4f else .36f) * small
            val t = rememberInfiniteTransition(label = "bob")
            val k by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse), label = "k")
            val bob = if (BuildConfig.SHOTS || asleep) 0f else k
            Art(frame, Modifier.align(Alignment.TopCenter).offset(y = h * .80f - size).size(size)
                .graphicsLayer { scaleX = 1f + .03f * bob; scaleY = 1f - .03f * bob; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(.5f, 1f)
                    if (mood == Mood.LOW) { alpha = .92f } }
                .tap { nav.push(Screen.PetInfo(pet.id)) })
            val line = act.line?.takeIf { act.frame >= 0 } ?: talk(d)
            Text(line, style = T.smallB.copy(color = C.inkPaper), textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = h * .80f - size - 58.dp).widthIn(max = 260.dp)
                    .shadow(4.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp)).background(C.paper).padding(horizontal = 12.dp, vertical = 8.dp))
        } else if (mood == Mood.AWAY) {
            Text(tr("${pet?.name ?: ""} гостит у бабушки в деревне. На столе записка: «Три дня заботы – и я вернусь».", "${pet?.name ?: ""} is at grandma's in the village. The note says: «Three days of care and I'll be back»."),
                style = T.note, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.TopCenter).offset(y = h * .58f).padding(horizontal = 40.dp)
                    .clip(RoundedCornerShape(4.dp)).background(C.paper).padding(14.dp))
        } else if (d.eggs.isNotEmpty()) {
            Art("egg_${d.eggs.first().species}", Modifier.align(Alignment.TopCenter).offset(y = h * .80f - w * .2f).size(w * .2f))
        }
    }
}

private val C.inkPaper get() = Color(0xFF3B2E26)

/** What the pet says when nothing has just happened. */
fun talk(d: Data): String {
    val b = d.book; val n = b.needs; val s = d.s
    val o = Outside.of(s.outside)
    val hr = hourOf(b.now)
    if (d.open != null) return tr("Я рядом. Отпустит – нажми «Отпустило».", "I'm here. When it eases, say so.")
    if (b.asleep) return tr("Хр-р… (спит)", "Zzz… (asleep)")
    if (today() in s.plaidDays) return tr("Сегодня под пледом. Ничего не надо – просто отдыхай.", "A blanket day. Nothing to do – just rest.")
    when (b.health.mood) {
        Mood.SICK -> return tr("Апчхи. Два хороших дня – и я поправлюсь.", "Achoo. Two good days and I'll be well.")
        Mood.BED -> return tr("Совсем расклеиваюсь… Поешь, попей и выпей лекарства – мне сразу легче.", "I'm laid up… Eat, drink, take your medicine – it helps me too.")
        else -> {}
    }
    if (s.pressureWarn && o != null && o.fall <= -6) return tr("Давление падает на ${fmt1(-o.fall)} гПа. Держи таблетку под рукой.", "The pressure is falling ${fmt1(-o.fall)} hPa. Keep a pill close.")
    // a dose missed long ago isn't asked for all day (#21): three hours after its time the pet lets it go
    val due = b.dosesLeft().firstOrNull { it.second <= b.nowMin && b.nowMin - it.second <= 180 }
    if (due != null) return tr("Пора «${due.first.name}». Я жду свою ягодку!", "Time for «${due.first.name}». I'm waiting for my berry!")
    val t = b.todayCare
    return when {
        hr in 5..10 && !t.breakfast -> tr("Доброе утро! Позавтракаем? Я – кашу.", "Good morning! Breakfast? Porridge for me.")
        n.eat < .3 -> tr("Есть хочется… Давно мы не ели.", "I'm hungry… We haven't eaten for a while.")
        n.water < .4 -> tr("Хочется пить. Стаканчик воды?", "I'm thirsty. A glass of water?")
        hr in 12..19 && t.moveMins == 0 -> tr("Может, пройдёмся? Хоть десять минут.", "A walk, maybe? Even ten minutes.")
        t.junk > 0 && !t.veg -> tr("Вкусно было! А овощей сегодня ещё не было.", "That was tasty! No vegetables yet today though.")
        t.caffeine > 400 -> tr("Сегодня уже ${t.caffeine} мг кофеина – сердечку хватит. Может, воды?", "${t.caffeine} mg of caffeine today – that's plenty. Some water?")
        t.lateCaffeine && hr in 16..21 -> tr("Кофе после четырёх может помешать уснуть. Я буду пить чай с ромашкой!", "Coffee after four can keep you awake. I'll have chamomile tea!")
        hr >= 22 -> tr("Зеваю… Пора бы ложиться.", "Yawn… Time for bed soon.")
        n.min > .8 -> tr("Мне хорошо! Спасибо, что заботишься о нас.", "I feel great! Thank you for looking after us.")
        else -> tr("Я тут. Как ты?", "I'm here. How are you?")
    }.let(::g)
}

@Composable
private fun Hud(d: Data, nav: Nav, modifier: Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        val (lvl, into, need) = d.level
        Row(Modifier.shadow(2.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50)).background(C.paper.copy(alpha = .95f)).clickable { nav.tab = 3 }
            .padding(start = 4.dp, end = 14.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val sw = 4.dp.toPx(); val r = Size(size.width - sw, size.height - sw)
                    drawCircle(Color(0xFFFFF3D6)); drawArc(Color(0xFFEFE0BF), 0f, 360f, false, Offset(sw / 2, sw / 2), r, style = Stroke(sw))
                    drawArc(Color(0xFFD9A441), -90f, 360f * into / need, false, Offset(sw / 2, sw / 2), r, style = Stroke(sw, cap = StrokeCap.Round))
                }
                Text("$lvl", style = T.h3.copy(color = C.inkPaper))
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(d.pet?.name ?: tr("Заботогочи", "Caregotchi"), style = T.h3.copy(color = C.inkPaper))
                Text("${Stage.name(d.stage)} · $into/$need", style = T.tiny.copy(color = Color(0xFF7A6656)))
            }
        }
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).shadow(2.dp, CircleShape).clip(CircleShape).background(C.paper.copy(alpha = .95f)).clickable { nav.push(Screen.Settings) }, contentAlignment = Alignment.Center) {
                    Art("icon_gear", Modifier.size(24.dp))
                }
                Spacer(Modifier.width(6.dp))
                Pill(Modifier.clickable { nav.tab = 4 }) { Leaves(d.s.leaves, size = 22.dp, color = C.inkPaper) /* the pill is paper in both themes (#16) */ }
            }
            Pill { Art("icon_flame", Modifier.size(22.dp)); Spacer(Modifier.width(3.dp)); Text(tr("${d.book.streak} дн.", "${d.book.streak} d"), style = T.num.copy(color = C.inkPaper)) }
            Outside.of(d.s.outside)?.let { o ->
                Pill(Modifier.clickable { nav.push(Screen.Settings) }) {
                    Text("${if (o.temp.isNaN()) "" else "%+d°".format(o.temp.toInt())}${if (o.pressure.isNaN()) "" else " · ${o.pressure.toInt()} " + tr("гПа", "hPa")}", style = T.tiny.copy(color = Color(0xFF7A6656)))
                    if (o.fall <= -4) Text(" ↓${(-o.fall).toInt()}", style = T.tiny.copy(color = Color(0xFFA33B2C)))
                }
            } ?: Pill(Modifier.clickable { nav.push(Screen.Settings) }) { Art("icon_gear", Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun Pill(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(modifier.shadow(2.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50)).background(C.paper.copy(alpha = .95f)).padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically) { content() }
}

/** The day's tasks (a scroll) and the week's chest in the bottom corners of the room. */
@Composable
private fun Corners(d: Data, nav: Nav, modifier: Modifier) {
    val done = d.quests.count { d.s.questDay == today() && it.id in d.s.questsDone }
    val monday = weekStart(today())
    val good = (monday..today()).count { d.book.care(it)?.good == true }
    val ready = good >= 5 && d.s.chestWeek != monday
    val t = rememberInfiniteTransition(label = "chest")
    val wig by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(260), RepeatMode.Reverse), label = "w")
    Row(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.Bottom) {
        Corner("icon_scroll", "$done/${d.quests.size}") { nav.sheet = "quests" }
        Spacer(Modifier.weight(1f))
        if (d.open != null) Row(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF3A1D18)).clickable { nav.dark = d.open.id }.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Text(openLabel(d.open), style = T.smallB.copy(color = Color(0xFFE8B0A5)))
        }
        Spacer(Modifier.weight(1f))
        Corner("icon_chest", if (d.s.chestWeek == monday) "✓" else "$good/5", Modifier.graphicsLayer { rotationZ = if (ready && !BuildConfig.SHOTS) wig * 6f else 0f }) { nav.sheet = "chest" }
    }
}

@Composable
private fun Corner(icon: String, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(Modifier.tap(onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Art(icon, modifier.size(50.dp))
        Text(label, style = T.tiny.copy(color = Color.White), modifier = Modifier.offset(y = (-6).dp).clip(RoundedCornerShape(50)).background(Color(0xFF6B4A3A)).padding(horizontal = 8.dp, vertical = 1.dp))
    }
}

@Composable
private fun Float(r: app.caregotchi.Reward, modifier: Modifier) {
    val t = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(1400)) }
    Row(modifier.graphicsLayer { translationY = -t.value * 220f; alpha = 1f - t.value * t.value }, verticalAlignment = Alignment.CenterVertically) {
        if (r.leaves > 0) { Text("+${r.leaves}", style = T.btn.copy(color = Color.White, fontSize = androidx.compose.ui.unit.TextUnit(20f, androidx.compose.ui.unit.TextUnitType.Sp), shadow = androidx.compose.ui.graphics.Shadow(Color(0xFF6B4A3A), Offset(0f, 3f), 6f))); Art("icon_leaf", Modifier.size(28.dp)) }
        if (r.xp > 0) { Spacer(Modifier.width(8.dp)); Text("+${r.xp}", style = T.btn.copy(color = Color.White, fontSize = androidx.compose.ui.unit.TextUnit(20f, androidx.compose.ui.unit.TextUnitType.Sp), shadow = androidx.compose.ui.graphics.Shadow(Color(0xFF6B4A3A), Offset(0f, 3f), 6f))); Art("icon_star", Modifier.size(28.dp)) }
    }
}

/** The five needs, the five quick marks and «I feel bad». */
@Composable
private fun Panel(d: Data, nav: Nav, onEat: () -> Unit, onWater: () -> Unit, onPill: () -> Unit, onMove: () -> Unit, onSleep: () -> Unit) {
    val n = d.book.needs
    Column(Modifier.fillMaxWidth().shadow(10.dp, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp), ambientColor = C.shadow, spotColor = C.shadow)
        .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)).background(C.bg).padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp)) {
        val needs = listOf(Triple("eat", tr("Сытость", "Fed"), n.eat to C.honey), Triple("water", tr("Вода", "Water"), n.water to C.water),
            Triple("pill", tr("Лекарства", "Medicine"), n.pill to C.rose), Triple("walk", tr("Движение", "Moving"), n.move to C.sage), Triple("sleep", tr("Сон", "Sleep"), n.sleep to C.sleep))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for ((k, label, vc) in needs) Column(Modifier.weight(1f).tap { nav.sheet = "need:$k" }, horizontalAlignment = Alignment.CenterHorizontally) {
                Ring(vc.first.toFloat(), vc.second, "icon_$k", size = 54.dp)
                Text(label, style = T.tiny, maxLines = 1)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            QuickBtn("icon_eat", tr("Поел{|а}", "Ate").let(::g), onEat)
            QuickBtn("icon_water", tr("Вода", "Water"), onWater)
            QuickBtn("icon_pill", tr("Таблетка", "Pill"), onPill)
            QuickBtn("icon_walk", tr("Спорт", "Sport"), onMove)
            QuickBtn(if (d.book.asleep) "icon_sunrise" else "icon_sleep", if (d.book.asleep) tr("Подъём", "Up") else tr("Отбой", "Bed"), onSleep)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val mg = d.book.todayCare.caffeine
            Row(Modifier.lip(C.card2, lerpDark(C.card2)).clickable { nav.sheet = "caf" }.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Art("drink_coffee", Modifier.size(30.dp)); Spacer(Modifier.width(4.dp))
                Text(if (mg > 0) tr("$mg мг", "$mg mg") else tr("Кофе", "Coffee"), style = T.smallB.copy(color = if (mg > 400) C.bad else C.ink))
            }
            Row(Modifier.weight(1f).lip(C.badSoft, lerpDark(C.badSoft)).clickable { if (d.open != null) nav.dark = d.open.id else nav.sheet = "bad" }.padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Art("icon_heart", Modifier.size(26.dp)); Spacer(Modifier.width(8.dp))
                Text(if (d.open != null) openLabel(d.open) + tr(" · открыть", " · open") else tr("Мне плохо", "I feel bad"), style = T.btn.copy(color = C.bad), maxLines = 1)
            }
        }
    }
}

private fun lerpDark(c: Color) = app.caregotchi.ui.components.lerp(c, Color.Black, .12f)

@Composable
private fun androidx.compose.foundation.layout.RowScope.QuickBtn(icon: String, label: String, onClick: () -> Unit) {
    Column(Modifier.weight(1f).lip(C.card2, lerpDark(C.card2), 14.dp).clickable(onClick = onClick).padding(vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Art(icon, Modifier.size(34.dp))
        Text(label, style = T.tiny.copy(color = C.ink), maxLines = 1)
    }
}


/** «Мигрень · идёт 8 ч»: what the open bad spell is and how long it has lasted. */
fun openLabel(a: app.caregotchi.data.Attack): String {
    val sec = ((System.currentTimeMillis() - a.start) / 1000).toInt()
    val long = if (sec >= 3600) tr("${sec / 3600} ч", "${sec / 3600} h") else dur(sec)
    return app.caregotchi.domain.Troubles.title(a.kind) + tr(" · идёт $long", " · $long so far")
}
