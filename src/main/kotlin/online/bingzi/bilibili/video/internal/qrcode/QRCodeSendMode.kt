package online.bingzi.bilibili.video.internal.qrcode

/**
 * 二维码发送模式枚举
 */
enum class QRCodeSendMode(
    val displayName: String,
    val description: String
) {
    /**
     * 二维码地图模式
     * 将二维码渲染成地图并发送给玩家
     */
    MAP("地图模式", "将二维码渲染成Minecraft地图物品"),
    
    /**
     * 聊天框二维码模式
     * 使用ASCII字符在聊天框显示二维码
     */
    CHAT("聊天框模式", "在聊天框使用字符显示二维码"),
    
    /**
     * OneBot QQ机器人模式
     * 通过QQ机器人发送二维码图片
     */
    ONEBOT("QQ机器人模式", "通过OneBot协议发送到QQ")
}