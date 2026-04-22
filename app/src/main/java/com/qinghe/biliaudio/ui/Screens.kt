package com.qinghe.biliaudio.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.TitleCard
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.qinghe.biliaudio.model.AuthMethod
import com.qinghe.biliaudio.model.VideoItem

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
        item { AppChip("搜索", "热门词与结果列表", onSearch) }
        item { AppChip("登录", if (viewModel.userProfile.isLoggedIn) "已登录：${viewModel.userProfile.name}" else "支持扫码登录", onLogin) }
        item { AppChip("我的收藏", "收藏夹 / 稍后再听 / 历史", onFavorites) }
        item { AppChip("播放历史", "查看最近播放记录", onHistory) }
        item { AppChip("个人中心", "账号信息与阶段规划", onProfile) }
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
                val video = viewModel.featuredVideos[index]
                AppCompactChip(
                    label = video.title,
                    secondary = "${video.author} · ${video.durationLabel}",
                    onClick = { onOpenVideo(video) }
                )
            }
        }
    }
}

@Composable
fun LoginScreen(viewModel: AppViewModel) {
    WatchListScreen(title = "登录") {
        // QR code display when scan is in progress
        val qrUrl = viewModel.qrUrl
        if (qrUrl != null) {
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    QrCodeImage(url = qrUrl)
                }
            }
            item {
                TextBlock(title = "请用B站App扫码", body = viewModel.qrStatusMessage)
            }
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

@Composable
fun SearchScreen(viewModel: AppViewModel, openDetail: () -> Unit) {
    WatchListScreen(title = "搜索") {
        item {
            TextBlock(
                title = if (viewModel.searchQuery.isBlank()) "热门搜索" else "当前关键词：${viewModel.searchQuery}",
                body = "点击下方关键词即可搜索相关视频。"
            )
        }
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
        } else if (viewModel.searchResults.isEmpty()) {
            item { TextBlock(title = "暂无结果", body = "可继续尝试热门关键词。") }
        } else {
            items(viewModel.searchResults.size) { index ->
                val video = viewModel.searchResults[index]
                AppChip(
                    label = video.title,
                    secondary = "${video.author} · ${video.durationLabel}",
                    onClick = {
                        viewModel.selectVideo(video)
                        openDetail()
                    }
                )
            }
        }
    }
}

@Composable
fun VideoDetailScreen(
    viewModel: AppViewModel,
    openPlayer: () -> Unit,
    openInteractions: () -> Unit,
    openComments: () -> Unit
) {
    val video = viewModel.selectedVideo
    WatchListScreen(title = "视频详情") {
        item {
            TextBlock(
                title = video?.title ?: "未选择视频",
                body = video?.summary ?: "请先在首页或搜索页选择一条内容。"
            )
        }
        if (video != null) {
            item {
                AppChip(
                    label = if (viewModel.isLoadingPlayback) "获取播放地址…" else "开始播放",
                    secondary = "获取B站音频流并播放",
                    onClick = { viewModel.startPlayback(); openPlayer() }
                )
            }
            item { AppChip("互动操作", "点赞 / 投币 / 收藏", onClick = openInteractions) }
            item { AppChip("评论区", "查看评论与反馈", onClick = openComments) }
            item {
                TextBlock(
                    title = "创作者 · ${video.author}",
                    body = "时长 ${video.durationLabel}"
                )
            }
        }
    }
}

