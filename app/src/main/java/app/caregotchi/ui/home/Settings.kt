package app.caregotchi.ui.home

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.caregotchi.container
import app.caregotchi.currentLanguage
import app.caregotchi.data.Outside
import app.caregotchi.data.OutsideSource
import app.caregotchi.data.Settings
import app.caregotchi.data.Town
import app.caregotchi.domain.Lang
import app.caregotchi.domain.Stage
import app.caregotchi.domain.hhmm
import app.caregotchi.domain.Plan
import app.caregotchi.ui.home.MEALS
import app.caregotchi.domain.tr
import app.caregotchi.setLanguage
import app.caregotchi.ui.Data
import app.caregotchi.ui.Nav
import app.caregotchi.ui.Screen
import app.caregotchi.ui.components.Art
import app.caregotchi.ui.components.Btn
import app.caregotchi.ui.components.Field
import app.caregotchi.ui.components.Page
import app.caregotchi.ui.components.Paper
import app.caregotchi.ui.components.PicGrid
import app.caregotchi.ui.components.PicTile
import app.caregotchi.ui.components.Toggle
import app.caregotchi.ui.components.grey
import app.caregotchi.ui.components.tap
import app.caregotchi.ui.diary.Report
import app.caregotchi.ui.theme.C
import app.caregotchi.ui.theme.T
import app.caregotchi.work.Reminders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Settings: a keeper's card and a grid of drawn sections, each opening its own small page. */
@Composable
fun SettingsPage(d: Data, nav: Nav) {
    var section by remember { mutableStateOf<String?>(Start0.section.also { Start0.section = null }) }
    val s = d.s
    section?.let { Section(it, d, nav) { section = null }; return }
    val o = Outside.of(s.outside)
    val onCount = listOf(s.remindMeds, s.remindBp, s.remindMeals, s.remindWater, s.remindBed, s.reliefAsk).count { it }
    val items = listOf(
        Triple("day", "icon_sunrise", tr("Режим дня", "Daily rhythm") to "${hhmm(s.wake)} – ${hhmm(s.bedtime)}"),
        Triple("goals", "icon_hourglass", tr("Нормы", "Goals") to tr("${s.waterGoal} ст. · ${s.moveGoal} мин · ${s.sleepGoal} ч", "${s.waterGoal} gl · ${s.moveGoal} min · ${s.sleepGoal} h")),
        Triple("remind", "icon_bell", tr("Напоминания", "Reminders") to tr("включено $onCount из 6", "$onCount of 6 on")),
        Triple("health", "icon_thermo", tr("Болячки", "Troubles") to (s.kinds.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: tr("давление, свои", "pressure, own"))),
        Triple("look", "icon_gear", tr("Вид и язык", "Look & language") to when (s.theme) { "light" -> tr("светлая", "light"); "dark" -> tr("при свече", "candlelight"); else -> tr("как в системе", "system") }),
        Triple("weather", "icon_baro", tr("Погода", "Weather") to (if (o != null && !o.temp.isNaN()) "${s.town.ifEmpty { "—" }} · %+d°".format(o.temp.toInt()) else s.town.ifEmpty { tr("город не выбран", "no town") })),
        Triple("backup", "icon_letter", tr("Перенос", "Move phones") to tr("копия прогресса", "a backup")),
        Triple("doctor", "icon_rx", tr("Для врача", "For the doctor") to tr("PDF за 3 месяца", "3-month PDF")),
        Triple("stillcraft", "icon_star", "StillCraft" to tr("ошибки, идеи", "bugs, ideas")),
    )
    Page(tr("Настройки", "Settings"), { nav.pop() }) {
        KeeperCard(d) { d.pet?.let { nav.push(Screen.PetInfo(it.id)) } }
        NotifyStatus(Modifier.padding(top = 12.dp), quiet = true)
        Spacer(Modifier.height(14.dp))
        PicGrid(items, 3, gap = 10.dp) { (id, icon, t), mod ->
            Column(mod.shadow(3.dp, RoundedCornerShape(16.dp), ambientColor = C.shadow, spotColor = C.shadow).clip(RoundedCornerShape(16.dp)).background(C.card)
                .clickable { section = id }.padding(horizontal = 6.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(58.dp).clip(CircleShape).background(C.card2), contentAlignment = Alignment.Center) { Art(icon, Modifier.size(42.dp)) }
                Text(t.first, style = T.smallB, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.padding(top = 6.dp))
                Text(t.second, style = T.tiny, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.height(28.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Art("icon_lock", Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
            Text(tr("Все записи хранятся только на этом телефоне.", "Everything stays on this phone."), style = T.tiny)
        }
    }
}

/** Opens a settings section straight away (for links from other screens). */
object Start0 { var section: String? = null }

@Composable
private fun KeeperCard(d: Data, onClick: () -> Unit) {
    val pet = d.pet
    Box(Modifier.fillMaxWidth().height(118.dp).shadow(4.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick)) {
        Art("room_bg", Modifier.fillMaxSize(), ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xEE2A1C14), Color(0x662A1C14), Color.Transparent))))
        Row(Modifier.fillMaxSize().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Хранитель дупла", "Keeper of the hollow"), style = T.tiny.copy(color = Color(0xFFE8D2A8)))
                Text(tr("Уровень ${d.level.first}", "Level ${d.level.first}"), style = T.title.copy(color = Color.White))
                Text(tr("${d.book.streak} дн. подряд · ${d.s.leaves} листиков", "${d.book.streak} days in a row · ${d.s.leaves} leaves"), style = T.small.copy(color = Color(0xFFF3DFC0)))
                if (pet != null) Text("${pet.name} · ${Stage.name(Stage.of(pet.xp)).lowercase()}", style = T.small.copy(color = Color(0xFFF3DFC0)))
            }
            if (pet != null) Art("pet_${pet.species}_6", Modifier.size(96.dp))
        }
    }
}

