package app.caregotchi.ui.diary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.caregotchi.container
import app.caregotchi.data.Mark
import app.caregotchi.domain.Troubles
import app.caregotchi.domain.dayOf
import app.caregotchi.domain.dur
import app.caregotchi.domain.fmt1
import app.caregotchi.domain.hhmm
import app.caregotchi.domain.minuteOf
import app.caregotchi.domain.monthName
import app.caregotchi.domain.startOf
import app.caregotchi.domain.today
import app.caregotchi.domain.tr
import app.caregotchi.domain.weekday
import app.caregotchi.ui.Data
import app.caregotchi.ui.Nav
import app.caregotchi.ui.Screen
import app.caregotchi.ui.components.Art
import app.caregotchi.ui.components.PicGrid
import app.caregotchi.ui.components.PicTile
import app.caregotchi.ui.components.Ring
import app.caregotchi.ui.components.tap
import app.caregotchi.ui.home.markText
import app.caregotchi.ui.theme.C
import app.caregotchi.ui.theme.T
import kotlinx.coroutines.launch
import java.time.LocalDate

/** One thing on the day's thread: a mark or a bad spell. */
private class Event(val at: Long, val icon: String, val text: String, val mark: Mark? = null, val spell: app.caregotchi.data.Attack? = null)

/**
 * A day as a diary spread: a tear-off calendar leaf with the day's fabric and the pet's word, five small rings,
 * the day's thread from morning to night with everything on it, the weather, and tiles for what was forgotten.
 */
@Composable
fun DayPage(day0: Long, d: Data, nav: Nav) {
    var day by remember(day0) { mutableLongStateOf(day0) }
    BackHandler { nav.pop() }
    Column(Modifier.fillMaxSize().background(C.bg).tap {}.statusBarsPadding().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("←", style = T.h2, modifier = Modifier.clip(CircleShape).clickable { nav.pop() }.padding(10.dp))
            Spacer(Modifier.weight(1f))
            Text("‹", style = T.title, modifier = Modifier.clip(CircleShape).clickable { day-- }.padding(horizontal = 16.dp))
            Text("›", style = T.title.copy(color = if (day < today()) C.ink else C.line), modifier = Modifier.clip(CircleShape).clickable(enabled = day < today()) { day++ }.padding(horizontal = 16.dp))
        }
        Leaf(day, d)
        Rings(day, d)
        DayThread(day, d, nav)
        Weather(day, d)
        if (day <= today()) Forgot(day, d)
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun Leaf(day: Long, d: Data) {
    val care = d.book.care(day)
    val date = LocalDate.ofEpochDay(day)
    val pet = d.pet
    val (frame, word) = when {
        care == null || !care.any && !care.plaid -> 8 to tr("Этот день прошёл без отметок. Бывает.", "No marks this day. That happens.")
        care.plaid -> 7 to tr("День под пледом – отдыхали и выздоравливали.", "A blanket day – resting and getting well.")
        care.attacks > 0 && care.good -> 2 to tr("Было тяжело, но мы держались молодцом.", "It was hard, but we held on well.")
        care.score >= .8 -> 6 to tr("Отличный день – всё успели!", "A great day – we did it all!")
        care.good -> 2 to tr("Хороший день. Почти всё сделали.", "A good day. Nearly all done.")
        else -> 7 to tr("Заботы было маловато… Завтра наверстаем.", "A bit little care… We'll catch up tomorrow.")
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // a tear-off calendar leaf
        Column(Modifier.width(118.dp).rotate(-2f).shadow(5.dp, RoundedCornerShape(6.dp)).clip(RoundedCornerShape(6.dp)).background(C.paper)) {
            Box(Modifier.fillMaxWidth().height(26.dp).background(Color(0xFFB0342A)), contentAlignment = Alignment.Center) {
                Text(monthName(date.monthValue, false).uppercase(), style = T.tiny.copy(color = Color.White))
            }
            Text("${date.dayOfMonth}", style = T.title.copy(color = Color(0xFF3B2E26), fontSize = androidx.compose.ui.unit.TextUnit(46f, androidx.compose.ui.unit.TextUnitType.Sp)),
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text(weekday(day), style = T.small.copy(color = Color(0xFF7A6656)), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), textAlign = TextAlign.Center)
            // the day's fabric along the bottom
            Box(Modifier.fillMaxWidth().height(14.dp).drawBehind { fabric(clothOf(care)) })
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            if (pet != null) Art("pet_${pet.species}_$frame", Modifier.size(84.dp))
            Box(Modifier.shadow(3.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp)).background(C.paper).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(word, style = T.smallB.copy(color = Color(0xFF3B2E26)))
            }
        }
    }
}

