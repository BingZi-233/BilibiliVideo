# Repository Guidelines

## 项目结构与模块组织
- Kotlin/Gradle 项目（Java 8，TabooLib）。
- 入口：`src/main/kotlin/online/bingzi/bilibili/bilibilivideo/BilibiliVideo.kt`。
- 公共 API/事件：`src/main/kotlin/.../api/**`（如二维码登录与事件分发）。
- 内部实现：`.../internal/**`（命令、B 站 API、数据库、会话、奖励）。
- 资源：`src/main/resources`（`setting.yml`、`database.yml`、`lang/*`）。
- 文档：`docs/*`（模块说明与 API 收集）。

## 构建、测试与开发命令
- `./gradlew build`：编译与检查，产物位于 `build/libs/`。
- `./gradlew taboolibBuild`：构建可部署插件（合并依赖，遵循 relocate 规则）。
- `./gradlew taboolibBuildApi`：构建 API JAR（发布所需）。
- `./gradlew publish`：发布到 AeolianCloud Maven（需 `MAVEN_USERNAME/MAVEN_PASSWORD` 或 `gradle.properties` 的 `mavenUsername/mavenPassword`）。

## 代码风格与命名
- Kotlin 1.8，缩进 4 空格，行宽 ≤ 120。
- 包名小写；类用 `PascalCase`；方法/变量用 `camelCase`；常量用 `UPPER_SNAKE_CASE`。
- 公共 API 必须包含 KDoc；显式可空性与返回类型。
- 依赖重定位：保持 `com.google.gson` 与 `okhttp3` 的 relocate 设置；新增库需评估冲突并同步配置。

## 测试指南
- 当前未包含测试目录；建议新增 `src/test/kotlin`，使用 JUnit 5 + MockK。
- 测试文件以 `*Test.kt` 结尾，与被测类同包组织。
- 运行：`./gradlew test`（添加测试依赖后）；逻辑模块覆盖率目标 ≥ 70%。

## 提交与 PR 规范
- 提交信息：动词开头中文简述（示例：`修复…`、`重构…`、`新增…`），必要时注明影响范围。
- PR 要求：变更摘要、动机与影响面、关联 Issue、风险/回滚方案；命令或文案改动请附运行截图或日志。
- 同步更新：相关 `docs/*` 与 `src/main/resources/lang/*` 文档与多语言。

## 安全与配置
- 切勿提交密钥/令牌/Cookie；使用环境变量或本地 `gradle.properties`。
- 修改默认配置（`setting.yml`、`database.yml`）需在 PR 描述中给出迁移步骤与兼容性说明。

## 语言设置
- 使用中文简体进行回复

