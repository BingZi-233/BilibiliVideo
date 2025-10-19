# Config包功能分析

## 主要用途和功能概述

`online.bingzi.bilibili.bilibilivideo.internal.config` 包是BilibiliVideo插件的配置管理中心，负责管理插件的所有配置文件和设置。该包提供了对数据库连接配置和奖励系统设置的统一管理，使用TabooLib框架的Configuration模块实现配置文件的自动加载和同步。

### 核心功能：
- **数据库配置管理**：支持MySQL和SQLite双数据库模式的动态切换
- **奖励系统配置**：管理默认奖励和特定视频奖励的配置
- **配置文件自动同步**：基于TabooLib Configuration注解实现配置的自动加载和更新

## 重要的类和接口

### 1. DatabaseConfig (数据库配置类)

**主要职责**：
- 管理数据库连接参数配置
- 提供MySQL和SQLite的双数据库支持
- 自动创建TabooLib Database模块所需的Host对象

**关键配置项**：
```kotlin
// 数据库类型开关
var enable: Boolean = false  // true=MySQL, false=SQLite

// MySQL配置
var mysqlHost: String = "localhost"
var mysqlPort: Int = 3306
var mysqlDatabase: String = "bilibili_video"
var mysqlUsername: String = "root"
var mysqlPassword: String = ""

// SQLite配置
var sqliteFile: String = "data/database.db"

// 通用配置
var tablePrefix: String = "bv_"
```

### 2. SettingConfig (设置配置类)

**主要职责**：
- 管理setting.yml配置文件
- 提供默认奖励和特定BV号奖励配置的访问接口
- 管理奖励系统的各项设置参数

## 主要方法和功能点

### DatabaseConfig 核心方法

#### 1. `createHost(): Host<*>`
```kotlin
fun createHost(): Host<*>
```
- **功能**：根据enable配置项创建对应的数据库Host对象
- **返回值**：HostSQL（MySQL）或HostSQLite（SQLite）实例
- **特性**：自动配置MySQL连接参数，包括字符编码、SSL和自动重连

#### 2. `createDataSource(): DataSource`
```kotlin
fun createDataSource(autoRelease: Boolean = true, withoutConfig: Boolean = false): DataSource
```
- **功能**：创建标准的DataSource数据源对象
- **参数**：支持自动释放连接和配置忽略选项

#### 3. `getTableName(tableName: String): String`
```kotlin
fun getTableName(tableName: String): String
```
- **功能**：为表名添加配置的前缀
- **用途**：确保数据表名称的统一性和唯一性

### SettingConfig 核心方法

#### 1. `getDefaultRewardConfig(): DefaultRewardConfig`
```kotlin
fun getDefaultRewardConfig(): DefaultRewardConfig
```
- **功能**：获取默认奖励配置
- **返回**：包含启用状态、三连要求和奖励列表的配置对象

#### 2. `getVideoRewardConfig(bvid: String): VideoRewardConfig?`
```kotlin
fun getVideoRewardConfig(bvid: String): VideoRewardConfig?
```
- **功能**：获取特定BV号的奖励配置
- **参数**：BV号字符串
- **返回**：视频专属奖励配置，未配置时返回null

#### 3. `getRewardSettings(): RewardSettings`
```kotlin
fun getRewardSettings(): RewardSettings
```
- **功能**：获取奖励系统设置
- **包含**：重复奖励防护、延迟设置、音效配置等

#### 4. `getRewardConfigForBvid(bvid: String): Pair<VideoRewardConfig?, DefaultRewardConfig?>`
```kotlin
fun getRewardConfigForBvid(bvid: String): Pair<VideoRewardConfig?, DefaultRewardConfig?>
```
- **功能**：智能获取BV号对应的奖励配置
- **逻辑**：优先返回特定视频配置，否则返回默认配置

## 使用示例或说明

### 数据库配置使用示例

```kotlin
// 创建数据库连接
val dataSource = DatabaseConfig.createDataSource()

// 获取带前缀的表名
val tableName = DatabaseConfig.getTableName("user_records") // 结果: "bv_user_records"

// 检查当前使用的数据库类型
val isUsingMySQL = DatabaseConfig.enable
```

### 奖励配置使用示例

```kotlin
// 获取特定BV号的奖励配置
val (videoConfig, defaultConfig) = SettingConfig.getRewardConfigForBvid("BV1234567890")

if (videoConfig != null) {
    // 使用视频专属配置
    println("视频名称: ${videoConfig.name}")
    println("奖励列表: ${videoConfig.rewards}")
} else if (defaultConfig != null) {
    // 使用默认配置
    println("使用默认奖励配置")
    println("奖励列表: ${defaultConfig.rewards}")
}

// 获取奖励系统设置
val settings = SettingConfig.getRewardSettings()
if (settings.playSound) {
    // 播放奖励音效
    val sound = settings.getSoundType()
}

// 检查BV号是否已配置
val isConfigured = SettingConfig.isVideoConfigured("BV1234567890")
```

### 配置文件结构示例

**database.yml**:
```yaml
database:
  enable: false  # true使用MySQL，false使用SQLite
  mysql:
    host: localhost
    port: 3306
    database: bilibili_video
    username: root
    password: ""
    use-ssl: false
    charset: utf8mb4
  sqlite:
    file: "data/database.db"
  table:
    prefix: "bv_"
  advanced:
    auto-reconnect: true
```

**setting.yml**:
```yaml
default-reward:
  enabled: true
  require-complete-triple: true
  rewards:
    - "money give %player% 100"
    - "exp give %player% 50"

videos:
  BV1234567890:
    name: "精彩视频示例"
    enabled: true
    require-complete-triple: true
    rewards:
      - "money give %player% 500"
      - "give %player% diamond 1"

settings:
  prevent-duplicate-rewards: true
  reward-delay: 1000
  play-sound: true
  sound-type: "ENTITY_PLAYER_LEVELUP"
  sound-volume: 1.0
  sound-pitch: 1.0
```

## 技术特色

1. **TabooLib集成**：充分利用TabooLib框架的Configuration模块，实现配置的自动管理和同步
2. **双数据库支持**：灵活支持MySQL生产环境和SQLite开发环境
3. **类型安全**：使用Kotlin数据类确保配置数据的类型安全
4. **智能配置选择**：自动在特定配置和默认配置间进行选择
5. **扩展性设计**：配置结构支持未来功能的扩展和定制

该包为整个插件提供了稳定、灵活的配置管理基础，确保了数据库连接的可靠性和奖励系统的可配置性。