@Composable
private fun Rings(day: Long, d: Data) {
    val t = d.book.care(day) ?: return
    val s = d.s
    val items = listOf(Triple("icon_eat", tr("еда ${t.meals}", "food ${t.meals}"), (t.meals / 3f) to C.honey),
        Triple("icon_water", tr("вода ${t.water}/${s.waterGoal}", "water ${t.water}/${s.waterGoal}"), (t.water / s.waterGoal.toFloat()) to C.water),
        Triple("icon_pill", if (t.due > 0) "${t.taken}/${t.due}" else tr("нет", "none"), (if (t.due > 0) t.taken / t.due.toFloat() else 1f) to C.rose),
        Triple("icon_walk", tr("${t.moveMins} мин", "${t.moveMins} min"), (t.moveMins / s.moveGoal.toFloat()) to C.sage),
        Triple("icon_sleep", if (t.slept >= 0) tr("${fmt1(t.slept)} ч", "${fmt1(t.slept)} h") else "—", (if (t.slept >= 0) (t.slept / s.sleepGoal).toFloat() else 0f) to C.sleep))
    Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        items.forEach { (icon, label, v) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Ring(v.first.coerceIn(0f, 1f), v.second, icon, size = 54.dp)
                Text(label, style = T.tiny, maxLines = 1)
            }
        }
    }
}

/** The day's thread from morning to night: marks on either side, a red band for each bad spell, the night's sleep as a moon band. */
@Composable
private fun DayThread(day: Long, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val events = d.book.marksOn(day).map { Event(it.at, markIcon(it), markText(it, d), mark = it) } +
        d.book.attacksOn(day).map { a -> Event(a.start, "icon_${Troubles.of(a.kind).icon}", Troubles.title(a.kind) + (if (a.strength > 0) " · ${a.strength}/10" else "") +
            (if (a.end > a.start) " · " + dur(((a.end - a.start) / 1000).toInt()) else tr(" · идёт", " · ongoing")), spell = a) }
    Text(tr("Нить дня", "The day's thread"), style = T.h3, modifier = Modifier.padding(top = 20.dp))
    d.book.periods.lastOrNull { day in dayOf(it.start)..d.book.periodLast(it) }?.let { p ->
        Row(Modifier.padding(top = 6.dp).clip(RoundedCornerShape(50)).background(Color(0x33C98B86)).clickable { nav.push(Screen.AttackEdit(p.id)) }.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Art("icon_period", Modifier.size(22.dp)); Spacer(Modifier.width(6.dp))
            Text(tr("Месячные · день ${day - dayOf(p.start) + 1}", "Period · day ${day - dayOf(p.start) + 1}"), style = T.smallB)
        }
    }
    if (events.isEmpty()) Text(tr("Ни одной отметки.", "Not a single mark."), style = T.small, modifier = Modifier.padding(vertical = 10.dp))
    else Thread(events, d, nav, scope, c::undo)
    app.caregotchi.ui.components.Ghost(tr("+ Записать на этот день", "+ Add to this day"), Modifier.fillMaxWidth().padding(top = 10.dp)) { nav.sheet = "add:$day" }
    if (events.any { it.mark != null }) Text(tr("Нажмите на запись, чтобы поправить время или подробности.", "Tap a record to change its time or details."), style = T.tiny, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun Thread(events: List<Event>, d: Data, nav: Nav, scope: kotlinx.coroutines.CoroutineScope, undo: suspend (Mark) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp).drawBehind {
        val x = size.width / 2
        drawLine(Color(0xFFC98B86), Offset(x, 0f), Offset(x, size.height), 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 9f)))
    }) {
        events.sortedBy { it.at }.forEachIndexed { i, e ->
            val left = i % 2 == 0
            val spell = e.spell
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f), contentAlignment = if (left) Alignment.CenterEnd else Alignment.CenterStart) {
                    if (left) Card(e, d, nav, scope, undo) else Text(hhmm(minuteOf(e.at)), style = T.smallB.copy(color = C.mute), modifier = Modifier.padding(start = 10.dp))
                }
                Box(Modifier.size(if (spell != null) 18.dp else 12.dp).clip(CircleShape).background(if (spell != null) Color(0xFFB0342A) else C.honey).border(2.dp, C.bg, CircleShape))
                Box(Modifier.weight(1f), contentAlignment = if (left) Alignment.CenterEnd else Alignment.CenterStart) {
                    if (!left) Card(e, d, nav, scope, undo) else Text(hhmm(minuteOf(e.at)), style = T.smallB.copy(color = C.mute), modifier = Modifier.padding(end = 10.dp))
                }
            }
        }
    }
}

