package sheridan.dheripu.fitnutrition.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class HealthTextTest {
    @Test
    fun sleepShowsHoursAndPaddedMinutesOrPlaceholder() {
        assertEquals("7h 05m", HealthText.sleep(425))
        assertEquals("--", HealthText.sleep(0))
    }
}
