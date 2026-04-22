package com.qinghe.biliaudio.api

import com.qinghe.biliaudio.model.VideoItem
import com.qinghe.biliaudio.network.BiliApiService

class SearchApiClient(private val videoApiClient: VideoApiClient) {

    /** Synchronous stub – ViewModel uses async overloads. */
    fun getHotKeywords(): List<String> = BiliApiService.DEFAULT_HOT_KEYWORDS

    /** Synchronous stub. */
    fun search(query: String): List<VideoItem> = emptyList()

    // ── Real async operations ────────────────────────────────────────────

    suspend fun fetchHotKeywords(): List<String> = BiliApiService.fetchHotKeywords()

    suspend fun searchVideos(keyword: String): List<VideoItem> =
        BiliApiService.searchVideos(keyword)
}
