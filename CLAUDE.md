# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个Minecraft Bukkit插件，用于实现Bilibili视频一键三连奖励系统。插件基于TabooLib框架开发，使用Kotlin语言编写。

## 构建和开发命令

### 核心构建命令
```bash
# 清理并构建项目（推荐）
./gradlew clean build

# 仅构建
./gradlew build

# 仅清理
./gradlew clean

# 运行测试
./gradlew test

# 生成开发环境jar包
./gradlew buildDev

# 查看所有可用任务
./gradlew tasks
```

### TabooLib特定任务
```bash
# 执行TabooLib主任务
./gradlew taboolibMainTask

# 刷新TabooLib依赖
./gradlew taboolibRefreshDependencies
```

### 构建产物位置
- 插件JAR包：`build/libs/BilibiliVideo-{version}.jar`

## 代码架构

### 核心技术栈
- **语言**: Kotlin 1.9.23
- **框架**: TabooLib 6.2.3
- **构建工具**: Gradle (Kotlin DSL)
- **目标平台**: Bukkit/Spigot/Paper (Minecraft 1.8+)
- **Java版本**: 1.8 (source/target compatibility)

### 项目结构

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

#### 命名规范
- 所有内部实现都在`internal`包下
- 使用object单例模式管理主要组件
- 配置文件使用小驼峰命名（如`lang/zh_CN.yml`中的key）
- 类文件使用大驼峰命名
- 每个文件只包含一个类

#### TabooLib框架约定
- 命令使用`@CommandHeader`注解定义
- 子命令使用`@CommandBody`注解
- 事件系统基于TabooLib的事件机制
- 配置管理使用TabooLib的Config模块
- 数据库操作使用TabooLib的Database模块

#### 代码组织原则
- 采用事件驱动架构处理业务逻辑
- 网络请求统一通过`NetworkEngine`处理
- 缓存使用Caffeine框架管理
- 支持PlaceholderAPI变量扩展
- 多语言支持通过lang文件实现

### 版本管理

- 版本号定义：`gradle.properties`中的`version`属性
- 当前版本：`1.6.8-beta1`
- 版本格式：`major.minor.patch[-suffix]`

## 开发流程

### 添加新功能流程
1. 在`internal`包下创建对应的功能模块
2. 如需对外暴露事件，在`api/event`包下创建事件类
3. 更新`lang/zh_CN.yml`添加相关语言键
4. 如需新命令，在`MainCommand.kt`中添加子命令
5. 运行`./gradlew clean build`确保编译通过

### 调试技巧
- 使用`DebugHelper`进行调试输出
- 检查`logs/latest.log`查看详细日志
- TabooLib错误通常会在控制台显示详细堆栈信息

### 网络请求开发
- 所有B站API调用通过`drive`包下的接口定义
- 使用Retrofit2进行HTTP请求
- Cookie管理通过`CookieCache`和拦截器实现
- 请求结果统一封装为`BilibiliResult<T>`

### 数据持久化
- 支持MySQL和SQLite两种数据库
- 数据库配置在`database.yml`
- 玩家数据通过`Database`抽象层访问
- 使用TabooLib的数据容器API

### 项目链接

- **GitHub仓库**: https://github.com/BingZi-233/BilibiliVideo
- **Wiki文档**: https://wiki.ooci.co/zh/BilibiliVideo/Index
- **CI/CD构建**: https://ci-dev.bingzi.online/job/BilibiliVideo

### 命令和权限

#### 主要命令
- `bilibilivideo login [player]` - 绑定B站账户
- `bilibilivideo unbind <player>` - 解绑账户（OP）
- `bilibilivideo receive <bv> [show|auto]` - 领取奖励
- `bilibilivideo video <bv>` - 生成视频二维码
- `bilibilivideo show` - 查看绑定信息
- `bilibilivideo logout` - 清理Cookie
- `bilibilivideo reload` - 重载配置（OP）
- `bilibilivideo version` - 查看版本（OP）

#### 权限节点
- `BilibiliVideo.command.use` - 基础命令权限
- `BilibiliVideo.command.{subcommand}` - 各子命令权限
- 详细权限配置参见README.md

### PlaceholderAPI变量
- `%BilibiliVideo_uid%` - 用户UID
- `%BilibiliVideo_uname%` - 用户名称  
- `%BilibiliVideo_check_[bv]%` - 检查视频奖励状态