# 消息图谱 Message Atlas

消息图谱是一款本地优先的 Android 通知聚合与 AI 重点整理应用。它通过系统通知使用权自动记录各应用的新通知，并在用户主动操作时，将指定日期的消息交给用户配置的 AI 接口整理成清晰的重点卡片。

> 当前版本：`v0.2.1` 测试版。最低支持 Android 8.0（API 26），目标 Android 15。

## 下载与体验

请在仓库右侧 **Releases** 中下载最新的 `message-atlas-v0.2.1-optimized.apk`。这是启用 R8 和 release 级优化、使用 Android Debug 证书签名的体验版本，适合安装测试；正式商店发布版本将使用独立发行签名。

安装后首次打开应用，按引导进入系统页面开启“通知使用权”。通知抓取能力建议使用真机测试。

## 核心能力

- 自动记录通知标题、正文、时间、来源应用及原始状态
- 按日期浏览，支持搜索、来源筛选、重点标记和删除
- 全部收录、白名单、黑名单三种规则模式
- 自定义 OpenAI Chat Completions 兼容接口、密钥和 Prompt
- 通过兼容接口的 `GET /models` 自动获取提供方模型列表，同时允许手动输入模型 ID
- AI 输出严格结构化 JSON，应用原生展示“立即关注、待办、重要、一般、次要”卡片
- API 密钥使用 Android Keystore AES-GCM 加密，仅保存在本机
- Compose 原生页面滑动动画，由 Android 帧时钟跟随设备刷新节奏绘制，并支持关闭动画
- AI 日报本地存档、收藏、删除和纯文本分享

## AI 接口兼容说明

填写 Base URL，例如：

```text
https://api.example.com/v1
```

应用会调用：

```text
GET  /v1/models
POST /v1/chat/completions
```

模型提供方需要返回 OpenAI 兼容的模型列表和 Chat Completions 响应。若提供方没有 `/models` 接口，仍可直接在模型输入框填写模型 ID。Anthropic、Gemini 等原生协议暂未直接适配，可使用兼容网关。

## 隐私与权限

Manifest 仅声明网络权限。通知读取由 Android 专用设置页授权，应用不申请通讯录、短信、相册、定位或“查询全部应用”权限。

- 浏览、筛选与本地归档完全离线
- 只有用户主动生成 AI 报告时，当天消息才会发送到用户配置的接口
- 密钥不会写入日志或上传到本项目服务器
- 为兼容可信局域网私有模型，测试版允许 HTTP 明文接口；公网接口应使用 HTTPS

## 本地构建

环境要求：Android Studio、JDK 17、Android SDK Platform 35。

```powershell
$env:JAVA_HOME='你的 JDK 17 路径'
./gradlew.bat assembleDebug
```

生成文件：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 技术栈

- Kotlin 2.1
- Jetpack Compose + Material 3
- Room + DataStore
- NotificationListenerService
- OkHttp + kotlinx.serialization
- Android Keystore

## 已知边界

- Android 各厂商后台策略不同，无法绝对保证进程永不被系统限制；通知监听服务由系统绑定，配合电池优化与自启动设置可降低漏录概率。
- 通知内容由发送方决定，部分应用可能只提供摘要、隐藏敏感内容或使用自定义布局。
- 超大量通知尚未加入分批摘要和 token 预算策略。
- 当前为 Debug 测试包，正式发布前还需发行签名、数据库迁移测试、OEM 机型测试、隐私政策和商店素材。

## 版本变化

### v0.2.1

- 优化消息、规则、历史和设置列表的可用布局范围，减少不必要的测量
- 移除消息卡片滚动时的常驻入场与尺寸弹簧动画，降低重组和绘制负担
- 缓存消息筛选和来源列表，并为 LazyColumn 项目补充内容类型以改善复用
- 新增启用 R8、资源收缩与 release 级优化的可安装性能测试包

### v0.2.0

- 新增提供方模型列表自动获取与下拉选择
- 重做页面水平滑动、淡入淡出和卡片展开动画
- AI 报告从 Markdown 改为 JSON 结构化重点卡片
- 保留旧报告兼容查看和手动模型输入能力

## License

本项目采用 MIT License。
