package app.caregotchi.domain

import app.caregotchi.data.Attack
import app.caregotchi.data.Settings
import java.time.LocalDate

/** A kind of bad spell. [dark]: it gets the dimmed screen with big buttons (heads hurt in the light). */
class Trouble(val id: String, val ru: String, val en: String, val icon: String, val dark: Boolean = false) {
    val title get() = tr(ru, en)
}

object Troubles {
    val all = listOf(
        Trouble("migraine", "Мигрень", "Migraine", "migraine", dark = true),
        Trouble("headache", "Голова болит", "Headache", "headache", dark = true),
        Trouble("stomach", "Живот", "Stomach", "stomach"),
        Trouble("back", "Спина", "Back", "back"),
        Trouble("anxiety", "Тревога", "Anxiety", "anxiety"),
        Trouble("allergy", "Аллергия", "Allergy", "allergy"),
        Trouble("pressure", "Давление", "Blood pressure", "tonometer"),
        Trouble("cold", "Простуда", "A cold", "cold"),
        Trouble("insomnia", "Бессонница", "Insomnia", "sleep"),
        Trouble(PERIOD, "Месячные", "Period", "period"),
    )
    /** What «I feel bad» offers: the built-in troubles and one's own. The period is apart – it's a state, not a spell. */
    fun kinds(s: Settings) = all.filter { it.id != PERIOD } + s.kinds.map { of(it) }
    fun of(id: String) = all.firstOrNull { it.id == id } ?: Trouble(id, id, id, "thermo")
    fun title(id: String) = of(id).title

    val sides = listOf("left" to ("Слева" to "Left"), "right" to ("Справа" to "Right"), "both" to ("С обеих сторон" to "Both sides"), "back" to ("Затылок" to "Back of the head"), "front" to ("Лоб" to "Forehead"))
    val symptoms = listOf("aura" to ("Аура" to "Aura"), "nausea" to ("Тошнота" to "Nausea"), "light" to ("Свет режет" to "Light hurts"), "sound" to ("Звуки мешают" to "Sound hurts"),
        "smell" to ("Запахи" to "Smells"), "dizzy" to ("Кружится голова" to "Dizzy"), "neck" to ("Шея" to "Neck"), "numb" to ("Онемение" to "Numbness"))
    val triggers = listOf("sleep" to ("Недосып" to "Little sleep"), "meal" to ("Пропустил{|а} еду" to "Skipped a meal"), "water" to ("Мало воды" to "Little water"),
        "stress" to ("Стресс" to "Stress"), "screen" to ("Экран" to "Screens"), "alcohol" to ("Алкоголь" to "Alcohol"), "weather" to ("Погода" to "Weather"),
        "cycle" to ("Цикл" to "Cycle"), "smell" to ("Запах" to "A smell"), "coffee" to ("Кофе" to "Coffee"), "food" to ("Еда" to "Some food"))
    fun word(list: List<Pair<String, Pair<String, String>>>, id: String) = list.firstOrNull { it.first == id }?.second?.let { tr(it.first, it.second) } ?: id

    /** Triggers already plain from the day's marks: short sleep, no meal yet by noon, little water, a pressure fall. */
    fun guess(b: Book, at: Long): List<String> {
        val d = b.care(dayOf(at)) ?: return emptyList()
        val out = mutableListOf<String>()
        if (d.slept in 0.0..5.99) out += "sleep"
        if (d.meals == 0 && minuteOf(at) > 11 * 60) out += "meal"
        if (d.water < b.s.waterGoal / 3 && minuteOf(at) > 13 * 60) out += "water"
        val a = b.air(dayOf(at)); if (a != null && a.max - a.min >= 6) out += "weather"
        if (d.caffeine >= 300) out += "coffee"
        val day = dayOf(at)
        if (d.period || b.nextPeriod?.let { it - day in 0..2 } == true) out += "cycle"
        return out
    }
}

/** A pattern the detective found, with a stable [id] to keep or strike. */
data class Clue(val id: String, val text: String, val strength: Double)

object Detective {
    private fun times(x: Double) = if (Lang.ru) fmt1(x).replace(".", ",") else fmt1(x)

