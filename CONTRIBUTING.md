# 贡献指南

感谢参与消息图谱开发。提交应保持功能真实、改动聚焦且可复现。

## 开发环境

- JDK 17
- Android SDK Platform 35
- Android Studio 当前稳定版或项目自带 Gradle Wrapper

## 提交前检查

```powershell
./gradlew.bat testDebugUnitTest lintDebug assembleDebug
git status --short
```

依赖变更后还需更新并提交 `app/gradle.lockfile`：

```powershell
./gradlew.bat :app:dependencies --write-locks
```

## 改动要求

- 修复或新增可独立验证的逻辑时补充单元测试。
- 修改 Room 实体时提升数据库版本、提供 Migration，并检查 `app/schemas/` 的差异。
- 不提交 `local.properties`、真实 `.env`、API 密钥、签名文件、构建产物或个人会话记录。
- 涉及通知监听、后台调度和强提醒的改动需要在真机上验证权限拒绝、重启和息屏场景。
- 提交信息说明实际改动，不通过空实现、无意义文件或拆分提交制造质量指标。
