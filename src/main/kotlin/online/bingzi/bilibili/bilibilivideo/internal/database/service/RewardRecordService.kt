package online.bingzi.bilibili.bilibilivideo.internal.database.service

import online.bingzi.bilibili.bilibilivideo.internal.database.entity.VideoRewardRecord
import online.bingzi.bilibili.bilibilivideo.internal.database.factory.TableFactory
import taboolib.common.platform.function.severe
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submitAsync

/**
 * 奖励记录服务
 *
 * 提供对视频奖励记录表的异步增查能力，用于防重复领奖与审计。
 *
 * @since 1.0.0
 */
object RewardRecordService {

    /**
     * 检查玩家是否已领取过某视频奖励
     *
     * @param playerUuid 玩家 UUID
     * @param bvid 视频 BV 号
     * @param callback 回调，true 表示已领取
     */
    fun hasPlayerReceivedReward(playerUuid: String, bvid: String, callback: (Boolean) -> Unit) {
        submitAsync {
            try {
                val table = TableFactory.getVideoRewardRecordTable()
                val dataSource = TableFactory.getDataSource()

                val results = table.select(dataSource) {
                    where { "player_uuid" eq playerUuid; "bvid" eq bvid }
                }

                submit { callback(results.find()) }
            } catch (e: Exception) {
                submit {
                    severe("检查奖励记录失败: ${e.message}")
                    callback(false)
                }
            }
        }
    }

    /**
     * 保存视频奖励记录
     *
     * @param record 奖励记录实体
     * @param callback 回调，true 表示保存成功
     */
    fun saveVideoRewardRecord(record: VideoRewardRecord, callback: (Boolean) -> Unit) {
        submitAsync {
            try {
                val table = TableFactory.getVideoRewardRecordTable()
                val dataSource = TableFactory.getDataSource()
                val currentTime = System.currentTimeMillis()

                val result = table.insert(dataSource) {
                    value("bvid", record.bvid)
                    value("mid", record.mid)
                    value("player_uuid", record.playerUuid)
                    value("reward_type", record.rewardType)
                    value("reward_data", record.rewardData)
                    value("is_liked", if (record.isLiked) 1 else 0)
                    value("is_coined", if (record.isCoined) 1 else 0)
                    value("is_favorited", if (record.isFavorited) 1 else 0)
                    value("create_time", currentTime)
                    value("update_time", currentTime)
                    value("create_player", record.createPlayer)
                    value("update_player", record.updatePlayer)
                } > 0

                submit { callback(result) }
            } catch (e: Exception) {
                submit {
                    severe("保存视频奖励记录失败: ${e.message}")
                    callback(false)
                }
            }
        }
    }
}
