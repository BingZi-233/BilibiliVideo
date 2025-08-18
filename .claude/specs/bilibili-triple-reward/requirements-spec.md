# Bilibili三连奖励系统技术实现规格

## 问题陈述

- **业务问题**: 需要为玩家对指定UP主新视频进行三连操作（点赞、投币、收藏）后提供奖励机制，增加玩家互动积极性
- **当前状态**: 现有系统具备完整的Bilibili API集成和UP主监控功能，但缺少奖励分发机制
- **期望结果**: 玩家绑定B站账号后，对监控UP主7天内新视频进行三连操作，可获得可配置的Kether脚本奖励，每视频限领一次，每日最多3次

## 解决方案概述

- **核心方法**: 在现有UP主监控和三连检测基础上，添加奖励记录、检测和分发系统
- **主要变更**: 新增奖励数据模型、奖励服务层、配置管理、命令系统和事件集成
- **成功标准**: 玩家可以通过命令查询和领取奖励，管理员可以配置UP主列表和奖励脚本，系统自动处理限制条件

## 技术实现

### 数据库变更

#### 新增数据表

**1. 视频奖励记录表 (video_reward_records)**
```sql
CREATE TABLE video_reward_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_uuid VARCHAR(36) NOT NULL,
    uploader_uid BIGINT NOT NULL,
    bv_id VARCHAR(20) NOT NULL,
    video_title VARCHAR(500) NOT NULL,
    reward_type VARCHAR(50) NOT NULL DEFAULT 'TRIPLE_ACTION',
    reward_claimed_at BIGINT NOT NULL,
    reward_content TEXT,
    created_at BIGINT NOT NULL,
    UNIQUE KEY uk_player_video (player_uuid, bv_id),
    INDEX idx_player_date (player_uuid, reward_claimed_at),
    INDEX idx_uploader (uploader_uid),
    INDEX idx_video (bv_id)
);
```

**2. 奖励配置表 (reward_configs)**
```sql
CREATE TABLE reward_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uploader_uid BIGINT NOT NULL,
    uploader_name VARCHAR(200) NOT NULL,
    reward_script TEXT NOT NULL,
    is_enabled BOOLEAN DEFAULT TRUE,
    min_video_age_days INTEGER DEFAULT 0,
    max_video_age_days INTEGER DEFAULT 7,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    UNIQUE KEY uk_uploader (uploader_uid),
    INDEX idx_enabled (is_enabled)
);
```

**3. 玩家奖励统计表 (player_reward_stats)**
```sql
CREATE TABLE player_reward_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_uuid VARCHAR(36) NOT NULL,
    reward_date VARCHAR(10) NOT NULL,
    daily_reward_count INTEGER DEFAULT 0,
    total_reward_count BIGINT DEFAULT 0,
    last_reward_time BIGINT DEFAULT 0,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    UNIQUE KEY uk_player_date (player_uuid, reward_date),
    INDEX idx_player (player_uuid)
);
```

### 代码变更

#### 新增文件

**1. 数据实体类**
- `/src/main/kotlin/online/bingzi/bilibili/video/internal/database/entity/VideoRewardRecord.kt` - 视频奖励记录实体
- `/src/main/kotlin/online/bingzi/bilibili/video/internal/database/entity/RewardConfig.kt` - 奖励配置实体  
- `/src/main/kotlin/online/bingzi/bilibili/video/internal/database/entity/PlayerRewardStats.kt` - 玩家奖励统计实体

**2. 数据访问层**
- `/src/main/kotlin/online/bingzi/bilibili/video/internal/database/dao/VideoRewardRecordDaoService.kt` - 视频奖励记录DAO
- `/src/main/kotlin/online/bingzi/bilibili/video/internal/database/dao/RewardConfigDaoService.kt` - 奖励配置DAO
- `/src/main/kotlin/online/bingzi/bilibili/video/internal/database/dao/PlayerRewardStatsDaoService.kt` - 玩家奖励统计DAO

