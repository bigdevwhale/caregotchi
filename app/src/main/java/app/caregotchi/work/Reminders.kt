package app.caregotchi.work

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.caregotchi.MainActivity
import app.caregotchi.R
import app.caregotchi.container
import app.caregotchi.data.Attack
import app.caregotchi.domain.Gender
import app.caregotchi.domain.Lang
import app.caregotchi.domain.Plan
import app.caregotchi.domain.Troubles
import app.caregotchi.domain.dayOf
import app.caregotchi.domain.doseNote
import app.caregotchi.domain.g
import app.caregotchi.domain.hhmm
import app.caregotchi.domain.minuteOf
import app.caregotchi.domain.startOf
import app.caregotchi.domain.today
import app.caregotchi.domain.tr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * The pet's reminders: doses on time (after-meal ones follow the meal), meals at one's own times and water if behind, bedtime,
 * a word the evening before a planned course, and «better?» two hours after a bad spell.
 * One alarm is set for the next thing due; when it rings, it says what's due and sets the next.
 */
object Reminders {
    const val CH_MEDS = "meds"; const val CH_CARE = "care"; const val CH_RELIEF = "relief"

    fun channels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_MEDS, tr("Лекарства", "Medicine"), NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel(CH_CARE, tr("Еда, вода и сон", "Food, water and sleep"), NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel(CH_RELIEF, tr("Полегчало?", "Feeling better?"), NotificationManager.IMPORTANCE_DEFAULT))
    }

    private fun pi(ctx: Context, code: Int, i: Intent) = PendingIntent.getBroadcast(ctx, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun set(ctx: Context, at: Long, p: PendingIntent) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p) else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
    }

    /** Works out the next reminder and sets the alarm for it. */
    suspend fun schedule(ctx: Context) {
        val c = ctx.container; val s = c.settings.now()
        val now = System.currentTimeMillis()
        val next = Plan.of(c.book(s, now), s).map { it.at }.filter { it > now + 30_000 }.minOrNull() ?: return
        set(ctx, next, pi(ctx, 1, Intent(ctx, RemindReceiver::class.java).putExtra("at", next)))
    }

    /** Asks «better?» two hours after the medicine for a bad spell. */
    fun relief(ctx: Context, a: Attack) {
        if (a.id == 0L) return
        set(ctx, System.currentTimeMillis() + 2 * 3600_000L, pi(ctx, 1000 + a.id.toInt(), Intent(ctx, ReliefReceiver::class.java).putExtra("id", a.id)))
    }

    fun open(ctx: Context, open: String = ""): PendingIntent = PendingIntent.getActivity(ctx, open.hashCode(),
        Intent(ctx, MainActivity::class.java).putExtra("open", open).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun post(ctx: Context, id: Int, ch: String, title: String, text: String, actions: List<Pair<String, PendingIntent>> = emptyList(), open: String = "") {
        val n = NotificationCompat.Builder(ctx, ch).setSmallIcon(R.drawable.ic_stat).setContentTitle(title).setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text)).setAutoCancel(true).setContentIntent(open(ctx, open))
        actions.forEach { (label, p) -> n.addAction(0, label, p) }
        runCatching { NotificationManagerCompat.from(ctx).notify(id, n.build()) }
    }
}

private fun BroadcastReceiver.async(block: suspend () -> Unit) {
    val r = goAsync()
    CoroutineScope(Dispatchers.Default).launch { try { block() } finally { r.finish() } }
}