@Composable
fun PlayerScreen(viewModel: AppViewModel, openSpeed: () -> Unit, openTimer: () -> Unit) {
    WatchListScreen(title = "播放器") {
        item {
            TextBlock(
                title = if (viewModel.playbackSettings.isPlaying) viewModel.playbackSettings.currentTitle
                        else if (viewModel.isLoadingPlayback) "获取播放地址中…"
                        else "等待播放",
                body = "当前倍率 ${String.format("%.1f", viewModel.playbackSettings.playbackSpeed)}x · ${timerLabel(viewModel.playbackSettings.sleepTimerMinutes)}"
            )
        }
        if (viewModel.isLoadingPlayback) {
            item {
                Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            item {
                AppChip(
                    label = if (viewModel.playbackSettings.isPlaying) "暂停" else "继续播放",
                    secondary = if (viewModel.playbackSettings.isPlaying) "点击暂停音频" else "点击继续播放",
                    onClick = viewModel::pauseOrResume
                )
            }
        }
        item { SectionLabel("倍速预设") }
        items(viewModel.speedPresets().size) { index ->
            val speed = viewModel.speedPresets()[index]
            AppCompactChip(
                label = "${String.format("%.1f", speed)}x",
                secondary = if (speed == viewModel.playbackSettings.playbackSpeed) "当前使用中" else "切换播放速度",
                onClick = { viewModel.setPlaybackSpeed(speed) }
            )
        }
        item { AppChip("自定义倍率", "0.5x - 3.0x", onClick = openSpeed) }
        item { SectionLabel("定时关闭") }
        items(viewModel.timerPresets().size) { index ->
            val minutes = viewModel.timerPresets()[index]
            AppCompactChip(
                label = "${minutes} 分钟",
                secondary = if (minutes == viewModel.playbackSettings.sleepTimerMinutes) "当前定时" else "点击启用",
                onClick = { viewModel.setSleepTimer(minutes) }
            )
        }
        item { AppChip("自定义定时", "5 - 180 分钟", onClick = openTimer) }
        item { AppCompactChip("关闭定时", "清空休眠倒计时", onClick = { viewModel.setSleepTimer(0) }) }
    }
}

@Composable
fun InteractionsScreen(viewModel: AppViewModel) {
    WatchListScreen(title = "互动") {
        item {
            TextBlock(
                title = viewModel.selectedVideo?.title ?: "未选择视频",
                body = "点赞 ${viewModel.interactionSummary.likes} · 投币 ${viewModel.interactionSummary.coins} · 收藏 ${viewModel.interactionSummary.favorites} · 评论 ${viewModel.interactionSummary.comments}"
            )
        }
        item { AppChip("点赞", "同步增长本地状态", onClick = viewModel::like) }
        item { AppChip("投币", "后续可扩展硬币余额校验", onClick = viewModel::coin) }
        item { AppChip("收藏", "可继续接入多收藏夹选择", onClick = viewModel::favorite) }
    }
}

@Composable
fun CommentsScreen(viewModel: AppViewModel) {
    WatchListScreen(title = "评论") {
        items(viewModel.comments.size) { index ->
            val comment = viewModel.comments[index]
            TextBlock(title = comment.author, body = comment.content)
        }
    }
}

@Composable
fun FavoritesScreen(viewModel: AppViewModel, openHistory: () -> Unit) {
    WatchListScreen(title = "收藏与历史") {
        item {
            TextBlock(
                title = "首版范围",
                body = "先承载收藏夹、稍后再听与历史入口，后续再拆分为独立子页。"
            )
        }
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

@Composable
fun HistoryScreen(viewModel: AppViewModel, openDetail: () -> Unit) {
    WatchListScreen(title = "播放历史") {
        if (viewModel.playHistory.isEmpty()) {
            item {
                TextBlock(
                    title = "暂无历史记录",
                    body = "开始播放任意内容后会自动记录。"
                )
            }
        } else {
            items(viewModel.playHistory.size) { index ->
                val video = viewModel.playHistory[index]
                AppChip(
                    label = video.title,
                    secondary = "${video.author} · ${video.durationLabel}",
                    onClick = {
                        viewModel.selectVideo(video)
                        openDetail()
                    }
                )
            }
        }
    }
}

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
                body = "扫码登录、搜索（实时B站）、排行榜推荐、ExoPlayer音频播放、播放历史。"
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

@Composable
fun CustomSpeedScreen(viewModel: AppViewModel, close: () -> Unit) {
    WatchListScreen(title = "自定义倍率") {
        item {
            TextBlock(
                title = "${String.format("%.1f", viewModel.customSpeedDraft)}x",
                body = "使用加减按钮微调倍率，再应用到播放器。"
            )
        }
        item { AppChip("-0.1x", "降低播放速度", onClick = { viewModel.nudgeCustomSpeed(-0.1) }) }
        item { AppChip("+0.1x", "提高播放速度", onClick = { viewModel.nudgeCustomSpeed(0.1) }) }
        item { AppChip("应用倍率", "保存并返回播放器", onClick = { viewModel.applyCustomSpeed(); close() }) }
    }
}

@Composable
fun SleepTimerScreen(viewModel: AppViewModel, close: () -> Unit) {
    WatchListScreen(title = "自定义定时") {
        item {
            TextBlock(
                title = "${viewModel.customTimerDraft} 分钟",
                body = "步进式调节更适合手表设备。"
            )
        }
        item { AppChip("-5 分钟", "缩短休眠时间", onClick = { viewModel.nudgeCustomTimer(-5) }) }
        item { AppChip("+5 分钟", "延长休眠时间", onClick = { viewModel.nudgeCustomTimer(5) }) }
        item { AppChip("应用定时", "保存并返回播放器", onClick = { viewModel.applyCustomTimer(); close() }) }
    }
}

// ── QR Code composable ────────────────────────────────────────────────────

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
    } catch (_: Exception) { null }
}

// ── Shared primitives ─────────────────────────────────────────────────────

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
            contentPadding = PaddingValues(
                start = 8.dp,
                top = 30.dp,
                end = 8.dp,
                bottom = 20.dp
            )
        ) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.title3,
                        textAlign = TextAlign.Center
                    )
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
        label = {
            Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        secondaryLabel = {
            Text(text = secondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
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
                Text(text = secondary, style = MaterialTheme.typography.caption3, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
    if (minutes <= 0) "未设置定时关闭" else "${minutes} 分钟后关闭"

