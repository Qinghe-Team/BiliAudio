package com.qinghe.biliaudio.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qinghe.biliaudio.core.AppContainer
import com.qinghe.biliaudio.model.AuthMethod
import com.qinghe.biliaudio.model.CommentItem
import com.qinghe.biliaudio.model.InteractionSummary
import com.qinghe.biliaudio.model.VideoItem
import com.qinghe.biliaudio.network.BiliApiService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class AppViewModel(private val appContainer: AppContainer) : ViewModel() {

    // ── UI State ────────────────────────────────────────────────────────
    var userProfile by mutableStateOf(appContainer.authRepository.currentUser)
        private set
    var loginMethods by mutableStateOf(appContainer.authRepository.supportedMethods)
        private set
    var featuredVideos by mutableStateOf<List<VideoItem>>(emptyList())
        private set
    var hotKeywords by mutableStateOf(appContainer.searchRepository.hotKeywords)
        private set
    var searchQuery by mutableStateOf("")
        private set
    var searchResults by mutableStateOf<List<VideoItem>>(emptyList())
        private set
    var selectedVideo by mutableStateOf<VideoItem?>(null)
        private set
    var comments by mutableStateOf<List<CommentItem>>(emptyList())
        private set
    var interactionSummary by mutableStateOf(loadInteraction(null))
        private set
    var favoriteCollections by mutableStateOf(appContainer.favoriteRepository.collections)
        private set
    var playHistory by mutableStateOf(appContainer.historyRepository.recentHistory)
        private set
    var playbackSettings by mutableStateOf(appContainer.playerController.playbackSettings)
        private set
    var customSpeedDraft by mutableDoubleStateOf(playbackSettings.playbackSpeed)
        private set
    var customTimerDraft by mutableIntStateOf(
        if (playbackSettings.sleepTimerMinutes == 0) 45 else playbackSettings.sleepTimerMinutes
    )
        private set

    // ── Loading / error states ──────────────────────────────────────────
    var isLoadingHome by mutableStateOf(false)
        private set
    var isLoadingSearch by mutableStateOf(false)
        private set
    var isLoadingPlayback by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    // ── QR Login State ──────────────────────────────────────────────────
    var qrUrl by mutableStateOf<String?>(null)
        private set
    var qrStatusMessage by mutableStateOf("")
        private set
    var isQrExpired by mutableStateOf(false)
        private set

    private var qrPollJob: Job? = null
    private var currentQrKey: String? = null

    init {
        appContainer.playerController.ensurePlayerCreated()
        loadHome()
    }

    // ── Home ────────────────────────────────────────────────────────────

    fun loadHome() {
        viewModelScope.launch {
            isLoadingHome = true
            errorMessage = null
            try {
                val videos = appContainer.videoApiClient.fetchFeaturedVideos()
                if (videos.isNotEmpty()) featuredVideos = videos
            } catch (e: Exception) {
                errorMessage = "推荐内容加载失败：${e.message}"
            }
            try {
                val keywords = appContainer.searchApiClient.fetchHotKeywords()
                if (keywords.isNotEmpty()) hotKeywords = keywords
            } catch (_: Exception) { }
            isLoadingHome = false
        }
    }

    // ── QR Login ────────────────────────────────────────────────────────

    fun startQrLogin() {
        qrPollJob?.cancel()
        isQrExpired = false
        qrStatusMessage = "正在生成二维码…"
        viewModelScope.launch {
            val result = appContainer.authApiClient.generateQrCode()
            if (result == null) {
                qrStatusMessage = "生成失败，请重试"
                return@launch
            }
            qrUrl = result.url
            currentQrKey = result.qrcodeKey
            qrStatusMessage = "请用B站App扫码"
            pollQrStatus(result.qrcodeKey)
        }
    }

    private fun pollQrStatus(key: String) {
        qrPollJob?.cancel()
        qrPollJob = viewModelScope.launch {
            while (true) {
                delay(2000)
                val poll = appContainer.authApiClient.pollQrLogin(key)
                qrStatusMessage = poll.message
                when (poll.status) {
                    BiliApiService.QrStatus.SUCCESS -> {
                        qrUrl = null
                        val user = appContainer.authApiClient.fetchLoggedInUser()
                        if (user != null) {
                            userProfile = user
                            appContainer.authRepository.setCurrentUser(user)
                        }
                        break
                    }
                    BiliApiService.QrStatus.EXPIRED -> {
                        isQrExpired = true
                        break
                    }
                    BiliApiService.QrStatus.ERROR -> break
                    else -> { /* NOT_SCANNED or SCANNED – keep polling */ }
                }
            }
        }
    }

    fun login(method: AuthMethod) {
        if (method == AuthMethod.QR_CODE) {
            startQrLogin()
        }
    }

    fun logout() {
        qrPollJob?.cancel()
        qrUrl = null
        appContainer.authApiClient.logout()
        userProfile = appContainer.authRepository.logout()
    }

    // ── Search ──────────────────────────────────────────────────────────

    fun search(keyword: String) {
        searchQuery = keyword
        if (keyword.isBlank()) {
            searchResults = emptyList()
            return
        }
        isLoadingSearch = true
        viewModelScope.launch {
            try {
                val results = appContainer.searchApiClient.searchVideos(keyword)
                searchResults = results
                if (results.isNotEmpty()) selectVideo(results.first())
            } catch (_: Exception) {
                searchResults = emptyList()
            }
            isLoadingSearch = false
        }
    }

    // ── Video Selection ──────────────────────────────────────────────────

    fun selectVideo(videoItem: VideoItem) {
        selectedVideo = videoItem
        viewModelScope.launch {
            // Fetch full detail (with cid) in background
            val detail = appContainer.videoApiClient.fetchVideoDetail(videoItem.id)
            if (detail != null) selectedVideo = detail
            comments = loadComments(selectedVideo)
            interactionSummary = loadInteraction(selectedVideo)
        }
    }

    // ── Playback ────────────────────────────────────────────────────────

    fun startPlayback() {
        val video = selectedVideo ?: return
        if (isLoadingPlayback) return
        isLoadingPlayback = true
        viewModelScope.launch {
            try {
                playbackSettings = appContainer.playerController.play(video)
                appContainer.historyRepository.recordPlay(video)
                playHistory = appContainer.historyRepository.recentHistory
            } catch (_: Exception) { }
            isLoadingPlayback = false
        }
    }

    fun pauseOrResume() {
        playbackSettings = appContainer.playerController.pauseOrResume()
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

    // ── Interactions ─────────────────────────────────────────────────────

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

    fun dismissError() { errorMessage = null }

    // ── Private helpers ──────────────────────────────────────────────────

    private fun loadComments(videoItem: VideoItem?): List<CommentItem> =
        appContainer.commentRepository.getCommentsForVideo(videoItem?.id ?: "default")

    private fun loadInteraction(videoItem: VideoItem?): InteractionSummary =
        appContainer.interactionRepository.getSummary(videoItem?.id ?: "default")

    override fun onCleared() {
        super.onCleared()
        appContainer.playerController.release()
    }

    companion object {
        fun factory(appContainer: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AppViewModel(appContainer) as T
            }
    }
}

