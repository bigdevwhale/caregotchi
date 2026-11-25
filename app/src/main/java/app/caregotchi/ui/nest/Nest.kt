package app.caregotchi.ui.nest

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.caregotchi.BuildConfig
import app.caregotchi.container
import app.caregotchi.domain.Badges
import app.caregotchi.domain.Stage
import app.caregotchi.domain.Zoo
import app.caregotchi.domain.fmtDay
import app.caregotchi.domain.stats
import app.caregotchi.domain.today
import app.caregotchi.domain.tr
import app.caregotchi.ui.Data
import app.caregotchi.ui.Nav
import app.caregotchi.ui.Screen
import app.caregotchi.ui.components.Art
import app.caregotchi.ui.components.Banner
import app.caregotchi.ui.components.Btn
import app.caregotchi.ui.components.Field
import app.caregotchi.ui.components.Page
import app.caregotchi.ui.components.Popup
import app.caregotchi.ui.components.Section
import app.caregotchi.ui.components.Thread
import app.caregotchi.ui.components.grey
import app.caregotchi.ui.theme.C
import app.caregotchi.ui.theme.T
import kotlinx.coroutines.launch

@Composable
fun NestScreen(d: Data, nav: Nav) {
    val st = remember(d.book) { d.book.stats() }
    val owned = d.pets.filter { it.hatched > 0 }
    var info by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Banner("banner_nursery", tr("Жильцы", "Pets"), tr("${owned.size} из ${Zoo.all.size} · нашивок ${d.s.badges.size} из ${Badges.all.size}", "${owned.size} of ${Zoo.all.size} · ${d.s.badges.size} of ${Badges.all.size} badges"))
        Column(Modifier.padding(horizontal = 14.dp)) {
            Spacer(Modifier.height(8.dp))
            Incubator(d, st)
            d.pet?.let { pet ->
                Section(tr("Путь: ${pet.name}", "${pet.name}'s way"), aside = Stage.next(pet.xp)?.let { tr("до следующего ${it - pet.xp} опыта", "${it - pet.xp} xp to the next") } ?: tr("путь пройден", "all the way"))
                Road(pet.species, Stage.of(pet.xp))
            }
            Section(tr("Коллекция", "Collection"), aside = tr("каждый – за свой вид заботы", "each for its own kind of care"))
            Zoo.all.chunked(6).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { sp ->
                        val p = d.pets.firstOrNull { it.species == sp.id }
                        Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(C.card2)
                            .then(if (p != null && p.id == d.pet?.id) Modifier.border(3.dp, C.honey, RoundedCornerShape(12.dp)) else Modifier)
                            .clickable { if (p != null && p.hatched > 0) nav.push(Screen.PetInfo(p.id)) else info = sp.id }, contentAlignment = Alignment.Center) {
                            when {
                                p != null && p.hatched > 0 -> Art("pet_${sp.id}_${minOf(Stage.of(p.xp), 2)}", Modifier.fillMaxSize().padding(4.dp))
                                p != null -> Art("egg_${sp.id}", Modifier.fillMaxSize().padding(6.dp))
                                else -> { Art("egg_${sp.id}", Modifier.fillMaxSize().padding(6.dp), alpha = .5f, tint = grey); Text("?", style = T.btn.copy(color = Color.White)) }
                            }
                        }
                    }
                }
            }
            Section(tr("Нашивки на одеяле", "Badges on the blanket"), aside = "${d.s.badges.size}/${Badges.all.size}")
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Color(0xFFE9D9BD), Color(0xFFDCC7A4))))
                .border(2.dp, Color(0x55A8743F), RoundedCornerShape(14.dp)).padding(8.dp)) {
                Badges.all.chunked(8).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { b ->
                            val has = "${b.id}" in d.s.badges
                            Art("badge_${b.id}", Modifier.weight(1f).aspectRatio(1f).padding(2.dp).clickable { info = "badge:${b.id}" }, alpha = if (has) 1f else .3f, tint = if (has) null else grey)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    info?.let { k ->
        Popup({ info = null }) {
            if (k.startsWith("badge:")) {
                val b = Badges.all[k.drop(6).toInt()]
                val lvl = d.level.first
                Art("badge_${b.id}", Modifier.size(90.dp), tint = if ("${b.id}" in d.s.badges) null else grey)
                Text(b.title, style = T.h2)
                Text(b.why, style = T.small, textAlign = TextAlign.Center)
                val p = b.progress(st, d.s, lvl).coerceAtMost(b.need)
                if ("${b.id}" !in d.s.badges) { Thread(p / b.need.toFloat(), Modifier.padding(top = 10.dp)); Text("$p / ${b.need}", style = T.tiny) }
            } else {
                val sp = Zoo.of(k)
                Art("egg_${sp.id}", Modifier.size(90.dp))
                Text(sp.title, style = T.h2)
                Text(tr("Яйцо даётся за: ${sp.how}", "The egg comes for: ${sp.how}"), style = T.small, textAlign = TextAlign.Center)
                val p = sp.progress(st).coerceAtMost(sp.need)
                Thread(p / sp.need.toFloat(), Modifier.padding(top = 10.dp)); Text("$p / ${sp.need}", style = T.tiny)
            }
        }
    }
}

/** The egg being warmed, or the nearest egg to earn. */
@Composable
private fun Incubator(d: Data, st: app.caregotchi.domain.Stats) {
    val egg = d.eggs.firstOrNull()
    val t = rememberInfiniteTransition(label = "egg")
    val r by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "r")
    val (sp, frac, text) = if (egg != null) {
        val good = (egg.found..today()).count { d.book.care(it)?.good == true }
        Triple(Zoo.of(egg.species), (good / 2f).coerceAtMost(1f), (2 - good).coerceAtLeast(0).let { n -> if (n == 0) tr("Тёплое яйцо. Вот-вот вылупится.", "A warm egg. It's about to hatch.") else tr("Тёплое яйцо. Ещё ${if (n == 1) "один хороший день" else "$n хороших дня"} – и вылупится.", "A warm egg. ${if (n == 1) "One more good day" else "$n more good days"} and it hatches.") })
    } else {
        val owned = d.pets.map { it.species }.toSet()
        val next = Zoo.all.filter { it.id !in owned }.maxByOrNull { it.progress(st) / it.need.toFloat() }
        if (next == null) Triple(null, 1f, tr("Все жильцы найдены!", "Every pet found!"))
        else Triple(next, (next.progress(st) / next.need.toFloat()).coerceAtMost(1f), tr("Следующее яйцо – ${next.title.lowercase()}: ${next.how} (${next.progress(st).coerceAtMost(next.need)} из ${next.need}).",
            "Next egg – ${next.title.lowercase()}: ${next.how} (${next.progress(st).coerceAtMost(next.need)} of ${next.need})."))
    }
    Row(Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)).background(Brush.verticalGradient(listOf(Color(0xFFF8E7C4), Color(0xFFF1D9A8))))
        .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (sp != null) Art("egg_${sp.id}", Modifier.size(64.dp).graphicsLayer { rotationZ = if (egg != null && !BuildConfig.SHOTS) r * 5f else 0f }, alpha = if (egg != null) 1f else .6f, tint = if (egg != null) null else grey)
        else Art("icon_trophy", Modifier.size(64.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if (egg != null) tr("В инкубаторе", "In the incubator") else tr("Инкубатор пуст", "The incubator is empty"), style = T.h3.copy(color = Color(0xFF3B2E26)))
            Text(text, style = T.small.copy(color = Color(0xFF6B5545)))
            Thread(frac, Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun Road(species: String, stage: Int) {
    val steps = listOf("egg_$species" to tr("Яйцо", "Egg"), "pet_${species}_0" to Stage.name(0), "pet_${species}_1" to Stage.name(1), "pet_${species}_2" to Stage.name(2), "icon_star" to Stage.name(3))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        steps.forEachIndexed { i, (img, label) ->
            val at = i - 1
            val done = at < stage; val now = at == stage
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(if (now) Color(0xFFFFF3D6) else if (done) C.okSoft else C.card2)
                    .border(3.dp, if (now) C.honey else if (done) C.sage else C.line, CircleShape), contentAlignment = Alignment.Center) {
                    Art(img, Modifier.size(36.dp), alpha = if (done || now) 1f else .5f, tint = if (done || now) null else grey)
                }
                Text(label, style = T.tiny.copy(color = if (now) C.ink else C.mute), maxLines = 1)
            }
        }
    }
}

