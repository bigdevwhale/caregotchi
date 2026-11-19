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

    fun refresh(ctx: Context) {
        val app = ctx.applicationContext
        val mgr = AppWidgetManager.getInstance(app)
        val ids = mgr.getAppWidgetIds(ComponentName(app, WidgetProvider::class.java))
        if (ids.isEmpty()) return
        CoroutineScope(Dispatchers.Default).launch {
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
                v.setTextViewText(R.id.state, moodText(mood, b.asleep) + if (low.isNotEmpty()) " · $low" else "")
                fun act(code: Int, a: String) = PendingIntent.getBroadcast(app, 9000 + code, Intent(app, ActionReceiver::class.java).putExtra("act", a), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                v.setOnClickPendingIntent(R.id.eat, act(1, "eat"))
                v.setOnClickPendingIntent(R.id.water, act(2, "water"))
                v.setOnClickPendingIntent(R.id.pill, if (b.dosesLeft().isNotEmpty()) act(3, "pill") else Reminders.open(app, "pill"))
                v.setOnClickPendingIntent(R.id.bad, Reminders.open(app, "bad"))
                v.setOnClickPendingIntent(R.id.pet, Reminders.open(app, ""))
                mgr.updateAppWidget(ids, v)
            }
        }
    }
}
