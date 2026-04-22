package com.qinghe.biliaudio.network

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Cookie jar backed by SharedPreferences for session persistence across restarts.
 */
class BiliCookieStore(context: Context) : CookieJar {

    private val prefs = context.getSharedPreferences("bili_cookies", Context.MODE_PRIVATE)
    private val store = mutableMapOf<String, MutableList<Cookie>>()

    init {
        // Restore persisted cookies on startup
        prefs.all.forEach { (key, value) ->
            if (value is String) {
                try {
                    val parts = value.split("|")
                    if (parts.size >= 3) {
                        val host = parts[0]
                        val name = parts[1]
                        val cookieVal = parts[2]
                        val url = HttpUrl.Builder()
                            .scheme("https").host(host).build()
                        val cookie = Cookie.Builder()
                            .domain(host).name(name).value(cookieVal).build()
                        store.getOrPut(host) { mutableListOf() }.add(cookie)
                    }
                } catch (_: Exception) { /* skip malformed entries */ }
            }
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val list = store.getOrPut(host) { mutableListOf() }
        val editor = prefs.edit()
        cookies.forEach { cookie ->
            list.removeAll { it.name == cookie.name }
            list.add(cookie)
            editor.putString("${host}_${cookie.name}", "${host}|${cookie.name}|${cookie.value}")
        }
        editor.apply()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        return store[host]?.toList() ?: emptyList()
    }

    fun getCookieValue(name: String): String? {
        return store.values.flatten().find { it.name == name }?.value
    }

    fun clearAll() {
        store.clear()
        prefs.edit().clear().apply()
    }
}
