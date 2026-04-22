package com.qinghe.biliaudio.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.qinghe.biliaudio.core.AppContainer
import com.qinghe.biliaudio.model.AuthMethod
import com.qinghe.biliaudio.model.CommentItem
import com.qinghe.biliaudio.model.InteractionSummary
import com.qinghe.biliaudio.model.VideoItem
import java.util.Locale

class AppViewModel(private val appContainer: AppContainer) : ViewModel() {
    var userProfile by mutableStateOf(appContainer.userRepository.currentUser)
        private set
    var loginMethods by mutableStateOf(appContainer.authRepository.supportedMethods)
        private set
    var featuredVideos by mutableStateOf(appContainer.videoRepository.featuredVideos)
        private set
    var hotKeywords by mutableStateOf(appContainer.searchRepository.hotKeywords)
        private set
    var searchQuery by mutableStateOf("")
        private set
    var searchResults by mutableStateOf(appContainer.searchRepository.search(""))
        private set
    var selectedVideo by mutableStateOf(appContainer.videoRepository.featuredVideos.firstOrNull())
        private set
    var comments by mutableStateOf(loadComments(selectedVideo))
        private set
    var interactionSummary by mutableStateOf(loadInteraction(selectedVideo))
        private set
    var favoriteCollections by mutableStateOf(appContainer.favoriteRepository.collections)
        private set
    var playbackSettings by mutableStateOf(appContainer.playerController.playbackSettings)
        private set
    var customSpeedDraft by mutableDoubleStateOf(playbackSettings.playbackSpeed)
        private set
    var customTimerDraft by mutableIntStateOf(if (playbackSettings.sleepTimerMinutes == 0) 45 else playbackSettings.sleepTimerMinutes)
        private set

    fun login(method: AuthMethod) {
        userProfile = appContainer.authRepository.login(method)
    }

    fun search(keyword: String) {
        searchQuery = keyword
        searchResults = appContainer.searchRepository.search(keyword)
        if (searchResults.isNotEmpty()) {
            selectVideo(searchResults.first())
        }
    }

    fun selectVideo(videoItem: VideoItem) {
        selectedVideo = appContainer.videoRepository.getVideoDetail(videoItem.id)
        comments = loadComments(selectedVideo)
        interactionSummary = loadInteraction(selectedVideo)
    }

    fun startPlayback() {
        selectedVideo?.let {
            playbackSettings = appContainer.playerController.play(it)
        }
    }

    fun setPlaybackSpeed(speed: Double) {
        playbackSettings = appContainer.playerController.setPlaybackSpeed(speed)
        customSpeedDraft = playbackSettings.playbackSpeed
    }

    fun setSleepTimer(minutes: Int) {
        playbackSettings = appContainer.playerController.setSleepTimerMinutes(minutes)
        customTimerDraft = if (minutes == 0) 45 else minutes
    }

    fun resetCustomSpeedDraft() {
        customSpeedDraft = playbackSettings.playbackSpeed
    }

    fun nudgeCustomSpeed(delta: Double) {
        customSpeedDraft = (customSpeedDraft + delta).coerceIn(0.5, 3.0)
    }

    fun applyCustomSpeed() {
        setPlaybackSpeed(String.format(Locale.US, "%.1f", customSpeedDraft).toDouble())
    }

    fun resetCustomTimerDraft() {
        customTimerDraft = if (playbackSettings.sleepTimerMinutes == 0) 45 else playbackSettings.sleepTimerMinutes
    }

    fun nudgeCustomTimer(delta: Int) {
        customTimerDraft = (customTimerDraft + delta).coerceIn(5, 180)
    }

    fun applyCustomTimer() {
        setSleepTimer(customTimerDraft)
    }

    fun like() {
        interactionSummary = appContainer.interactionRepository.like()
    }

    fun coin() {
        interactionSummary = appContainer.interactionRepository.coin()
    }

    fun favorite() {
        interactionSummary = appContainer.interactionRepository.favorite()
    }

    fun nowPlayingTitle(): String = playbackSettings.currentTitle

    fun speedPresets(): List<Double> = appContainer.playerController.speedPresets

    fun timerPresets(): List<Int> = appContainer.playerController.sleepTimerPresets

    private fun loadComments(videoItem: VideoItem?): List<CommentItem> {
        return appContainer.commentRepository.getCommentsForVideo(videoItem?.id ?: "default")
    }

    private fun loadInteraction(videoItem: VideoItem?): InteractionSummary {
        return appContainer.interactionRepository.getSummary(videoItem?.id ?: "default")
    }

    companion object {
        fun factory(appContainer: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AppViewModel(appContainer) as T
            }
        }
    }
}
