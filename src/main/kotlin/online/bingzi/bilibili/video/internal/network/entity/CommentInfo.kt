package online.bingzi.bilibili.video.internal.network.entity

/**
 * 评论信息
 *
 * @param rpid 评论ID
 * @param oid 对象ID（视频AV号）
 * @param type 评论类型（1=视频）
 * @param mid 评论者UID
 * @param root 根评论ID（0表示主评论）
 * @param parent 父评论ID
 * @param dialog 对话ID
 * @param count 子评论数量
 * @param rcount 回复数量
 * @param state 评论状态
 * @param fansgrade 粉丝等级
 * @param attr 评论属性
 * @param ctime 评论时间戳
 * @param like 点赞数
 * @param action 当前用户对该评论的操作状态
 * @param member 评论者信息
 * @param content 评论内容
 * @param replies 回复列表
 * @param assist 评论辅助信息
 * @param folder 折叠信息
 * @param upAction UP主操作信息
 * @param showFollow 是否显示关注
 */
data class CommentInfo(
    val rpid: Long,
    val oid: Long,
    val type: Int,
    val mid: Long,
    val root: Long,
    val parent: Long,
    val dialog: Long,
    val count: Int,
    val rcount: Int,
    val state: Int,
    val fansgrade: Int,
    val attr: Int,
    val ctime: Long,
    val like: Int,
    val action: Int,
    val member: CommentMember,
    val content: CommentContent,
    val replies: CommentReplies?,
    val assist: Int,
    val folder: CommentFolder?,
    val upAction: CommentUpAction?,
    val showFollow: Boolean
)