/** A reminder rang: says what is due now, from the pet. */
class RemindReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) = async {
        val c = ctx.container; val s = c.settings.now(); Gender.fem = s.fem
        Lang.ru = ctx.resources.configuration.locales[0].language == "ru"
        val at = intent.getLongExtra("at", System.currentTimeMillis()); val min = minuteOf(at)
        val b = c.book(s)
        val pet = c.dao.petList().firstOrNull { it.home }?.name ?: tr("Заботогочи", "Caregotchi")
        val pings = Plan.of(b, s).filter { abs(it.at - at) < 60_000 }
        val d = b.todayCare
        val eatPI = PendingIntent.getBroadcast(ctx, 7, Intent(ctx, ActionReceiver::class.java).putExtra("act", "eat"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val waterPI = PendingIntent.getBroadcast(ctx, 8, Intent(ctx, ActionReceiver::class.java).putExtra("act", "water"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        for (p in pings) when (p.kind) {
            "pill" -> {
                val m = p.med ?: continue; val slot = p.slot
                if (b.marksOn(b.today).any { it.kind == "pill" && it.med == m.id && it.slot == slot }) continue
                val take = Intent(ctx, ActionReceiver::class.java).putExtra("act", "pill").putExtra("med", m.id).putExtra("slot", slot)
                val food = Plan.foodWord(m).let { if (it.isEmpty()) "" else " · $it" }
                Reminders.post(ctx, (m.id * 10 + slot / 60).toInt(), Reminders.CH_MEDS, tr("$pet: пора лекарство", "$pet: medicine time"),
                    "${m.name}${if (m.dose.isNotBlank()) " " + m.dose else ""}${doseNote(m, b.today).let { if (it.isEmpty()) "" else " · $it" }} · ${hhmm(slot)}$food", listOf(tr("Принял{|а}", "Taken").let(::g) to PendingIntent.getBroadcast(ctx, (m.id * 100 + slot).toInt(), take, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)))
            }
            "start" -> {
                val m = p.med ?: continue
                Reminders.post(ctx, 30 + (m.id % 1000).toInt(), Reminders.CH_MEDS, tr("Завтра начинаем «${m.name}»", "«${m.name}» starts tomorrow"),
                    tr("Первый приём – в ${hhmm(m.timeList().firstOrNull() ?: 0)}. $pet напомнит.", "The first dose is at ${hhmm(m.timeList().firstOrNull() ?: 0)}. $pet will remind you."), open = "med:${m.id}")
            }
            "meal" -> if (Plan.hungry(b, s, p.meal)) Reminders.post(ctx, 20, Reminders.CH_CARE, tr("$pet: пора поесть", "$pet is hungry"),
                when (Plan.names(s.mealTimes)[p.meal]) { "breakfast" -> tr("Завтракаем? Я – кашу.", "Breakfast? Porridge for me."); "lunch" -> tr("Обед! Без него я грустнею.", "Lunch! I get glum without it.")
                    "dinner" -> tr("Ужинать пора.", "Dinner time."); else -> tr("Перекусим? По часам – так по часам.", "A bite? On the clock it is.") },
                listOf(tr("Поел{|а}", "Ate").let(::g) to eatPI))
            "water" -> if (d.water < s.waterGoal * (min - s.wake) / (s.bedtime - s.wake).coerceAtLeast(60))
                Reminders.post(ctx, 21, Reminders.CH_CARE, tr("Стаканчик воды?", "A glass of water?"), tr("Сегодня выпито ${d.water} из ${s.waterGoal}.", "${d.water} of ${s.waterGoal} today."), listOf(tr("Выпил{|а}", "Drank").let(::g) to waterPI))
            "bed" -> Reminders.post(ctx, 22, Reminders.CH_CARE, tr("$pet зевает", "$pet is yawning"), tr("Через полчаса отбой. Отложим телефон?", "Bedtime in half an hour. Phone away?"))
            "bp" -> if (b.marks.none { it.kind == "bp" && at - it.at < 3 * 3600_000L })
                Reminders.post(ctx, 23, Reminders.CH_CARE, tr("Пора измерить давление", "Time to check your blood pressure"),
                    tr("Измерьте и запишите – $pet всё сохранит.", "Measure it and write it down – $pet will keep it."),
                    listOf(tr("Записать", "Write it down") to Reminders.open(ctx, "sheet:bp")), open = "sheet:bp")
        }
        Reminders.schedule(ctx)
    }
}

/** «Better?» after a bad spell's medicine. */
class ReliefReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) = async {
        val c = ctx.container
        Lang.ru = ctx.resources.configuration.locales[0].language == "ru"
        if (!c.settings.now().reliefAsk) return@async
        val id = intent.getLongExtra("id", 0); val a = c.dao.attack(id) ?: return@async
        if (a.relief > 0) return@async
        fun act(v: Int) = PendingIntent.getBroadcast(ctx, (id * 10 + v).toInt(), Intent(ctx, ActionReceiver::class.java).putExtra("act", "relief").putExtra("id", id).putExtra("v", v),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        Reminders.post(ctx, 5000 + id.toInt(), Reminders.CH_RELIEF, tr("Полегчало?", "Feeling better?"), tr("${Troubles.title(a.kind)}: прошло два часа после лекарства.", "${Troubles.title(a.kind)}: two hours since the medicine."),
            listOf(tr("Да", "Yes") to act(3), tr("Немного", "A little") to act(2), tr("Нет", "No") to act(1)))
    }
}

/** Buttons in the notifications and on the widget. */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) = async {
        val c = ctx.container; Gender.fem = c.settings.now().fem
        when (intent.getStringExtra("act")) {
            "pill" -> {
                val med = intent.getLongExtra("med", 0); val slot = intent.getIntExtra("slot", -1)
                if (med == 0L) {
                    // the dose nearest now, not a morning one missed hours ago (#21)
                    val b = c.book()
                    val next = b.dosesLeft().minByOrNull { abs(it.second - b.nowMin) }
                    if (next != null) c.log("pill", med = next.first.id, slot = next.second)
                } else c.log("pill", med = med, slot = slot)
                NotificationManagerCompat.from(ctx).cancel((med * 10 + slot / 60).toInt())
            }
            "eat" -> { c.log("eat", sub = mealNow(c.settings.now().mealTimes)); NotificationManagerCompat.from(ctx).cancel(20) }
            "water" -> { c.log("water"); NotificationManagerCompat.from(ctx).cancel(21) }
            // a second press within three seconds is an accidental double tap: it doesn't undo the first (#13)
            "sleep" -> { val last = c.book().marks.filter { it.kind == "sleep" || it.kind == "wake" }.maxByOrNull { it.at }
                if (last == null || System.currentTimeMillis() - last.at > 3_000) c.log(if (c.book().asleep) "wake" else "sleep") }
            "relief" -> {
                val id = intent.getLongExtra("id", 0)
                c.dao.attack(id)?.let { c.dao.update(it.copy(relief = intent.getIntExtra("v", 0), end = if (intent.getIntExtra("v", 0) == 3 && it.end == 0L) System.currentTimeMillis() else it.end)) }
                NotificationManagerCompat.from(ctx).cancel(5000 + id.toInt())
            }
        }
        Widget.draw(ctx)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) = async { Reminders.schedule(ctx) }
}

/** The meal that fits the hour. */
fun mealNow(times: List<Int>, at: Long = System.currentTimeMillis()): String = Plan.mealAt(times, minuteOf(at))

