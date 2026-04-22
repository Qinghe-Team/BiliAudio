package com.qinghe.biliaudio.network

import android.content.Context
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Singleton OkHttpClient with Bilibili cookie jar and appropriate headers.
 */
object BiliHttpClient {

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 12; Pixel 6 Build/SQ1D.220205.004; wv) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.5414.117 " +
        "Mobile Safari/537.36 BiliApp/7.51.0 os/android model/Pixel_6 mobi_app/android"

    private lateinit var _cookieStore: BiliCookieStore
    val cookieStore: BiliCookieStore get() = _cookieStore

    lateinit var client: OkHttpClient
        private set

    fun init(context: Context) {
        _cookieStore = BiliCookieStore(context.applicationContext)
        client = OkHttpClient.Builder()
            .cookieJar(_cookieStore)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", "https://www.bilibili.com")
                    .header("Origin", "https://www.bilibili.com")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}
