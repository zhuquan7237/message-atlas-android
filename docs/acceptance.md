# 验收标准对应表

本页把冻结的三项项目验收标准映射到可核查的实现和人工验证步骤，不替代实际真机验收。

## 1. 自动收录、本地归档、浏览和搜索

- 系统通知入口：`service/MessageNotificationListener.kt`
- 本地持久化：`data/Entities.kt`、`Daos.kt`、`AppDatabase.kt`
- 数据访问与按日范围：`data/MessageRepository.kt`
- 浏览、搜索、来源筛选和日期切换：`ui/HomeScreen.kt`、`ui/AppUi.kt`、`data/MessageFilters.kt` 与 `MainViewModel.kt`

验收：开启系统通知使用权，从两个不同 App 产生通知；断网并重启应用后，仍能按日期浏览，并可按关键词和来源筛选。

## 2. 五类呈现和完整上下文

- 分类契约：`data/SettingsStore.kt` 中的默认结构化提示词
- 响应解析及兼容：`data/ReportContent.kt`
- 五类卡片和分享文本：`ui/ReportContentUi.kt` 与 `ui/HistoryScreen.kt`
- 自动化解析测试：`app/src/test/.../ReportContentTest.kt`

五类分别为立即关注、待办、重要、一般、次要/广告。结构化项目包含来源 App、时间、标题、必要细节和建议行动。

验收：准备覆盖五类的通知样本，生成日报，确认分类标题、来源、时间和建议行动均可见。

## 3. 可配置 AI 与紧急强提醒

- 地址、密钥、模型、提示词及巡检设置：`data/SettingsStore.kt` 与设置页
- 模型列表和 AI 请求：`network/AiClient.kt`
- 自定义巡检调度：`service/SmartInspector.kt`、`SmartInspectionReceiver.kt`
- 高优先级提醒：`service/NotificationHelper.kt`

验收：更换 Base URL、模型和巡检分钟数后保存；触发一条明确紧急的测试消息，确认应用标记重点并按用户开关产生高优先级横幅、震动或提示音。

人物对话即时提醒：发送一条带 CATEGORY_MESSAGE 或 MessagingStyle 元数据的聊天通知，确认无需等待 AI 巡检即可归档并产生高重要性驻留通知；点击通知后进入消息图谱并清除驻留提醒。分别测试响铃、仅震动和静默渠道，并在系统通知设置中确认锁屏显示与弹出权限。