@Composable
private fun Section(id: String, d: Data, nav: Nav, back: () -> Unit) {
    val ctx = LocalContext.current
    val c = ctx.container
    val scope = rememberCoroutineScope()
    val s = d.s
    fun set(f: (Settings) -> Settings) = scope.launch { c.settings.update(f); Reminders.schedule(ctx) }
    val title = when (id) { "day" -> tr("Режим дня", "Daily rhythm"); "goals" -> tr("Нормы", "Goals"); "remind" -> tr("Напоминания", "Reminders"); "health" -> tr("Болячки", "Troubles")
        "look" -> tr("Вид и язык", "Look & language"); "weather" -> tr("Погода", "Weather"); "backup" -> tr("Перенос на другой телефон", "Move to another phone"); "doctor" -> tr("Для врача", "For the doctor"); else -> "StillCraft" }
    Page(title, back) {
        when (id) {
            "day" -> {
                Text(tr("От этого зависит, когда питомец просыпается, хочет пить и зевает.", "This sets when the pet wakes, gets thirsty and yawns."), style = T.small)
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TimeCard("icon_sunrise", tr("Подъём", "Wake-up"), s.wake, Modifier.weight(1f), Color(0xFFFFF0CF)) { v -> set { it.copy(wake = v) } }
                    TimeCard("icon_sleep", tr("Отбой", "Bedtime"), s.bedtime, Modifier.weight(1f), Color(0xFFE4DDF2)) { v -> set { it.copy(bedtime = v) } }
                }
                DayRibbon(s.wake, s.bedtime)
            }
            "goals" -> {
                Text(tr("Вода в день", "Water a day"), style = T.h3, modifier = Modifier.padding(top = 4.dp))
                Text(tr("Коснитесь стакана – столько и будет нормой: ${s.waterGoal}", "Tap a glass to set the goal: ${s.waterGoal}"), style = T.tiny)
                PicGrid((1..12).toList(), 6, Modifier.padding(top = 8.dp), gap = 4.dp) { n, mod ->
                    Box(mod.aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(if (n <= s.waterGoal) Color(0x338DBDD6) else Color.Transparent).clickable { set { it.copy(waterGoal = n.coerceAtLeast(2)) } }, contentAlignment = Alignment.Center) {
                        Art("icon_water", Modifier.size(34.dp), alpha = if (n <= s.waterGoal) 1f else .3f, tint = if (n <= s.waterGoal) null else grey)
                    }
                }
                Text(tr("Движение в день", "Moving a day"), style = T.h3, modifier = Modifier.padding(top = 18.dp))
                PicGrid(listOf(10, 20, 30, 45, 60, 90), 6, Modifier.padding(top = 8.dp), gap = 4.dp) { m, mod ->
                    PicTile("icon_walk", tr("$m мин", "$m min"), s.moveGoal == m, mod, pic = 28.dp) { set { it.copy(moveGoal = m) } }
                }
                Text(tr("Сон", "Sleep"), style = T.h3, modifier = Modifier.padding(top = 18.dp))
                PicGrid((6..10).toList(), 5, Modifier.padding(top = 8.dp), gap = 4.dp) { h, mod ->
                    PicTile("icon_sleep", tr("$h ч", "$h h"), s.sleepGoal == h, mod, pic = 30.dp) { set { it.copy(sleepGoal = h) } }
                }
            }
            "remind" -> {
                Text(tr("Питомец пишет в уведомлениях – прямо оттуда можно отметить «Принял».", "The pet writes in notifications – you can mark «Taken» right there."), style = T.small)
                NotifyStatus(Modifier.padding(top = 10.dp))
                Spacer(Modifier.height(8.dp))
                RemindRow("icon_pill", tr("Лекарства", "Medicine"), tr("в назначенное время", "on time"), s.remindMeds) { v -> set { it.copy(remindMeds = v) } }
                RemindRow("icon_tonometer", tr("Измерить давление", "Check blood pressure"), tr("в ${s.bpTimes.sorted().joinToString(" и ") { hhmm(it) }} – с кнопкой «Записать»", "at ${s.bpTimes.sorted().joinToString(" and ") { hhmm(it) }} – with a «Write it down» button"), s.remindBp) { v -> set { it.copy(remindBp = v) } }
                if (s.remindBp) Row(Modifier.fillMaxWidth().padding(start = 56.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    s.bpTimes.sorted().forEachIndexed { i, t ->
                        Row(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(C.card2).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("−", style = T.btn.copy(color = C.ink), modifier = Modifier.clip(CircleShape).clickable { set { st -> st.copy(bpTimes = st.bpTimes.sorted().toMutableList().also { l -> l[i] = (t - 30 + 1440) % 1440 }) } }.padding(horizontal = 10.dp))
                            Text((if (i == 0) tr("утро ", "am ") else tr("вечер ", "pm ")) + hhmm(t), style = T.smallB, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("+", style = T.btn.copy(color = C.ink), modifier = Modifier.clip(CircleShape).clickable { set { st -> st.copy(bpTimes = st.bpTimes.sorted().toMutableList().also { l -> l[i] = (t + 30) % 1440 }) } }.padding(horizontal = 10.dp))
                        }
                    }
                }
                RemindRow("icon_eat", tr("Еда", "Meals"), tr("${s.mealTimes.joinToString(", ") { hhmm(it) }} – если ещё не ели", "${s.mealTimes.joinToString(", ") { hhmm(it) }} – if you haven't eaten"), s.remindMeals) { v -> set { it.copy(remindMeals = v) } }
                MealTimes(s.mealTimes) { times, remap -> scope.launch { c.setMealTimes(times, remap) } }
                RemindRow("icon_water", tr("Вода", "Water"), tr("11:00, 15:00, 18:00 – если отстаёте", "11, 15, 18 – if behind"), s.remindWater) { v -> set { it.copy(remindWater = v) } }
                RemindRow("icon_sleep", tr("Отбой", "Bedtime"), tr("в ${hhmm((s.bedtime - 30 + 1440) % 1440)}, за полчаса до сна", "at ${hhmm((s.bedtime - 30 + 1440) % 1440)}"), s.remindBed) { v -> set { it.copy(remindBed = v) } }
                RemindRow("icon_heart", tr("«Полегчало?»", "«Feeling better?»"), tr("через 2 часа после лекарства от приступа", "2 hours after medicine for a spell"), s.reliefAsk) { v -> set { it.copy(reliefAsk = v) } }
                val exact = Build.VERSION.SDK_INT < 31 || ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
                if (!exact) Paper(Modifier.padding(top = 14.dp), tilt = -.5f) {
                    Text(tr("Чтобы лекарства напоминались минута в минуту", "So medicine is reminded on the minute"), style = T.note)
                    Text(tr("→ Разрешить точные будильники", "→ Allow exact alarms"), style = T.smallB.copy(color = C.rose),
                        modifier = Modifier.clickable { ctx.startActivity(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}"))) }.padding(vertical = 6.dp))
                }
            }
            "health" -> {
                RemindRow("icon_baro", tr("Давление за окном", "Pressure outside"), tr("питомец предупредит, если оно резко падает", "the pet warns when it falls fast"), s.pressureWarn) { v -> set { it.copy(pressureWarn = v) } }
                RemindRow("icon_period", tr("Месячные", "Periods"), tr("начало и конец в «Мне плохо», день цикла; сыщик свяжет симптомы с циклом", "start and end in «I feel bad», the cycle day; the detective ties symptoms to the cycle"), s.cycle) { v -> set { it.copy(cycle = v) } }
                RemindRow("icon_plaid", tr("Строгий режим", "Strict mode"), tr("неделя без заботы – питомец уедет к бабушке, вернётся после трёх дней заботы", "a week without care and the pet leaves for grandma's"), s.strict) { v -> set { it.copy(strict = v) } }
                Text(tr("Свои болячки", "Your own troubles"), style = T.h3, modifier = Modifier.padding(top = 16.dp))
                Text(tr("Появятся в «Мне плохо» рядом с остальными.", "They appear in «I feel bad» next to the others."), style = T.tiny)
                PicGrid(s.kinds, 3, Modifier.padding(top = 8.dp)) { k, mod ->
                    Box(mod) {
                        PicTile("icon_thermo", k, false, Modifier.fillMaxWidth(), pic = 40.dp) {}
                        Box(Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).clip(CircleShape).background(C.badSoft).clickable { set { it.copy(kinds = it.kinds - k) } }, contentAlignment = Alignment.Center) {
                            Text("×", style = T.smallB.copy(color = C.bad))
                        }
                    }
                }
                var kind by remember { mutableStateOf("") }
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Field(kind, { kind = it }, tr("Например, «Колено»", "E.g. «Knee»"), Modifier.weight(1f))
                    Btn(tr("Добавить", "Add"), small = true, enabled = kind.isNotBlank()) { val k = kind.trim(); set { it.copy(kinds = (it.kinds + k).distinct()) }; kind = "" }
                }
            }
            "look" -> {
                Text(tr("Тема", "Theme"), style = T.h3)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("light" to tr("Светлая", "Light"), "dark" to tr("При свече", "Candlelight"), "system" to tr("Как в системе", "System")).forEach { (t, l) ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).border(if (s.theme == t) 3.dp else 1.dp, if (s.theme == t) C.honey else C.line, RoundedCornerShape(14.dp))
                            .clickable { set { it.copy(theme = t) } }, horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.fillMaxWidth().aspectRatio(.8f)) {
                                Art("room_bg", Modifier.fillMaxSize(), ContentScale.Crop)
                                when (t) {
                                    "dark" -> Box(Modifier.fillMaxSize().background(Color(0x99140A20)))
                                    "system" -> Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Color.Transparent, .5f to Color.Transparent, .5f to Color(0x99140A20), 1f to Color(0x99140A20))))
                                }
                            }
                            Text(l, style = T.smallB, modifier = Modifier.padding(6.dp))
                        }
                    }
                }
                Text(tr("Язык", "Language"), style = T.h3, modifier = Modifier.padding(top = 18.dp))
                val cur = currentLanguage()
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("" to tr("Как в системе", "System"), "ru" to "Русский", "en" to "English").forEach { (t, l) ->
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (cur == t) C.acc else C.card2).clickable { setLanguage(t) }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Text(l, style = T.smallB.copy(color = if (cur == t) C.accInk else C.ink))
                        }
                    }
                }
                if (Lang.ru) {
                    Spacer(Modifier.height(10.dp))
                    Toggle("Пишу о себе в женском роде", "«поела», «приняла»", s.fem) { v -> set { it.copy(fem = v) }; app.caregotchi.domain.Gender.fem = v }
                }
            }
            "weather" -> {
                val o = Outside.of(s.outside)
                Row(Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).background(Brush.verticalGradient(listOf(Color(0xFFBFD8E6), Color(0xFFE9F0F2))))
                    .padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Art("icon_baro", Modifier.size(64.dp)); Spacer(Modifier.width(12.dp))
                    Column {
                        Text(s.town.ifEmpty { tr("Город не выбран", "No town yet") }, style = T.h2.copy(color = Color(0xFF2E3A44)))
                        if (o != null) Text("%+d° · %d %s".format(o.temp.toInt(), o.pressure.toInt(), tr("гПа", "hPa")) + if (o.fall <= -4) tr(" · падает на ${(-o.fall).toInt()}", " · falling ${(-o.fall).toInt()}") else "", style = T.bodyB.copy(color = Color(0xFF2E3A44)))
                    }
                }
                Text(tr("Другой город", "Another town"), style = T.h3, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
                var q by remember { mutableStateOf("") }
                var towns by remember { mutableStateOf<List<Town>>(emptyList()) }
                LaunchedEffect(q) { if (q.length >= 2) { kotlinx.coroutines.delay(400); towns = withContext(Dispatchers.IO) { runCatching { OutsideSource.towns(q, Lang.ru) }.getOrDefault(emptyList()) } } else towns = emptyList() }
                Field(q, { q = it }, tr("Начните писать название", "Start typing the name"), Modifier.fillMaxWidth())
                towns.forEach { t -> Row(Modifier.fillMaxWidth().clickable {
                    scope.launch { c.settings.update { it.copy(town = t.name, lat = t.lat, lon = t.lon, guessed = true, outside = "") }; c.refreshOutside(true) }; q = ""; towns = emptyList()
                }.padding(vertical = 10.dp)) { Text(t.name, style = T.bodyB); Text("  ${t.region}", style = T.small) } }
                Text(tr("Отправляются только координаты города – чтобы узнать погоду и давление.", "Only the town's coordinates are sent, for the weather and the pressure."), style = T.tiny, modifier = Modifier.padding(top = 10.dp))
            }
            "backup" -> {
                var done by remember { mutableStateOf<String?>(null) }
                val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
                    if (uri != null) scope.launch { runCatching { app.caregotchi.data.Backup.write(ctx, uri) }.onSuccess { done = tr("Копия сохранена", "Backup saved") }.onFailure { done = tr("Не получилось сохранить", "Couldn't save") } } }
                val load = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri != null) scope.launch { val ok = runCatching { app.caregotchi.data.Backup.read(ctx, uri) }.getOrDefault(false); done = if (ok) tr("Всё перенесено", "All restored") else tr("Это не копия «Заботогочи»", "Not a Caregotchi backup"); if (ok) { c.tick(); Reminders.schedule(ctx) } } }
                Text(tr("Один файл со всем: отметки, лекарства, приступы, питомцы, листики и вещи.", "One file with everything: marks, medicine, spells, pets, leaves and things."), style = T.body)
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StepCard("1", "icon_letter", tr("На старом телефоне – «Сохранить копию»", "On the old phone – «Save a backup»"), tr("Отправьте файл себе: в Telegram, на почту или на диск.", "Send the file to yourself: Telegram, mail or a drive.")) { save.launch("caregotchi-${java.time.LocalDate.now()}.json") }
                    StepCard("2", "icon_chest", tr("На новом – «Восстановить»", "On the new one – «Restore»"), tr("Выберите этот файл. Всё, что было на новом телефоне, заменится.", "Pick the file. Whatever is on the new phone is replaced.")) { load.launch(arrayOf("application/json", "application/octet-stream", "*/*")) }
                }
                done?.let { Text(it, style = T.bodyB.copy(color = C.acc), modifier = Modifier.padding(top = 10.dp)) }
                Text(tr("Можно и сразу при первом запуске: «У меня есть копия с другого телефона».", "Or right at first start: «I have a backup from another phone»."), style = T.tiny, modifier = Modifier.padding(top = 10.dp))
            }
            "doctor" -> {
                Paper(tilt = -.6f, stamp = "icon_doctor") {
                    Text(tr("Отчёт за 3 месяца", "A 3-month report"), style = T.note.copy(fontSize = androidx.compose.ui.unit.TextUnit(19f, androidx.compose.ui.unit.TextUnitType.Sp)))
                    listOf(tr("сколько было приступов и какой силы", "how many spells and how strong"), tr("календарь приступов и дней с обезболивающими", "a calendar of spells and painkiller days"),
                        tr("какие лекарства принимались и как часто", "which medicine was taken and how often"), tr("что помогало – по ответам «Полегчало?»", "what helped – from «Feeling better?»")).forEach {
                        Text("· $it", style = T.note.copy(fontSize = androidx.compose.ui.unit.TextUnit(15f, androidx.compose.ui.unit.TextUnitType.Sp)))
                    }
                }
                Btn(tr("Собрать PDF и отправить", "Make the PDF and send"), Modifier.fillMaxWidth().padding(top = 16.dp), icon = "icon_doctor") { scope.launch { Report.share(ctx, d) } }
                Text(tr("Это дневник, а не диагноз.", "This is a diary, not a diagnosis."), style = T.tiny, modifier = Modifier.padding(top = 8.dp))
            }
            else -> {
                val bot = "https://t.me/ArcaneWhisperBot?start="
                listOf(Triple("icon_thermo", tr("Сообщить об ошибке", "Report a bug"), "${bot}bug_caregotchi"), Triple("icon_seedling", tr("Предложить идею", "Suggest an idea"), "${bot}wish_caregotchi"),
                    Triple("icon_bell", tr("Следить за обновлениями", "Follow the updates"), "${bot}f_caregotchi"), Triple("icon_letter", tr("Чат StillCraft", "StillCraft chat"), "https://t.me/+Q4gFEDk8O842OGVi"),
                    Triple("icon_heart", tr("Поддержать «Заботогочи»", "Support Caregotchi"), "https://bigdevwhale.github.io/stillcraft/support/"), Triple("icon_star", tr("Другие приложения StillCraft", "More StillCraft apps"), "https://bigdevwhale.github.io/stillcraft/"),
                    Triple("icon_books", tr("Исходный код", "Source code"), "https://github.com/bigdevwhale/caregotchi")).forEach { (icon, l, u) ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(C.card2), contentAlignment = Alignment.Center) { Art(if (icon == "icon_books") "item_books" else icon, Modifier.size(30.dp)) }
                        Text(l, style = T.bodyB, modifier = Modifier.padding(start = 12.dp).weight(1f)); Text("›", style = T.h2.copy(color = C.mute))
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeCard(icon: String, label: String, min: Int, modifier: Modifier, tint: Color, onChange: (Int) -> Unit) {
    Column(modifier.shadow(3.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).background(Brush.verticalGradient(listOf(tint, C.card))).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Art(icon, Modifier.size(54.dp))
        Text(label, style = T.small.copy(color = Color(0xFF5A4636)))
        Text(hhmm(min), style = T.title.copy(color = Color(0xFF3B2E26)))
        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("−" to -30, "+" to 30).forEach { (t, dv) ->
                Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0x22000000)).clickable { onChange((min + dv + 1440) % 1440) }, contentAlignment = Alignment.Center) { Text(t, style = T.btn.copy(color = Color(0xFF3B2E26))) }
            }
        }
    }
}

