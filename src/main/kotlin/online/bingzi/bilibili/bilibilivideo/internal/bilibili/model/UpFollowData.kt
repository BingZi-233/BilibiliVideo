package online.bingzi.bilibili.bilibilivideo.internal.bilibili.model

/**
 * 用户卡片API响应数据类
 * 
 * @property code 响应状态码，0表示成功
 * @property message 响应消息
 * @property ttl 生存时间
 * @property data 用户卡片数据
 */
data class UserCardResponse(
    val code: Int,
    val message: String,
    val ttl: Int,
    val data: UserCardData?
)

/**
 * 用户卡片详细信息数据类
 * 
 * 包含Bilibili用户的完整公开信息。
 * 
 * @property mid 用户MID
 * @property name 用户昵称
 * @property approve 是否已认证
 * @property sex 性别
 * @property rank 等级
 * @property face 头像URL
 * @property DisplayRank 显示等级
 * @property regtime 注册时间
 * @property spacesta 空间状态
 * @property birthday 生日
 * @property place 地区
 * @property description 个人简介
 * @property article 专栏数
 * @property attentions 关注列表
 * @property fans 粉丝数
 * @property friend 好友数
 * @property attention 关注数
 * @property sign 个性签名
 * @property level_info 等级信息
 * @property pendant 头像挂件
 * @property nameplate 勋章
 * @property official 认证信息
 * @property official_verify 官方认证
 * @property vip 大会员信息
 * @property following 当前用户是否关注此用户
 * @property archive 投稿信息
 */
data class UserCardData(
    val mid: Long,
    val name: String,
    val approve: Boolean,
    val sex: String,
    val rank: String,
    val face: String,
    val DisplayRank: String,
    val regtime: Long,
    val spacesta: Int,
    val birthday: String,
    val place: String,
    val description: String,
    val article: Int,
    val attentions: List<Long>?,
    val fans: Long,
    val friend: Long,
    val attention: Long,
    val sign: String,
    val level_info: UserLevelInfo?,
    val pendant: UserPendant?,
    val nameplate: UserNameplate?,
    val official: UserOfficial?,
    val official_verify: UserOfficialVerify?,
    val vip: UserVip?,
    val following: Boolean,
    val archive: UserArchive?
)

/**
 * 用户等级信息
 */
data class UserLevelInfo(
    val current_level: Int,
    val current_min: Int,
    val current_exp: Int,
    val next_exp: Int
)

/**
 * 用户头像挂件信息
 */
data class UserPendant(
    val pid: Int,
    val name: String,
    val image: String,
    val expire: Int,
    val image_enhance: String,
    val image_enhance_frame: String
)

/**
 * 用户勋章信息
 */
data class UserNameplate(
    val nid: Int,
    val name: String,
    val image: String,
    val image_small: String,
    val level: String,
    val condition: String
)

/**
 * 用户认证信息
 */
data class UserOfficial(
    val role: Int,
    val title: String,
    val desc: String
)

/**
 * 官方认证信息
 */
data class UserOfficialVerify(
    val type: Int,
    val desc: String
)

/**
 * 大会员信息
 */
data class UserVip(
    val type: Int,
    val status: Int,
    val due_date: Long,
    val vip_pay_type: Int,
    val theme_type: Int,
    val label: UserVipLabel?
)

/**
 * 大会员标签信息
 */
data class UserVipLabel(
    val text: String,
    val label_theme: String,
    val text_color: String,
    val bg_style: Int,
    val bg_color: String,
    val border_color: String
)

/**
 * 用户投稿信息
 */
data class UserArchive(
    val view: Long
)

/**
 * UP主关注数据类
 * 
 * 表示用户对UP主的关注状态信息。
 * 提供状态格式化显示功能。
 * 
 * @property upMid UP主的Bilibili MID
 * @property upName UP主昵称
 * @property followerMid 关注者的Bilibili MID
 * @property playerUuid Minecraft玩家UUID
 * @property isFollowing 是否正在关注
 * @property timestamp 数据获取时间戳（毫秒）
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
data class UpFollowData(
    val upMid: Long,
    val upName: String,
    val followerMid: Long,
    val playerUuid: String,
    val isFollowing: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * 获取关注状态的文字描述
     * 
     * @return "已关注"或"未关注"
     */
    fun getStatusMessage(): String {
        return if (isFollowing) "已关注" else "未关注"
    }
}