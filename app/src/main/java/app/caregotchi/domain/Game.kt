package app.caregotchi.domain

import app.caregotchi.data.Mark
import app.caregotchi.data.Settings
import kotlin.random.Random

/** A kind of pet. Each but the first four hatches from an egg earned by its own kind of care. */
class Species(
    val id: String, val ru: String, val en: String, val names: List<String>, val namesEn: List<String>,
    val howRu: String, val howEn: String, val need: Int, val progress: (Stats) -> Int,
) {
    val title get() = tr(ru, en)
    val how get() = tr(howRu, howEn)
}

/** Totals over the whole history, for eggs, badges and the nest. */
data class Stats(
    val fullDays: Int, val breakfasts: Int, val dinners: Int, val onTime: Int, val goodNights: Int, val waterDays: Int,
    val allDosesDays: Int, val recoveries: Int, val bedRecoveries: Int, val streak: Int, val bestStreak: Int, val earlyBeds: Int,
    val attacks: Int, val moves: Int, val moveDays: Int, val wholesome: Int, val vegDays: Int, val earlyWakes: Int, val goodDays: Int, val winterGood: Int, val daysWithCare: Int,
    val coursesDone: Int, val cluesKept: Int,
)

fun Book.stats(): Stats {
    val ds = days
    var best = 0; var run = 0
    for (d in ds) { if (d.any || d.plaid) { run++; best = maxOf(best, run) } else run = 0 }
    val h = health
    val courses = meds.count { m -> m.until in 1 until today && m.times.isNotBlank() &&
        (m.from..m.until).let { r -> val due = r.sumOf { dueOn(m, it).size }; due > 0 && r.sumOf { d -> marksOn(d).count { it.kind == "pill" && it.med == m.id } } >= due * .8 } }
    return Stats(
        fullDays = ds.count { it.score >= .85 }, breakfasts = ds.count { it.breakfast }, dinners = ds.count { it.dinner },
        onTime = marks.count { it.kind == "pill" && it.slot >= 0 && it.late <= 60 },
        goodNights = ds.count { it.slept >= s.sleepGoal - 1 }, waterDays = ds.count { it.water >= s.waterGoal },
        allDosesDays = ds.count { it.due > 0 && it.taken >= it.due }, recoveries = h.recoveries, bedRecoveries = h.bedRecoveries,
        streak = streak, bestStreak = best, earlyBeds = ds.count { it.bedEarly }, attacks = attacks.size, moves = ds.sumOf { it.moves }, moveDays = ds.count { it.moveMins >= s.moveGoal },
        wholesome = ds.sumOf { it.wholesome }, vegDays = ds.count { it.veg },
        earlyWakes = ds.count { it.wokeEarly }, goodDays = ds.count { it.good },
        winterGood = ds.count { it.good && java.time.LocalDate.ofEpochDay(it.day).monthValue in listOf(12, 1, 2) },
        daysWithCare = ds.count { it.any }, coursesDone = courses, cluesKept = s.clues.count { it.value > 0 },
    )
}

