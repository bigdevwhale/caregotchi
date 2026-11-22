package app.caregotchi

import app.caregotchi.domain.Plan
import org.junit.Assert.assertEquals
import org.junit.Test

/** Meals are named by their place among one's own times, not by the clock alone (#19). */
class MealNamesTest {
    @Test fun aLateMorningMealIsNotLunch() {
        assertEquals(listOf("breakfast", "snack", "lunch", "dinner"), Plan.names(listOf(8 * 60, 11 * 60 + 30, 14 * 60, 19 * 60)))
        assertEquals(listOf("breakfast", "lunch", "dinner"), Plan.names(listOf(10 * 60, 14 * 60, 20 * 60)))
        assertEquals("snack", Plan.mealAt(listOf(8 * 60, 11 * 60 + 30, 14 * 60, 19 * 60), 11 * 60 + 40))
        assertEquals("lunch", Plan.mealAt(listOf(8 * 60, 11 * 60 + 30, 14 * 60, 19 * 60), 14 * 60 + 10))
    }
}