/** The day as a ribbon from midnight to midnight: the awake part in daylight, the night in blue. */
@Composable
private fun DayRibbon(wake: Int, bed: Int) {
    val awake = ((bed - wake + 1440) % 1440)
    Column(Modifier.padding(top = 16.dp)) {
        Box(Modifier.fillMaxWidth().height(26.dp).clip(RoundedCornerShape(50)).background(Color(0xFF3E4A78))) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val a = wake / 1440f * w; val b = (wake + awake) / 1440f * w
                if (b <= w) drawRect(Brush.horizontalGradient(listOf(Color(0xFFFFD98A), Color(0xFFFFF0CF), Color(0xFFF2B27A)), a, b), androidx.compose.ui.geometry.Offset(a, 0f), androidx.compose.ui.geometry.Size(b - a, size.height))
                else { drawRect(Color(0xFFFFF0CF), androidx.compose.ui.geometry.Offset(a, 0f), androidx.compose.ui.geometry.Size(w - a, size.height)); drawRect(Color(0xFFFFF0CF), size = androidx.compose.ui.geometry.Size(b - w, size.height)) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) { listOf("0", "6", "12", "18", "24").forEach { Text(it, style = T.tiny, modifier = Modifier.weight(1f), textAlign = if (it == "24") TextAlign.End else TextAlign.Start) } }
        Text(tr("Бодрствуете ${awake / 60} ч ${awake % 60} мин, спите ${(1440 - awake) / 60} ч", "Awake ${awake / 60} h ${awake % 60} min, asleep ${(1440 - awake) / 60} h"), style = T.smallB, modifier = Modifier.padding(top = 6.dp))
    }
}

