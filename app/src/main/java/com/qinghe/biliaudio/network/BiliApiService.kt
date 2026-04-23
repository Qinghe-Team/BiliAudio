package com.qinghe.biliaudio.network

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.qinghe.biliaudio.model.UserProfile
import com.qinghe.biliaudio.model.VideoItem
import com.qinghe.biliaudio.model.VideoPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request

/**
 * Centralised Bilibili API calls. All functions are suspend and must be
 * called from a coroutine context; they run on Dispatchers.IO internally.
 */
object BiliApiService {

    // ──────────────────────────────────────────────────────────────────────
    // QR Code Login
    // ──────────────────────────────────────────────────────────────────────

    data class QrGenerateResult(val url: String, val qrcodeKey: String)

    enum class QrStatus { NOT_SCANNED, SCANNED, SUCCESS, EXPIRED, ERROR }
    data class QrPollResult(val status: QrStatus, val message: String = "")

    /** Step 1: Generate a new QR code login URL. */
    suspend fun generateQrCode(): QrGenerateResult? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://passport.bilibili.com/x/passport-login/web/qrcode/generate")
                .post(FormBody.Builder().build())
                .build()
            val body = BiliHttpClient.client.newCall(req).execute().use { it.body?.string() ?: "" }
            val root = JsonParser.parseString(body).asJsonObject
            if (root["code"].asInt != 0) return@withContext null
            val data = root["data"].asJsonObject
            QrGenerateResult(
                url = data["url"].asString,
                qrcodeKey = data["qrcode_key"].asString
            )
        } catch (e: Exception) {
            null
        }
    }

    /** Step 2: Poll QR code scan status. Returns SUCCESS when login completes. */
    suspend fun pollQrLogin(qrcodeKey: String): QrPollResult = withContext(Dispatchers.IO) {
        try {
            val url = "https://passport.bilibili.com/x/passport-login/web/qrcode/poll" +
                    "?qrcode_key=$qrcodeKey&source=main-mini-login"
            val req = Request.Builder().url(url).get().build()
            val body = BiliHttpClient.client.newCall(req).execute().use { it.body?.string() ?: "" }
            val root = JsonParser.parseString(body).asJsonObject
            val data = root["data"].asJsonObject
            when (data["code"].asInt) {
                0 -> QrPollResult(QrStatus.SUCCESS, "登录成功")
                86090 -> QrPollResult(QrStatus.SCANNED, "已扫码，等待确认")
                86101 -> QrPollResult(QrStatus.NOT_SCANNED, "等待扫码")
                86038 -> QrPollResult(QrStatus.EXPIRED, "二维码已过期")
                else -> QrPollResult(QrStatus.ERROR, data["message"]?.asString ?: "未知错误")
            }
        } catch (e: Exception) {
            QrPollResult(QrStatus.ERROR, e.message ?: "网络错误")
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // User Info
    // ──────────────────────────────────────────────────────────────────────

    /** Fetch the currently logged-in user's nav info. Returns null if not logged in. */
    suspend fun fetchUserNav(): UserProfile? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.bilibili.com/x/web-interface/nav")
                .get().build()
            val body = BiliHttpClient.client.newCall(req).execute().use { it.body?.string() ?: "" }
            val root = JsonParser.parseString(body).asJsonObject
            if (root["code"].asInt != 0) return@withContext null
            val data = root["data"].asJsonObject
            if (!data["isLogin"].asBoolean) return@withContext null
            val level = data.getAsJsonObject("level_info")?.get("current_level")?.asInt ?: 0
            UserProfile(
                data["uname"].asString,
                level,
                true,
                "B站Lv$level · ${data["uname"].asString}"
            )
        } catch (e: Exception) {
            null
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Search & Hot Keywords
    // ──────────────────────────────────────────────────────────────────────

    /** Fetch trending / hot search keywords. */
    suspend fun fetchHotKeywords(): List<String> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://app.bilibili.com/x/v2/search/trending/ranking?limit=10")
                .get().build()
            val body = BiliHttpClient.client.newCall(req).execute().use { it.body?.string() ?: "" }
            val root = JsonParser.parseString(body).asJsonObject
            if (root["code"].asInt != 0) return@withContext defaultHotKeywords()
            val list = root["data"].asJsonObject["trending"].asJsonObject["list"].asJsonArray
            list.mapNotNull { it.asJsonObject["keyword"]?.asString }.take(8)
        } catch (e: Exception) {
            defaultHotKeywords()
        }
    }

    /** Search for videos by keyword, returning a list of VideoItem. */
    suspend fun searchVideos(keyword: String): List<VideoItem> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf("keyword" to keyword, "search_type" to "video")
            val query = WbiSigner.buildSignedQuery(params)
            val url = "https://api.bilibili.com/x/web-interface/wbi/search/type?$query"
            val req = Request.Builder().url(url).get().build()
            val body = BiliHttpClient.client.newCall(req).execute().use { it.body?.string() ?: "" }
            val root = JsonParser.parseString(body).asJsonObject
            if (root["code"].asInt != 0) return@withContext emptyList()
            val result = root["data"].asJsonObject["result"]?.asJsonArray ?: return@withContext emptyList()
            result.take(20).mapNotNull { parseVideoSearchItem(it.asJsonObject) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseVideoSearchItem(obj: JsonObject): VideoItem? {
        return try {
            val bvid = obj["bvid"]?.asString ?: return null
            val title = obj["title"]?.asString?.replace(Regex("</?em>"), "") ?: return null
            val author = obj["author"]?.asString ?: ""
            val desc = obj["description"]?.asString ?: obj["desc"]?.asString ?: ""
            val duration = obj["duration"]?.asString ?: ""
            val coverUrl = normalizeCoverUrl(obj["pic"]?.asString ?: "")
            VideoItem(bvid, title, author, desc, duration, "", 0L, 0L, coverUrl, emptyList())
        } catch (_: Exception) { null }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Ranked / Featured Videos  (音频区排行 rid=3)
    // ──────────────────────────────────────────────────────────────────────

    /** Fetch the music/audio ranking list (tid 3 = 音乐). Falls back to search. */
    suspend fun fetchRankingVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.bilibili.com/x/web-interface/ranking/v2?rid=193&type=all&ps=20&pn=1")
                .get().build()
            val body = BiliHttpClient.client.newCall(req).execute().use { it.body?.string() ?: "" }
            val root = JsonParser.parseString(body).asJsonObject
            if (root["code"].asInt != 0) return@withContext searchVideos("有声书")
            val list = root["data"].asJsonObject["list"].asJsonArray
            list.take(20).mapNotNull { parseRankingItem(it.asJsonObject) }
        } catch (e: Exception) {
            searchVideos("有声书")
        }
    }

    private fun parseRankingItem(obj: JsonObject): VideoItem? {
        return try {
            val bvid = obj["bvid"]?.asString ?: return null
            val title = obj["title"]?.asString ?: return null
            val author = obj["owner"]?.asJsonObject?.get("name")?.asString ?: ""
            val desc = obj["desc"]?.asString ?: obj["description"]?.asString ?: ""
            val duration = formatDuration(obj["duration"]?.asInt ?: 0)
            val coverUrl = normalizeCoverUrl(obj["pic"]?.asString ?: "")
            VideoItem(bvid, title, author, desc, duration, "", 0L, 0L, coverUrl, emptyList())
        } catch (_: Exception) { null }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Video Detail (by BVID)
    // ──────────────────────────────────────────────────────────────────────

    /** Fetch detailed video info including cid (needed for stream URL). */
    suspend fun fetchVideoInfo(bvid: String): VideoInfoResult? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.bilibili.com/x/web-interface/view?bvid=$bvid")
                .get().build()
            val body = BiliHttpClient.client.newCall(req).execute().use { it.body?.string() ?: "" }
            val root = JsonParser.parseString(body).asJsonObject
            if (root["code"].asInt != 0) return@withContext null
            val data = root["data"].asJsonObject
            val pagesArray = data["pages"]?.asJsonArray
            val pages = pagesArray?.mapNotNull { p ->
                try {
                    val po = p.asJsonObject
                    VideoPage(
                        po["page"]?.asInt ?: 1,
                        po["cid"]?.asLong ?: 0L,
                        po["part"]?.asString ?: "",
                        po["duration"]?.asInt ?: 0
                    )
                } catch (_: Exception) { null }
            } ?: emptyList()
            VideoInfoResult(
                avid = data["aid"].asLong,
                bvid = data["bvid"].asString,
                cid = data["cid"].asLong,
                title = data["title"].asString,
                author = data["owner"].asJsonObject["name"].asString,
                desc = data["desc"].asString,
                duration = formatDuration(data["duration"].asInt),
                coverUrl = normalizeCoverUrl(data["pic"]?.asString ?: ""),
                pages = pages
            )
        } catch (e: Exception) {
            null
        }
    }

    data class VideoInfoResult(
        val avid: Long,
        val bvid: String,
        val cid: Long,
        val title: String,
        val author: String,
        val desc: String,
        val duration: String,
        val coverUrl: String,
        val pages: List<VideoPage>
    )

    // ──────────────────────────────────────────────────────────────────────
    // Stream URL
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Fetch the direct MP4 stream URL for audio playback.
     * Uses html5 platform to avoid Referer restrictions.
     * qn=16 = 360P which is sufficient for audio-only content.
     */
    suspend fun fetchStreamUrl(avid: Long, cid: Long): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.bilibili.com/x/player/playurl" +
                    "?avid=$avid&cid=$cid&qn=16&fnval=1&fnver=0&fourk=0" +
                    "&platform=html5&otype=json&type=&high_quality=0"
            val req = Request.Builder().url(url).get().build()
            val body = BiliHttpClient.client.newCall(req).execute().use { it.body?.string() ?: "" }
            val root = JsonParser.parseString(body).asJsonObject
            if (root["code"].asInt != 0) return@withContext null
            root["data"].asJsonObject["durl"]?.asJsonArray
                ?.firstOrNull()?.asJsonObject?.get("url")?.asString
        } catch (e: Exception) {
            null
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────

    private fun normalizeCoverUrl(url: String): String {
        if (url.isBlank()) return ""
        return if (url.startsWith("http")) url else "https:$url"
    }

    private fun formatDuration(seconds: Int): String {
        if (seconds <= 0) return "--:--"
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    private fun defaultHotKeywords() = DEFAULT_HOT_KEYWORDS

    /** Fallback hot keywords shown when the API is unavailable. */
    val DEFAULT_HOT_KEYWORDS = listOf(
        "有声书", "广播剧", "纪录片", "英语听力", "故事会", "科学", "音乐", "脱口秀"
    )
}
