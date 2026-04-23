package com.qinghe.biliaudio.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.InlineSlider
import androidx.wear.compose.material.InlineSliderDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.TitleCard
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import coil.compose.AsyncImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.qinghe.biliaudio.model.AuthMethod
import com.qinghe.biliaudio.model.VideoItem
import com.qinghe.biliaudio.model.VideoPage

// ── Home ──────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onLogin: () -> Unit,
    onSearch: () -> Unit,
    onFavorites: () -> Unit,
    onHistory: () -> Unit,
    onProfile: () -> Unit,
    onPlayer: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit
) {
    WatchListScreen(title = "首页") {
        item {
            TitleCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                onClick = onPlayer,
                title = { Text("当前播放") }
            ) {
                Text(if (viewModel.playbackSettings.isPlaying) viewModel.nowPlayingTitle() else "尚未开始播放，点击进入播放器")
            }
        }
        item { AppChip("搜索", "热门词与语音/关键词搜索", onSearch) }
        item { AppChip("登录", if (viewModel.userProfile.isLoggedIn) "已登录：${viewModel.userProfile.name}" else "支持扫码登录", onLogin) }
        item { AppChip("我的收藏", "收藏夹 / 稍后再听 / 历史", onFavorites) }
        item { AppChip("播放历史", "查看最近播放记录", onHistory) }
        item { AppChip("个人中心", "账号信息", onProfile) }
        item { SectionLabel(if (viewModel.isLoadingHome) "推荐内容（加载中）" else "推荐内容") }
        if (viewModel.isLoadingHome) {
            item {
                Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (viewModel.featuredVideos.isEmpty()) {
            item { AppChip("刷新推荐", "从B站获取最新内容", onClick = viewModel::loadHome) }
        } else {
            items(viewModel.featuredVideos.size) { index ->
                VideoCard(video = viewModel.featuredVideos[index], onClick = { onOpenVideo(viewModel.featuredVideos[index]) })
            }
        }
    }
}

// ── Login ─────────────────────────────────────────────────────────────────────

@Composable
fun LoginScreen(viewModel: AppViewModel) {
    WatchListScreen(title = "登录") {
        val qrUrl = viewModel.qrUrl
        if (qrUrl != null) {
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    QrCodeImage(url = qrUrl)
                }
            }
            item { TextBlock(title = "请用B站App扫码", body = viewModel.qrStatusMessage) }
            if (viewModel.isQrExpired) {
                item { AppChip("二维码已过期", "点击重新生成", onClick = viewModel::startQrLogin) }
            }
            return@WatchListScreen
        }
        item {
            TextBlock(
                title = if (viewModel.userProfile.isLoggedIn) "当前已登录" else "选择登录方式",
                body = if (viewModel.qrStatusMessage.isNotBlank()) viewModel.qrStatusMessage
                       else viewModel.userProfile.signature
            )
        }
        items(viewModel.loginMethods.size) { index ->
            val method = viewModel.loginMethods[index]
            AppChip(
                label = method.displayName,
                secondary = loginHint(method),
                onClick = { viewModel.login(method) }
            )
        }
        if (viewModel.userProfile.isLoggedIn) {
            item { AppChip("退出登录", "切回游客模式", onClick = viewModel::logout) }
        }
    }
}

// ── Search ────────────────────────────────────────────────────────────────────

@Composable
fun SearchScreen(viewModel: AppViewModel, openDetail: () -> Unit) {
    // Voice search launcher
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull() ?: return@rememberLauncherForActivityResult
            if (text.isNotBlank()) viewModel.search(text)
        }
    }

    WatchListScreen(title = "搜索") {
        item {
            TextBlock(
                title = if (viewModel.searchQuery.isBlank()) "热门搜索" else "关键词：${viewModel.searchQuery}",
                body = "点击热词搜索，或用语音输入自定义词。"
            )
        }
        // Voice input button
        item {
            AppChip(
                label = "🎤 语音搜索",
                secondary = "说出想听的内容关键词",
                onClick = {
                    voiceLauncher.launch(
                        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出搜索关键词")
                            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                        }
                    )
                }
            )
        }
        item { SectionLabel("热门关键词") }
        items(viewModel.hotKeywords.size) { index ->
            val keyword = viewModel.hotKeywords[index]
            AppCompactChip(
                label = keyword,
                secondary = "点击搜索相关视频",
                onClick = { viewModel.search(keyword) }
            )
        }
        item { SectionLabel(if (viewModel.isLoadingSearch) "搜索结果（加载中）" else "搜索结果") }
        if (viewModel.isLoadingSearch) {
            item {
                Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (viewModel.searchResults.isEmpty() && viewModel.searchQuery.isNotBlank()) {
            item { TextBlock(title = "暂无结果", body = "尝试其他关键词。") }
        } else {
            items(viewModel.searchResults.size) { index ->
                val video = viewModel.searchResults[index]
                VideoCard(
                    video = video,
                    onClick = {
                        viewModel.selectVideo(video)
                        openDetail()
                    }
                )
            }
        }
    }
}