/**
 * One's own meal times, for those who eat by the clock: each with − and + by a quarter of an hour (never past its neighbours),
 * a cross to drop it, and a button for one more. Medicine tied to meals follows them.
 */
@Composable
private fun MealTimes(times: List<Int>, onChange: (List<Int>, (Int) -> Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(start = 56.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        times.forEachIndexed { i, t ->
            val lo = (times.getOrNull(i - 1) ?: -30) + 30; val hi = (times.getOrNull(i + 1) ?: 1470) - 30
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.card2).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("−", style = T.btn.copy(color = if (t - 15 >= lo) C.ink else C.line), modifier = Modifier.clip(CircleShape).clickable(enabled = t - 15 >= lo) { onChange(times.toMutableList().also { it[i] = t - 15 }) { x -> x } }.padding(horizontal = 10.dp))
                Text(word(MEALS, Plan.names(times)[i]) + " " + hhmm(t), style = T.smallB, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("+", style = T.btn.copy(color = if (t + 15 <= hi) C.ink else C.line), modifier = Modifier.clip(CircleShape).clickable(enabled = t + 15 <= hi) { onChange(times.toMutableList().also { it[i] = t + 15 }) { x -> x } }.padding(horizontal = 10.dp))
                if (times.size > 1) Text("×", style = T.smallB.copy(color = C.bad), modifier = Modifier.clip(CircleShape).clickable { onChange(times - t) { x -> if (x == i) -1 else if (x > i) x - 1 else x } }.padding(horizontal = 10.dp))
            }
        }
        val last = times.lastOrNull() ?: (8 * 60)
        if (times.size < 6 && last <= 22 * 60) Text(tr("+ ещё приём пищи", "+ one more meal"), style = T.smallB.copy(color = C.acc),
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onChange(times + minOf(last + 180, 23 * 60)) { x -> x } }.padding(horizontal = 8.dp, vertical = 6.dp))
        Text(tr("Своё время для тех, кто ест по часам. От него считаются и лекарства «до еды» и «после еды».", "Your own times, for those who eat by the clock. Medicine «before» or «after» a meal follows them too."), style = T.tiny)
    }
}

