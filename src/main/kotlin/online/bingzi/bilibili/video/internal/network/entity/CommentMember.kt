package online.bingzi.bilibili.video.internal.network.entity

/**
 * 评论者信息
 *
 * @param mid 用户UID
 * @param uname 用户名
 * @param sex 性别
 * @param sign 个性签名
 * @param avatar 头像URL
 * @param rank 等级
 * @param displayRank 显示等级
 * @param levelInfo 等级信息
 * @param pendant 挂件信息
 * @param nameplate 勋章信息
 * @param officialVerify 认证信息
 * @param vip VIP信息
 * @param fansDetail 粉丝详情
 * @param following 是否关注
 * @param isFollowed 是否被关注
 */
data class CommentMember(
    val mid: String,
    val uname: String,
    val sex: String,
    val sign: String,
    val avatar: String,
    val rank: String,
    val displayRank: String,
    val levelInfo: CommentLevelInfo?,
    val pendant: CommentPendant?,
    val nameplate: CommentNameplate?,
    val officialVerify: CommentOfficialVerify?,
    val vip: CommentVip?,
    val fansDetail: CommentFansDetail?,
    val following: Int,
    val isFollowed: Int
)