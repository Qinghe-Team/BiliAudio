package com.qinghe.biliaudio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateBottomPadding
import androidx.compose.foundation.layout.calculateTopPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.wear.compose.foundation.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.TitleCard
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.qinghe.biliaudio.model.AuthMethod
import com.qinghe.biliaudio.model.VideoItem

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onLogin: () -> Unit,
    onSearch: () -> Unit,
    onFavorites: () -> Unit,
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
                Text(if (viewModel.playbackSettings.playing) viewModel.nowPlayingTitle() else "尚未开始播放，点击进入播放器")
            }
        }
        item { AppChip("搜索", "热门词与结果列表", onSearch) }
        item { AppChip("登录", if (viewModel.userProfile.loggedIn) "已登录：${viewModel.userProfile.name}" else "支持扫码/验证码/密码", onLogin) }
        item { AppChip("我的收藏", "收藏夹 / 稍后再听 / 历史", onFavorites) }
        item { AppChip("个人中心", "账号信息与阶段规划", onProfile) }
        item { SectionLabel("推荐内容") }
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

@Composable
fun LoginScreen(viewModel: AppViewModel) {
    WatchListScreen(title = "登录") {
        item {
            TextBlock(
                title = if (viewModel.userProfile.loggedIn) "当前已登录" else "选择登录方式",
                body = viewModel.userProfile.signature
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
    }
}

@Composable
fun SearchScreen(viewModel: AppViewModel, openDetail: () -> Unit) {
    WatchListScreen(title = "搜索") {
        item {
            TextBlock(
                title = if (viewModel.searchQuery.isBlank()) "热门搜索" else "当前关键词：${viewModel.searchQuery}",
                body = "手表优先采用短路径选择，不强依赖键盘输入。"
            )
        }
        items(viewModel.hotKeywords.size) { index ->
            val keyword = viewModel.hotKeywords[index]
            AppCompactChip(
                label = keyword,
                secondary = "点击筛选相关视频",
                onClick = { viewModel.search(keyword) }
            )
        }
        item { SectionLabel("搜索结果") }
        if (viewModel.searchResults.isEmpty()) {
            item {
                TextBlock(title = "暂无结果", body = "可继续尝试热门关键词。")
            }
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
            item { AppChip("开始播放", "音频化播放 / 队列 / 定时", onClick = { viewModel.startPlayback(); openPlayer() }) }
            item { AppChip("互动操作", "点赞 / 投币 / 收藏", onClick = openInteractions) }
            item { AppChip("评论区", "查看评论与反馈", onClick = openComments) }
            item {
                TextBlock(
                    title = "创作者 · ${video.author}",
                    body = "时长 ${video.durationLabel}，后续可在此接入分 P、相关推荐、清晰度与播放地址解析。"
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
                title = if (viewModel.playbackSettings.playing) viewModel.playbackSettings.currentTitle else "等待播放",
                body = "当前倍率 ${String.format("%.1f", viewModel.playbackSettings.playbackSpeed)}x · ${timerLabel(viewModel.playbackSettings.sleepTimerMinutes)}"
            )
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
fun FavoritesScreen(viewModel: AppViewModel) {
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
    }
}

@Composable
fun ProfileScreen(viewModel: AppViewModel) {
    WatchListScreen(title = "个人中心") {
        item {
            TextBlock(
                title = viewModel.userProfile.name,
                body = "等级 ${viewModel.userProfile.level} · ${if (viewModel.userProfile.loggedIn) "已登录" else "游客模式"}"
            )
        }
        item {
            TextBlock(
                title = "一期已接入域",
                body = "认证、搜索、视频、互动、评论、收藏、播放控制；后续补齐个人投稿、历史同步、完整 API。"
            )
        }
        item {
            TextBlock(
                title = "UI 结构",
                body = "主界面保留高频入口；登录、搜索、详情、播放器、互动、评论等均拆成子页。"
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

@Composable
private fun WatchListScreen(title: String, content: ScalingLazyListScope.() -> Unit) {
    val listState = rememberScalingLazyListState()
    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) { paddingValues ->
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            autoCentering = AutoCenteringParams(itemIndex = 0),
            contentPadding = PaddingValues(
                start = 8.dp,
                top = paddingValues.calculateTopPadding(),
                end = 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 10.dp
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
    AuthMethod.QR_CODE -> "适合手机配合扫描授权"
    AuthMethod.SMS_CODE -> "适合无扫码环境的快速验证"
    AuthMethod.PASSWORD -> "适合老账号直接输入"
}

private fun timerLabel(minutes: Int): String {
    return if (minutes <= 0) "未设置定时关闭" else "${minutes} 分钟后关闭"
}
