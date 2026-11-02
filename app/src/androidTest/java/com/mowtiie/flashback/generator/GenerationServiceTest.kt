package com.mowtiie.flashback.generator

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.service.notification.StatusBarNotification
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mowtiie.flashback.FlashbackApp
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GenerationServiceTest {

    private val app get() = ApplicationProvider.getApplicationContext<FlashbackApp>()
    private val session get() = app.container.generationSession
    private val manager get() = app.getSystemService(NotificationManager::class.java)

    @Before
    fun allowNotifications() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(app.packageName, Manifest.permission.POST_NOTIFICATIONS)
    }

    @After
    fun endTheRun() = session.end()

    private fun shown(): StatusBarNotification? = manager.activeNotifications.firstOrNull { it.id == NOTIFICATION_ID }

    private fun text(n: StatusBarNotification) = n.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

    private fun waitFor(what: String, check: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 8_000
        while (System.currentTimeMillis() < deadline) {
            if (check()) return
            Thread.sleep(100)
        }
        throw AssertionError("Timed out waiting for $what")
    }

    @Test
    fun aRunShowsItsProgressAndTheNotificationGoesWhenItEnds() {
        session.begin(total = 3) {}
        waitFor("the notification") { shown() != null }
        assertEquals("Part 1 of 3, 0 cards so far", text(shown()!!))

        session.update(finished = 1, cards = 12)
        waitFor("the update") { shown()?.let { text(it) } == "Part 2 of 3, 12 cards so far" }

        session.end()
        waitFor("the notification to go") { shown() == null }
    }

    @Test
    fun theCancelActionReachesTheRun() {
        var cancelled = false
        session.begin(total = 2) { cancelled = true }
        waitFor("the notification") { shown() != null }

        val cancel = shown()!!.notification.actions.single { it.title == "Cancel" }
        cancel.actionIntent.send()

        waitFor("the cancel") { cancelled }
        assertTrue(cancelled)
    }

    private companion object {
        const val NOTIFICATION_ID = 2
    }
}