**3. 业务服务层**
- `/src/main/kotlin/online/bingzi/bilibili/video/internal/rewards/TripleRewardService.kt` - 三连奖励核心服务
- `/src/main/kotlin/online/bingzi/bilibili/video/internal/rewards/RewardExecutor.kt` - 奖励执行器
- `/src/main/kotlin/online/bingzi/bilibili/video/internal/rewards/RewardChecker.kt` - 奖励检查器

**4. 配置管理**
- `/src/main/kotlin/online/bingzi/bilibili/video/internal/config/RewardConfig.kt` - 奖励配置管理器

**5. 命令系统**
- `/src/main/kotlin/online/bingzi/bilibili/video/internal/commands/RewardCommand.kt` - 奖励命令主类
- `/src/main/kotlin/online/bingzi/bilibili/video/internal/commands/subcommands/RewardClaimSubCommand.kt` - 领取奖励子命令
- `/src/main/kotlin/online/bingzi/bilibili/video/internal/commands/subcommands/RewardListSubCommand.kt` - 奖励列表子命令
- `/src/main/kotlin/online/bingzi/bilibili/video/internal/commands/subcommands/RewardAdminSubCommand.kt` - 管理员命令

**6. 事件定义**
- `/src/main/kotlin/online/bingzi/bilibili/video/api/event/reward/RewardClaimEvent.kt` - 奖励领取事件
- `/src/main/kotlin/online/bingzi/bilibili/video/api/event/reward/RewardCheckEvent.kt` - 奖励检查事件

#### 修改文件

**1. 数据库管理器扩展**
- 修改 `/src/main/kotlin/online/bingzi/bilibili/video/internal/database/DatabaseManager.kt` 添加新表创建逻辑
- 修改 `/src/main/kotlin/online/bingzi/bilibili/video/internal/database/ServiceFactory.kt` 添加新服务创建

**2. 调度器集成**
- 修改 `/src/main/kotlin/online/bingzi/bilibili/video/internal/scheduler/UploaderVideoScheduler.kt` 集成奖励检查逻辑

### API变更

#### 新增REST端点（内部API）
无需新增外部REST API，所有功能通过Minecraft命令系统提供

#### 新增Minecraft命令

**1. 玩家命令**
```
/bilibili reward list - 查看可领取的奖励
/bilibili reward claim <bv号> - 领取指定视频奖励
/bilibili reward stats - 查看奖励统计
```

**2. 管理员命令**  
```
/bilibili reward admin add <UP主UID> <奖励脚本> - 添加UP主奖励配置
/bilibili reward admin remove <UP主UID> - 移除UP主奖励配置
/bilibili reward admin list - 查看所有奖励配置
/bilibili reward admin reload - 重载奖励配置
```

### 配置变更

#### 主配置文件修改 (config.yml)

```yaml
# 三连奖励系统配置
reward:
  # 是否启用奖励系统
  enabled: true
  
  # 每日奖励限制
  daily-limit: 3
  
  # 视频有效天数（新视频定义）
  video-valid-days: 7
  
  # 默认奖励脚本
  default-reward-script: "tell player '恭喜获得三连奖励！'"
  
  # 奖励检查间隔（分钟）
  check-interval: 60
  
  # 是否需要完整三连（点赞+投币+收藏）
  require-full-triple: false
  
  # 最低要求操作（支持：LIKE, COIN, FAVORITE 的组合）
  minimum-actions: 
    - "LIKE"
    - "COIN"
```

#### 语言文件扩展 (lang/zh_CN.yml)

