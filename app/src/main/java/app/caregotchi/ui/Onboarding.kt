package app.caregotchi.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.caregotchi.container
import app.caregotchi.data.Backup
import app.caregotchi.data.Med
import app.caregotchi.data.Pet
import app.caregotchi.domain.Gender
import app.caregotchi.domain.Lang
import app.caregotchi.domain.Zoo
import app.caregotchi.domain.hhmm
import app.caregotchi.domain.today
import app.caregotchi.domain.tr
import app.caregotchi.ui.components.Art
import app.caregotchi.ui.components.Btn
import app.caregotchi.ui.components.Chip
import app.caregotchi.ui.components.Field
import app.caregotchi.ui.components.Ghost
import app.caregotchi.ui.components.Stepper
import app.caregotchi.ui.components.Toggle
import app.caregotchi.ui.sill.HERBS
import app.caregotchi.ui.theme.C
import app.caregotchi.ui.theme.T
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Onboarding() {
    val ctx = LocalContext.current
    val c = ctx.container
    val scope = rememberCoroutineScope()
    var step by remember { mutableIntStateOf(0) }
    var species by remember { mutableStateOf("fox") }
    var name by remember { mutableStateOf(Zoo.randomName("fox")) }
    val meds = remember { mutableStateListOf<Med>() }
    var medName by remember { mutableStateOf("") }
    var medTimes by remember { mutableStateOf(setOf(8 * 60)) }
    var wake by remember { mutableIntStateOf(7 * 60 + 30) }
    var bed by remember { mutableIntStateOf(23 * 60) }
    var water by remember { mutableIntStateOf(6) }
    var move by remember { mutableIntStateOf(30) }
    var fem by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val notify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { if (!runCatching { Backup.read(ctx, uri) }.getOrDefault(false)) err = tr("Это не копия «Заботогочи»", "Not a Caregotchi backup") }
    }

    Column(Modifier.fillMaxSize().background(C.bg).statusBarsPadding().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(20.dp)) {
        when (step) {
            0 -> {
                Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(20.dp))) {
                    Art("room_bg", Modifier.fillMaxSize(), ContentScale.Crop)
                    Art("pet_${species}_0", Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp).size(110.dp))
                }
                Text(tr("Питомец, который болеет за вас", "A pet that cares as you do"), style = T.title, modifier = Modifier.padding(top = 16.dp))
                Text(tr("Он ест, когда едите вы, пьёт, когда пьёте, получает свою ягодку, когда вы принимаете лекарство, и спит, когда спите. Мигрень или другая беда у него ничего не отнимают – он просто приносит плед.",
                    "It eats when you eat, drinks when you drink, gets its berry when you take your medicine, and sleeps when you sleep. A migraine or any other trouble takes nothing from it – it just brings a blanket."), style = T.body, modifier = Modifier.padding(top = 6.dp))
                Text(tr("Выберите первого жильца", "Choose your first pet"), style = T.h3, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Zoo.starters.forEach { sp ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(C.card2).border(if (sp.id == species) 3.dp else 0.dp, C.honey, RoundedCornerShape(14.dp))
                            .clickable { species = sp.id; name = Zoo.randomName(sp.id) }.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Art("pet_${sp.id}_2", Modifier.fillMaxWidth().aspectRatio(1f)); Text(sp.title, style = T.tiny.copy(color = C.ink), maxLines = 1)
                        }
                    }
                }
                Field(name, { name = it }, tr("Имя", "Name"), Modifier.fillMaxWidth().padding(top = 12.dp))
                Btn(tr("Дальше", "Next"), Modifier.fillMaxWidth().padding(top = 18.dp), enabled = name.isNotBlank()) { step = 1 }
                Ghost(tr("У меня есть копия с другого телефона", "I have a backup from another phone"), Modifier.fillMaxWidth().padding(top = 10.dp)) { restore.launch(arrayOf("application/json", "application/octet-stream", "*/*")) }
                err?.let { Text(it, style = T.smallB.copy(color = C.bad), modifier = Modifier.padding(top = 6.dp)) }
            }
            1 -> {
                Art("pot_mint_2", Modifier.align(Alignment.CenterHorizontally).size(100.dp))
                Text(tr("Лекарства", "Medicine"), style = T.title)
                Text(tr("Что вы принимаете регулярно? Каждое лекарство вырастет на подоконнике горшком, а питомец будет напоминать о нём. Можно пропустить и добавить потом.",
                    "What do you take regularly? Each medicine grows as a pot on the windowsill, and the pet reminds you of it. You can skip this and add later."), style = T.body, modifier = Modifier.padding(top = 6.dp))
                meds.forEach { m -> Text("• ${m.name} – ${m.timeList().joinToString(", ") { hhmm(it) }}", style = T.bodyB, modifier = Modifier.padding(top = 8.dp)) }
                Field(medName, { medName = it }, tr("Название, например «Магний»", "Name, e.g. «Magnesium»"), Modifier.fillMaxWidth().padding(top = 12.dp))
                FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(8 * 60, 9 * 60, 13 * 60, 14 * 60, 19 * 60, 20 * 60, 22 * 60).forEach { t -> Chip(hhmm(t), t in medTimes) { medTimes = if (t in medTimes) medTimes - t else medTimes + t } }
                }
                Ghost(tr("+ Добавить", "+ Add"), Modifier.padding(top = 10.dp), small = true) {
                    if (medName.isNotBlank() && medTimes.isNotEmpty()) { meds += Med(name = medName.trim(), times = medTimes.sorted().joinToString(","), herb = HERBS[meds.size % HERBS.size], from = today()); medName = "" }
                }
                Text(tr("Лекарства «по необходимости» (от приступа) добавляются на подоконнике.", "As-needed medicine (for a spell) is added on the windowsill."), style = T.tiny, modifier = Modifier.padding(top = 8.dp))
                Btn(tr("Дальше", "Next"), Modifier.fillMaxWidth().padding(top = 18.dp)) {
                    if (medName.isNotBlank() && medTimes.isNotEmpty()) { meds += Med(name = medName.trim(), times = medTimes.sorted().joinToString(","), herb = HERBS[meds.size % HERBS.size], from = today()); medName = "" }
                    step = 2
                }
            }
            else -> {
                Art("icon_sunrise", Modifier.align(Alignment.CenterHorizontally).size(90.dp))
                Text(tr("Ваш ритм", "Your rhythm"), style = T.title)
                Text(tr("От этого зависит, когда питомец проголодается, захочет пить и спать. Всё можно поменять в настройках.", "This sets when the pet gets hungry, thirsty and sleepy. You can change it all in the settings."), style = T.body, modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
                Stepper(tr("Подъём", "Wake-up"), hhmm(wake), { wake = (wake - 30 + 1440) % 1440 }, { wake = (wake + 30) % 1440 })
                Stepper(tr("Отбой", "Bedtime"), hhmm(bed), { bed = (bed - 30 + 1440) % 1440 }, { bed = (bed + 30) % 1440 })
                Stepper(tr("Вода в день", "Water a day"), tr("$water ст.", "$water gl."), { water = (water - 1).coerceAtLeast(2) }, { water = (water + 1).coerceAtMost(14) })
                Stepper(tr("Движение в день", "Moving a day"), tr("$move мин", "$move min"), { move = (move - 10).coerceAtLeast(10) }, { move = (move + 10).coerceAtMost(180) })
                if (Lang.ru) Toggle("Пишу о себе в женском роде", "«Поела», «приняла»", fem) { fem = it; Gender.fem = it }
                Btn(tr("Поселиться в дупле", "Move into the hollow"), Modifier.fillMaxWidth().padding(top = 18.dp), icon = "icon_home") {
                    scope.launch {
                        meds.forEach { c.saveMed(it) }
                        c.dao.insert(Pet(species = species, name = name.trim(), found = today(), hatched = today(), home = true))
                        c.settings.update { it.copy(onboarded = true, wake = wake, bedtime = bed, waterGoal = water, moveGoal = move, fem = fem, leaves = 30, leavesEver = 30, eggsSeen = setOf(species)) }
                        if (Build.VERSION.SDK_INT >= 33) notify.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                Text(tr("Приложение – дневник и друг, а не врач. Дозы и схемы назначает доктор.", "The app is a diary and a friend, not a doctor. Your doctor sets doses and schedules."), style = T.tiny, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
            }
        }
    }
}
