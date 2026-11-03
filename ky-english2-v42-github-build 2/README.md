# 考研英语二 V4.2 · Android 离线学习 App

适配小米13 / 澎湃 OS 1 等 Android 设备的标准 Gradle / Android SDK 构建工程，保留 V4.1 内置词库与训练内容。旧 APK 闪退，因此这次必须使用 GitHub Actions 或 Android Studio **正规构建**的 `app-debug.apk`，不要继续安装旧版。

## GitHub Actions 一键构建（不需要电脑装 Android Studio）

1. 将项目内容（包含隐藏的 `.github` 文件夹）上传到 GitHub 仓库的 `main` 分支。
2. 在 GitHub 仓库打开 **Actions → Build Android APK → Run workflow → Run workflow**，等待任务完成并显示绿色 ✓。
3. 打开这次运行记录底部 **Artifacts → ky-english2-v4.2-debug-apk**，下载 ZIP，解压得到 `app-debug.apk`。
4. 把 `app-debug.apk` 发送到手机并安装。旧版若已安装且签名不同，先卸载旧 APK（会清除旧版的本地学习进度）。

## 特性与边界

- 全部词汇、原创模拟题及学习脚本打包在 App 内，不需要联网学习，且 **没有声明 INTERNET 权限**。
- 网页内容由 AndroidX WebViewAssetLoader 加载，提供安全的 HTTPS 本地域名；学习记录保存在手机 WebView 本地存储。
- 语音朗读调用手机 TTS；若离线发音不可用，需提前在系统里安装英语语音数据。
- 支持 `kyenglish2://study/today`、`kyenglish2://study/review`、`kyenglish2://study/reading` 深度链接。滴答清单能否识别自定义 Scheme 为可点击链接，取决于它的客户端，不要在验证前更改原任务。
- GitHub Actions 产物是 **debug 测试签名 APK**，可用于自己侧载和测试，不能作为已验证的上架发布包。正式长期使用/升级需要稳定的 release 签名与手机测试。
- 未在小米13实体设备验证；本项目避免旧版手工拼接 AndroidManifest.xml / classes.dex 的方式，但无法保证所有设备首次启动都正常。
- WebView App 与原网页版进度不自动同步。
