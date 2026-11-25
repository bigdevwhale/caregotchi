package app.caregotchi

import app.caregotchi.data.Mark
import app.caregotchi.data.Settings
import app.caregotchi.domain.Day
import app.caregotchi.domain.Letter
import app.caregotchi.domain.Quests
import app.caregotchi.domain.plural
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Sunday letter speaks of the breakfasts that were (#24); the wake-up task keeps to one's own hour (#23). */
class LetterTest {
    private fun eat(tags: String, q: Int = 0) = Mark(at = 0, kind = "eat", sub = "breakfast", tags = tags, q = q)

    @Test fun goodDaysAgree() {
        assertEquals("хороших дня", plural(4, "хороший день", "хороших дня", "хороших дней"))
        assertEquals("хороших дней", plural(5, "хороший день", "хороших дня", "хороших дней"))
    }

    @Test fun breakfastFromWhatWasEaten() {
        app.caregotchi.domain.Lang.ru = false
        assertTrue(Letter.breakfastWords(listOf(eat("carbs"), eat("carbs,veg"), eat("protein"))).contains("porridge"))
        assertTrue(Letter.breakfastWords(listOf(eat("protein"), eat("protein,carbs"))).contains("protein"))
        assertTrue(!Letter.breakfastWords(listOf(eat(""))).contains("porridge"))
    }

    @Test fun upByOwnTime() {
        val q = Quests.byId.getValue("morning")
        val late = Settings(wake = 9 * 60)
        assertEquals(1, q.progress(Day(0, wokeAt = 9 * 60 + 10), late))
        assertEquals(0, q.progress(Day(0, wokeAt = 9 * 60 + 40), late))
        assertEquals(0, q.progress(Day(0), late))
        assertEquals(1, q.progress(Day(0, wokeAt = 7 * 60 + 50), Settings()))
    }

    @Test fun tomorrowIsKnown() = assertEquals(Quests.deal(20_000, true), Quests.deal(20_000, true))
}
