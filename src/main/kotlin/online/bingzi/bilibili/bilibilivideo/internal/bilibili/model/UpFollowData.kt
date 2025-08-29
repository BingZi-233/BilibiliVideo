package online.bingzi.bilibili.bilibilivideo.internal.bilibili.model

data class UserCardResponse(
    val code: Int,
    val message: String,
    val ttl: Int,
    val data: UserCardData?
)

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
    val following: Boolean, // 是否正在关注
    val archive: UserArchive?
)

data class UserLevelInfo(
    val current_level: Int,
    val current_min: Int,
    val current_exp: Int,
    val next_exp: Int
)

data class UserPendant(
    val pid: Int,
    val name: String,
    val image: String,
    val expire: Int,
    val image_enhance: String,
    val image_enhance_frame: String
)

data class UserNameplate(
    val nid: Int,
    val name: String,
    val image: String,
    val image_small: String,
    val level: String,
    val condition: String
)

data class UserOfficial(
    val role: Int,
    val title: String,
    val desc: String
)

data class UserOfficialVerify(
    val type: Int,
    val desc: String
)

data class UserVip(
    val type: Int,
    val status: Int,
    val due_date: Long,
    val vip_pay_type: Int,
    val theme_type: Int,
    val label: UserVipLabel?
)

data class UserVipLabel(
    val text: String,
    val label_theme: String,
    val text_color: String,
    val bg_style: Int,
    val bg_color: String,
    val border_color: String
)

data class UserArchive(
    val view: Long
)

data class UpFollowData(
    val upMid: Long,
    val upName: String,
    val followerMid: Long,
    val playerUuid: String,
    val isFollowing: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getStatusMessage(): String {
        return if (isFollowing) "已关注" else "未关注"
    }
}