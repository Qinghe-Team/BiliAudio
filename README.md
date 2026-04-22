# BiliAudio

一款为小天才手表设计的听书软件，音源为 bilibili。

## 当前实现

本仓库已初始化为一个 **手表优先** 的 Android 单模块工程：

- 包名：`com.qinghe.biliaudio`
- 目标设备：Android 7.1 / 8.0 / 8.1 方屏手表
- UI：Kotlin + Compose for Wear OS
- 业务：Java 分域结构（认证、搜索、视频、互动、评论、收藏、播放、用户）
- 首版闭环：**首页 → 登录 → 搜索 → 视频详情 → 播放器**

## 目录职责

- `app/src/main/java/com/qinghe/biliaudio/ui`：Compose UI、导航、状态桥接
- `app/src/main/java/com/qinghe/biliaudio/ui/theme`：手表主题
- `app/src/main/java/com/qinghe/biliaudio/api`：B 站能力接入层占位
- `app/src/main/java/com/qinghe/biliaudio/auth`：登录域
- `app/src/main/java/com/qinghe/biliaudio/search`：搜索域
- `app/src/main/java/com/qinghe/biliaudio/video`：视频详情域
- `app/src/main/java/com/qinghe/biliaudio/interaction`：点赞 / 投币 / 收藏域
- `app/src/main/java/com/qinghe/biliaudio/comment`：评论域
- `app/src/main/java/com/qinghe/biliaudio/favorite`：收藏夹 / 稍后再听域
- `app/src/main/java/com/qinghe/biliaudio/player`：播放器控制域
- `app/src/main/java/com/qinghe/biliaudio/user`：个人中心域
- `app/src/main/java/com/qinghe/biliaudio/model`：共享模型

## 已落地的页面树

### 主界面

- 首页
- 搜索
- 我的收藏 / 稍后再听 / 历史
- 个人中心
- 当前播放入口

### 子界面

- 登录页（扫码 / 验证码 / 密码）
- 搜索结果页
- 视频详情页
- 播放器页
- 自定义倍率页
- 自定义定时关闭页
- 点赞 / 投币 / 收藏页
- 评论页
- 收藏页
- 个人中心页

## 当前 UI 方案

为 320×360 方屏手表采用：

- 单列层级导航
- 大点击区域卡片 / Chip
- 使用滚动列表承载主要信息
- 高频入口留在首页，复杂操作拆为子页
- 自定义倍率、自定义定时采用步进调节，避免依赖键盘

## 当前业务骨架

当前仓库先使用 **本地假数据和占位 API Client** 打通结构，后续可逐步替换为真实 bilibili API：

- 认证域：登录方式与登录结果
- 搜索域：热门词、搜索结果
- 视频域：推荐视频、详情
- 互动域：点赞 / 投币 / 收藏
- 评论域：评论列表
- 收藏域：收藏夹列表
- 播放域：倍速、定时关闭、当前播放状态

## 下一步建议

1. 将 `api` 目录中的占位实现替换为真实 bilibili API 封装
2. 接入登录态持久化、Cookie / Token 刷新
3. 接入真实播放地址解析与后台播放服务
4. 增加历史记录、收藏夹详情、评论回复、个人投稿等子页
5. 优化弱网重试、小屏长文本、省电与后台稳定性

## 构建说明

```bash
./gradlew assembleDebug
```

> 当前沙箱环境无法解析 Google Maven，因此依赖下载与完整构建校验可能失败；工程结构与源码已按 Android / Compose for Wear OS 方式搭建完成。

## GitHub Actions

已从 `9xhk-1/163MusicPro` 迁移并适配以下 workflow：

- `.github/workflows/build.yml`：分支 / PR 构建与测试
- `.github/workflows/release.yml`：`main` 分支自动构建并发布 Release APK
- `.github/workflows/opencode.yml`：评论触发 opencode

### 需要配置的 Secrets

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`
- `MINIMAX_API_KEY`（仅 opencode 需要）

### 签名说明

CI 会优先使用 `KEYSTORE_BASE64` 解码出的签名文件构建正式 APK；未提供签名信息时，release 构建会退回到 debug signing，保证 workflow 可持续产出安装包用于联调。
