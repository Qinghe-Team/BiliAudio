package com.qinghe.biliaudio.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Foreground service that owns the ExoPlayer instance.
 *
 * Back-ground keep-alive strategy:
 * 1. Runs as a foreground service (mediaPlayback type) so Android never kills it.
 * 2. Acquires a partial WakeLock so the CPU stays active during playback.
 * 3. Uses ExoPlayer's built-in WAKE_MODE_NETWORK to also keep the Wi-Fi radio on.
 * 4. Returns START_STICKY so the OS restarts the service after resource pressure kills it.
 * 5. Exposes the ExoPlayer via [companion.getPlayer] so [PlayerController] reuses the
 *    same instance instead of creating a private player that can't be guarded by a service.
 */
class AudioPlaybackService : MediaSessionService() {

    inner class LocalBinder : Binder() {
        val player: ExoPlayer? get() = exoPlayer
    }

    private val localBinder = LocalBinder()
    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val CHANNEL_ID = "biliaudio_playback"
        const val NOTIFICATION_ID = 1001

        /** Accessed by [PlayerController] to reuse the service-owned player. */
        @Volatile
        private var instance: AudioPlaybackService? = null

        fun getPlayer(): ExoPlayer? = instance?.exoPlayer

        /** Called by [PlayerController] / ViewModel to start the service (idempotent). */
        fun startIfNeeded(context: Context) {
            context.startForegroundService(
                Intent(context, AudioPlaybackService::class.java)
            )
        }

        /** Stop the service (e.g. user presses stop and no media session is active). */
        fun stopService(context: Context) {
            context.stopService(Intent(context, AudioPlaybackService::class.java))
        }
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        acquireWakeLock()

        exoPlayer = ExoPlayer.Builder(this)
            .setWakeMode(C.WAKE_MODE_NETWORK)   // keep Wi-Fi + partial CPU wake
            .setHandleAudioBecomingNoisy(true)   // pause on headphone unplug
            .build()

        mediaSession = MediaSession.Builder(this, exoPlayer!!).build()

        // Post a placeholder notification immediately so startForeground() is happy.
        startForeground(NOTIFICATION_ID, buildNotification("BiliAudio", "后台播放就绪"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY   // restart with null intent after being killed
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onBind(intent: Intent?): IBinder {
        // Return our LocalBinder for in-process clients; super handles MediaSession binding.
        super.onBind(intent)
        return localBinder
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Keep running even after the user swipes the app out of recents.
        // We only stop when there is no active media session.
        val player = exoPlayer
        if (player == null || !player.playWhenReady) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        instance = null
        mediaSession?.run {
            player.release()
            release()
        }
        exoPlayer = null
        mediaSession = null
        releaseWakeLock()
        super.onDestroy()
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /** Update the ongoing notification with current track title. */
    fun updateNotification(title: String, subtitle: String = "") {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(title, subtitle))
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "BiliAudio 播放",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "后台音频播放通知"
            setShowBadge(false)
        }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(title: String, subtitle: String): android.app.Notification {
        val openIntent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.let { PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE) }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(subtitle.ifBlank { "BiliAudio" })
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "BiliAudio::PlaybackWakeLock"
        ).also { it.acquire(12 * 60 * 60 * 1000L) }  // up to 12 h, released in onDestroy
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }
}