// ── Video Detail ───────────────────────────────────────────────────────────────

@Composable
fun VideoDetailScreen(
    viewModel: AppViewModel,
    openPlayer: () -> Unit,
    openInteractions: () -> Unit,
    openComments: () -> Unit,
    openPartSelect: () -> Unit
) {
    val video = viewModel.selectedVideo
    WatchListScreen(title = "视频详情") {
        if (video != null) {
            // Cover + info card
            item {
                VideoCoverCard(video = video)
            }
            item {
                AppChip(
                    label = if (viewModel.isLoadingPlayback) "获取播放地址…" else "开始播放",
                    secondary = "获取B站音频流并播放",
                    onClick = { viewModel.startPlayback(); openPlayer() }
                )
            }
            if (video.hasMultipleParts()) {
                item {
                    AppChip(
                        label = "选择分P",
                        secondary = "共 ${video.pages.size} 集，当前 P${currentPartIndex(video)}",
                        onClick = openPartSelect
                    )
                }
            }
            item { AppChip("互动操作", "点赞 / 投币 / 收藏", onClick = openInteractions) }
            item { AppChip("评论区", "查看评论与反馈", onClick = openComments) }
        } else {
            item { TextBlock(title = "未选择视频", body = "请先在首页或搜索页选择内容。") }
        }
    }
}

private fun currentPartIndex(video: VideoItem): Int {
    val page = video.pages.firstOrNull { it.cid == video.cid } ?: video.pages.firstOrNull()
    return page?.page ?: 1
}

// ── Part Select ───────────────────────────────────────────────────────────────

@Composable
fun PartSelectScreen(viewModel: AppViewModel, close: () -> Unit) {
    val video = viewModel.selectedVideo
    WatchListScreen(title = "选择分P") {
        if (video == null || video.pages.isEmpty()) {
            item { TextBlock(title = "无分P信息", body = "该视频未包含分P数据。") }
            return@WatchListScreen
        }
        items(video.pages.size) { index ->
            val page = video.pages[index]
            val isCurrentPart = page.cid == video.cid
            AppChip(
                label = page.displayTitle(),
                secondary = if (isCurrentPart) "当前选中" else formatSeconds(page.durationSeconds),
                onClick = {
                    viewModel.selectPart(page)
                    close()
                }
            )
        }
    }
}

private fun formatSeconds(s: Int): String {
    if (s <= 0) return "--:--"
    val m = s / 60; val sec = s % 60
    return "%d:%02d".format(m, sec)
}

// ── Player ─────────────────────────────────────────────────────────────────────

