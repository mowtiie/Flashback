package com.mowtiie.flashback.ui.settings

import org.junit.Assert.assertTrue
import org.junit.Test

class AppLinksTest {

    @Test
    fun thePrivacyPolicyIsAnHttpsPageNotAFile() {
        val url = AppLinks.PRIVACY_POLICY
        assertTrue(url.startsWith("https://"))
        assertTrue(!url.endsWith(".pdf"))
        assertTrue(url.contains("PRIVACY"))
    }
}
