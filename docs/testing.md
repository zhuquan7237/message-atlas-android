# 测试与质量门禁

## 自动化测试

当前 JVM 单元测试覆盖 4 组核心纯逻辑，共 13 个测试用例：

- `ReportContentTest`：五类结构化报告字段、JSON 代码块、空摘要和旧版纯文本兼容。
- `ConvertersTest`：Room 枚举转换的完整往返。
- `DateRangeTest`：本地时区下日期边界和时间格式化。
- `UpdateClientTest`：主/次/补丁版本、缺失段、预发布后缀及降级判断。

运行测试：

```powershell
./gradlew.bat testDebugUnitTest
```

HTML 报告位于 `app/build/reports/tests/testDebugUnitTest/index.html`。

## 静态检查与构建

Android Lint 使用 `config/lint/lint.xml`，把新 API、缺失权限和不安全 opt-in 等运行时风险作为错误。CI 会验证依赖锁未漂移，然后依次运行单元测试、lint 和 Debug APK 构建。

```powershell
./gradlew.bat :app:dependencies --write-locks
git diff --exit-code -- app/gradle.lockfile
./gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

格式约定记录在根目录 `.editorconfig`。新增业务逻辑应优先提取为可在 JVM 上运行的纯函数并补充边界测试；涉及系统通知、Room 迁移或 OEM 后台策略的能力还需在 Android 真机上验证。

## 人工验收重点

1. 授予通知使用权后，从其他 App 发送测试通知，确认来源、时间、标题和正文进入当天列表。
2. 验证搜索、来源筛选、日期浏览、重点标记、删除与重启后数据保留。
3. 配置测试 AI 接口，验证五类报告、模型列表、自定义周期和紧急强提醒。
4. 在 Android 13+ 分别测试允许和拒绝通知发布权限，拒绝时应用不得崩溃。
