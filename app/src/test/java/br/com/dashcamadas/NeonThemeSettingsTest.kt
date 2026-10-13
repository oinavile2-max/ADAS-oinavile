package br.com.dashcamadas

import org.junit.Assert.*
import org.junit.Test

class NeonThemeSettingsTest {
    @Test fun existingPreferencesKeepCyanAndRedPopupDefaults() {
        val theme = NeonThemeSettings()
        assertEquals(0xff00aeff.toInt(), theme.accent)
        assertEquals(0xffff233e.toInt(), theme.popupAccent)
        assertEquals(NeonColor.CYAN, NeonColor.fromId("old-unknown-theme"))
    }
    @Test fun themeColorRequiresExplicitPopupOptInAndToneChangesHue() {
        val green = NeonThemeSettings(NeonColor.GREEN, tone = 20)
        assertNotEquals(NeonColor.GREEN.argb, green.accent)
        assertEquals(NeonThemeSettings.ALERT_RED, green.popupAccent)
        assertEquals(green.accent, green.copy(popupFollowsTheme = true).popupAccent)
    }
    @Test fun backgroundTransparencyDoesNotFadeOutlineOrChangeOtherControl() {
        val theme = NeonThemeSettings(NeonColor.PINK, buttonTransparency = 80, popupTransparency = 0)
        val base = 0xff112031.toInt()
        assertEquals(51, theme.buttonFill(base) ushr 24)
        assertEquals(255, theme.popupFill(base) ushr 24)
        assertEquals(255, theme.accent ushr 24)
        assertEquals(255, theme.popupAccent ushr 24)
    }
    @Test fun criticalPopupKeepsRedPaletteAndOpacityDespiteThemeOrTransparency() {
        val green = NeonThemeSettings(NeonColor.GREEN, popupTransparency = 80, popupFollowsTheme = true)
        val red = NeonThemeSettings(NeonColor.RED, popupTransparency = 80)
        val base = 0xe6210d14.toInt()
        assertEquals(red.popupFill(base, true), green.popupFill(base, true))
        assertTrue(green.popupFill(base, true) ushr 24 >= 191)
        assertTrue(green.popupFill(base, false) ushr 24 < 191)
    }
    @Test fun corruptedStoredRangesNormalizeBeforeRendering() {
        val clean = NeonThemeSettings(tone = -900, buttonTransparency = -200, popupTransparency = 500).normalized()
        assertEquals(-30, clean.tone)
        assertEquals(0, clean.buttonTransparency)
        assertEquals(80, clean.popupTransparency)
        for (color in NeonColor.entries) assertEquals(255, NeonThemeSettings(color, tone = 30).accent ushr 24)
    }
}
