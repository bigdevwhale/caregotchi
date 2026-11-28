package app.caregotchi.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.caregotchi.BuildConfig
import app.caregotchi.container
import app.caregotchi.data.Air
import app.caregotchi.data.Attack
import app.caregotchi.data.Mark
import app.caregotchi.data.Med
import app.caregotchi.data.Pet
import app.caregotchi.data.Settings
import app.caregotchi.domain.Book
import app.caregotchi.domain.Gender
import app.caregotchi.domain.Level
import app.caregotchi.domain.Quests
import app.caregotchi.domain.Stage
import app.caregotchi.domain.today
import app.caregotchi.domain.tr
import app.caregotchi.ui.components.Art
import app.caregotchi.ui.components.tap
import app.caregotchi.ui.diary.AttackPage
import app.caregotchi.ui.diary.DayPage
import app.caregotchi.ui.diary.DiaryScreen
import app.caregotchi.ui.home.DarkRoom
import app.caregotchi.ui.home.HomeScreen
import app.caregotchi.ui.home.Inbox
import app.caregotchi.ui.home.SettingsPage
import app.caregotchi.ui.nest.NestScreen
import app.caregotchi.ui.nest.PetPage
import app.caregotchi.ui.shop.ShopScreen
import app.caregotchi.ui.sill.MedPage
import app.caregotchi.ui.sill.SillScreen
import app.caregotchi.ui.theme.C
import app.caregotchi.ui.theme.T
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Where a launch opens: a tab, a page or the dark room (from the widget, notifications or demo intents). */
object Start { var tab = 2; var open by mutableStateOf<String?>(null); /** Food or water pressed on the widget: done on the home screen, as by its own button. */ var act by mutableStateOf<String?>(null) }

sealed class Screen {
    data class MedEdit(val id: Long) : Screen()
    data class DayView(val day: Long) : Screen()
    data class AttackEdit(val id: Long) : Screen()
    data class PetInfo(val id: Long) : Screen()
    data object Settings : Screen()
}

class Nav {
    var tab by mutableIntStateOf(2)
    val stack = mutableStateListOf<Screen>()
    var dark by mutableLongStateOf(0L)        // the bad spell in the dark room, 0 when closed
    var sheet by mutableStateOf<String?>(null) // «bad», «quests», «need:eat», «pill», «chest»
    fun push(s: Screen) { stack.add(s) }
    fun pop() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
}

/** Everything the screens read, collected once. */
class Data(val s: Settings, val marks: List<Mark>, val meds: List<Med>, val attacks: List<Attack>, val pets: List<Pet>, val air: List<Air>, now: Long) {
    val book = Book(marks, meds, attacks, air, s, now)
    val pet: Pet? = pets.firstOrNull { it.home && it.hatched > 0 } ?: pets.firstOrNull { it.hatched > 0 }
    val eggs = pets.filter { it.hatched == 0L }
    val level = Level.of(s.xp)
    val stage = pet?.let { Stage.of(it.xp) } ?: 0
    val activeMeds = meds.filter { !it.archived }
    val quests = (if (s.questDay == today()) s.quests else Quests.deal(today(), activeMeds.any { !it.asNeeded })).mapNotNull { Quests.byId[it] }
    /** Bad spells going on now: several at once (#27) – pressure and a racing heart may start together and ease apart. */
    val opens = attacks.filter { it.kind != app.caregotchi.domain.PERIOD && it.end == 0L && System.currentTimeMillis() - it.start < 3 * 86_400_000L }.sortedBy { it.start }
    val open = opens.firstOrNull()
}

