package com.qinghe.biliaudio.api

import com.qinghe.biliaudio.model.AuthMethod
import com.qinghe.biliaudio.model.UserProfile
import com.qinghe.biliaudio.network.BiliApiService
import com.qinghe.biliaudio.network.BiliHttpClient

class AuthApiClient {

    fun getSupportedMethods(): List<AuthMethod> = AuthMethod.values().toList()

    /** Blocking stub kept for legacy call-sites; prefer suspend overloads from ViewModel. */
    fun login(method: AuthMethod): UserProfile =
        UserProfile("B站用户", 0, false, "请通过扫码完成登录")

    // ── Real async operations ───────────────────────────────────────────

    suspend fun generateQrCode(): BiliApiService.QrGenerateResult? =
        BiliApiService.generateQrCode()

    suspend fun pollQrLogin(key: String): BiliApiService.QrPollResult =
        BiliApiService.pollQrLogin(key)

    suspend fun fetchLoggedInUser(): UserProfile? = BiliApiService.fetchUserNav()

    fun logout() {
        BiliHttpClient.cookieStore.clearAll()
    }
}
