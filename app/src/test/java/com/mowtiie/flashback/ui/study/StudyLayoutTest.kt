package com.mowtiie.flashback.ui.study

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyLayoutTest {

    @Test
    fun aLandscapePhoneWithTheKeyboardOpenGetsTheCompactLayout() {
        assertTrue(isCompactForKeyboard(windowHeightDp = 411f, keyboardOpen = true))
    }

    @Test
    fun withoutTheKeyboardNothingIsHidden() {
        assertFalse(isCompactForKeyboard(windowHeightDp = 411f, keyboardOpen = false))
    }

    @Test
    fun aTallWindowKeepsItsTopBarEvenWithTheKeyboardOpen() {
        assertFalse(isCompactForKeyboard(windowHeightDp = 891f, keyboardOpen = true))
        assertFalse(isCompactForKeyboard(windowHeightDp = 700f, keyboardOpen = true))
    }

    @Test
    fun theThresholdIsMaterialsCompactHeight() {
        assertTrue(isCompactForKeyboard(windowHeightDp = 479.9f, keyboardOpen = true))
        assertFalse(isCompactForKeyboard(windowHeightDp = 480f, keyboardOpen = true))
    }
}
