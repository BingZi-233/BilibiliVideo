# Manager包功能文档

## 包的主要用途和功能概述

`online.bingzi.bilibili.bilibilivideo.internal.manager`包是BilibiliVideo插件的核心管理层，主要负责管理和协调BV号（哔哩哔哩视频ID）相关的业务逻辑。该包提供了BV号的配置管理、奖励获取、状态检查等核心功能，是连接配置文件和业务逻辑的重要桥梁。

### 核心功能：
- **BV号管理**：管理配置文件中的BV号列表，提供查询和验证功能
- **配置整合**：整合特定视频配置和默认奖励配置
- **命令支持**：为插件命令提供BV号补全和验证支持
- **奖励管理**：协调奖励系统的配置获取和状态检查

## 重要的类和接口

### BvManager（单例对象）

`BvManager`是该包的核心类，采用Kotlin的`object`单例模式实现，负责所有BV号相关的管理功能。

**职责：**
- 从配置文件获取BV号列表
- 验证BV号格式和状态
- 获取奖励配置信息
- 提供命令补全支持

**依赖关系：**
- 依赖`SettingConfig`获取配置信息
- 与`VideoRewardConfig`和`DefaultRewardConfig`协作
- 被`RewardManager`调用获取奖励配置

## 主要方法和功能点

### 配置管理方法

#### `getAllBvids(): List<String>`
- **功能**：获取所有配置的BV号列表
- **用途**：命令补全、配置检查
- **异常处理**：捕获异常并返回空列表
- **返回值**：BV号字符串列表

#### `getEnabledBvids(): List<String>`
- **功能**：获取已启用奖励的BV号列表
- **逻辑**：过滤出enabled=true的视频配置
- **返回值**：启用奖励的BV号列表

#### `isConfigured(bvid: String): Boolean`
- **功能**：检查指定BV号是否已在配置文件中配置
- **参数**：bvid - 要检查的BV号
- **返回值**：true表示已配置，false表示未配置

### 奖励配置获取

#### `getRewardConfig(bvid: String): Pair<VideoRewardConfig?, DefaultRewardConfig?>`
- **功能**：获取BV号对应的奖励配置
- **逻辑**：优先返回特定视频配置，其次返回默认配置
- **返回值**：Pair对象，第一个元素为特定配置，第二个为默认配置

#### `isRewardEnabled(bvid: String): Boolean`
- **功能**：检查BV号是否启用奖励
- **逻辑**：先检查特定配置，再检查默认配置
- **返回值**：true表示启用奖励

#### `requiresCompleteTriple(bvid: String): Boolean`
- **功能**：检查是否需要完整三连（点赞+投币+收藏）
- **逻辑**：按配置优先级检查三连要求
- **返回值**：true表示需要完整三连

### 状态查询方法

#### `getVideoName(bvid: String): String`
- **功能**：获取视频显示名称
- **逻辑**：从配置获取name字段，未配置则返回BV号
- **返回值**：视频名称或BV号

#### `getRewardEnabledBvidCount(): Int`
- **功能**：统计启用奖励的BV号数量
- **用途**：管理面板显示、统计信息
- **返回值**：启用奖励的视频数量

### 验证和辅助方法

#### `isValidBvid(bvid: String): Boolean`
- **功能**：验证BV号格式是否正确
- **规则**：`BV` + 10位大小写字母和数字（不包含0、I、O、l）
- **正则表达式**：`^BV[1-9a-km-zA-HJ-NP-Z]{10}$`
- **返回值**：true表示格式正确

#### `addDynamicBvid(bvid: String, playerName: String)`
- **功能**：添加动态BV号到建议列表（预留接口）
- **用途**：记录玩家对未配置视频的三连行为
- **状态**：当前为预留实现，用于未来扩展

## 使用示例或说明

### 基础使用示例

```kotlin
// 检查BV号是否已配置
if (BvManager.isConfigured("BV1234567890")) {
    // 获取奖励配置
    val (videoConfig, defaultConfig) = BvManager.getRewardConfig("BV1234567890")

    // 检查是否启用奖励
    if (BvManager.isRewardEnabled("BV1234567890")) {
        println("视频已启用奖励")
    }
}

// 验证BV号格式
if (BvManager.isValidBvid(input)) {
    // 处理有效的BV号
    val videoName = BvManager.getVideoName(input)
    println("视频名称: $videoName")
}
```

### 命令补全集成

```kotlin
// 在命令处理中获取BV号列表用于补全
val availableBvids = BvManager.getAllBvids()
val enabledBvids = BvManager.getEnabledBvids()

// 提供给TabComplete使用
override fun onTabComplete(sender: CommandSender, args: Array<String>): List<String> {
    return when (args.size) {
        1 -> BvManager.getAllBvids().filter { it.startsWith(args[0], true) }
        else -> emptyList()
    }
}
```

### 奖励系统集成

```kotlin
// 在奖励管理器中使用BvManager
class RewardManager {
    fun processReward(player: Player, bvid: String) {
        // 检查奖励配置
        val (videoConfig, defaultConfig) = BvManager.getRewardConfig(bvid)

        // 检查三连要求
        if (BvManager.requiresCompleteTriple(bvid)) {
            // 需要完整三连
        } else {
            // 部分操作即可
        }

        // 获取视频名称用于消息显示
        val videoName = BvManager.getVideoName(bvid)
        player.sendMessage("获得 $videoName 的奖励!")
    }
}
```

### 配置文件结构示例

```yaml
# setting.yml
default-reward:
  enabled: true
  require-complete-triple: true
  rewards:
    - "give %player% diamond 1"
    - "money give %player% 100"

videos:
  BV1234567890:
    name: "精彩视频"
    enabled: true
    require-complete-triple: false
    rewards:
      - "give %player% emerald 1"
      - "money give %player% 200"
```

## 架构特点

### 设计模式
- **单例模式**：BvManager使用object单例，确保全局唯一实例
- **代理模式**：作为配置系统和业务逻辑之间的代理层
- **策略模式**：根据配置动态选择奖励策略

### 异常处理
- 所有配置读取操作都有异常保护
- 失败时返回安全的默认值
- 不会因配置错误导致插件崩溃

### 扩展性
- 预留了动态BV号管理接口
- 支持未来添加更多配置验证规则
- 易于集成新的奖励类型

这个Manager包是整个BilibiliVideo插件的核心组件之一，为插件的配置管理和奖励系统提供了稳定可靠的基础服务。