    /** Patterns between the bad spells and the days before them, over the last four months. */
    fun clues(b: Book): List<Clue> {
        val from = maxOf(b.first, b.today - 120)
        val days = b.days.filter { it.day >= from && it.day < b.today }
        val spells = days.count { it.attacks > 0 }
        if (spells < 3) return emptyList()
        val out = mutableListOf<Clue>()
        fun factor(id: String, test: (Day) -> Boolean?, ru: (Int, Int, String) -> String, en: (Int, Int, String) -> String) {
            val known = days.mapNotNull { d -> test(d)?.let { d to it } }
            val yes = known.filter { it.second }; val no = known.filter { !it.second }
            if (yes.size < 3 || no.size < 3) return
            val a = yes.count { it.first.attacks > 0 }; val c = no.count { it.first.attacks > 0 }
            val r1 = a / yes.size.toDouble(); val r0 = c / no.size.toDouble()
            if (a < 2 || r1 < 1.6 * r0 || r1 == 0.0) return
            val ratio = if (r0 == 0.0) 3.0 else r1 / r0
            val total = a + c
            out += Clue("f:$id", tr(ru(a, total, times(ratio)), en(a, total, times(ratio))), ratio)
        }
        fun prev(d: Day) = b.care(d.day - 1)
        factor("sleep", { d -> d.slept.takeIf { it >= 0 }?.let { it < 6 } },
            { a, t, x -> "$a из $t приступов были после ночи короче 6 часов. В такие дни голова болела в $x раза чаще." },
            { a, t, x -> "$a of $t spells came after a night shorter than 6 hours – $x times as often as after a full night." })
        factor("breakfast", { d -> if (d.marks > 0) !d.breakfast else null },
            { a, t, x -> "В дни без завтрака приступ был в $x раза чаще ($a из $t)." },
            { a, t, x -> "On days without breakfast a spell came $x times as often ($a of $t)." })
        factor("water", { d -> if (d.marks > 0) d.water < b.s.waterGoal / 2 else null },
            { a, t, x -> "Когда воды было меньше половины нормы, приступ был в $x раза чаще ($a из $t)." },
            { a, t, x -> "With less than half your water a spell came $x times as often ($a of $t)." })
        factor("doses", { d -> if (d.due > 0) d.taken < d.due else null },
            { a, t, x -> "После пропущенных лекарств голова болела в $x раза чаще ($a из $t)." },
            { a, t, x -> "After missed doses a spell came $x times as often ($a of $t)." })
        factor("late", { d -> prev(d)?.takeIf { it.marks > 0 }?.let { !it.bedEarly } },
            { a, t, x -> "После ночей, когда ложились за полночь, приступ бывал в $x раза чаще ($a из $t)." },
            { a, t, x -> "After going to bed past midnight a spell came $x times as often ($a of $t)." })
        factor("still", { d -> if (d.marks > 0) d.moves == 0 else null },
            { a, t, x -> "В дни совсем без движения голова болела в $x раза чаще ($a из $t)." },
            { a, t, x -> "On days with no moving at all a spell came $x times as often ($a of $t)." })
        factor("caffeine", { d -> if (d.marks > 0) d.caffeine >= 300 else null },
            { a, t, x -> "В дни, когда кофеина было 300 мг и больше, приступ был в $x раза чаще ($a из $t)." },
            { a, t, x -> "On days with 300 mg of caffeine or more a spell came $x times as often ($a of $t)." })
        // only for those who drink coffee at all: a day without it can bring a withdrawal headache
        if (days.count { it.caffeine > 0 } >= days.size / 2) factor("nocoffee", { d -> if (d.marks > 0) d.caffeine == 0 else null },
            { a, t, x -> "В дни совсем без кофеина голова болела в $x раза чаще ($a из $t) – похоже на отмену." },
            { a, t, x -> "On days with no caffeine at all a spell came $x times as often ($a of $t) – it looks like withdrawal." })
        factor("junk", { d -> if (d.meals > 0) d.junk > 0 else null },
            { a, t, x -> "После фастфуда или сладкого приступ бывал в $x раза чаще ($a из $t)." },
            { a, t, x -> "After fast food or sweets a spell came $x times as often ($a of $t)." })
        // around the period: two days before it and its days (menstrual migraine)
        if (b.periods.size >= 2) factor("cycle", { d -> if (d.cycleDay > 0 || d.toNext > 0) d.period || d.toNext in 1..2 else null },
            { a, t, x -> "Около месячных (за пару дней и во время) приступ был в $x раза чаще ($a из $t)." },
            { a, t, x -> "Around the period (two days before and during it) a spell came $x times as often ($a of $t)." })
        factor("air", { d -> b.air(d.day)?.let { it.max - it.min >= 6 } },
            { a, t, x -> "Когда давление скакало на 6 гПа и больше, приступ был в $x раза чаще ($a из $t)." },
            { a, t, x -> "When the pressure swung by 6 hPa or more a spell came $x times as often ($a of $t)." })
        // the weekday
        val perDow = days.groupBy { LocalDate.ofEpochDay(it.day).dayOfWeek.value }
        val avg = spells / days.size.toDouble()
        perDow.forEach { (dow, ds) ->
            val n = ds.count { it.attacks > 0 }
            if (n >= 3 && n / ds.size.toDouble() >= 2 * avg) out += Clue("w:$dow", tr("Чаще всего голова болит по ${dowRu(dow)}: $n раз.", "Most spells come on ${dowEn(dow)}s: $n of them."), n / ds.size / avg)
        }
        if (b.periods.size >= 2) out += cycle(b, days)
        // what helped
        val rated = b.spells.filter { it.relief > 0 }
        rated.flatMap { a -> a.list(a.took).map { it to a.relief } }.groupBy({ it.first }, { it.second }).forEach { (k, rs) ->
            if (rs.size < 3) return@forEach
            val name = k.toLongOrNull()?.let { b.medById[it]?.name } ?: k
            val yes = rs.count { it == 3 }; val some = rs.count { it == 2 }
            out += Clue("m:$k", tr("«$name» помог в $yes из ${rs.size} раз" + (if (some > 0) ", ещё $some – немного." else "."),
                "«$name» helped $yes of ${rs.size} times" + (if (some > 0) ", a little $some more." else ".")), yes / rs.size.toDouble() + 1)
        }
        return out.sortedByDescending { it.strength }
    }

