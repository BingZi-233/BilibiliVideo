# 更新日志

## 1.9.1-beta (2025-10-21)

改进
- 完善奖励系统持久化逻辑：
  - 新增 DatabaseService.hasVideoRewardRecord 与 saveVideoRewardRecord 两个方法，支持查询/写入奖励记录。
  - RewardManager 现在会在发放奖励前进行重复领取检查（可通过 setting.yml 的 prevent-duplicate-rewards 控制）。
  - 发放成功后会写入 video_reward_record 表，记录玩家、BV 号、三连状态与奖励类型。
- 优化异步流程：奖励判定与数据持久化均在异步任务中完成，保持主线程流畅。

其他
- 升级版本号至 1.9.1-beta（gradle.properties）。
- 调整 gradlew 执行权限（修复部分环境无法直接构建的问题）。

内部实现细节
- 新增导入：DatabaseService.kt 增加 VideoRewardRecord 实体引用。
- DatabaseService：补充视频奖励记录相关接口，统一使用 TabooLib Table API 访问。
- RewardManager：移除占位实现，改为调用 DatabaseService 完成重复检查与记录写入。

兼容性
- 无破坏性改动；数据库表结构未变更（video_reward_record 表已存在）。

