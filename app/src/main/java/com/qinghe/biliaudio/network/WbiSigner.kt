package com.qinghe.biliaudio.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.net.URLEncoder
import java.security.MessageDigest

/**
 * WBI (Web BI) signing for Bilibili APIs that require signature authentication.
 *
 * Algorithm:
 * 1. Fetch mixin key from /x/web-interface/nav (img + sub URL filenames)
 * 2. Apply fixed index mix permutation on concatenated 32-char key
 * 3. Append wts (Unix timestamp) to params, sort, concat, MD5 → w_rid
 */
object WbiSigner {

    private val MIX_INDEXES = intArrayOf(
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
        27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13
    )

    @Volatile private var cachedMixinKey: String? = null
    @Volatile private var keyExpiry: Long = 0L

    /** Append wts + w_rid to the given param map and return the signed map. */
    suspend fun sign(params: Map<String, String>): Map<String, String> {
        val key = getMixinKey()
        val wts = System.currentTimeMillis() / 1000L
        val mutable = LinkedHashMap<String, String>(params)
        mutable["wts"] = wts.toString()
        val query = mutable.entries
            .sortedBy { it.key }
            .joinToString("&") { "${encode(it.key)}=${encode(it.value)}" }
        mutable["w_rid"] = md5("$query$key")
        return mutable
    }

    fun invalidateCache() {
        cachedMixinKey = null
        keyExpiry = 0L
    }

    private suspend fun getMixinKey(): String {
        val now = System.currentTimeMillis()
        cachedMixinKey?.takeIf { now < keyExpiry }?.let { return it }
        return withContext(Dispatchers.IO) {
            val (imgKey, subKey) = fetchWbiKeys()
            val raw = imgKey + subKey
            val mixed = MIX_INDEXES.mapNotNull { raw.getOrNull(it) }.joinToString("").take(32)
            cachedMixinKey = mixed
            keyExpiry = now + 12 * 3600 * 1000L // 12h
            mixed
        }
    }

    private fun fetchWbiKeys(): Pair<String, String> {
        val url = "https://api.bilibili.com/x/web-interface/nav"
        val req = Request.Builder().url(url).get().build()
        val body = BiliHttpClient.client.newCall(req).execute().use { it.body?.string() ?: "" }
        val imgUrl = Regex(""""img_url"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.get(1) ?: ""
        val subUrl = Regex(""""sub_url"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.get(1) ?: ""
        return Pair(
            imgUrl.substringAfterLast("/").substringBefore("."),
            subUrl.substringAfterLast("/").substringBefore(".")
        )
    }

    private fun encode(s: String): String =
        URLEncoder.encode(s, "UTF-8").replace("+", "%20").replace("*", "%2A").replace("%7E", "~")

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** Build a signed query string suitable for appending to a URL. */
    suspend fun buildSignedQuery(params: Map<String, String>): String {
        val signed = sign(params)
        return signed.entries.joinToString("&") { "${encode(it.key)}=${encode(it.value)}" }
    }
}
