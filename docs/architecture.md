# 架构与消息处理链路

消息图谱采用单 Activity + Jetpack Compose 的本地优先架构。系统入口、业务编排、持久化和外部接口各自有明确边界。

## 主要组件

| 层级 | 组件 | 职责 |
| --- | --- | --- |
| 系统入口 | `MessageNotificationListener` | 接收 Android 系统通知，读取来源、标题、正文和时间，按应用规则决定是否收录 |
| 后台巡检 | `SmartInspectionReceiver`、`SmartInspector` | 按用户设置的周期调度，将新增通知送交 AI 判断紧急程度 |
| 提醒 | `NotificationHelper` | 对人物对话即时创建高优先级 MessagingStyle 驻留通知；AI 紧急结果创建高优先级通知，支持横幅、震动和提示音设置 |
| 状态编排 | `MainViewModel` | 聚合设置、消息、规则和报告状态，向 Compose UI 暴露统一状态 |
| 数据访问 | `MessageRepository` | 封装 Room DAO、日期范围、规则判断、日报生成和模型查询 |
| 本地存储 | `AppDatabase`、`SettingsStore` | Room 保存消息/规则/报告；DataStore 保存偏好；Keystore 保护 API 密钥 |
| 外部接口 | `AiClient`、`UpdateClient` | 调用用户配置的 OpenAI 兼容接口与版本更新接口 |

## 通知采集链路

1. Android 将新通知交给 `NotificationListenerService`。
2. `MessageNotificationListener` 读取通知字段并通过 `MessageRepository.isAllowed` 应用全部、白名单或黑名单规则。
3. 允许的通知写入 Room 的 `messages` 表；`notificationKey` 唯一索引避免重复归档。
4. UI 订阅按日期查询的 `Flow`，支持本地浏览、来源筛选、关键词搜索、重点标记和删除。

## AI 分类与强提醒链路

1. 用户在设置页配置 Base URL、API 密钥、模型、提示词和 1–720 分钟巡检周期。
2. `SmartInspector` 只读取上次巡检后的新增消息，并调用 `AiClient.evaluateUrgent`。
3. 日报整理要求模型返回结构化 JSON，映射为立即关注、待办、重要、一般、次要五类；每项包含来源、时间、标题、细节和建议行动。
4. 紧急结果会被标记为重点，并由 `NotificationHelper` 发出高优先级提醒。Android 13 及以上未授予通知权限时安全跳过发送。

## 人物对话即时提醒

监听服务在本地归档成功后，用通知类别、MessagingStyle 和人物元数据做一次轻量判断。命中人物对话时立即发出驻留在通知栏的高重要性消息通知，不联网、不启动常驻前台服务，也不额外轮询。声音、震动和静默使用独立通知渠道组合，用户点击后清除驻留提醒。

该提醒仍受 Android 系统通知权限、锁屏通知、勿扰模式、省电策略和厂商后台管理影响。首次使用或修改提醒行为后，应在系统的“通知 > 人物对话即时提醒”中确认允许声音、震动、锁屏显示和弹出通知。

## 安全边界

- 通知、规则和日报默认只保存在本机 Room 数据库。
- API 密钥由 Android Keystore 派生的 AES-GCM 密钥加密后再写入设置存储。
- 只有用户主动生成日报或开启 AI 巡检时，相关通知文本才会发往用户配置的接口。
- Manifest 不申请通讯录、短信、相册、麦克风或定位权限。
