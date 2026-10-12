package br.com.dashcamadas

import org.junit.Assert.*
import org.junit.Test

class AdasSettingsValuesTest {
    @Test fun brazilianDecimalAndExistingDefaultsRemainValid() {
        val value = AdasSettingsValues.parse("30", "2,5", "900", "10000", "500")!!
        assertEquals(30f, value.speed); assertEquals(2.5f, value.ttc)
        assertEquals(900L, value.confirmation)
    }
    @Test fun malformedNonfiniteOrOverflowValueRejectsWholeForm() {
        for (invalid in listOf("", "NaN", "Infinity", "-Infinity", "999999999999999999999999")) {
            assertNull(AdasSettingsValues.parse(invalid, "2.5", "900", "10000", "500"))
            assertNull(AdasSettingsValues.parse("30", "2.5", invalid, "10000", "500"))
        }
    }
    @Test fun outOfRangeHiddenAdvancedValueCannotProducePartialSettings() {
        assertNull(AdasSettingsValues.parse("30", "0.9", "900", "10000", "500"))
        assertNull(AdasSettingsValues.parse("30", "2.5", "299", "10000", "500"))
        assertNull(AdasSettingsValues.parse("30", "2.5", "900", "4999", "500"))
        assertNull(AdasSettingsValues.parse("30", "2.5", "900", "10000", "1001"))
    }
}