@Composable
fun PlayerScreen(
    viewModel: AppViewModel,
    openMore: () -> Unit
) {
    val settings = viewModel.playbackSettings
    WatchListScreen(title = "播放器") {
        // Title
        item {
            TitleCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                onClick = {},
                title = {
                    Text(
                        when {
                            settings.isPlaying -> settings.currentTitle
                            viewModel.isLoadingPlayback -> "获取播放地址中…"
                            else -> "等待播放"
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            ) {
                Text(
                    "${String.format("%.1f", settings.playbackSpeed)}x · ${timerLabel(settings.sleepTimerMinutes)}",
                    style = MaterialTheme.typography.caption3
                )
            }
        }

        // Progress bar
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                InlineSlider(
                    value = settings.progress,
                    onValueChange = { viewModel.seekTo(it) },
                    increments = 100,
                    segmented = false,
                    decreaseIcon = { InlineSliderDefaults.Decrease },
                    increaseIcon = { InlineSliderDefaults.Increase },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatMs(settings.positionMs), style = MaterialTheme.typography.caption3)
                    Text(formatMs(settings.durationMs), style = MaterialTheme.typography.caption3)
                }
            }
        }

        // Prev / Play-Pause / Next row
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (viewModel.hasPrevious()) {
                    Button(
                        onClick = viewModel::playPrevious,
                        colors = ButtonDefaults.secondaryButtonColors(),
                        modifier = Modifier.size(40.dp)
                    ) { Text("⏮", style = MaterialTheme.typography.caption1) }
                }
                if (viewModel.isLoadingPlayback) {
                    CircularProgressIndicator(modifier = Modifier.size(40.dp))
                } else {
                    Button(
                        onClick = viewModel::pauseOrResume,
                        colors = ButtonDefaults.primaryButtonColors(),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Text(if (settings.isPlaying) "⏸" else "▶", style = MaterialTheme.typography.title3)
                    }
                }
                if (viewModel.hasNext()) {
                    Button(
                        onClick = viewModel::playNext,
                        colors = ButtonDefaults.secondaryButtonColors(),
                        modifier = Modifier.size(40.dp)
                    ) { Text("⏭", style = MaterialTheme.typography.caption1) }
                }
            }
        }

        // Volume control
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("音量", style = MaterialTheme.typography.caption1)
                InlineSlider(
                    value = viewModel.volumeDraft,
                    onValueChange = { viewModel.setVolume(it) },
                    increments = 20,
                    segmented = false,
                    decreaseIcon = { InlineSliderDefaults.Decrease },
                    increaseIcon = { InlineSliderDefaults.Increase },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Speed presets (compact)
        item { SectionLabel("倍速") }
        items(viewModel.speedPresets().size) { index ->
            val speed = viewModel.speedPresets()[index]
            AppCompactChip(
                label = "${String.format("%.1f", speed)}x",
                secondary = if (speed == settings.playbackSpeed) "✓ 使用中" else "",
                onClick = { viewModel.setPlaybackSpeed(speed) }
            )
        }

        // More button
        item { AppChip("更多设置", "定时 / 自定义倍率 / 投币收藏", onClick = openMore) }
    }
}

// ── Player More ────────────────────────────────────────────────────────────────

@Composable
fun PlayerMoreScreen(
    viewModel: AppViewModel,
    openSpeed: () -> Unit,
    openTimer: () -> Unit,
    openInteractions: () -> Unit,
    close: () -> Unit
) {
    WatchListScreen(title = "更多设置") {
        item { SectionLabel("定时关闭") }
        items(viewModel.timerPresets().size) { index ->
            val minutes = viewModel.timerPresets()[index]
            AppCompactChip(
                label = "${minutes} 分钟",
                secondary = if (minutes == viewModel.playbackSettings.sleepTimerMinutes) "✓ 当前定时" else "",
                onClick = { viewModel.setSleepTimer(minutes) }
            )
        }
        item { AppChip("自定义定时", "自由输入分钟数", onClick = { openTimer(); close() }) }
        item { AppCompactChip("关闭定时", "清除定时关闭", onClick = { viewModel.setSleepTimer(0) }) }
        item { SectionLabel("自定义倍速") }
        item { AppChip("自定义倍率", "0.5x – 3.0x", onClick = { openSpeed(); close() }) }
        item { SectionLabel("互动") }
        item { AppChip("互动操作", "点赞 / 投币 / 收藏", onClick = { openInteractions(); close() }) }
    }
}

// ── Custom Speed ───────────────────────────────────────────────────────────────

@Composable
fun CustomSpeedScreen(viewModel: AppViewModel, close: () -> Unit) {
    WatchListScreen(title = "自定义倍率") {
        item {
            TextBlock(
                title = "${String.format("%.1f", viewModel.customSpeedDraft)}x",
                body = "当前草稿倍率"
            )
        }
        item { AppChip("-0.1x", "降低播放速度", onClick = { viewModel.nudgeCustomSpeed(-0.1) }) }
        item { AppChip("+0.1x", "提高播放速度", onClick = { viewModel.nudgeCustomSpeed(0.1) }) }
        item { AppChip("-0.5x", "快速降速", onClick = { viewModel.nudgeCustomSpeed(-0.5) }) }
        item { AppChip("+0.5x", "快速加速", onClick = { viewModel.nudgeCustomSpeed(0.5) }) }
        item { AppChip("应用倍率", "保存并返回播放器", onClick = { viewModel.applyCustomSpeed(); close() }) }
    }
}

