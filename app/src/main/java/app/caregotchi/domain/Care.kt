package app.caregotchi.domain

import app.caregotchi.data.Air
import app.caregotchi.data.Attack
import app.caregotchi.data.Mark
import app.caregotchi.data.Med
import app.caregotchi.data.Settings
import java.time.Instant
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min

/** Minute of the day a moment falls on. */
fun minuteOf(ms: Long): Int = Instant.ofEpochMilli(ms).atZone(zone).let { it.hour * 60 + it.minute }
fun startOf(day: Long): Long = LocalDate.ofEpochDay(day).atStartOfDay(zone).toInstant().toEpochMilli()
fun hhmm(min: Int): String = "%d:%02d".format(min / 60 % 24, min % 60)

/** One day of care, put together from the marks. */
data class Day(
    val day: Long,
    val meals: Int = 0,
    val breakfast: Boolean = false,
    val dinner: Boolean = false,
    val water: Int = 0,
    val waterNoon: Int = 0,
    val due: Int = 0,               // scheduled doses that day (so far, for today)
    val taken: Int = 0,
    val onTime: Int = 0,
    val slept: Double = -1.0,       // hours slept the night that ended this morning; -1 unknown
    val bedEarly: Boolean = false,  // went to bed between 20:00 and midnight
    val wokeEarly: Boolean = false, // up before 8
    val moves: Int = 0,
    val moveMins: Int = 0,
    val caffeine: Int = 0,          // mg of caffeine that day
    val lateCaffeine: Boolean = false, // caffeine after 16:00
    val wholesome: Int = 0,         // meals marked wholesome
    val junk: Int = 0,              // meals marked fast food or sweets
    val veg: Boolean = false,       // vegetables or fruit eaten
    val attacks: Int = 0,
    val plaid: Boolean = false,
    val marks: Int = 0,
    val painPill: Boolean = false,
    val score: Double = 0.0,
) {
    val any get() = marks > 0 || attacks > 0
    val good get() = score >= .6
}

/** How the pet is: from the last days of care. */
enum class Mood { OK, LOW, SICK, BED, AWAY }

data class Needs(val eat: Double, val water: Double, val pill: Double, val sleep: Double, val move: Double, val asleep: Boolean) {
    val min get() = minOf(eat, water, pill, sleep)
}

/**
 * Everything worked out from the records, once per change: each day's care, the pet's state, the streak.
 * Pure, so it can be tested and drawn without a phone.
 */
