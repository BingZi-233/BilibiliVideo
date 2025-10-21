# 更新日志

## 1.9.1-beta1 (2025-10-21)

改进
- 完善奖励系统数据层：
  - 新增 DatabaseService.hasVideoRewardRecord、getVideoRewardRecord、saveVideoRewardRecord 等方法。
  - 统一使用 TabooLib Table API 封装异步回调，便于后续业务接入，保持主线程流畅。

其他
- 升级版本号至 1.9.1-beta1（gradle.properties）。
- 调整 gradlew 执行权限（修复部分环境无法直接构建的问题）。

内部实现细节
- 新增导入：DatabaseService.kt 增加 VideoRewardRecord 实体引用。
- DatabaseService：补充视频奖励记录相关接口，统一使用 TabooLib Table API 访问。
- RewardManager：仍保留占位实现，后续将接入 DatabaseService 完成重复检查与记录写入。

兼容性
- 无破坏性改动；数据库表结构未变更（video_reward_record 表已存在）。