// ── Sleep Timer ───────────────────────────────────────────────────────────────

@Composable
fun SleepTimerScreen(viewModel: AppViewModel, close: () -> Unit) {
    WatchListScreen(title = "自定义定时") {
        item {
            TitleCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                onClick = {},
                title = { Text("${viewModel.customTimerDraft} 分钟") }
            ) {
                Text("调节后点击「应用定时」保存")
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("分钟数 (1–300)", style = MaterialTheme.typography.caption1)
                InlineSlider(
                    value = viewModel.customTimerDraft.toFloat(),
                    onValueChange = { viewModel.setCustomTimerDraft(it.toInt()) },
                    valueRange = 1f..300f,
                    increments = 299,
                    segmented = false,
                    decreaseIcon = { InlineSliderDefaults.Decrease },
                    increaseIcon = { InlineSliderDefaults.Increase },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item { AppChip("-5 分钟", "缩短休眠时间", onClick = { viewModel.nudgeCustomTimer(-5) }) }
        item { AppChip("+5 分钟", "延长休眠时间", onClick = { viewModel.nudgeCustomTimer(5) }) }
        item { AppChip("-15 分钟", "快速缩短", onClick = { viewModel.nudgeCustomTimer(-15) }) }
        item { AppChip("+15 分钟", "快速延长", onClick = { viewModel.nudgeCustomTimer(15) }) }
        item { AppChip("应用定时", "保存并返回", onClick = { viewModel.applyCustomTimer(); close() }) }
    }
}

// ── Interactions ───────────────────────────────────────────────────────────────

@Composable
fun InteractionsScreen(viewModel: AppViewModel) {
    WatchListScreen(title = "互动") {
        item {
            TextBlock(
                title = viewModel.selectedVideo?.title ?: "未选择视频",
                body = "点赞 ${viewModel.interactionSummary.likes} · 投币 ${viewModel.interactionSummary.coins} · 收藏 ${viewModel.interactionSummary.favorites}"
            )
        }
        item { AppChip("点赞", "同步增长本地状态", onClick = viewModel::like) }
        item { AppChip("投币", "后续可接入硬币余额校验", onClick = viewModel::coin) }
        item { AppChip("收藏", "可继续接入多收藏夹选择", onClick = viewModel::favorite) }
    }
}

// ── Comments ───────────────────────────────────────────────────────────────────

@Composable
fun CommentsScreen(viewModel: AppViewModel) {
    WatchListScreen(title = "评论") {
        items(viewModel.comments.size) { index ->
            val comment = viewModel.comments[index]
            TextBlock(title = comment.author, body = comment.content)
        }
    }
}

// ── Favorites ─────────────────────────────────────────────────────────────────

@Composable
fun FavoritesScreen(viewModel: AppViewModel, openHistory: () -> Unit) {
    WatchListScreen(title = "收藏与历史") {
        items(viewModel.favoriteCollections.size) { index ->
            val collection = viewModel.favoriteCollections[index]
            AppChip(
                label = collection.name,
                secondary = "${collection.itemCount} 项 · ${collection.description}",
                onClick = {}
            )
        }
        item { AppChip("查看播放历史", "按最近播放时间排序", onClick = openHistory) }
    }
}

// ── History ───────────────────────────────────────────────────────────────────

@Composable
fun HistoryScreen(viewModel: AppViewModel, openDetail: () -> Unit) {
    WatchListScreen(title = "播放历史") {
        if (viewModel.playHistory.isEmpty()) {
            item { TextBlock(title = "暂无历史记录", body = "开始播放任意内容后会自动记录。") }
        } else {
            items(viewModel.playHistory.size) { index ->
                val video = viewModel.playHistory[index]
                VideoCard(video = video, onClick = { viewModel.selectVideo(video); openDetail() })
            }
        }
    }
}

// ── Profile ───────────────────────────────────────────────────────────────────

@Composable
fun ProfileScreen(viewModel: AppViewModel) {
    WatchListScreen(title = "个人中心") {
        item {
            TextBlock(
                title = viewModel.userProfile.name,
                body = "等级 ${viewModel.userProfile.level} · ${if (viewModel.userProfile.isLoggedIn) "已登录" else "游客模式"}"
            )
        }
        item {
            TextBlock(
                title = "已接入功能",
                body = "扫码登录 / 搜索（含语音）/ 排行榜推荐 / ExoPlayer音频 / 分P选择 / 进度条 / 音量控制 / 播放历史"
            )
        }
        item {
            TextBlock(
                title = "B站API来源",
                body = "基于 xtcqinghe/bac 文档实现，含WBI签名认证与Cookie会话管理。"
            )
        }
    }
}

// ── QR Code composable ────────────────────────────────────────────────────────

@Composable
private fun QrCodeImage(url: String) {
    val bitmap = remember(url) { generateQrBitmap(url) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "登录二维码",
            modifier = Modifier.size(160.dp)
        )
    } else {
        Text("二维码生成失败", textAlign = TextAlign.Center)
    }
}

private fun generateQrBitmap(url: String, sizePx: Int = 320): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val matrix = writer.encode(url, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        bmp
    } catch (e: Exception) {
        android.util.Log.e("BiliAudio", "QR code generation failed", e)
        null
    }
}

