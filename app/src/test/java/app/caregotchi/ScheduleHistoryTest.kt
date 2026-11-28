package app.caregotchi

import app.caregotchi.data.Med
import org.junit.Assert.assertEquals
import org.junit.Test

/** A changed schedule counts from the day of the change; the past keeps its doses (#28). */
class ScheduleHistoryTest {
    private val morning = Med(id = 1, name = "Давление", times = "480", created = 1)

    @Test fun aSecondDoseCountsFromTodayOnly() {
        val m = morning.withTimes("480,1200", today = 100)
        assertEquals(listOf(480), m.timesOn(99))
        assertEquals(listOf(480, 1200), m.timesOn(100))
        assertEquals(listOf(480, 1200), m.timesOn(150))
    }

    @Test fun twoChangesTheSameDayKeepTheDaysFirst() {
        val m = morning.withTimes("480,1200", 100).withTimes("480,1260", 100)
        assertEquals(listOf(480), m.timesOn(99))
        assertEquals(listOf(480, 1260), m.timesOn(100))
    }

    @Test fun changesOnDifferentDaysStack() {
        val m = morning.withTimes("480,1200", 100).withTimes("1200", 110)
        assertEquals(listOf(480), m.timesOn(50))
        assertEquals(listOf(480, 1200), m.timesOn(105))
        assertEquals(listOf(1200), m.timesOn(110))
    }

    @Test fun aNewMedicineHasNoHistory() {
        assertEquals("", Med(name = "Новое", times = "480").withTimes("480,1200", 100).past)
    }

    @Test fun anEveningDoseFirstMarkedLaterWasAddedLater() {
        val m = Med(id = 1, name = "Давление", times = "480,1200").guessPast(mapOf(480 to 10L, 1200 to 40L))
        assertEquals(listOf(480), m.timesOn(39))
        assertEquals(listOf(480, 1200), m.timesOn(40))
        // doses that began together stay as they are
        assertEquals("", Med(id = 1, name = "A", times = "480,1200").guessPast(mapOf(480 to 10L, 1200 to 11L)).past)
    }
}
