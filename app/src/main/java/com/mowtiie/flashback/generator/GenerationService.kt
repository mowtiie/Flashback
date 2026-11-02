package com.mowtiie.flashback.generator

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.mowtiie.flashback.FlashbackApp
import com.mowtiie.flashback.MainActivity
import com.mowtiie.flashback.R
import com.mowtiie.flashback.notifications.canPostNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Keeps the app running while cards are made with the screen off or the app in the background, and shows
 * the progress. The run itself stays in the view model; this only holds the process and the notification.
 */
class GenerationService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watching: Job? = null

    private val session: GenerationSession
        get() = (application as FlashbackApp).container.generationSession

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            session.cancel()
            return START_NOT_STICKY
        }

        ensureChannel(this)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification(session.progress.value),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )

        if (watching == null) {
            watching = scope.launch {
                session.progress.collect { progress ->
                    if (progress == null) {
                        ServiceCompat.stopForeground(this@GenerationService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    } else {
                        update(progress)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    // The system gave this kind of service its time limit; stop the run instead of being killed.
    override fun onTimeout(startId: Int, fgsType: Int) {
        session.cancel()
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // The permission is checked through canPostNotifications(), which lint cannot see into.
    @SuppressLint("MissingPermission")
    private fun update(progress: GenerationSession.Progress) {
        if (canPostNotifications()) {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification(progress))
        }
    }

    private fun notification(progress: GenerationSession.Progress?): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val cancel = PendingIntent.getService(
            this,
            1,
            Intent(this, GenerationService::class.java).setAction(ACTION_CANCEL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val parts = progress?.total ?: 0
        val text = if (progress != null && parts > 1) {
            getString(
                R.string.generation_notification_parts,
                (progress.finished + 1).coerceAtMost(parts),
                parts,
                resources.getQuantityString(R.plurals.generation_notification_cards, progress.cards, progress.cards),
            )
        } else {
            getString(R.string.generation_notification_single)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.generation_notification_title))
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(parts, progress?.finished ?: 0, parts <= 1)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(open)
            .addAction(0, getString(R.string.action_cancel), cancel)
            .build()
    }

    private companion object {
        const val CHANNEL_ID = "card_generation"
        const val NOTIFICATION_ID = 2
        const val ACTION_CANCEL = "com.mowtiie.flashback.action.CANCEL_GENERATION"

        fun ensureChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.generation_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(R.string.generation_channel_body) }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
}