@Composable
fun PetPage(id: Long, d: Data, nav: Nav) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val p = d.pets.firstOrNull { it.id == id } ?: return
    var name by remember(id) { mutableStateOf(p.name) }
    val stage = Stage.of(p.xp)
    Page(p.name, { scope.launch { if (name != p.name) c.rename(p, name) }; nav.pop() }) {
        Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(18.dp)).background(Brush.radialGradient(listOf(Color(0xFFFFF3D6), C.bg))), contentAlignment = Alignment.Center) {
            Art("pet_${p.species}_${if (stage >= 2) 6 else stage}", Modifier.size(180.dp))
        }
        Text("${Zoo.of(p.species).title} · ${Stage.name(stage)}", style = T.h3, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Center)
        Stage.next(p.xp)?.let { nx -> Thread((p.xp - Stage.at[stage]) / (nx - Stage.at[stage]).toFloat(), Modifier.padding(top = 8.dp)); Text(tr("${p.xp} опыта · до следующей ступени ${nx - p.xp}", "${p.xp} xp · ${nx - p.xp} to the next stage"), style = T.tiny) }
        Section(tr("Имя", "Name"))
        Field(name, { name = it }, tr("Имя", "Name"), Modifier.fillMaxWidth())
        Section(tr("О питомце", "About"))
        Text(tr("Из яйца – ${fmtDay(p.hatched)}. Яйцо было за «${Zoo.of(p.species).how}».", "Hatched ${fmtDay(p.hatched)}. The egg came for «${Zoo.of(p.species).how}»."), style = T.body)
        Section(tr("Как он выглядит", "Looks"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(3, 4, 5, 7, 8).forEach { f -> Art("pet_${p.species}_$f", Modifier.size(60.dp)) }
        }
        if (d.pet?.id != p.id) Btn(tr("Поселить в дупле", "Move into the hollow"), Modifier.fillMaxWidth().padding(top = 18.dp), icon = "icon_home") { scope.launch { c.setHome(p); nav.pop(); nav.tab = 2 } }
        else Text(tr("Живёт в дупле и делит с тобой всю заботу.", "Lives in the hollow and shares all your care."), style = T.small, modifier = Modifier.padding(top = 14.dp))
        Text(tr("Уровень хранителя: ${d.level.first}", "Keeper level: ${d.level.first}"), style = T.tiny, modifier = Modifier.padding(top = 10.dp))
    }
}

