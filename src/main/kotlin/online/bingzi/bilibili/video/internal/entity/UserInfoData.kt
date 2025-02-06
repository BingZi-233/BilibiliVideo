package online.bingzi.bilibili.video.internal.entity

/**
 * 用户信息数据类
 *
 * 该类用于存储用户的基本信息，包括用户ID和用户名。它通常用于在应用程序中表示用户身份。
 *
 * @property mid 用户的唯一标识符，类型为字符串，通常用于在系统中唯一标识一个用户。
 * @property uname 用户的名称，类型为字符串，表示用户在系统中的显示名称。
 * @constructor 创建一个空的用户信息数据对象。
 */
data class UserInfoData(
    val mid: String,  // 用户的唯一标识符
    val uname: String // 用户的名称
)