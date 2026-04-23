package com.qinghe.biliaudio.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.qinghe.biliaudio.api.PlayerApiClient
import com.qinghe.biliaudio.api.VideoApiClient
import com.qinghe.biliaudio.model.PlaybackSettings
import com.qinghe.biliaudio.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Controls audio playback.
 *
 * ExoPlayer source priority:
 * 1. Service-owned player ([AudioPlaybackService.getPlayer]) — used whenever the service is alive.
 *    This player lives inside a foreground service so Android cannot kill it in the background.
 * 2. Local fallback player — used in unit tests or when the service is not yet started.
 *
 * [ensurePlayerCreated] starts the service (if a Context is available) and sets up listeners.
 * Callers should call it on the main thread before any playback operation.
 */
class PlayerController(
    context: Context?,
    private val playerApiClient: PlayerApiClient,
    private val videoApiClient: VideoApiClient
) {
    private val appContext = context?.applicationContext

    // Fallback local player when the service is unavailable (tests / cold start).
    private var localPlayer: ExoPlayer? = null

    @Volatile
    private var _settings = PlaybackSettings("未开始播放", 1.0, 0, false, 0L, 0L, 1.0f)

    val playbackSettings: PlaybackSettings get() = _settings
    val speedPresets: List<Double> get() = playerApiClient.speedPresets
    val sleepTimerPresets: List<Int> get() = playerApiClient.sleepTimerPresets

    // ── Player acquisition ─────────────────────────────────────────────────

    /** Returns the service player if available, otherwise the local fallback. */
    private val player: ExoPlayer?
        get() = AudioPlaybackService.getPlayer() ?: localPlayer

    /** Start the background service and attach a listener. Does nothing if Context is null. */
    fun ensurePlayerCreated() {
        val ctx = appContext ?: return

        // Start foreground service for keep-alive.
        AudioPlaybackService.startIfNeeded(ctx)

        // If the service player is not yet available (e.g. service not yet bound),
        // create a local player as fallback so the UI is immediately usable.
        if (AudioPlaybackService.getPlayer() == null && localPlayer == null) {
            localPlayer = ExoPlayer.Builder(ctx)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setHandleAudioBecomingNoisy(true)
                .build()
        }

        player?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val p = player ?: return
                _settings = PlaybackSettings(
                    _settings.currentTitle, _settings.playbackSpeed,
                    _settings.sleepTimerMinutes, isPlaying,
                    p.currentPosition.coerceAtLeast(0L),
                    p.duration.coerceAtLeast(0L),
                    _settings.volume
                )
                // Keep notification in sync.
                (AudioPlaybackService.getPlayer()?.let {
                    appContext?.let { ctx2 ->
                        AudioPlaybackService.startIfNeeded(ctx2)
                    }
                })
            }
        })
    }

    // ── Playback ───────────────────────────────────────────────────────────

    suspend fun play(videoItem: VideoItem): PlaybackSettings {
        ensurePlayerCreated()
        _settings = PlaybackSettings(videoItem.title, _settings.playbackSpeed,
            _settings.sleepTimerMinutes, false, 0L, 0L, _settings.volume)

        val url = withContext(Dispatchers.IO) {
            videoApiClient.fetchStreamUrl(videoItem)
        }

        withContext(Dispatchers.Main) {
            val p = player ?: return@withContext
            p.stop()
            if (url != null) {
                val mediaItem = MediaItem.Builder()
                    .setUri(url)
                    .setMediaId(videoItem.id)
                    .build()
                p.setMediaItem(mediaItem)
                p.prepare()
                p.playWhenReady = true
                applySpeedToPlayer(p, _settings.playbackSpeed)
                p.volume = _settings.volume
            }
            _settings = PlaybackSettings(
                videoItem.title, _settings.playbackSpeed, _settings.sleepTimerMinutes,
                url != null,
                0L, p.duration.coerceAtLeast(0L), _settings.volume
            )
        }
        return _settings
    }

    /** Snapshot current position & duration (call from main thread). */
    fun snapshotProgress(): PlaybackSettings {
        val p = player ?: return _settings
        _settings = PlaybackSettings(
            _settings.currentTitle, _settings.playbackSpeed, _settings.sleepTimerMinutes,
            p.isPlaying, p.currentPosition.coerceAtLeast(0L),
            p.duration.coerceAtLeast(0L), _settings.volume
        )
        return _settings
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun setVolume(volume: Float): PlaybackSettings {
        val v = volume.coerceIn(0f, 1f)
        _settings = PlaybackSettings(_settings.currentTitle, _settings.playbackSpeed,
            _settings.sleepTimerMinutes, _settings.isPlaying,
            _settings.positionMs, _settings.durationMs, v)
        player?.volume = v
        return _settings
    }

    fun setPlaybackSpeed(speed: Double): PlaybackSettings {
        val normalized = speed.coerceIn(0.5, 3.0)
        _settings = PlaybackSettings(_settings.currentTitle, normalized,
            _settings.sleepTimerMinutes, _settings.isPlaying,
            _settings.positionMs, _settings.durationMs, _settings.volume)
        player?.let { applySpeedToPlayer(it, normalized) }
        return _settings
    }

    fun setSleepTimerMinutes(minutes: Int): PlaybackSettings {
        val normalized = minutes.coerceIn(0, 300)
        _settings = PlaybackSettings(_settings.currentTitle, _settings.playbackSpeed,
            normalized, _settings.isPlaying,
            _settings.positionMs, _settings.durationMs, _settings.volume)
        return _settings
    }

    fun pauseOrResume(): PlaybackSettings {
        val p = player ?: return _settings
        if (p.isPlaying) p.pause() else p.play()
        _settings = PlaybackSettings(_settings.currentTitle, _settings.playbackSpeed,
            _settings.sleepTimerMinutes, p.isPlaying,
            p.currentPosition.coerceAtLeast(0L),
            p.duration.coerceAtLeast(0L), _settings.volume)
        return _settings
    }

    fun release() {
        localPlayer?.release()
        localPlayer = null
        // Service player is managed by AudioPlaybackService; don't release it here.
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun applySpeedToPlayer(player: ExoPlayer, speed: Double) {
        player.playbackParameters = player.playbackParameters.withSpeed(speed.toFloat())
    }
}