object Zoo {
    val all = listOf(
        Species("fox", "Лисёнок", "Fox kit", listOf("Рыжик", "Огонёк", "Лиска", "Апельсин"), listOf("Ginger", "Ember", "Rusty", "Maple"),
            "7 дней полной заботы", "7 days of full care", 7) { it.fullDays },
        Species("bunny", "Крольчонок", "Bunny", listOf("Овсянка", "Пушок", "Зефир", "Булочка"), listOf("Oatmeal", "Puff", "Marshmallow", "Bun"),
            "10 завтраков", "10 breakfasts", 10) { it.breakfasts },
        Species("hedgehog", "Ёжик", "Hedgehog", listOf("Колючка", "Фыр", "Иголка", "Шишка"), listOf("Prickles", "Huff", "Needle", "Pinecone"),
            "30 приёмов лекарств вовремя", "30 doses on time", 30) { it.onTime },
        Species("owl", "Совёнок", "Owlet", listOf("Угу", "Филя", "Сплюшка", "Ночка"), listOf("Hoot", "Philo", "Snoozy", "Midnight"),
            "7 ночей с нормальным сном", "7 nights of good sleep", 7) { it.goodNights },
        Species("newt", "Тритон", "Newt", listOf("Капля", "Плюх", "Ручеёк"), listOf("Drop", "Splash", "Brook"),
            "7 дней с нормой воды", "7 days of enough water", 7) { it.waterDays },
        Species("turtle", "Черепашка", "Turtle", listOf("Тортилла", "Панцирь", "Не-спеши"), listOf("Shelly", "Steady", "Slowpoke"),
            "30 дней без пропуска лекарств", "30 days with every dose", 30) { it.allDosesDays },
        Species("phoenix", "Фениксёнок", "Phoenix chick", listOf("Уголёк", "Искра", "Заря"), listOf("Cinder", "Spark", "Dawn"),
            "поправиться после простуды", "get well after a cold", 1) { it.recoveries },
        Species("sloth", "Ленивец", "Sloth", listOf("Соня", "Потягуша", "Тихон"), listOf("Snooze", "Stretch", "Hush"),
            "30 дней подряд", "30 days in a row", 30) { it.bestStreak },
        Species("raccoon", "Енотик", "Raccoon", listOf("Полоскун", "Маска", "Котлетка"), listOf("Rinsey", "Bandit", "Dumpling"),
            "30 полезных обедов и ужинов", "30 wholesome meals", 30) { it.wholesome },
        Species("mole", "Кротик", "Mole", listOf("Очкарик", "Норушка", "Бархат"), listOf("Specs", "Burrow", "Velvet"),
            "10 раз лечь до полуночи", "10 nights in bed before midnight", 10) { it.earlyBeds },
        Species("moth", "Мотылёк", "Moth", listOf("Пыльца", "Ночница", "Лунник"), listOf("Dusty", "Nightie", "Moonwing"),
            "10 записанных приступов", "10 bad spells written down", 10) { it.attacks },
        Species("frog", "Лягушонок", "Froglet", listOf("Кваки", "Лопух", "Кувшинка"), listOf("Croaky", "Burdock", "Lily"),
            "20 прогулок и тренировок", "20 walks or workouts", 20) { it.moves },
    )
    val byId = all.associateBy { it.id }
    val starters = all.take(4)
    fun of(id: String) = byId[id] ?: all[0]
    fun randomName(id: String) = of(id).let { if (Lang.ru) it.names.random() else it.namesEn.random() }
}

/** Growth of a pet by the care it shared. */
object Stage {
    val at = listOf(0, 250, 1000, 3000)
    fun of(xp: Int) = at.indexOfLast { xp >= it }.coerceAtLeast(0)
    fun name(stage: Int) = when (stage) { 0 -> tr("Малыш", "Baby"); 1 -> tr("Подросток", "Youngster"); 2 -> tr("Взрослый", "Grown-up"); else -> tr("Мудрец", "Elder") }
    fun next(xp: Int) = at.firstOrNull { it > xp }
}

/** The keeper's level: each one asks a little more experience than the last. */
object Level {
    fun need(level: Int) = 100 + 25 * (level - 1)
    fun of(xp: Int): Triple<Int, Int, Int> {   // level, xp into it, xp it needs
        var l = 1; var left = xp
        while (left >= need(l)) { left -= need(l); l++ }
        return Triple(l, left, need(l))
    }
}

/** What a mark brings, given what was marked earlier the same day. */
object Pay {
    fun of(m: Mark, sameDay: List<Mark>, s: Settings): Pair<Int, Int> {
        val n = sameDay.count { it.kind == m.kind }
        return when (m.kind) {
            "eat" -> if (n < 4) (10 + if (m.q == 3) 4 else 0) to (3 + if (m.q == 3) 2 else 0) else 2 to 0
            "water" -> if (n < s.waterGoal + 2) 4 to 1 else 0 to 0
            "pill" -> when { m.slot < 0 -> 5 to 1; m.late <= 60 -> 15 to 5; else -> 8 to 2 }
            "sleep", "wake" -> if (n < 1) 8 to 2 else 0 to 0
            "move" -> if (n < 3) (10 + (m.mins / 5).coerceAtMost(10)) to (3 + (m.mins / 15).coerceAtMost(4)) else 2 to 0
            else -> 0 to 0
        }
    }
    const val ATTACK_XP = 20
    const val ATTACK_LEAVES = 10
}

/** A small task for the day; [progress] gets the day and the settings (for the goals). */
class Quest(val id: String, val ru: String, val en: String, val icon: String, val leaves: Int, val of: Int, val needsMeds: Boolean = false, val progress: (Day, Settings) -> Int) {
    val text get() = tr(ru, en)
}

