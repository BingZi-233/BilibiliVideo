# Project Context

## Purpose
本项目是一个基于 TabooLib 的 Minecraft（Bukkit/Spigot/Paper）插件，用于在游戏内与哔哩哔哩账号体系打通。目标是让玩家在游戏内完成账号绑定、视频三连状态检测与奖励领取，并保证所有网络请求异步执行以避免卡服。默认使用 SQLite，支持切换到 MySQL。

## Tech Stack
- Kotlin 2.2.0（JVM 1.8 目标）/ Java 8 兼容
- TabooLib 6.2.4（Bukkit 插件框架）
- Gradle（Kotlin DSL）
- Bukkit/Spigot/Paper API（建议 1.12+）
- Ktorm 3.6.0 ORM + HikariCP 4.0.3
- SQLite JDBC 3.45.1.0 / MySQL Connector/J 8.3.0
- OkHttp 4.12.0 + Okio 3.6.0
- Gson 2.11.0（JSON）
- ZXing 3.5.2（二维码）
- Kether（奖励脚本）

## Project Conventions

### Code Style
- 函数小且单一职责，避免超过 3 层缩进。
- 优先简单直白的数据流与控制流，拒绝不必要的抽象。
- 网络与 IO 必须异步执行，严禁阻塞主线程。
- 尽量使用 TabooLib 提供的 DSL/扩展减少样板代码。
- 包结构遵循分层：command / service / repository / http / ui / config / database。

### Architecture Patterns
- 分层架构：
  - 命令层：玩家/管理员交互入口
  - 服务层：业务逻辑（绑定、三连、奖励、凭证）
  - 仓库层：数据库访问（Ktorm）
  - HTTP 层：Bilibili API 封装
  - UI 层：二维码地图渲染
- 配置使用 YAML，数据库表前缀默认 `bv_`。

### Testing Strategy
- 以真实服务器手动验证为主。
- 纯逻辑与仓库层尽可能补充单元测试。
- 任何涉及异步/网络/数据库的改动必须在实际服务器验证。

### Git Workflow
- 使用功能分支（feature/xxx），通过 PR 合并。
- 提交遵循 Conventional Commits（feat/fix/docs/refactor 等）。
- 不得破坏现有命令、配置与数据；如需变更必须提供迁移路径。

## Domain Context
- 玩家与 B 站账号一一绑定，数据必须隔离。
- “三连”指点赞/投币/收藏的组合状态，用于奖励判定。
- 凭证包含 SESSDATA、bili_jct、buvid3、refresh_token 等生命周期信息。

## Important Constraints
- 主线程零阻塞：所有网络/数据库 IO 必须异步。
- 兼容 Java 8 与常见 Bukkit/Paper 版本。
- 绝不破坏已有玩家数据、奖励记录与配置格式。
- B 站 API 可能不稳定或有限流，需稳健处理失败场景。

## External Dependencies
- Bilibili 公共 API（登录/状态/三连）
- TabooLib 模块与 Bukkit API
- SQLite 或 MySQL 数据库
- Maven 发布仓库（AeolianCloud）
