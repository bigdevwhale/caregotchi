package app.caregotchi

import android.graphics.Bitmap
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import app.caregotchi.data.DemoSeed
import app.caregotchi.domain.Lang
import app.caregotchi.ui.Onboarding
import app.caregotchi.ui.Root
import app.caregotchi.ui.Start
import app.caregotchi.ui.diary.DiaryTab
import app.caregotchi.ui.theme.MendTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Every screen of the demo drawn to PNGs in app/build/art/screens (run with `-Prender`). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "ru-w393dp-h851dp-xxhdpi")
class RenderScreens {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val en = System.getProperty("render.lang") == "en"
    private val out = File(System.getProperty("render.out") ?: "build/art", if (en) "screens-en" else "screens").apply { mkdirs() }
    private val dark = System.getProperty("render.dark") == "1"

    private fun settle() = repeat(6) { rule.mainClock.advanceTimeBy(400); Thread.sleep(150); org.robolectric.shadows.ShadowLooper.idleMainLooper() }
    private fun shot(name: String) {
        settle()
        val v = rule.activity.window.decorView
        val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { v.draw(android.graphics.Canvas(b)) }
        File(out, "${if (dark) "d-" else ""}$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun render() {
        if (en) org.robolectric.RuntimeEnvironment.setQualifiers("+en")
        Lang.ru = !en
        val c = ApplicationProvider.getApplicationContext<CaregotchiApp>().container
        runBlocking { DemoSeed.seedIfEmpty(c, awake = true) }
        val meds = runBlocking { c.dao.medList() }
        val pets = runBlocking { c.dao.petList() }
        val attack = runBlocking { c.dao.attackList().first() }
        val meal = runBlocking { c.dao.markList().first { it.kind == "eat" } }
        var open = 0L
        val shots = listOf(
            "1-home" to (2 to null), "2-sill" to (0 to null), "3-diary" to (1 to null), "4-nest" to (3 to null), "5-shop" to (4 to null),
            "6-rate" to (2 to "sheet:rate:${meal.id}"), "7-move" to (2 to "sheet:move"), "8-pill" to (2 to "pill"), "9-bad" to (2 to "bad"), "10-quests" to (2 to "quests"),
            "11-need-eat" to (2 to "sheet:need:eat"), "12-dark" to (2 to "dark"), "13-med" to (0 to "med:${meds.first().id}"), "14-day" to (1 to "day:${app.caregotchi.domain.today() - 1}"),
            "15-attack" to (1 to "attack:${attack.id}"), "16-pet" to (3 to "pet:${pets.first().id}"), "17-settings" to (2 to "settings"), "18-chest" to (2 to "sheet:chest"),
        )
        var i by mutableIntStateOf(-1)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val s by c.settings.settings.collectAsState(null)
            MendTheme(dark) { s?.let { st -> key(i) { if (i in 0..499) Root(st) else if (i >= 500) Onboarding() } } }
        }
        for ((n, sh) in shots.withIndex()) {
            Start.tab = sh.second.first; Start.open = sh.second.second
            if (sh.first == "12-dark") {
                open = runBlocking { c.startAttack("migraine").also { id -> c.dao.attack(id)?.let { c.dao.update(it.copy(strength = 6, side = "left", symptoms = "light,nausea")) } } }
                Start.open = "dark:$open"
            }
            if (sh.first == "13-med") runBlocking { c.dao.deleteAttack(open) }
            rule.runOnUiThread { i = n }
            shot(sh.first)
        }
        DiaryTab.tab = 1; Start.tab = 1; Start.open = null; rule.runOnUiThread { i = 100 }; shot("3b-detective")
        DiaryTab.tab = 2; rule.runOnUiThread { i = 101 }; shot("3c-letter")
        DiaryTab.tab = 0
        listOf("day", "goals", "remind", "health", "look", "weather", "backup", "doctor").forEachIndexed { k, sec ->
            app.caregotchi.ui.home.Start0.section = sec; Start.tab = 2; Start.open = "settings"; rule.runOnUiThread { i = 200 + k }; shot("19-set-$sec")
        }
        rule.runOnUiThread { i = 500 }; shot("0-onboarding")
    }
}
