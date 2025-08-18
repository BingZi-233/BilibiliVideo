# QQ绑定模块重构 - 技术规范

## 问题声明

### 业务问题
现有的QQ绑定系统采用独立的`/qqbind`命令且缺乏内存缓存层，导致：
1. **性能问题**：每次查询QQ绑定都需要数据库查询，频繁IO操作影响响应速度
2. **命令分散**：QQ绑定命令与主插件命令分离，用户体验不统一
3. **绑定流程复杂**：玩家需要手动输入QQ号，无自动化绑定方案

### 当前状态
- **独立命令系统**：`QQBindCommand`使用单独的`/qqbind`命令
- **直接数据库访问**：`PlayerQQBindingService`和`QQBindingDaoService`每次操作都访问数据库
- **手动绑定流程**：玩家必须通过`/qqbind bind <QQ号>`手动输入QQ号码完成绑定
- **无缓存机制**：频繁的数据库查询操作，特别是在二维码发送等场景中

### 期望结果
1. **高性能缓存系统**：玩家-QQ绑定关系查询毫秒级响应
2. **统一命令入口**：所有绑定操作整合到`/bilibilivideo`主命令下
3. **自动化绑定流程**：玩家无需手动输入QQ号，通过自动验证方式完成绑定

## 解决方案概览

### 方法
基于TabooLib的内存缓存系统和命令系统，对现有QQ绑定模块进行全面重构：
1. 实现基于ConcurrentHashMap的双向内存缓存系统（UUID→QQ号 & QQ号→UUID）
2. 将QQ绑定命令集成到主命令树结构中
3. 设计基于QQ机器人的自动绑定验证流程

### 核心变更
1. **缓存层重构**：新增`QQBindingCacheService`提供内存缓存管理
2. **命令系统整合**：废弃`QQBindCommand`，集成到新的`BilibiliVideoCommand`主命令
3. **绑定流程优化**：新增验证码自动绑定机制，减少手动输入
4. **服务层优化**：`PlayerQQBindingService`优先使用缓存，降级到数据库查询

### 成功标准
1. **性能提升**：QQ绑定查询响应时间从平均50ms降至5ms以内
2. **用户体验**：统一使用`/bilibilivideo qqbind`命令访问所有绑定功能
3. **绑定便捷性**：支持通过QQ机器人发送验证码完成自动绑定，无需手动输入QQ号

## 技术实现

### 数据库变更
无需修改现有数据表结构，`bv_qq_bindings`表保持不变：
```sql
-- 现有表结构保持不变
-- bv_qq_bindings (id, player_uuid, qq_number, qq_nickname, bind_time, last_verify_time, bind_status, is_active)
```

### 代码变更

#### 新增文件
```
src/main/kotlin/online/bingzi/bilibili/video/internal/cache/
├── QQBindingCacheService.kt                    # QQ绑定缓存服务
└── CacheConfig.kt                              # 缓存配置类

src/main/kotlin/online/bingzi/bilibili/video/internal/commands/
├── BilibiliVideoCommand.kt                     # 主命令入口
├── subcommands/
│   ├── QQBindSubCommand.kt                     # QQ绑定子命令
│   ├── BilibiliBindSubCommand.kt               # Bilibili绑定子命令
│   └── InfoSubCommand.kt                       # 信息查看子命令

src/main/kotlin/online/bingzi/bilibili/video/internal/binding/
├── AutoBindingService.kt                       # 自动绑定服务
├── VerificationCodeService.kt                  # 验证码服务
└── QQVerificationHandler.kt                    # QQ验证处理器
```

#### 修改文件
```
src/main/kotlin/online/bingzi/bilibili/video/internal/database/PlayerQQBindingService.kt
├── 添加缓存层集成
├── 修改查询方法优先使用缓存
└── 添加缓存更新逻辑

src/main/kotlin/online/bingzi/bilibili/video/internal/database/dao/QQBindingDaoService.kt
├── 添加缓存通知机制
├── 保存/删除操作后更新缓存
└── 添加批量加载方法

删除文件：
src/main/kotlin/online/bingzi/bilibili/video/internal/commands/QQBindCommand.kt
```

#### 核心函数签名
```kotlin
// QQBindingCacheService.kt
object QQBindingCacheService {
    fun getQQByPlayer(playerUuid: UUID): Long?
    fun getPlayerByQQ(qqNumber: Long): UUID?
    fun cacheBinding(playerUuid: UUID, qqNumber: Long)
    fun removeBinding(playerUuid: UUID)
    fun preloadCache(): CompletableFuture<Int>
    fun getCacheStats(): CacheStats
}

// BilibiliVideoCommand.kt
@CommandHeader(name = "bilibilivideo", aliases = ["bv", "bili"])
object BilibiliVideoCommand {
    val main: SimpleCommandBody
    val qqbind: SimpleCommandBody
    val bilibind: SimpleCommandBody
    val info: SimpleCommandBody
}

// AutoBindingService.kt
object AutoBindingService {
    fun startVerification(player: ProxyPlayer): CompletableFuture<String>
    fun completeBinding(verificationCode: String, qqNumber: Long): CompletableFuture<Boolean>
    fun cancelVerification(player: ProxyPlayer): Boolean
}
```

### API变更