// ── Video card composable (cover + title + author + duration) ─────────────────

@Composable
private fun VideoCard(video: VideoItem, onClick: () -> Unit) {
    Chip(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp),
        colors = ChipDefaults.secondaryChipColors(),
        onClick = onClick,
        icon = {
            if (video.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = video.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colors.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎵", style = MaterialTheme.typography.caption1)
                }
            }
        },
        label = {
            Text(video.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        secondaryLabel = {
            Text(
                "${video.author} · ${video.durationLabel}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.caption3
            )
        }
    )
}

// ── Cover detail card (used in VideoDetailScreen) ─────────────────────────────

@Composable
private fun VideoCoverCard(video: VideoItem) {
    TitleCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp),
        onClick = {},
        title = { Text(video.title, maxLines = 2, overflow = TextOverflow.Ellipsis) }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (video.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = video.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("👤 ${video.author}", style = MaterialTheme.typography.caption3, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("⏱ ${video.durationLabel}", style = MaterialTheme.typography.caption3)
            }
            if (video.hasMultipleParts()) {
                Text("共 ${video.pages.size} 集", style = MaterialTheme.typography.caption3)
            }
        }
    }
}

// ── Shared primitives ─────────────────────────────────────────────────────────

@Composable
private fun WatchListScreen(title: String, content: ScalingLazyListScope.() -> Unit) {
    val listState = rememberScalingLazyListState()
    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            autoCentering = AutoCenteringParams(itemIndex = 0),
            contentPadding = PaddingValues(start = 8.dp, top = 30.dp, end = 8.dp, bottom = 20.dp)
        ) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = title, style = MaterialTheme.typography.title3, textAlign = TextAlign.Center)
                }
            }
            content()
        }
    }
}

@Composable
private fun AppChip(label: String, secondary: String, onClick: () -> Unit) {
    Chip(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp),
        colors = ChipDefaults.primaryChipColors(),
        onClick = onClick,
        label = { Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        secondaryLabel = { Text(text = secondary, maxLines = 2, overflow = TextOverflow.Ellipsis) }
    )
}

@Composable
private fun AppCompactChip(label: String, secondary: String, onClick: () -> Unit) {
    CompactChip(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp),
        onClick = onClick,
        label = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (secondary.isNotBlank()) {
                    Text(text = secondary, style = MaterialTheme.typography.caption3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    )
}

@Composable
private fun SectionLabel(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = text, style = MaterialTheme.typography.caption1)
    }
}

@Composable
private fun TextBlock(title: String, body: String) {
    TitleCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp),
        onClick = {},
        title = { Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis) }
    ) {
        Text(body, maxLines = 4, overflow = TextOverflow.Ellipsis)
    }
}

private fun loginHint(method: AuthMethod): String = when (method) {
    AuthMethod.QR_CODE -> "用手机B站App扫描二维码"
    AuthMethod.SMS_CODE -> "适合无扫码环境的快速验证"
    AuthMethod.PASSWORD -> "适合老账号直接输入"
}

private fun timerLabel(minutes: Int): String =
    if (minutes <= 0) "无定时" else "${minutes}min"

private fun formatMs(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
