package online.bingzi.bilibili.video.internal.network.entity

/**
 * 视频评论区响应
 * 
 * @param comments 评论列表
 * @param total 评论总数
 * @param pages 总页数
 * @param currentPage 当前页码
 * @param pageSize 每页数量
 * @param cursor 游标（用于下一页获取）
 * @param hasMore 是否还有更多评论
 * @param topComments 置顶评论列表
 */
data class VideoCommentsResponse(
    val comments: List<CommentInfo>,
    val total: Long,
    val pages: Int,
    val currentPage: Int,
    val pageSize: Int,
    val cursor: CommentCursor?,
    val hasMore: Boolean,
    val topComments: List<CommentInfo>
)

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

/**
 * 评论内容
 * 
 * @param message 评论文本
 * @param plat 平台
 * @param device 设备
 * @param members 提到的用户列表
 * @param emote 表情信息
 * @param jumpUrl 跳转链接信息
 * @param maxLine 最大行数
 */
data class CommentContent(
    val message: String,
    val plat: Int,
    val device: String,
    val members: List<CommentMentionedMember>?,
    val emote: Map<String, CommentEmote>?,
    val jumpUrl: Map<String, CommentJumpUrl>?,
    val maxLine: Int
)

/**
 * 评论等级信息
 */
data class CommentLevelInfo(
    val currentLevel: Int,
    val currentMin: Int,
    val currentExp: Int,
    val nextExp: Int
)

/**
 * 评论挂件信息
 */
data class CommentPendant(
    val pid: Int,
    val name: String,
    val image: String,
    val expire: Int,
    val imageEnhance: String,
    val imageEnhanceFrame: String
)

/**
 * 评论勋章信息
 */
data class CommentNameplate(
    val nid: Int,
    val name: String,
    val image: String,
    val imageSmall: String,
    val level: String,
    val condition: String
)

/**
 * 评论认证信息
 */
data class CommentOfficialVerify(
    val type: Int,
    val desc: String
)

/**
 * 评论VIP信息
 */
data class CommentVip(
    val vipType: Int,
    val vipDueDate: Long,
    val dueRemark: String,
    val accessStatus: Int,
    val vipStatus: Int,
    val vipStatusWarn: String,
    val themeType: Int,
    val label: CommentVipLabel?,
    val avatarSubscript: Int,
    val nicknameColor: String
)

/**
 * VIP标签信息
 */
data class CommentVipLabel(
    val path: String,
    val text: String,
    val labelTheme: String,
    val textColor: String,
    val bgStyle: Int,
    val bgColor: String,
    val borderColor: String,
    val useImgLabel: Boolean,
    val imgLabelUriHans: String,
    val imgLabelUriHant: String,
    val imgLabelUriHansStatic: String,
    val imgLabelUriHantStatic: String
)

/**
 * 粉丝详情
 */
data class CommentFansDetail(
    val uid: Long,
    val medalId: Int,
    val medalName: String,
    val score: Int,
    val level: Int,
    val intimacy: Int,
    val masterStatus: Int,
    val isReceive: Int
)

/**
 * 提到的用户
 */
data class CommentMentionedMember(
    val uid: Long,
    val uname: String
)

/**
 * 评论表情
 */
data class CommentEmote(
    val id: Int,
    val packageId: Int,
    val state: Int,
    val type: Int,
    val attr: Int,
    val text: String,
    val url: String,
    val meta: CommentEmoteMeta?,
    val mtime: Long
)

/**
 * 表情元数据
 */
data class CommentEmoteMeta(
    val size: Int,
    val alias: String
)

/**
 * 跳转链接
 */
data class CommentJumpUrl(
    val title: String,
    val state: Int,
    val prefixIcon: String,
    val appUrlSchema: String,
    val appName: String,
    val appPackageName: String,
    val clickReport: String
)

/**
 * 评论回复信息
 */
data class CommentReplies(
    val replies: List<CommentInfo>?
)

/**
 * 评论折叠信息
 */
data class CommentFolder(
    val hasFolded: Boolean,
    val isFolded: Boolean,
    val rule: String
)

/**
 * UP主操作信息
 */
data class CommentUpAction(
    val like: Boolean,
    val reply: Boolean
)

/**
 * 评论游标信息
 * 用于分页获取更多评论
 */
data class CommentCursor(
    val allCount: Long,
    val isBegin: Boolean,
    val prev: Long,
    val next: Long,
    val isEnd: Boolean,
    val mode: Int,
    val showType: Int,
    val supportMode: List<Int>,
    val name: String
)