package com.qinghe.biliaudio.api

import com.qinghe.biliaudio.model.VideoItem
import com.qinghe.biliaudio.network.BiliApiService

class VideoApiClient {

    /** Synchronous stub – real data is loaded asynchronously via ViewModel. */
    fun getFeaturedVideos(): List<VideoItem> = emptyList()

    fun getVideoDetail(videoId: String): VideoItem =
        VideoItem(videoId, "加载中…", "", "", "", "")

    // ── Real async operations ────────────────────────────────────────────

    /** Fetch ranked / popular audio-related videos from Bilibili. */
    suspend fun fetchFeaturedVideos(): List<VideoItem> =
        BiliApiService.fetchRankingVideos()

    /** Fetch full video detail (including cid, cover URL, and pages needed for playback). */
    suspend fun fetchVideoDetail(bvid: String): VideoItem? {
        val info = BiliApiService.fetchVideoInfo(bvid) ?: return null
        return VideoItem(
            info.bvid, info.title, info.author, info.desc, info.duration, "",
            info.cid, info.avid, info.coverUrl, info.pages
        )
    }

    /** Fetch the direct MP4 stream URL for the given video. */
    suspend fun fetchStreamUrl(item: VideoItem): String? {
        // If we already have cid, go straight to stream URL
        if (item.cid != 0L && item.avid != 0L) {
            return BiliApiService.fetchStreamUrl(item.avid, item.cid)
        }
        // Otherwise fetch video info first to get cid
        val info = BiliApiService.fetchVideoInfo(item.id) ?: return null
        return BiliApiService.fetchStreamUrl(info.avid, info.cid)
    }
}
