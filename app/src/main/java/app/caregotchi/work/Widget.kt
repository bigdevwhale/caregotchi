package app.caregotchi.work

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import app.caregotchi.R
import app.caregotchi.container
import app.caregotchi.domain.Lang
import app.caregotchi.domain.Mood
import app.caregotchi.domain.Stage
import app.caregotchi.domain.tr
import app.caregotchi.domain.hhmm
import app.caregotchi.domain.dayOf
import app.caregotchi.domain.minuteOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** The pet on the home screen with four buttons: ate, water, the next dose, «I feel bad». */
class WidgetProvider : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) { Widget.refresh(ctx) }
}

object Widget {
    /** The frame of a pet's sheet for its state: 0 baby, 1 youngster, 2 grown, 7 unwell, 8 asleep. */
    fun frame(species: String, stage: Int, mood: Mood, asleep: Boolean): String {
        val n = when { asleep -> 8; mood == Mood.SICK || mood == Mood.BED -> 7; else -> minOf(stage, 2) }
        return "pet_${species}_$n"
    }

    fun moodText(mood: Mood, asleep: Boolean) = when {
        asleep -> tr("Спит", "Asleep")
        mood == Mood.OK -> tr("Всё хорошо", "Cheerful")
        mood == Mood.LOW -> tr("Грустит", "A bit low")
        mood == Mood.SICK -> tr("Простуда", "Has a cold")
        mood == Mood.BED -> tr("Болеет", "In bed, ill")
        else -> tr("У бабушки", "At grandma's")
    }

    fun refresh(ctx: Context) { CoroutineScope(Dispatchers.Default).launch { draw(ctx) } }

    /**
     * Draws the widget and returns once it is drawn (#13). A button press waits for this: before, the redraw ran on
     * its own and the system could freeze the app first, so the sleep button looked untouched and a second press undid it.
     */
    suspend fun draw(ctx: Context) {
        val app = ctx.applicationContext
        val mgr = AppWidgetManager.getInstance(app)
        val ids = mgr.getAppWidgetIds(ComponentName(app, WidgetProvider::class.java))
        if (ids.isEmpty()) return
        run {
            runCatching {
                Lang.ru = app.resources.configuration.locales[0].language == "ru"
                val c = app.container; val b = c.book()
                val pet = c.dao.petList().firstOrNull { it.home && it.hatched > 0 }
                val v = RemoteViews(app.packageName, R.layout.widget)
                val mood = b.health.mood
                val res = pet?.let { app.resources.getIdentifier(frame(it.species, Stage.of(it.xp), mood, b.asleep), "drawable", app.packageName) } ?: 0
                if (res != 0) v.setImageViewResource(R.id.pet, res)
                v.setTextViewText(R.id.name, pet?.name ?: tr("Заботогочи", "Caregotchi"))
                val n = b.needs
                val low = listOf(n.eat to tr("голоден", "hungry"), n.water to tr("хочет пить", "thirsty"), n.pill to tr("ждёт лекарство", "waiting for medicine")).filter { it.first < .4 }.joinToString(", ") { it.second }
                // asleep: when the bed was marked, so a press is seen to have worked (#13)
                val bedAt = if (b.asleep) b.marks.filter { it.kind == "sleep" }.maxByOrNull { it.at }?.at else null
                // a press just made says so, or it looks as if nothing happened (#13): food, water, a pill in the last ten minutes
                val now = System.currentTimeMillis()
                val today = b.marks.filter { dayOf(it.at) == dayOf(now) }
                val just = b.marks.filter { it.kind in setOf("eat", "water", "pill") && now - it.at < 10 * 60_000 }.maxByOrNull { it.at }
                val done = just?.let { m -> "✓ " + when (m.kind) {
                    "water" -> tr("Вода записана · сегодня ", "Water noted · today ") + today.count { it.kind == "water" }
                    "eat" -> tr("Еда записана в ", "Meal noted at ") + hhmm(minuteOf(m.at))
                    else -> tr("Лекарство записано в ", "Medicine noted at ") + hhmm(minuteOf(m.at))
                } }
                v.setTextViewText(R.id.state, when {
                    done != null -> done
                    bedAt != null -> tr("✓ Спит с ", "✓ Asleep since ") + hhmm(minuteOf(bedAt))
                    else -> moodText(mood, b.asleep) + if (low.isNotEmpty()) " · $low" else ""
                })
                fun act(code: Int, a: String) = PendingIntent.getBroadcast(app, 9000 + code, Intent(app, ActionReceiver::class.java).putExtra("act", a), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                // food and water open the app and are noted there, so the meal can be told and the pet answers
                v.setOnClickPendingIntent(R.id.eat, Reminders.open(app, "eat"))
                v.setOnClickPendingIntent(R.id.water, Reminders.open(app, "water"))
                v.setOnClickPendingIntent(R.id.pill, if (b.dosesLeft().isNotEmpty()) act(3, "pill") else Reminders.open(app, "pill"))
                // bed at night, up in the morning: the button follows whether the pet is asleep
                v.setImageViewResource(R.id.sleep, if (b.asleep) R.drawable.icon_sunrise else R.drawable.icon_sleep)
                v.setInt(R.id.sleep, "setBackgroundResource", if (b.asleep) R.drawable.widget_btn_on else R.drawable.widget_btn)
                v.setOnClickPendingIntent(R.id.sleep, act(4, "sleep"))
                v.setOnClickPendingIntent(R.id.bad, Reminders.open(app, "bad"))
                v.setOnClickPendingIntent(R.id.pet, Reminders.open(app, ""))
                mgr.updateAppWidget(ids, v)
            }
        }
    }
}
