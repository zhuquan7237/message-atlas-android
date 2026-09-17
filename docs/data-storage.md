# 数据存储设计

项目使用 Room 作为结构化业务数据的唯一数据库入口，数据库文件名为 `message_atlas.db`，当前 schema 版本为 1。KSP 在构建时把 Room schema 导出到 `app/schemas/`，便于代码审查和后续迁移测试。

## Room 表

| 表 | 主键 | 用途 | 关键索引或约束 |
| --- | --- | --- | --- |
| `messages` | 自增 `id` | 归档通知正文、来源、发布时间、原始状态和重点标记 | `postedAt`、`packageName` 索引；`notificationKey` 唯一 |
| `app_rules` | `packageName` | 保存单个应用的允许/阻止规则 | 包名唯一 |
| `daily_reports` | `dateKey` | 保存某日结构化 AI 整理结果、模型和收藏状态 | 每日一份，可覆盖更新 |

`MessageDao` 提供按日实时观察、按时间增量读取、重点切换和删除；`RuleDao` 管理应用规则；`ReportDao` 管理日报存档。UI 和后台服务不直接拼接 SQL，而是统一通过 `MessageRepository` 调用 DAO。

## 设置与密钥

非关系型偏好由 Preferences DataStore 保存，包括规则模式、AI 地址、模型、提示词、巡检开关/周期以及提醒选项。API 密钥不会明文进入 Room 或 DataStore：`SettingsStore` 使用 Android Keystore 的 AES-GCM 密钥完成加解密，只持久化密文。

## 生命周期与隐私

- 浏览、搜索、筛选和归档不依赖网络。
- 卸载应用或清除应用数据会删除 Room、DataStore 和本机密钥。
- 当前 schema 为初始版本；以后变更实体时必须增加数据库版本、提供显式 Migration，并用导出的 schema 验证迁移。