@Composable
private fun Card(e: Event, d: Data, nav: Nav, scope: kotlinx.coroutines.CoroutineScope, undo: suspend (Mark) -> Unit) {
    val spell = e.spell
    Row(Modifier.padding(horizontal = 8.dp).shadow(3.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp))
        .background(if (spell != null) Color(0xFFF3D9D5) else C.paper)
        .clickable { when { spell != null -> nav.push(Screen.AttackEdit(spell.id)); e.mark != null -> nav.sheet = "mark:${e.mark.id}" } }
        .padding(start = 6.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Art(e.icon, Modifier.size(30.dp))
        Text(e.text, style = T.tiny.copy(color = Color(0xFF3B2E26)), maxLines = 3, modifier = Modifier.padding(start = 6.dp).width(92.dp))
        if (e.mark != null) Text("×", style = T.smallB.copy(color = Color(0xFFB0A090)), modifier = Modifier.clip(CircleShape).clickable { scope.launch { undo(e.mark) } }.padding(4.dp))
    }
}

@Composable
private fun Weather(day: Long, d: Data) {
    val a = d.book.air(day) ?: return
    val swing = a.max - a.min
    Row(Modifier.fillMaxWidth().padding(top = 18.dp).shadow(2.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp))
        .background(Brush.horizontalGradient(listOf(Color(0xFFDCE8EE), C.card))).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Art("icon_baro", Modifier.size(40.dp))
        Column(Modifier.padding(start = 10.dp)) {
            Text(tr("Давление ${a.min.toInt()}–${a.max.toInt()} гПа", "Pressure ${a.min.toInt()}–${a.max.toInt()} hPa"), style = T.bodyB.copy(color = Color(0xFF2E3A44)))
            Text(if (swing >= 6) tr("Скакало на ${swing.toInt()} гПа – сыщик это учтёт", "Swung by ${swing.toInt()} hPa – the detective notes it") else tr("Ровное, без скачков", "Steady"), style = T.tiny.copy(color = Color(0xFF4A5A66)))
        }
    }
}

/** What may have been forgotten that day, as drawn tiles. */
@Composable
private fun Forgot(day: Long, d: Data) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    fun at(min: Int) = if (day == today()) System.currentTimeMillis() else startOf(day) + min * 60_000L
    val t = d.book.care(day) ?: return
    data class F(val icon: String, val label: String, val act: () -> Unit)
    val items = mutableListOf<F>()
    if (!t.breakfast) items += F("icon_breakfast", tr("Завтрак", "Breakfast")) { scope.launch { c.log("eat", "breakfast", at = at(9 * 60)) } }
    if (d.book.marksOn(day).none { it.sub == "lunch" }) items += F("icon_lunch", tr("Обед", "Lunch")) { scope.launch { c.log("eat", "lunch", at = at(13 * 60)) } }
    if (!t.dinner) items += F("icon_dinner", tr("Ужин", "Dinner")) { scope.launch { c.log("eat", "dinner", at = at(19 * 60)) } }
    items += F("icon_water", tr("Стакан воды", "Water")) { scope.launch { c.log("water", at = at(12 * 60)) } }
    items += F("icon_walk", tr("Прогулка 30 мин", "Walk 30 min")) { scope.launch { c.log("move", "walk", at = at(17 * 60), mins = 30) } }
    d.activeMeds.filter { !it.asNeeded }.forEach { m -> m.timeList().forEach { tm -> if (d.book.marksOn(day).none { it.med == m.id && it.slot == tm } && d.book.dueOn(m, day).contains(tm))
        items += F("pot_${m.herb}_2", "${m.name} ${hhmm(tm)}") { scope.launch { c.log("pill", med = m.id, slot = tm, at = at(tm)) } } } }
    if (items.isEmpty()) return
    Text(if (day == today()) tr("Ещё не отмечено", "Not marked yet") else tr("Забыли отметить?", "Forgot to mark?"), style = T.h3, modifier = Modifier.padding(top = 20.dp))
    Text(if (day == today()) tr("Нажмите – отметится сейчас.", "Tap to mark it now.") else tr("Нажмите – отметится задним числом.", "Tap to add it to that day."), style = T.tiny)
    PicGrid(items.take(8), 4, Modifier.padding(top = 8.dp), gap = 6.dp) { f, mod -> PicTile(f.icon, f.label, false, mod, pic = 36.dp, onClick = f.act) }
}


/** A mark's own picture: the meal, the sport, or its kind. */
fun markIcon(m: Mark): String = when {
    m.kind == "eat" && m.sub in listOf("breakfast", "lunch", "dinner", "snack") -> "icon_${m.sub}"
    m.kind == "move" -> app.caregotchi.ui.home.moveIcon(m.sub)
    m.kind == "bp" -> "icon_tonometer"
    m.kind == "caf" -> "drink_${m.sub}"
    else -> icon(m.kind)
}
