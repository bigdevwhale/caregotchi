package app.caregotchi

import app.caregotchi.data.Attack
import app.caregotchi.data.Mark
import app.caregotchi.data.Med
import app.caregotchi.data.Settings
import app.caregotchi.domain.Book
import app.caregotchi.domain.Detective
import app.caregotchi.domain.PERIOD
import app.caregotchi.domain.Plan
import app.caregotchi.domain.dayOf
import app.caregotchi.domain.startOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reminders by one's own meal times and medicine tied to food, courses planned ahead, and the period kept apart from the spells. */
class PlanCycleTest {
    private val day = 20_000L // a fixed day, far from today
    private fun at(d: Long, h: Int, m: Int = 0) = startOf(d) + (h * 60 + m) * 60_000L
    private val s = Settings(remindMeds = true, remindMeals = true, mealTimes = listOf(8 * 60, 13 * 60, 19 * 60))

    @Test fun medicineTiedToMealsFollowsTheMealTimes() {
        val before = Med(id = 1, name = "A", food = 1, gap = 30, meals = "0,2")
        assertEquals(listOf(7 * 60 + 30, 18 * 60 + 30), before.timesFor(s.mealTimes))
        val after = Med(id = 2, name = "B", food = 3, gap = 60, meals = "1")
        assertEquals(listOf(14 * 60), after.timesFor(s.mealTimes))
        // a meal dropped from the list is dropped from the medicine
        assertEquals(listOf(7 * 60 + 30), Med(id = 1, name = "A", food = 1, gap = 30, meals = "0,5").timesFor(s.mealTimes))
    }

    @Test fun anAfterMealReminderMovesWithALateLunch() {
        val m = Med(id = 2, name = "B", times = "${14 * 60}", food = 3, gap = 60, meals = "1", from = day - 5)
        val plain = Plan.of(Book(emptyList(), listOf(m), emptyList(), emptyList(), s, at(day, 9)), s).first { it.kind == "pill" }
        assertEquals(at(day, 14), plain.at)
        val lunch = Mark(id = 1, at = at(day, 14, 10), kind = "eat", sub = "lunch")
        val moved = Plan.of(Book(listOf(lunch), listOf(m), emptyList(), emptyList(), s, at(day, 14, 15)), s).first { it.kind == "pill" }
        assertEquals(at(day, 15, 10), moved.at)
        // an early lunch never makes it earlier than planned
        val early = Mark(id = 1, at = at(day, 12, 15), kind = "eat", sub = "lunch")
        assertEquals(at(day, 14), Plan.of(Book(listOf(early), listOf(m), emptyList(), emptyList(), s, at(day, 12, 20)), s).first { it.kind == "pill" }.at)
    }

    @Test fun mealRemindersUseOwnTimesAndOnlyWhenHungry() {
        val b = Book(emptyList(), emptyList(), emptyList(), emptyList(), s, at(day, 13))
        val meals = Plan.of(b, s).filter { it.kind == "meal" && dayOf(it.at) == day }.map { it.at }
        assertEquals(listOf(at(day, 8), at(day, 13), at(day, 19)), meals)
        assertTrue(Plan.hungry(b, s, 1))
        val fed = Book(listOf(Mark(id = 1, at = at(day, 12, 40), kind = "eat")), emptyList(), emptyList(), emptyList(), s, at(day, 13))
        assertFalse(Plan.hungry(fed, s, 1))
    }

    @Test fun anAlarmAtTheWrongHourSaysNothing() {
        // #26: an alarm for the 20:00 doses rang at 11:33 – the reminder must check the phone's clock
        val m = Med(id = 4, name = "Магний", times = "${20 * 60}", from = day - 5)
        val b = Book(emptyList(), listOf(m), emptyList(), emptyList(), s, at(day, 11, 33))
        assertTrue(Plan.due(b, s, at(day, 20), now = at(day, 11, 33)).isEmpty())
        // on time, or a few minutes late, it still rings
        assertEquals(1, Plan.due(b, s, at(day, 20), now = at(day, 20)).count { it.kind == "pill" })
        assertEquals(1, Plan.due(b, s, at(day, 20), now = at(day, 20, 40)).count { it.kind == "pill" })
        // but not when held back for half a day
        assertTrue(Plan.due(b, s, at(day, 20), now = at(day + 1, 8)).none { it.kind == "pill" })
    }

    @Test fun aPlannedCourseWaitsAndIsAnnouncedTheEveningBefore() {
        val m = Med(id = 3, name = "D", times = "${9 * 60}", from = day + 1, created = at(day, 10))
        val pings = Plan.of(Book(emptyList(), listOf(m), emptyList(), emptyList(), s, at(day, 10)), s)
        assertEquals(listOf(at(day + 1, 9)), pings.filter { it.kind == "pill" }.map { it.at })
        assertEquals(listOf(at(day, 20)), pings.filter { it.kind == "start" }.map { it.at })
        val b = Book(emptyList(), listOf(m), emptyList(), emptyList(), s, at(day, 10))
        assertTrue(b.dosesLeft().isEmpty())
    }

    @Test fun thePeriodIsAStateNotASpell() {
        val p = Attack(id = 1, start = at(day, 9), end = at(day + 4, 20), kind = PERIOD)
        val head = Attack(id = 2, start = at(day, 15), end = at(day, 19), kind = "headache")
        val b = Book(emptyList(), emptyList(), listOf(head, p), emptyList(), s, at(day + 10, 12))
        assertEquals(listOf(head), b.spells)
        assertEquals(1, b.care(day)!!.attacks)
        assertTrue(b.care(day + 4)!!.period); assertFalse(b.care(day + 5)!!.period)
        assertEquals(1, b.care(day)!!.cycleDay); assertEquals(11, b.care(day + 10)!!.cycleDay)
    }

    @Test fun theDetectiveFindsInsomniaBeforeThePeriod() {
        // five 28-day cycles; sleepless nights two days before each period, a headache on its first day
        val starts = (0 until 5).map { day + it * 28L }
        val spells = mutableListOf<Attack>(); var id = 1L
        for (st in starts) {
            spells += Attack(id = id++, start = at(st, 8), end = at(st + 4, 20), kind = PERIOD)
            if (st > day) spells += Attack(id = id++, start = at(st - 2, 1), end = at(st - 2, 6), kind = "insomnia")
            spells += Attack(id = id++, start = at(st, 10), end = at(st, 16), kind = "headache")
        }
        // a few stray ones elsewhere
        spells += Attack(id = id++, start = at(day + 12, 10), end = at(day + 12, 12), kind = "headache")
        val marks = (day..day + 125).map { Mark(id = it, at = at(it, 12), kind = "water") }
        val b = Book(marks, emptyList(), spells, emptyList(), s, at(day + 126, 12))
        val clues = Detective.clues(b)
        assertEquals(b.cycles, listOf(28, 28, 28, 28))
        assertTrue(clues.joinToString { it.id }, clues.any { it.id == "c:insomnia:before" })
        assertTrue(clues.joinToString { it.id }, clues.any { it.id == "c:headache:first" })
    }
}