@Composable
fun Root(s: Settings) {
    val ctx = LocalContext.current
    val c = ctx.container
    val marks by c.dao.marks().collectAsState(initial = emptyList())
    val meds by c.dao.meds().collectAsState(initial = emptyList())
    val attacks by c.dao.attacks().collectAsState(initial = emptyList())
    val pets by c.dao.pets().collectAsState(initial = emptyList())
    val air by c.dao.air().collectAsState(initial = emptyList())
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(60_000); clock = System.currentTimeMillis() } }
    Gender.fem = s.fem
    val data = remember(s, marks, meds, attacks, pets, air, clock) { Data(s, marks, meds, attacks, pets, air, clock) }
    val nav = remember { Nav().apply { tab = Start.tab } }
    var note by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { c.tick() }
    LaunchedEffect(Unit) { if (!BuildConfig.SHOTS) while (true) { c.refreshOutside(); delay(30 * 60_000L) } }
    LaunchedEffect(Start.open) {
        val o = Start.open ?: return@LaunchedEffect
        Start.open = null
        when {
            o == "bad" -> nav.sheet = "bad"
            o == "eat" || o == "water" -> { nav.tab = 2; Start.act = o }
            o == "pill" -> nav.sheet = "pill"
            o == "quests" -> nav.sheet = "quests"
            o == "settings" -> nav.push(Screen.Settings)
            o.startsWith("dark:") -> nav.dark = o.drop(5).toLongOrNull() ?: 0
            o.startsWith("tab:") -> nav.tab = o.drop(4).toIntOrNull() ?: 2
            o.startsWith("med:") -> nav.push(Screen.MedEdit(o.drop(4).toLongOrNull() ?: 0))
            o.startsWith("day:") -> nav.push(Screen.DayView(o.drop(4).toLongOrNull() ?: today()))
            o.startsWith("attack:") -> nav.push(Screen.AttackEdit(o.drop(7).toLongOrNull() ?: 0))
            o.startsWith("pet:") -> nav.push(Screen.PetInfo(o.drop(4).toLongOrNull() ?: 0))
            o.startsWith("sheet:") -> nav.sheet = o.drop(6)
        }
    }
    // tasks done show as a short line, not a window
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(s.inbox) {
        val q = s.inbox.firstOrNull { it.startsWith("quest:") } ?: return@LaunchedEffect
        val quest = Quests.byId[q.drop(6)]
        if (quest != null) { val t = tr("Задание выполнено: ${quest.text(s)} · +${quest.leaves}", "Task done: ${quest.text(s)} · +${quest.leaves}"); note = t; scope.launch { delay(2800); if (note == t) note = null } }
        c.seen(q)
    }

    Box(Modifier.fillMaxSize().background(C.bg)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (nav.tab) {
                    0 -> SillScreen(data, nav)
                    1 -> DiaryScreen(data, nav)
                    3 -> NestScreen(data, nav)
                    4 -> ShopScreen(data, nav)
                    else -> HomeScreen(data, nav)
                }
            }
            Shelf(nav.tab) { nav.tab = it }
        }
        for (scr in nav.stack) Box(Modifier.fillMaxSize()) {
            when (scr) {
                is Screen.MedEdit -> MedPage(scr.id, data, nav)
                is Screen.DayView -> DayPage(scr.day, data, nav)
                is Screen.AttackEdit -> AttackPage(scr.id, data, nav)
                is Screen.PetInfo -> PetPage(scr.id, data, nav)
                Screen.Settings -> SettingsPage(data, nav)
            }
        }
        app.caregotchi.ui.home.Sheets(data, nav)
        if (nav.dark != 0L) DarkRoom(nav.dark, data, nav)
        if (nav.stack.isEmpty() && nav.dark == 0L && nav.sheet == null) Inbox(data)
        AnimatedVisibility(note != null, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 150.dp, start = 18.dp, end = 18.dp), enter = fadeIn(), exit = fadeOut()) {
            Text(note ?: "", style = T.smallB.copy(color = C.bg), modifier = Modifier.clip(RoundedCornerShape(50)).background(C.ink).padding(horizontal = 16.dp, vertical = 9.dp))
        }
    }
}

/** The bottom shelf: a wooden plank with drawn icons and the round door of the hollow in the middle. */
@Composable
private fun Shelf(sel: Int, onSel: (Int) -> Unit) {
    val tabs = listOf(tr("Окно", "Window") to "icon_sill", tr("Дневник", "Diary") to "icon_diary", tr("Дупло", "Hollow") to "icon_home",
        tr("Жильцы", "Pets") to "icon_nest", tr("Лавка", "Shop") to "icon_shop")
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Box(Modifier.fillMaxWidth().height(4.dp).background(C.woodDark))
            Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFFB8844C), Color(0xFF9A6534)))).navigationBarsPadding().height(70.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    drawLine(Color(0xFFD4A066), Offset(0f, 1.5f), Offset(size.width, 1.5f), 3f)
                    var y = 8f; while (y < size.height) { drawLine(Color(0x0FFFFFFF), Offset(0f, y), Offset(size.width, y), 2f); y += 7.dp.toPx() }
                    var x = size.width / 5; while (x < size.width - 1) { drawLine(Color(0x22000000), Offset(x, 0f), Offset(x, size.height), 2f); x += size.width / 5 }
                    listOf(8.dp.toPx(), size.width - 8.dp.toPx()).forEach { drawCircle(Color(0xFF5A3A1C), 3.dp.toPx(), Offset(it, 12.dp.toPx())) }
                }
            }
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding()) {
            tabs.forEachIndexed { i, (label, icon) ->
                val on = i == sel
                Column(Modifier.weight(1f).tap { onSel(i) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    if (i == 2) {
                        Box(Modifier.offset(y = (-14).dp).size(70.dp).shadow(4.dp, CircleShape).clip(CircleShape).background(Color(0xFF5A3A1C)).padding(bottom = 3.dp)
                            .clip(CircleShape).background(Color(0xFF7A4B22)).padding(4.dp).clip(CircleShape)
                            .background(Brush.radialGradient(listOf(Color(0xFFFBF0DA), Color(0xFFE8D2A8)))), contentAlignment = Alignment.Center) {
                            Art(icon, Modifier.padding(top = 2.dp).size(if (on) 42.dp else 40.dp))
                        }
                        Label(label, on, Modifier.offset(y = (-14).dp))
                    } else {
                        val lift by animateDpAsState(if (on) (-4).dp else 6.dp, label = "lift")
                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.offset(y = lift), contentAlignment = Alignment.Center) {
                            if (on) Box(Modifier.size(50.dp).background(Brush.radialGradient(listOf(Color(0xCCFFF3D6), Color.Transparent)), CircleShape))
                            Art(icon, Modifier.size(if (on) 44.dp else 38.dp), alpha = if (on) 1f else .82f)
                        }
                        Label(label, on, Modifier.offset(y = lift - 2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun Label(text: String, on: Boolean, modifier: Modifier) {
    Text(text, style = T.tiny.copy(color = if (on) C.inkPaper else Color(0xFFF3DFC0)), maxLines = 1,
        modifier = modifier.then(if (on) Modifier.clip(RoundedCornerShape(6.dp)).background(C.paper).border(1.dp, Color(0x22000000), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp) else Modifier))
}

private val C.inkPaper get() = Color(0xFF3B2E26)
