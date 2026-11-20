package app.caregotchi.domain

import app.caregotchi.data.Med
import app.caregotchi.data.Settings
import kotlin.math.abs
import kotlin.math.max

/**
 * One reminder: [kind] is pill (with [med] and [slot]), meal (with [meal], the index of the meal time), water, bed, bp,
 * or start (a planned course of [med] begins tomorrow).
 */
data class Ping(val at: Long, val kind: String, val med: Med? = null, val slot: Int = -1, val meal: Int = -1)

/** When the pet reminds of what, today and tomorrow. Pure, so the alarm and its receiver agree on it. */
object Plan {
    fun of(b: Book, s: Settings): List<Ping> {
        val out = mutableListOf<Ping>()
        val t = b.today
        fun at(day: Long, min: Int) = startOf(day) + min * 60_000L
        fun daily(kind: String, min: Int, meal: Int = -1) { for (d in t..t + 1) out += Ping(at(d, min), kind, meal = meal) }
        if (s.remindMeds) for (m in b.meds) {
            if (m.archived || m.asNeeded) continue
            val first = max(m.from, if (m.created > 0) dayOf(m.created) else Long.MIN_VALUE)
            for (d in t..t + 1) {
                if (d < first || (m.until > 0 && d > m.until) || m.halvesOn(d) == 0) continue
                for (slot in m.timeList()) out += Ping(at(d, slot) + shift(b, s, m, slot, d), "pill", med = m, slot = slot)
            }
            // a course planned ahead: a word the evening before it starts
            if (m.from > t && m.from - 1 in t..t + 1) out += Ping(at(m.from - 1, 20 * 60), "start", med = m)
        }
        if (s.remindMeals) s.mealTimes.forEachIndexed { i, min -> daily("meal", min, i) }
        if (s.remindWater) listOf(11 * 60, 15 * 60, 18 * 60).forEach { daily("water", it) }
        if (s.remindBed) daily("bed", (s.bedtime - 30 + 1440) % 1440)
        if (s.remindBp) s.bpTimes.forEach { daily("bp", it) }
        return out.sortedBy { it.at }
    }

    /**
     * For medicine taken after a meal: once the meal is marked, the reminder moves to the meal's real time plus the gap,
     * so a late lunch moves the pill too (never earlier than planned: then the receiver and the alarm still agree). Milliseconds to add.
     */
    private fun shift(b: Book, s: Settings, m: Med, slot: Int, day: Long): Long {
        if (m.food != 3 || day != b.today) return 0
        val meal = m.mealList().firstOrNull { i -> s.mealTimes.getOrNull(i)?.let { (it + m.gap) % 1440 == slot } == true } ?: return 0
        val mt = s.mealTimes[meal]
        val eat = b.marksOn(day).filter { it.kind == "eat" && abs(minuteOf(it.at) - mt) <= 120 }.minByOrNull { abs(minuteOf(it.at) - mt) } ?: return 0
        return (eat.at + m.gap * 60_000L - (startOf(day) + slot * 60_000L)).coerceAtLeast(0)
    }

    /** No meal marked yet around the [i]-th meal time today: from halfway since the meal before (two hours at most). */
    fun hungry(b: Book, s: Settings, i: Int): Boolean {
        val times = s.mealTimes
        val m = times.getOrNull(i) ?: return false
        val prev = times.getOrNull(i - 1) ?: (m - 240)
        val from = m - minOf(120, (m - prev) / 2)
        return b.marksOn(b.today).none { it.kind == "eat" && minuteOf(it.at) >= from }
    }

    /** What a meal at minute [min] is called. */
    fun mealOf(min: Int): String = when (min) {
        in 4 * 60 until 11 * 60 -> "breakfast"
        in 11 * 60 until 16 * 60 -> "lunch"
        in 16 * 60 until 22 * 60 -> "dinner"
        else -> "snack"
    }

    /** The medicine's word about food, for its slip and its reminder: «за 30 мин до еды». */
    fun foodWord(m: Med): String = when (m.food) {
        1 -> tr("за ${gapText(m.gap)} до еды", "${gapText(m.gap)} before a meal")
        2 -> tr("во время еды", "with a meal")
        3 -> tr("через ${gapText(m.gap)} после еды", "${gapText(m.gap)} after a meal")
        else -> ""
    }

    fun gapText(min: Int): String = when {
        min % 60 == 0 && min >= 60 -> if (min == 60) tr("час", "an hour") else tr("${min / 60} ч", "${min / 60} h")
        else -> tr("$min мин", "$min min")
    }
}
