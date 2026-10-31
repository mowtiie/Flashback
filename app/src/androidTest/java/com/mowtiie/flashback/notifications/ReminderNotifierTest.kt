package com.mowtiie.flashback.notifications

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderNotifierTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager = context.getSystemService(NotificationManager::class.java)

    @Before
    fun setUp() = manager.cancelAll()

    @After
    fun tearDown() = manager.cancelAll()

    /** Before Android 13 the permission does not exist, so the check must not block the reminder. */
    @Test
    fun olderAndroidVersionsCanAlwaysPost() {
        assumeTrue(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
        assertTrue(context.canPostNotifications())
    }

    @Test
    fun theReminderIsPostedWithTheDueCount() {
        assumeTrue(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)

        ReminderNotifier.notifyDue(context, 3)

        val posted = manager.activeNotifications.single()
        assertEquals(
            context.resources.getQuantityString(com.mowtiie.flashback.R.plurals.reminder_body, 3, 3),
            posted.notification.extras.getCharSequence(android.app.Notification.EXTRA_TEXT).toString(),
        )
    }
}
