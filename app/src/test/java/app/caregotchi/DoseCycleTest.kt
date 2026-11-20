package app.caregotchi

import app.caregotchi.data.Med
import app.caregotchi.domain.Lang
import app.caregotchi.domain.halves
import app.caregotchi.domain.pills
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Half pills, a dose that changes by the day of a cycle, and how long the stock lasts then (#9). */
class DoseCycleTest {
    private val day = 20_000L

    @Test fun halvesReadAsPills() {
        Lang.ru = true
        assertEquals("½", halves(1)); assertEquals("1", halves(2)); assertEquals("1½", halves(3))
        assertEquals("½ таблетки", pills(1)); assertEquals("1 таблетка", pills(2)); assertEquals("1½ таблетки", pills(3)); assertEquals("5 таблеток", pills(10))
        assertEquals("перерыв", pills(0))
    }

    @Test fun theCycleRepeatsFromTheFirstDay() {
        val m = Med(name = "A", times = "480", from = day, pattern = "2,2,1")
        assertEquals(listOf(2, 2, 1, 2, 2, 1), (0L until 6L).map { m.halvesOn(day + it) })
        // without a cycle every day is the usual dose
        assertEquals(1, Med(name = "B", times = "480", perDose = 1, from = day).halvesOn(day + 5))
    }

    @Test fun aDayOffUsesTheUsualDoseWhenTakenAnyway() {
        val m = Med(name = "A", times = "480", from = day, perDose = 2, pattern = "2,0")
        assertEquals(0, m.halvesOn(day + 1))
        assertEquals(2, m.taken(day + 1))
    }

    @Test fun theStockLastsByTheCycle() {
        // 1 / 1 / ½ once a day = 5 halves in 3 days; 10 halves last 6 days
        val m = Med(name = "A", times = "480", from = day, pattern = "2,2,1")
        assertEquals(6, Med.daysOf(m, 10, day))
        // twice a day half a pill: 7 halves last 3 days
        assertEquals(3, Med.daysOf(Med(name = "B", times = "480,1200", perDose = 1, from = day), 7, day))
        assertNull(Med.daysOf(Med(name = "C", times = "", from = day), 10, day))
    }
}