@Composable
private fun RemindRow(icon: String, title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(16.dp)).background(if (on) C.card else C.card2).clickable { onChange(!on) }.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(if (on) Color(0xFFFFF3D6) else C.bg), contentAlignment = Alignment.Center) { Art(icon, Modifier.size(32.dp), alpha = if (on) 1f else .5f, tint = if (on) null else grey) }
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) { Text(title, style = T.bodyB); Text(sub, style = T.tiny) }
        Switch(on, onChange, colors = SwitchDefaults.colors(checkedTrackColor = C.acc, checkedThumbColor = C.accInk, uncheckedTrackColor = C.card2, uncheckedBorderColor = C.line))
    }
}

@Composable
private fun StepCard(n: String, icon: String, title: String, sub: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)).background(C.card).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.TopStart) {
            Art(icon, Modifier.size(56.dp))
            Box(Modifier.size(22.dp).clip(CircleShape).background(C.honey), contentAlignment = Alignment.Center) { Text(n, style = T.tiny.copy(color = Color.White)) }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(title, style = T.bodyB); Text(sub, style = T.tiny) }
        Text("›", style = T.h2.copy(color = C.mute))
    }
}


/** Whether notifications really reach the user, read again each time the screen comes back (after the system settings). */
@Composable
fun rememberNotifyOn(): Boolean {
    val ctx = LocalContext.current
    var on by remember { mutableStateOf(androidx.core.app.NotificationManagerCompat.from(ctx).areNotificationsEnabled()) }
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(owner) {
        val o = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) on = androidx.core.app.NotificationManagerCompat.from(ctx).areNotificationsEnabled() }
        owner.lifecycle.addObserver(o)
        onDispose { owner.lifecycle.removeObserver(o) }
    }
    return on
}