#### 新增端点
无新增HTTP端点，仅内部API变更。

#### 内部API修改
```kotlin
// PlayerQQBindingService 方法增强缓存支持
fun getPlayerQQNumber(playerUuid: UUID): CompletableFuture<Long?> {
    // 1. 先查缓存
    // 2. 缓存未命中则查数据库
    // 3. 数据库结果写入缓存
}

// 新增缓存管理API
fun refreshCache(playerUuid: UUID): CompletableFuture<Boolean>
fun clearPlayerCache(playerUuid: UUID): Boolean
```

### 配置变更

#### 新增配置项
```yaml
# config.yml
binding:
  cache:
    enabled: true
    ttl: 3600  # 缓存TTL（秒）
    maxSize: 10000  # 最大缓存条目
    preloadOnStartup: true  # 启动时预加载
  verification:
    codeLength: 6  # 验证码长度
    expireMinutes: 5  # 验证码过期时间（分钟）
    maxAttempts: 3  # 最大尝试次数
```

#### 新增环境变量
```properties
# 可选：Redis缓存支持（未来扩展）
REDIS_CACHE_ENABLED=false
REDIS_HOST=localhost
REDIS_PORT=6379
```

## 实现顺序

### 阶段1：缓存系统基础架构
**任务**：
1. 创建`QQBindingCacheService`实现双向内存缓存
2. 实现缓存预加载机制，支持插件启动时从数据库加载现有绑定
3. 添加缓存统计和监控功能
4. 集成缓存到`PlayerQQBindingService`的查询方法

**具体文件操作**：
- 新建：`src/main/kotlin/online/bingzi/bilibili/video/internal/cache/QQBindingCacheService.kt`
- 新建：`src/main/kotlin/online/bingzi/bilibili/video/internal/cache/CacheConfig.kt`
- 修改：`src/main/kotlin/online/bingzi/bilibili/video/internal/database/PlayerQQBindingService.kt`
- 修改：`src/main/resources/config.yml`

### 阶段2：命令系统重构
**任务**：
1. 创建新的主命令`BilibiliVideoCommand`
2. 实现QQ绑定子命令，迁移原有功能到新命令树
3. 实现统一的信息查看子命令
4. 删除旧的独立命令类

**具体文件操作**：
- 新建：`src/main/kotlin/online/bingzi/bilibili/video/internal/commands/BilibiliVideoCommand.kt`
- 新建：`src/main/kotlin/online/bingzi/bilibili/video/internal/commands/subcommands/QQBindSubCommand.kt`
- 新建：`src/main/kotlin/online/bingzi/bilibili/video/internal/commands/subcommands/BilibiliBindSubCommand.kt`
- 新建：`src/main/kotlin/online/bingzi/bilibili/video/internal/commands/subcommands/InfoSubCommand.kt`
- 删除：`src/main/kotlin/online/bingzi/bilibili/video/internal/commands/QQBindCommand.kt`
- 删除：`src/main/kotlin/online/bingzi/bilibili/video/internal/commands/BilibiliBindCommand.kt`

### 阶段3：自动绑定系统实现
**任务**：
1. 实现验证码生成和管理服务
2. 创建基于OneBot的QQ验证处理器
3. 实现自动绑定服务，支持通过验证码完成绑定
4. 集成自动绑定到命令系统

**具体文件操作**：
- 新建：`src/main/kotlin/online/bingzi/bilibili/video/internal/binding/AutoBindingService.kt`
- 新建：`src/main/kotlin/online/bingzi/bilibili/video/internal/binding/VerificationCodeService.kt`
- 新建：`src/main/kotlin/online/bingzi/bilibili/video/internal/binding/QQVerificationHandler.kt`
- 修改：`src/main/kotlin/online/bingzi/bilibili/video/internal/commands/subcommands/QQBindSubCommand.kt`

## 验证计划

### 单元测试
**缓存系统测试**：
```kotlin
// QQBindingCacheServiceTest
- testCacheBinding_Success()
- testGetQQByPlayer_CacheHit()
- testGetQQByPlayer_CacheMiss()
- testRemoveBinding_Success()
- testPreloadCache_Success()
- testCacheStats_Accuracy()
```

**命令系统测试**：
```kotlin
// BilibiliVideoCommandTest  
- testMainCommand_ShowsHelp()
- testQQBindSubCommand_AllOperations()
- testCommandPermissions_Correct()
```

### 集成测试
**端到端绑定流程测试**：
1. 玩家执行`/bilibilivideo qqbind auto`启动自动绑定
2. 系统生成验证码并通过OneBot发送到玩家QQ
3. 玩家在QQ中回复验证码
4. 系统自动完成绑定并更新缓存
5. 验证绑定信息正确保存到数据库和缓存

**性能测试场景**：
1. 启动时预加载10000条绑定记录的时间测试
2. 并发1000次QQ绑定查询的响应时间测试
3. 缓存命中率在正常使用场景下的统计验证

### 业务逻辑验证
**绑定系统验证**：
- 验证缓存与数据库数据一致性
- 验证自动绑定流程的验证码安全性
- 验证命令权限和错误处理机制正确性
- 验证缓存在服务器重启后的数据恢复能力
- 验证旧命令完全废弃，新命令功能完整覆盖