新增奖励系统相关国际化消息：
```yaml
# 奖励系统消息
rewardSystemEnabled: "&a三连奖励系统已启用"
rewardSystemDisabled: "&c三连奖励系统已禁用"
rewardClaimSuccess: "&a成功领取视频 {0} 的奖励！"
rewardAlreadyClaimed: "&e您已经领取过视频 {0} 的奖励"
rewardDailyLimitReached: "&c今日奖励次数已达上限 ({0}/{1})"
rewardNotEligible: "&c该视频不符合奖励条件：{0}"
rewardTripleNotComplete: "&c请先完成对视频 {0} 的三连操作"
rewardVideoNotFound: "&c未找到视频 {0} 的信息"
rewardUploaderNotConfigured: "&c该UP主未配置奖励"
rewardExecuteSuccess: "&a奖励发放成功"
rewardExecuteError: "&c奖励发放失败：{0}"
rewardListEmpty: "&e当前没有可领取的奖励"
rewardListHeader: "&6=== 可领取奖励 ==="
rewardListItem: "&7- {0} ({1}) &f- &a点击领取"
rewardStatsHeader: "&6=== 奖励统计 ==="
rewardStatsDaily: "&7今日已领取：&e{0}&7/&e{1}"
rewardStatsTotal: "&7总计领取：&e{0}"
rewardAdminConfigAdded: "&a已添加UP主 {0} 的奖励配置"
rewardAdminConfigUpdated: "&e已更新UP主 {0} 的奖励配置"
rewardAdminConfigRemoved: "&a已移除UP主 {0} 的奖励配置"
rewardAdminConfigNotFound: "&c未找到UP主 {0} 的奖励配置"
rewardAdminListEmpty: "&e当前没有奖励配置"
rewardAdminListHeader: "&6=== 奖励配置列表 ==="
rewardAdminListItem: "&7- UP主: &e{0} &7(UID: {1}) - &a已启用"
rewardAdminReloadSuccess: "&a奖励配置重载成功"
```

## 实现序列

### 第一阶段：数据模型和基础服务
1. **创建数据实体类** - VideoRewardRecord, RewardConfig, PlayerRewardStats
2. **实现DAO服务** - 对应的数据访问层实现
3. **扩展数据库管理器** - 添加表创建和服务注册

### 第二阶段：核心业务逻辑  
1. **实现RewardChecker** - 检查玩家是否符合领取条件
2. **实现RewardExecutor** - 执行Kether脚本奖励分发
3. **实现TripleRewardService** - 整合奖励检查和执行逻辑

### 第三阶段：用户接口
1. **创建命令系统** - 玩家命令和管理员命令
2. **添加配置管理** - 解析和管理奖励配置
3. **集成事件系统** - 奖励相关事件定义和触发

### 第四阶段：调度和集成
1. **扩展UP主监控调度器** - 集成奖励检查逻辑
2. **添加配置文件支持** - config.yml和语言文件扩展
3. **集成测试** - 端到端功能测试

每个阶段都应独立部署和测试，确保系统稳定性。

## 验证计划

### 单元测试
- **RewardChecker测试** - 验证各种条件判断逻辑（日限制、视频时间、三连状态等）
- **RewardExecutor测试** - 验证Kether脚本执行和错误处理
- **DAO层测试** - 验证数据库操作的正确性

### 集成测试
- **完整奖励流程测试** - 从视频发布到玩家领取奖励的端到端测试
- **命令系统测试** - 验证所有命令的正确执行和权限检查
- **配置系统测试** - 验证配置加载、修改和重载功能

### 业务逻辑验证
- **日限制验证** - 确保每日奖励次数限制正常工作
- **视频时效性验证** - 确保只能对指定天数内的新视频领取奖励  
- **三连状态检查** - 确保只有完成三连操作的玩家才能领取奖励
- **重复领取防护** - 确保每个视频每个玩家只能领取一次奖励

## 关键约束

### MUST 要求
- **数据一致性** - 必须使用数据库事务确保奖励记录的准确性
- **性能优化** - 奖励检查不能影响现有UP主监控性能
- **配置热重载** - 支持不重启插件修改奖励配置
- **错误恢复** - Kether脚本执行失败时的优雅降级

### MUST NOT 要求  
- **不破坏现有功能** - 新功能不能影响现有的登录、视频操作等功能
- **不绕过限制条件** - 必须严格执行每日限制和重复领取检查
- **不硬编码配置** - 所有配置必须可通过配置文件或命令修改
- **不阻塞主线程** - 所有奖励检查和执行必须异步处理

该规格提供了实现Bilibili三连奖励系统的完整技术蓝图，确保与现有系统的完美集成和功能的可靠实现。