/** Opens the app's own notification switches in the system settings. */
fun openNotifySettings(ctx: android.content.Context) {
    val i = Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, ctx.packageName)
    runCatching { ctx.startActivity(i) }.onFailure { ctx.startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))) }
}

/**
 * «Notifications are on ✓», or a button that turns them on: the system question first (Android 13+); when Android
 * no longer shows it (it answers at once), or on older Android, the app's notification settings open instead.
 * [quiet]: show nothing while they are on.
 */
@Composable
fun NotifyStatus(modifier: Modifier = Modifier, quiet: Boolean = false) {
    val ctx = LocalContext.current
    val on = rememberNotifyOn()
    var asked by remember { mutableStateOf(0L) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // a refusal that comes back at once means Android didn't even show the question
        if (!granted && System.currentTimeMillis() - asked < 600) openNotifySettings(ctx)
    }
    if (on) {
        if (!quiet) Row(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.okSoft).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Art("icon_bell", Modifier.size(26.dp)); Spacer(Modifier.width(8.dp))
            Text(tr("Уведомления включены ✓", "Notifications are on ✓"), style = T.bodyB)
        }
        return
    }
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.badSoft).clickable {
        val needAsk = Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        if (needAsk) { asked = System.currentTimeMillis(); ask.launch(Manifest.permission.POST_NOTIFICATIONS) } else openNotifySettings(ctx)
    }.padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Art("icon_bell", Modifier.size(30.dp)); Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("Уведомления выключены", "Notifications are off"), style = T.bodyB.copy(color = C.bad))
                Text(tr("Питомец не сможет напомнить о лекарствах. Нажмите, чтобы включить.", "The pet can't remind you of your medicine. Tap to turn them on."), style = T.small)
            }
        }
    }
}
