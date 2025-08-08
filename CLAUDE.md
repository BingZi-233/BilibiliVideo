# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个Minecraft Bukkit插件，用于实现Bilibili视频一键三连奖励系统。插件基于TabooLib框架开发，使用Kotlin语言编写。

## 构建和开发命令

### 构建
```bash
./gradlew build
```

### 运行测试
```bash
./gradlew test
```

### 清理构建产物
```bash
./gradlew clean
```

### 生成开发环境jar包
```bash
./gradlew buildDev
```

### 查看所有可用任务
```bash
./gradlew tasks
```

### 主要TabooLib任务
- `./gradlew taboolibMainTask` - 执行TabooLib主任务
- `./gradlew taboolibRefreshDependencies` - 刷新TabooLib依赖

## 代码架构

### 核心模块结构

- **主入口**: `BilibiliVideo.kt` - 插件的主类，处理生命周期
- **API模块**: `api/event/` - 对外提供的事件API
- **内部实现**: `internal/` - 所有核心逻辑实现

### 内部模块组织

1. **缓存模块** (`internal/cache/`):
   - `BaffleCache` - 防刷验证缓存
   - `CookieCache` - Cookie信息缓存  
   - `QRCodeCache` - 二维码状态缓存
   - `BvCache`、`MidCache`、`Buvid3Cache` - 用户数据缓存

2. **数据库模块** (`internal/database/`):
   - `Database.kt` - 数据库抽象层
   - 支持MySQL和SQLite两种数据库类型
   - `DatabaseConfig.kt` - 数据库配置管理

3. **网络引擎** (`internal/engine/`):
   - `NetworkEngine.kt` - 网络请求核心
   - `drive/` - 各类API驱动实现
   - `BilibiliApiDrive` - B站API接口
   - `BilibiliPassportDrive` - 登录认证接口

4. **数据实体** (`internal/entity/`):
   - 所有与B站API交互的数据模型
   - `BilibiliResult`、`UserInfoData`、`QRCodeGenerateData`等

5. **业务处理器** (`internal/handler/`):
   - `CoinsHandler` - 投币处理
   - `LikeHandler` - 点赞处理 
   - `FavouredHandler` - 收藏处理
   - `FollowingHandler` - 关注处理

6. **辅助工具** (`internal/helper/`):
   - `MapHelper` - 地图生成工具
   - `ImageHelper` - 图片处理
   - `CookieRefreshHelper` - Cookie刷新
   - `OkHttpClient` - HTTP客户端配置

### 配置文件结构

- `setting.yml` - 主要设置（冷却时间、虚拟化选项等）
- `video.yml` - 视频奖励配置（支持Kether脚本）
- `database.yml` - 数据库连接配置
- `lang/zh_CN.yml` - 本地化文件

### 依赖管理

项目使用TabooLib作为核心框架，主要依赖包括：
- TabooLib 6.2.3 - 核心框架
- Retrofit2 2.9.0 - HTTP客户端
- Caffeine 2.9.3 - 缓存框架  
- ZXing 3.5.2 - 二维码生成
- Gson 2.10.1 - JSON处理

### 重要配置

- **Java版本**: 1.8 (source/target compatibility)
- **Kotlin版本**: 1.9.23
- **包重定位**: 所有第三方库都被重定位到 `online.bingzi.bilibili.video.libraries.*`
- **可选依赖**: PlaceholderAPI（用于变量扩展）

### 开发约定

- 所有内部实现都在`internal`包下
- 使用object单例模式管理主要组件
- 采用事件驱动架构处理业务逻辑
- 支持PlaceholderAPI变量扩展
- 多语言支持通过lang文件实现

### 版本管理

当前版本在`gradle.properties`中定义为`1.6.8-beta1`

### 项目链接

- **GitHub仓库**: https://github.com/BingZi-233/BilibiliVideo
- **Wiki文档**: https://wiki.ooci.co/zh/BilibiliVideo/Index
- **CI/CD构建**: https://ci-dev.bingzi.online/job/BilibiliVideo

### 命令和权限

主要命令包括：
- `bilibilivideo login` - 绑定账户
- `bilibilivideo receive [bv] [show|auto]` - 领取奖励
- `bilibilivideo video [bv]` - 生成视频二维码

详细命令和权限列表参见README.md