object Quests {
    val all = listOf(
        Quest("breakfast", "Позавтракать", "Have breakfast", "eat", 10, 1) { d, _ -> if (d.breakfast) 1 else 0 },
        Quest("water_noon", "Два стакана до обеда", "Two glasses before noon", "water", 15, 2) { d, _ -> d.waterNoon.coerceAtMost(2) },
        Quest("water_goal", "Выпить дневную норму воды", "Drink the day's water", "water", 20, 1) { d, g -> if (d.water >= g.waterGoal) 1 else 0 },
        Quest("meals3", "Поесть три раза", "Eat three times", "eat", 15, 3) { d, _ -> d.meals.coerceAtMost(3) },
        Quest("doses", "Все лекарства вовремя", "Every dose on time", "pill", 20, 1, needsMeds = true) { d, _ -> if (d.due > 0 && d.onTime >= d.due) 1 else 0 },
        Quest("bed", "Лечь до полуночи", "In bed before midnight", "sleep", 20, 1) { d, _ -> if (d.bedEarly) 1 else 0 },
        Quest("walk", "Подвигаться хоть немного", "Move a little", "walk", 15, 1) { d, _ -> d.moves.coerceAtMost(1) },
        Quest("move_goal", "Набрать норму движения", "Reach the day's moving", "walk", 20, 1) { d, g -> if (d.moveMins >= g.moveGoal) 1 else 0 },
        Quest("veg", "Съесть овощи или фрукты", "Eat vegetables or fruit", "eat", 10, 1) { d, _ -> if (d.veg) 1 else 0 },
        Quest("wholesome", "Два полезных приёма пищи", "Two wholesome meals", "eat", 15, 2) { d, _ -> d.wholesome.coerceAtMost(2) },
        Quest("dinner", "Поужинать", "Have dinner", "eat", 10, 1) { d, _ -> if (d.dinner) 1 else 0 },
        Quest("morning", "Встать до восьми", "Up before eight", "sunrise", 10, 1) { d, _ -> if (d.wokeEarly) 1 else 0 },
        Quest("water4", "Четыре стакана воды", "Four glasses of water", "water", 10, 4) { d, _ -> d.water.coerceAtMost(4) },
    )
    val byId = all.associateBy { it.id }

    /** Three tasks for [day], the same all day. */
    fun deal(day: Long, hasMeds: Boolean): List<String> =
        all.filter { hasMeds || !it.needsMeds }.shuffled(Random(day * 31 + 7)).take(3).map { it.id }
}

/** Embroidered patches for the pet's blanket. Their pictures are badge_0..15 in this order. */
class Badge(val id: Int, val ru: String, val en: String, val whyRu: String, val whyEn: String, val need: Int, val progress: (Stats, Settings, Int) -> Int) {
    val title get() = tr(ru, en)
    val why get() = tr(whyRu, whyEn)
}

object Badges {
    val all = listOf(
        Badge(0, "Сто таблеток", "A hundred pills", "100 приёмов вовремя", "100 doses on time", 100) { st, _, _ -> st.onTime },
        Badge(1, "Завтракаю", "Breakfast person", "30 завтраков", "30 breakfasts", 30) { st, _, _ -> st.breakfasts },
        Badge(2, "Высыпаюсь", "Well slept", "7 ночей с нормальным сном", "7 nights of good sleep", 7) { st, _, _ -> st.goodNights },
        Badge(3, "Пью воду", "Water drinker", "14 дней с нормой воды", "14 days of enough water", 14) { st, _, _ -> st.waterDays },
        Badge(4, "Пережил бурю", "Weathered a storm", "первый записанный приступ", "the first bad spell written down", 1) { st, _, _ -> st.attacks },
        Badge(5, "Сам о себе", "Self-keeper", "7 дней полной заботы", "7 days of full care", 7) { st, _, _ -> st.fullDays },
        Badge(6, "Зимовка", "Wintering", "30 хороших зимних дней", "30 good winter days", 30) { st, _, _ -> st.winterGood },
        Badge(7, "Ранняя пташка", "Early bird", "10 подъёмов до восьми", "10 mornings up before eight", 10) { st, _, _ -> st.earlyWakes },
        Badge(8, "Месяц подряд", "A month in a row", "серия 30 дней", "a 30-day streak", 30) { st, _, _ -> st.bestStreak },
        Badge(9, "Курс до конца", "Course finished", "допитый курс лекарства", "a course of medicine finished", 1) { st, _, _ -> st.coursesDone },
        Badge(10, "Звезда дупла", "Star of the hollow", "10-й уровень", "level 10", 10) { _, _, lvl -> lvl },
        Badge(11, "Листопад", "Leaf fall", "1000 листиков за всё время", "1000 leaves in all", 1000) { _, s, _ -> s.leavesEver },
        Badge(12, "Первая покупка", "First purchase", "купить вещь в лавке", "buy a thing in the shop", 1) { _, s, _ -> s.items.size },
        Badge(13, "Корона", "The crown", "20-й уровень", "level 20", 20) { _, _, lvl -> lvl },
        Badge(14, "Первая неделя", "First week", "7 дней с отметками", "7 days with marks", 7) { st, _, _ -> st.daysWithCare },
        Badge(15, "Сыщик", "Detective", "5 улик в деле", "5 clues kept", 5) { st, _, _ -> st.cluesKept },
    )
}

