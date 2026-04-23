package com.qinghe.biliaudio.player

import android.content.Context
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
 * Controls audio playback via ExoPlayer.  All long-running operations
 * (stream URL fetch) are suspend functions; player state mutations happen
 * on the main thread through ExoPlayer's thread-safe API.
 */
class PlayerController(
    context: Context?,
    private val playerApiClient: PlayerApiClient,
    private val videoApiClient: VideoApiClient
) {
    private val appContext = context?.applicationContext

    // ExoPlayer is main-thread only; lazy-initialised on first access from UI.
    private var exoPlayer: ExoPlayer? = null

    @Volatile
    private var _settings = PlaybackSettings("未开始播放", 1.0, 0, false, 0L, 0L, 1.0f)

    val playbackSettings: PlaybackSettings get() = _settings
    val speedPresets: List<Double> get() = playerApiClient.speedPresets
    val sleepTimerPresets: List<Int> get() = playerApiClient.sleepTimerPresets

    /** Call on the main thread before using the player. Does nothing if Context is unavailable. */
    fun ensurePlayerCreated() {
        val ctx = appContext ?: return
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(ctx).build().also { player ->
                player.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _settings = PlaybackSettings(
                            _settings.currentTitle,
                            _settings.playbackSpeed,
                            _settings.sleepTimerMinutes,
                            isPlaying,
                            player.currentPosition,
                            player.duration.coerceAtLeast(0L),
                            _settings.volume
                        )
                    }
                })
            }
        }
    }

    /**
     * Fetch the stream URL for [videoItem] and start playback.
     * Must be called from a coroutine (IO + Main switch handled internally).
     */
    suspend fun play(videoItem: VideoItem): PlaybackSettings {
        ensurePlayerCreated()
        _settings = PlaybackSettings(videoItem.title, _settings.playbackSpeed, _settings.sleepTimerMinutes,
            false, 0L, 0L, _settings.volume)

        val url = withContext(Dispatchers.IO) {
            videoApiClient.fetchStreamUrl(videoItem)
        }

        withContext(Dispatchers.Main) {
            val player = exoPlayer ?: return@withContext
            player.stop()
            if (url != null) {
                val mediaItem = MediaItem.Builder()
                    .setUri(url)
                    .setMediaId(videoItem.id)
                    .build()
                player.setMediaItem(mediaItem)
                player.prepare()
                player.playWhenReady = true
                applySpeedToPlayer(player, _settings.playbackSpeed)
                player.volume = _settings.volume
            }
            _settings = PlaybackSettings(
                videoItem.title, _settings.playbackSpeed, _settings.sleepTimerMinutes,
                url != null,
                0L, player.duration.coerceAtLeast(0L), _settings.volume
            )
        }
        return _settings
    }

    /** Snapshot current position & duration from the player (call from main thread). */
    fun snapshotProgress(): PlaybackSettings {
        val player = exoPlayer ?: return _settings
        val pos = player.currentPosition.coerceAtLeast(0L)
        val dur = player.duration.coerceAtLeast(0L)
        _settings = PlaybackSettings(_settings.currentTitle, _settings.playbackSpeed,
            _settings.sleepTimerMinutes, player.isPlaying, pos, dur, _settings.volume)
        return _settings
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun setVolume(volume: Float): PlaybackSettings {
        val v = volume.coerceIn(0f, 1f)
        _settings = PlaybackSettings(_settings.currentTitle, _settings.playbackSpeed,
            _settings.sleepTimerMinutes, _settings.isPlaying,
            _settings.positionMs, _settings.durationMs, v)
        exoPlayer?.volume = v
        return _settings
    }

    fun setPlaybackSpeed(speed: Double): PlaybackSettings {
        val normalized = speed.coerceIn(0.5, 3.0)
        _settings = PlaybackSettings(_settings.currentTitle, normalized, _settings.sleepTimerMinutes,
            _settings.isPlaying, _settings.positionMs, _settings.durationMs, _settings.volume)
        exoPlayer?.let { applySpeedToPlayer(it, normalized) }
        return _settings
    }

    fun setSleepTimerMinutes(minutes: Int): PlaybackSettings {
        val normalized = minutes.coerceIn(0, 180)
        _settings = PlaybackSettings(_settings.currentTitle, _settings.playbackSpeed, normalized,
            _settings.isPlaying, _settings.positionMs, _settings.durationMs, _settings.volume)
        return _settings
    }

    fun pauseOrResume(): PlaybackSettings {
        val player = exoPlayer ?: return _settings
        if (player.isPlaying) player.pause() else player.play()
        _settings = PlaybackSettings(_settings.currentTitle, _settings.playbackSpeed,
            _settings.sleepTimerMinutes, player.isPlaying,
            player.currentPosition.coerceAtLeast(0L),
            player.duration.coerceAtLeast(0L), _settings.volume)
        return _settings
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
    }

    private fun applySpeedToPlayer(player: ExoPlayer, speed: Double) {
        val params = player.playbackParameters.withSpeed(speed.toFloat())
        player.playbackParameters = params
    }
}
