# Reward 奖励系统包

## 包的主要用途和功能概述

`online.bingzi.bilibili.bilibilivideo.internal.reward` 包是BilibiliVideo插件的核心奖励系统模块，负责处理基于B站视频三连操作（点赞、投币、收藏）的奖励发放机制。该系统支持配置化的奖励管理、防重复领奖机制、Kether脚本奖励执行，以及完整的数据库记录功能。

### 主要功能特性：

- **自动奖励发放**：监听视频三连状态检查事件，自动发放配置的奖励
- **灵活配置系统**：支持特定视频奖励和默认奖励配置
- **防重复机制**：阻止玩家对同一视频重复领取奖励
- **脚本化奖励**：使用Kether脚本系统执行复杂奖励逻辑
- **音效系统**：支持奖励发放时的音效播放
- **数据库记录**：完整记录奖励发放历史和三连状态
- **管理员工具**：提供手动奖励发放功能

## 重要的类和接口

### 1. RewardManager（奖励管理器）

```kotlin
object RewardManager
```

**功能**：奖励系统的核心管理类，负责处理所有奖励发放逻辑

**主要职责**：
- 监听视频三连状态检查事件
- 验证奖励条件和防重复检查
- 执行Kether脚本奖励
- 播放奖励音效
- 记录奖励发放历史

### 2. 数据配置类

#### VideoRewardConfig（视频奖励配置）

```kotlin
data class VideoRewardConfig(
    val name: String = "未命名视频",
    val enabled: Boolean = true,
    val requireCompleteTriple: Boolean = true,
    val rewards: List<String> = emptyList()
)
```

**用途**：定义特定视频的奖励配置

#### DefaultRewardConfig（默认奖励配置）

```kotlin
data class DefaultRewardConfig(
    val enabled: Boolean = true,
    val requireCompleteTriple: Boolean = true,
    val rewards: List<String> = emptyList()
)
```

**用途**：定义未特殊配置视频的默认奖励

#### RewardSettings（奖励系统设置）

```kotlin
data class RewardSettings(
    val preventDuplicateRewards: Boolean = true,
    val rewardDelay: Long = 1000L,
    val playSound: Boolean = true,
    val soundType: String = "ENTITY_PLAYER_LEVELUP",
    val soundVolume: Float = 1.0f,
    val soundPitch: Float = 1.0f
)
```

**用途**：系统级别的奖励设置和音效配置

### 3. VideoRewardRecord（奖励记录实体）

```kotlin
data class VideoRewardRecord(
    val id: Long = 0L,
    val bvid: String,
    val mid: Long,
    val playerUuid: String,
    val rewardType: String,
    val rewardData: String? = null,
    val isLiked: Boolean,
    val isCoined: Boolean,
    val isFavorited: Boolean,
    val createTime: Long = System.currentTimeMillis(),
    val updateTime: Long = System.currentTimeMillis(),
    val createPlayer: String,
    val updatePlayer: String
)
```

**用途**：数据库实体类，记录奖励发放历史和当时的三连状态

## 主要方法和功能点

### RewardManager 核心方法

#### 1. 事件监听方法

```kotlin
@SubscribeEvent
fun onVideoTripleStatusCheck(event: VideoTripleStatusCheckEvent)
```

**功能**：监听视频三连状态检查事件，触发奖励处理流程

**处理流程**：
- 获取奖励延迟设置
- 异步处理奖励发放，避免与API请求冲突

#### 2. 奖励处理方法

```kotlin
private fun processReward(player: Player, tripleData: VideoTripleData)
```

**功能**：处理奖励发放的核心逻辑

**处理步骤**：
1. 检查防重复领奖设置
2. 获取视频特定或默认奖励配置
3. 验证三连操作要求（完整三连或部分操作）
4. 执行Kether脚本奖励
5. 播放奖励音效
6. 记录奖励发放历史

#### 3. 手动奖励方法

```kotlin
fun giveRewardManually(player: Player, bvid: String, forceGive: Boolean = false)
```

**功能**：管理员手动给予玩家奖励

**参数**：
- `player`：目标玩家
- `bvid`：视频BV号
- `forceGive`：是否忽略重复检查

### RewardSettings 音效处理

```kotlin
fun getSoundType(): Sound?
```

**功能**：安全地获取音效类型，处理无效音效名称

### VideoRewardRecord 状态检查方法

#### 1. 三连状态检查

```kotlin
fun hasCompleteTriple(): Boolean
fun hasAnyTripleAction(): Boolean
fun getTripleStatusDescription(): String
```

**功能**：检查和描述奖励记录中的三连操作状态

## 使用示例或说明

### 1. 基本配置示例

```yaml
# 视频特定奖励配置
video_rewards:
  "BV1234567890":
    name: "特殊活动视频"
    enabled: true
    requireCompleteTriple: true
    rewards:
      - "give @s diamond 5"
      - "tellraw @s \"恭喜获得钻石奖励！\""

# 默认奖励配置
default_reward:
  enabled: true
  requireCompleteTriple: false
  rewards:
    - "give @s emerald 1"
    - "tellraw @s \"感谢三连支持！\""

# 系统设置
reward_settings:
  preventDuplicateRewards: true
  rewardDelay: 1000
  playSound: true
  soundType: "ENTITY_PLAYER_LEVELUP"
  soundVolume: 1.0
  soundPitch: 1.0
```

### 2. Kether脚本奖励示例

```yaml
rewards:
  - "give @s diamond *random(1,5)"  # 随机给予1-5个钻石
  - "money give @s 100"             # 给予100金币（需要经济插件）
  - "tellraw @s \"三连奖励已发放\""   # 发送消息
  - "effect give @s speed 30 1"     # 给予速度效果
```

### 3. 管理员手动奖励

```kotlin
// 手动给予奖励
RewardManager.giveRewardManually(player, "BV1234567890", false)

// 强制给予奖励（忽略重复检查）
RewardManager.giveRewardManually(player, "BV1234567890", true)
```

### 4. 奖励条件逻辑

**完整三连要求**：
- `requireCompleteTriple: true` - 需要点赞 + 投币 + 收藏
- `requireCompleteTriple: false` - 任意一项操作即可

**防重复机制**：
- 系统自动检查数据库记录
- 防止玩家对同一视频多次领奖
- 管理员可强制发放奖励

### 5. 事件流程说明

1. **玩家执行三连操作**：点赞/投币/收藏B站视频
2. **系统检查三连状态**：通过API验证操作状态
3. **触发奖励检查事件**：`VideoTripleStatusCheckEvent`
4. **奖励系统处理**：
   - 检查防重复设置
   - 获取奖励配置
   - 验证操作要求
   - 执行Kether脚本
   - 播放音效
   - 记录数据库
5. **通知玩家**：发送奖励获得消息

## 技术特点

- **异步处理**：奖励发放使用异步处理，避免阻塞主线程
- **配置驱动**：通过配置文件灵活控制奖励规则
- **脚本化执行**：使用Kether脚本系统支持复杂奖励逻辑
- **数据完整性**：完整记录奖励历史和三连状态快照
- **错误处理**：完善的异常处理和降级策略
- **扩展性强**：支持特定视频和默认奖励的多层配置

该奖励系统为BilibiliVideo插件提供了完整的激励机制，鼓励玩家在B站进行三连操作，同时保证了系统的稳定性和公平性。