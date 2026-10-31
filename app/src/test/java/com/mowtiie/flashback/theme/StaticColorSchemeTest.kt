package com.mowtiie.flashback.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class StaticColorSchemeTest {

    @Test
    fun lightSchemesFollowContrast() {
        assertEquals(Color(0xFF415F91), staticColorScheme(false, ContrastLevel.Standard).primary)
        assertEquals(Color(0xFF133665), staticColorScheme(false, ContrastLevel.Medium).primary)
        assertEquals(Color(0xFF032B5B), staticColorScheme(false, ContrastLevel.High).primary)
    }

    @Test
    fun darkSchemesFollowContrast() {
        assertEquals(Color(0xFFAAC7FF), staticColorScheme(true, ContrastLevel.Standard).primary)
        assertEquals(Color(0xFFCDDDFF), staticColorScheme(true, ContrastLevel.Medium).primary)
        assertEquals(Color(0xFFEBF0FF), staticColorScheme(true, ContrastLevel.High).primary)
    }

    @Test
    fun surfaceTintMatchesPrimary() {
        val scheme = staticColorScheme(true, ContrastLevel.High)
        assertEquals(scheme.primary, scheme.surfaceTint)
    }
}
