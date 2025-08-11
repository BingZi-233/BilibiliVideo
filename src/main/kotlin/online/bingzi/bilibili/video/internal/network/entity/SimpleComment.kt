package online.bingzi.bilibili.video.internal.network.entity

/**
 * 精简版评论信息
 * 只保留最基本的评论数据
 * 
 * @param rpid 评论ID
 * @param mid 评论者UID
 * @param username 评论者用户名
 * @param content 评论内容文本
 * @param ctime 评论时间戳（秒）
 * @param like 点赞数
 * @param replyCount 回复数量
 */
data class SimpleComment(
    val rpid: Long,
    val mid: Long,
    val username: String,
    val content: String,
    val ctime: Long,
    val like: Int = 0,
    val replyCount: Int = 0
)