/** Things for the hollow. [x], [y], [w] place them on the room picture (fractions of its width and height). */
class Item(val id: String, val ru: String, val en: String, val price: Int, val level: Int = 0, val x: Float = 0f, val y: Float = 0f, val w: Float = 0f, val special: Boolean = false, val icon: String = "") {
    val title get() = tr(ru, en)
    val picture get() = icon.ifEmpty { "item_$id" }
}

object Shop {
    val cozy = listOf(
        Item("lights", "Гирлянда", "Fairy lights", 60, 0, .12f, .005f, .76f),
        Item("lantern", "Фонарик", "Paper lantern", 65, 0, .30f, .045f, .08f),
        Item("painting", "Картина «Горы»", "Mountain painting", 90, 0, .71f, .19f, .16f),
        Item("plush", "Плюшевый мишка", "Teddy bear", 80, 0, .70f, .66f, .12f),
        Item("hotwater", "Грелка", "Hot water bottle", 50, 0, .63f, .80f, .085f),
        Item("cushions", "Подушки", "Floor cushions", 110, 2, .12f, .75f, .21f),
        Item("books", "Книжная полка", "Bookshelf", 100, 3, .25f, .40f, .13f),
        Item("clock", "Часы", "Wall clock", 95, 4, .83f, .085f, .10f),
        Item("tea", "Чайный сервиз", "Tea set", 85, 5, .015f, .525f, .13f),
        Item("record", "Патефон", "Record player", 150, 6, .40f, .455f, .12f),
        Item("fern", "Папоротник", "Fern", 70, 7, .855f, .63f, .14f),
        Item("musicbox", "Шкатулка", "Music box", 120, 8, .295f, .315f, .065f),
        Item("fishbowl", "Аквариум", "Fish bowl", 180, 10, .545f, .45f, .10f),
        Item("rocker", "Кресло-качалка", "Rocking chair", 220, 12, .0f, .70f, .2f),
        Item("armchair", "Кресло с лампой", "Reading chair", 260, 14, .78f, .68f, .22f),
        Item("hammock", "Гамак", "Hammock", 300, 16, .56f, .86f, .38f),
    )
    val special = listOf(
        Item("plaid", "Плед для серии", "A streak blanket", 40, 0, special = true, icon = "icon_plaid"),
        Item("egg", "Загадочное яйцо", "Mystery egg", 300, 5, special = true, icon = "icon_hatch"),
        Item("chest", "Сундучок с сюрпризом", "Surprise chest", 120, 3, special = true, icon = "icon_chest"),
    )
    val byId = (cozy + special).associateBy { it.id }
}

/** What a weekly chest gives. */
data class Loot(val leaves: Int, val item: String?, val plaid: Boolean)

object Chest {
    fun roll(s: Settings, level: Int, rnd: Random = Random.Default): Loot {
        val free = Shop.cozy.filter { it.id !in s.items && it.price <= 160 && it.level <= level + 3 }
        val item = if (free.isNotEmpty() && rnd.nextFloat() < .5f) free.random(rnd).id else null
        return Loot(60 + rnd.nextInt(41), item, rnd.nextFloat() < .25f)
    }
}