    /**
     * Which trouble keeps to a part of the cycle: a few days before the period, its first day, or the days of it.
     * «Insomnia: 4 of 5 times in the 3 days before the period, about 6 h.»
     */
    private fun cycle(b: Book, days: List<Day>): List<Clue> {
        val known = days.filter { it.cycleDay > 0 || it.toNext > 0 }
        if (known.size < 20) return emptyList()
        val phases = listOf(
            Triple("before", { d: Day -> d.toNext in 1..3 }, tr("за 1–3 дня до месячных", "in the 3 days before the period")),
            Triple("first", { d: Day -> d.cycleDay == 1 }, tr("в первый день месячных", "on the period's first day")),
            Triple("during", { d: Day -> d.period }, tr("во время месячных", "during the period")))
        val out = mutableListOf<Clue>()
        val kinds = known.flatMap { d -> b.attacksOn(d.day).map { it.kind } }.distinct()
        for (k in kinds) {
            val hit = known.filter { d -> b.attacksOn(d.day).any { it.kind == k } }
            if (hit.size < 3) continue
            // the narrowest phase that holds at least half of them and is twice as likely as the rest of the cycle
            for ((id, test, words) in phases.sortedBy { (_, t, _) -> known.count(t) }) {
                val inside = known.filter(test); if (inside.isEmpty() || inside.size == known.size) continue
                val n = hit.count(test)
                val r1 = n / inside.size.toDouble(); val r0 = (hit.size - n) / (known.size - inside.size).toDouble()
                if (n < 2 || n * 2 < hit.size || r1 < 2 * r0) continue
                val spells = hit.filter(test).flatMap { d -> b.attacksOn(d.day).filter { it.kind == k && it.end > it.start } }
                val hours = spells.takeIf { it.isNotEmpty() }?.map { (it.end - it.start) / 3600_000.0 }?.average()
                val long = hours?.takeIf { it >= 1 }?.let { tr(", обычно около ${fmt(Math.round(it).toInt())} ч", ", usually about ${Math.round(it)} h") } ?: ""
                out += Clue("c:$k:$id", tr("${Troubles.title(k)}: $n из ${hit.size} раз – $words$long.", "${Troubles.title(k)}: $n of ${hit.size} times $words$long."),
                    if (r0 == 0.0) 3.0 else r1 / r0)
                break
            }
        }
        return out
    }

    private fun dowRu(d: Int) = listOf("понедельникам", "вторникам", "средам", "четвергам", "пятницам", "субботам", "воскресеньям")[d - 1]
    private fun dowEn(d: Int) = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")[d - 1]
}

