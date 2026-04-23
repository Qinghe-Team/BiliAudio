package com.qinghe.biliaudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.qinghe.biliaudio.ui.BiliAudioApp
import com.qinghe.biliaudio.ui.theme.BiliAudioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContainer = (application as BiliAudioApplication).appContainer
        setContent {
            BiliAudioTheme {
                BiliAudioApp(appContainer = appContainer)
            }
        }
    }
}
