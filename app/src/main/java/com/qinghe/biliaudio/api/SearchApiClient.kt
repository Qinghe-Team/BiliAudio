package com.qinghe.biliaudio.api

import com.qinghe.biliaudio.model.VideoItem
import com.qinghe.biliaudio.network.BiliApiService

class SearchApiClient(private val videoApiClient: VideoApiClient) {

    /** Synchronous stub – ViewModel uses async overloads. */
    fun getHotKeywords(): List<String> =
        listOf("有声书", "广播剧", "纪录片", "英语听力", "故事会", "音乐")

    /** Synchronous stub. */
    fun search(query: String): List<VideoItem> = emptyList()

    // ── Real async operations ────────────────────────────────────────────

    suspend fun fetchHotKeywords(): List<String> = BiliApiService.fetchHotKeywords()

    suspend fun searchVideos(keyword: String): List<VideoItem> =
        BiliApiService.searchVideos(keyword)
}