/** The pet's Sunday letter about the week that ended. */
object Letter {
    fun of(b: Book, petName: String, monday: Long): String {
        val days = (monday..monday + 6).mapNotNull { b.care(it) }
        val breakfasts = days.count { it.breakfast }
        val due = days.sumOf { it.due }; val taken = days.sumOf { it.taken }
        val spells = days.sumOf { it.attacks }
        val good = days.count { it.good }
        val nights = days.filter { it.slept >= 0 }
        val lines = mutableListOf<String>()
        lines += tr("Привет! Это $petName.", "Hi! It's $petName.")
        lines += when {
            good >= 6 -> tr("Какая хорошая была неделя: $good дней из семи мы с тобой жили как надо.", "What a good week: $good days of seven we did it right.")
            good >= 3 -> tr("Неделя была ничего: $good ${plural(good.toLong(), "хороший день", "хороших дня", "хороших дней")}.", "Not a bad week: $good good days.")
            else -> tr("Неделя вышла непростая. Ничего, бывает – я никуда не ухожу.", "It was a hard week. That's all right – I'm not going anywhere.")
        }
        if (breakfasts > 0) lines += tr("Мы завтракали ${count(breakfasts, "раз", "раза", "раз", "", "")}", "We had breakfast ${if (breakfasts == 1) "once" else "$breakfasts times"}") +
            " – " + breakfastWords((monday..monday + 6).flatMap { b.marksOn(it) }.filter { it.kind == "eat" && it.sub == "breakfast" })
        if (due > 0) lines += if (taken >= due) tr("Ни одного пропуска лекарств! Горшки на окне цветут.", "Not one dose missed! The pots on the sill are in bloom.")
        else tr("Лекарства: $taken из $due. Давай я буду напоминать погромче?", "Medicine: $taken of $due. Shall I remind you louder?")
        if (spells > 0) lines += tr("Голова болела ${count(spells, "раз", "раза", "раз", "time", "times")}. Всё записано – так мы быстрее поймём, откуда это берётся.",
            "It hurt ${if (spells == 1) "once" else "$spells times"}. I lay beside you and wrote it all down – so we'll find out where it comes from.")
        val moved = days.sumOf { it.moveMins }
        if (moved > 0) lines += tr("Двигались мы ${fmt(moved)} минут – сил явно прибавилось.", "We moved for ${fmt(moved)} minutes – I feel stronger already.")
        else lines += tr("Мы совсем не двигались. Может, короткая прогулка на неделе?", "We didn't move at all. A short walk this week?")
        val veg = days.count { it.veg }
        if (veg > 0) lines += tr("Овощи или фрукты были в $veg ${plural(veg.toLong(), "день", "дня", "дней")} из семи.", "Vegetables or fruit on $veg days of seven.")
        if (nights.isNotEmpty()) {
            val avg = nights.map { it.slept }.average()
            lines += if (avg < b.s.sleepGoal - 1) tr("Спали мы в среднем ${fmt1(avg)} ч. Может, в субботу ляжем пораньше?", "We slept ${fmt1(avg)} h on average. Bed a bit earlier on Saturday?")
            else tr("Спали хорошо – ${fmt1(avg)} ч в среднем.", "We slept well – ${fmt1(avg)} h on average.")
        }
        lines += "– $petName"
        return lines.joinToString("\n")
    }

    /** What the pet says of the week's breakfasts: from what was most often in them (#24), not a porridge it made up. */
    fun breakfastWords(eats: List<app.caregotchi.data.Mark>): String {
        val top = eats.flatMap { m -> m.tags.split(',').filter { it.isNotBlank() } }.groupingBy { it }.eachCount()
            .entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { ORDER.indexOf(it.key) }).firstOrNull()?.key
        return when (top) {
            "carbs" -> tr("каша и хлебушек по утрам – это я люблю.", "porridge and bread in the morning – just how I like it.")
            "protein" -> tr("с белковым завтраком сил хватает до самого обеда.", "a breakfast with protein keeps me going till lunch.")
            "veg" -> tr("даже с овощами! Хрум-хрум.", "with vegetables, even! Crunch, crunch.")
            "fruit" -> tr("фрукты по утрам – это вкусно.", "fruit in the morning – so tasty.")
            "sweet" -> tr("бывало и сладкое. Иногда можно.", "some of it sweet. Now and then that's fine.")
            else -> if (eats.any { it.q == 3 }) tr("и полезно, мне понравилось.", "and wholesome too, I liked it.")
                else tr("с завтраком день идёт веселее.", "a day with breakfast goes better.")
        }
    }
    private val ORDER = listOf("carbs", "protein", "veg", "fruit", "sweet")
}