class Book(
    val marks: List<Mark>, val meds: List<Med>, val attacks: List<Attack>, val air: List<Air>, val s: Settings,
    val now: Long = System.currentTimeMillis(),
) {
    val today = dayOf(now)
    val nowMin = minuteOf(now)
    private val byDay: Map<Long, List<Mark>> = marks.groupBy { dayOf(it.at) }
    private val attacksByDay: Map<Long, List<Attack>> = attacks.groupBy { dayOf(it.start) }
    val first: Long = listOfNotNull(marks.minOfOrNull { dayOf(it.at) }, attacks.minOfOrNull { dayOf(it.start) }).minOrNull() ?: today
    val medById = meds.associateBy { it.id }

    /** Scheduled doses of [m] on [day] (for today only those already due). */
    fun dueOn(m: Med, day: Long): List<Int> {
        if (m.asNeeded || m.archived) return emptyList()
        val created = if (m.created > 0) dayOf(m.created) else Long.MIN_VALUE
        val from = max(m.from, created)
        if (day < from || (m.until > 0 && day > m.until)) return emptyList()
        val t = m.timeList()
        return if (day == today) t.filter { it <= nowMin } else t
    }

    /** The sleep that ended on [day]: from the last «sleep» before the first «wake» of the day. */
    private fun sleptOn(day: Long): Double {
        val wake = byDay[day]?.filter { it.kind == "wake" }?.minByOrNull { it.at } ?: return -1.0
        val bed = marks.filter { it.kind == "sleep" && it.at < wake.at && wake.at - it.at < 16 * 3600_000L }.maxByOrNull { it.at } ?: return -1.0
        return (wake.at - bed.at) / 3600_000.0
    }

    fun dayOfCare(day: Long): Day {
        val ms = byDay[day].orEmpty()
        val eats = ms.filter { it.kind == "eat" }
        val water = ms.count { it.kind == "water" }
        var due = 0
        for (m in meds) due += dueOn(m, day).size
        val pills = ms.filter { it.kind == "pill" && it.slot >= 0 }.distinctBy { it.med to it.slot }
        val taken = min(pills.size, max(due, pills.size))
        val slept = sleptOn(day)
        val beds = ms.filter { it.kind == "sleep" }.map { minuteOf(it.at) }
        val plaid = day in s.plaidDays
        val att = attacksByDay[day].orEmpty()
        val painPill = ms.any { it.kind == "pill" && (medById[it.med]?.pain ?: 0) > 0 }
        val moves = ms.filter { it.kind == "move" }
        val rated = eats.filter { it.q > 0 }
        val parts = mutableListOf<Double>()
        // food: how many meals, a little coloured by how wholesome they were (never more than a quarter of it)
        val enough = min(1.0, eats.count { it.sub != "snack" }.coerceAtMost(3) / 3.0 + if (eats.any { it.sub == "snack" }) .1 else 0.0)
        val kind = if (rated.isEmpty()) .85 else rated.map { (it.q - 1) / 2.0 }.average()
        parts += enough * (.75 + .25 * kind)
        // moving: a day without sport is still half fine; the goal makes it whole
        parts += .5 + .5 * min(1.0, moves.sumOf { it.mins } / s.moveGoal.coerceAtLeast(1).toDouble())
        parts += min(1.0, water / s.waterGoal.coerceAtLeast(1).toDouble())
        if (due > 0) parts += min(1.0, taken / due.toDouble())
        parts += when { slept >= 0 -> min(1.0, slept / (s.sleepGoal - 1).coerceAtLeast(1)); ms.isNotEmpty() -> .5; else -> 0.0 }
        var score = parts.average()
        if (plaid || att.isNotEmpty()) score = max(score, .6)
        return Day(day, meals = eats.size, breakfast = eats.any { it.sub == "breakfast" }, dinner = eats.any { it.sub == "dinner" },
            water = water, waterNoon = ms.count { it.kind == "water" && minuteOf(it.at) < 12 * 60 },
            due = due, taken = taken, onTime = pills.count { it.late <= 60 }, slept = slept,
            bedEarly = beds.any { it in 20 * 60 until 24 * 60 }, wokeEarly = ms.any { it.kind == "wake" && minuteOf(it.at) < 8 * 60 },
            caffeine = ms.filter { it.kind == "caf" }.sumOf { it.mins }, lateCaffeine = ms.any { it.kind == "caf" && minuteOf(it.at) >= 16 * 60 },
            moves = moves.size, moveMins = moves.sumOf { it.mins }, wholesome = eats.count { it.q == 3 }, junk = eats.count { it.q == 1 },
            veg = eats.any { "veg" in it.tags || "fruit" in it.tags }, attacks = att.size, plaid = plaid, marks = ms.size, painPill = painPill || att.any { a -> a.list(a.took).any { (it.toLongOrNull()?.let { id -> medById[id]?.pain } ?: 0) > 0 } },
            score = score)
    }

    val days: List<Day> by lazy { (first..today).map { dayOfCare(it) } }
    val todayCare: Day get() = days.last()
    fun care(day: Long): Day? = if (day in first..today) days[(day - first).toInt()] else null

    /** The pet's state after each finished day, and how many times it got well again. */
    data class Health(val mood: Mood, val recoveries: Int, val bedRecoveries: Int, val sickDays: Int)

    val health: Health by lazy {
        var mood = Mood.OK; var poor = 0; var good = 0; var empty = 0; var rec = 0; var bedRec = 0; var sick = 0; var wasBed = false
        for (d in days.dropLast(1)) {
            if (d.score < .35) { poor++; good = 0 } else if (d.good) { good++; poor = 0 } else { poor = 0; good = 0 }
            empty = if (d.any || d.plaid) 0 else empty + 1
            mood = when (mood) {
                Mood.AWAY -> if (good >= 3) Mood.OK else Mood.AWAY
                Mood.OK -> if (poor >= 2) Mood.SICK else if (d.score < .5) Mood.LOW else Mood.OK
                Mood.LOW -> if (poor >= 2) Mood.SICK else if (d.good) Mood.OK else Mood.LOW
                Mood.SICK -> if (good >= 2) { rec++; if (wasBed) { bedRec++; wasBed = false }; Mood.OK } else if (poor >= 4) { wasBed = true; Mood.BED } else Mood.SICK
                Mood.BED -> if (good >= 2) Mood.SICK else Mood.BED
            }
            if (s.strict && empty >= 7) mood = Mood.AWAY
            if (mood == Mood.SICK || mood == Mood.BED) sick++
        }
        Health(mood, rec, bedRec, sick)
    }

    /** Days in a row with any care (or under the blanket), up to today; today counts once something is marked. */
    val streak: Int by lazy {
        var n = 0
        val ds = days
        var i = ds.size - 1
        if (i >= 0 && !(ds[i].any || ds[i].plaid)) i--
        while (i >= 0 && (ds[i].any || ds[i].plaid)) { n++; i-- }
        n
    }

    val asleep: Boolean get() {
        val last = marks.filter { it.kind == "sleep" || it.kind == "wake" }.maxByOrNull { it.at } ?: return false
        return last.kind == "sleep" && now - last.at < 14 * 3600_000L
    }

    /** How full each need is right now, 0..1. Under the blanket nothing runs down. */
    val needs: Needs by lazy {
        if (today in s.plaidDays) return@lazy Needs(1.0, 1.0, 1.0, 1.0, 1.0, asleep)
        val sleeping = asleep
        val lastMeal = marks.filter { it.kind == "eat" }.maxByOrNull { it.at }
        val ref = if (sleeping) marks.filter { it.kind == "sleep" }.maxOf { it.at } else now
        val eat = if (lastMeal == null) .3 else (1 - ((ref - lastMeal.at) / 3600_000.0 - 3) / 9).coerceIn(0.0, 1.0)
        val t = todayCare
        val span = (s.bedtime - s.wake).coerceAtLeast(60)
        val part = ((nowMin - s.wake).toDouble() / span).coerceIn(0.0, 1.0)
        val expected = max(1.0, s.waterGoal * part)
        val water = (t.water / expected).coerceIn(0.0, 1.0)
        val pill = if (t.due == 0) 1.0 else (t.taken / t.due.toDouble()).coerceIn(0.0, 1.0)
        val last = care(today)?.slept ?: -1.0
        var sleep = if (last >= 0) (last / s.sleepGoal).coerceIn(0.0, 1.0) else .6
        if (!sleeping && nowMin > s.bedtime + 60) sleep = (sleep - (nowMin - s.bedtime - 60) / 240.0).coerceAtLeast(0.0)
        val move = (t.moveMins / s.moveGoal.coerceAtLeast(1).toDouble()).coerceIn(0.0, 1.0)
        Needs(eat, water, pill, sleep, move, sleeping)
    }

    /** Doses still due today: (medicine, minute), the next first. */
    fun dosesLeft(): List<Pair<Med, Int>> {
        val taken = byDay[today].orEmpty().filter { it.kind == "pill" }.map { it.med to it.slot }.toSet()
        return meds.filter { !it.archived && !it.asNeeded && (it.until == 0L || today <= it.until) && it.from <= today }
            .flatMap { m -> m.timeList().map { m to it } }.filter { (m, t) -> (m.id to t) !in taken }.sortedBy { it.second }
    }

    /** Painkiller days this calendar month and the line not to cross (10 with a triptan, else 15). */
    fun painDays(): Pair<Int, Int> {
        val d = LocalDate.ofEpochDay(today)
        val from = d.withDayOfMonth(1).toEpochDay()
        val n = days.count { it.day >= from && it.painPill }
        val line = if (meds.any { it.pain == 2 }) 10 else 15
        return n to line
    }

    fun air(day: Long): Air? = air.firstOrNull { it.day == day }

    /** Pills left of [m] in days, at its schedule; null when the stock isn't counted. */
    fun daysLeft(m: Med): Int? {
        if (m.stock <= 0 && m.perDose > 0 && m.times.isNotBlank()) return null
        val perDay = m.timeList().size * m.perDose
        return if (perDay == 0 || m.stock <= 0) null else m.stock / perDay
    }

    /** How a medicine grows on the windowsill: 0 sprout .. 2 in bloom, 3 wilting; from the last fortnight. */
    fun potStage(m: Med): Int {
        if (m.asNeeded) return if (attacks.isEmpty()) 0 else 1
        var due = 0; var took = 0
        val from = max(first, today - 13)
        for (d in from..today) {
            val n = dueOn(m, d).size
            due += n
            took += min(n, byDay[d].orEmpty().count { it.kind == "pill" && it.med == m.id && it.slot >= 0 })
        }
        if (due == 0) return 0
        val r = took / due.toDouble()
        val age = today - max(m.from, if (m.created > 0) dayOf(m.created) else first)
        return when { r < .6 -> 3; age >= 10 && r >= .85 -> 2; age >= 3 -> 1; else -> 0 }
    }

    /** Days in a row each scheduled dose of [m] was taken. */
    fun medRun(m: Med): Int {
        var n = 0; var d = today
        if (dueOn(m, d).size > byDay[d].orEmpty().count { it.kind == "pill" && it.med == m.id }) d--
        while (d >= first) {
            val due = dueOn(m, d).size
            if (due == 0) { if (d < m.from || d < dayOf(m.created.coerceAtLeast(1))) break; d--; continue }
            if (byDay[d].orEmpty().count { it.kind == "pill" && it.med == m.id && it.slot >= 0 } < due) break
            n++; d--
        }
        return n
    }

    fun marksOn(day: Long) = byDay[day].orEmpty()
    fun attacksOn(day: Long) = attacksByDay[day].orEmpty()
}
