package app.caregotchi

import app.caregotchi.data.Attack
import org.junit.Assert.assertEquals
import org.junit.Test

/** A spell's strength over the hours (#11): each change a point, the worst as its strength. */
class CurveTest {
    @Test fun changesMakeACurve() {
        val h = 3_600_000L
        var a = Attack(start = 8 * h)
        a = a.felt(3, 8 * h + 60_000)          // at the start: belongs to the start
        a = a.felt(5, 9 * h)
        a = a.felt(6, 9 * h + 30_000)          // a correction within two minutes
        a = a.felt(3, 10 * h)
        assertEquals(listOf(8 * h to 3, 9 * h + 30_000 to 6, 10 * h to 3), a.points)
        assertEquals(6, a.strength)
        assertEquals(3, a.now)
    }
}
