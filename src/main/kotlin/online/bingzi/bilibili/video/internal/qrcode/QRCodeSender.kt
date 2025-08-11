package online.bingzi.bilibili.video.internal.qrcode

import taboolib.common.platform.ProxyPlayer
import java.awt.image.BufferedImage

/**
 * 二维码发送器接口
 * 支持多种发送模式：地图、聊天框、QQ机器人
 */
interface QRCodeSender {
    
    /**
     * 发送二维码给玩家
     * @param player 目标玩家
     * @param qrCodeImage 二维码图片
     * @param title 二维码标题
     * @param description 二维码描述
     * @return 是否发送成功
     */
    fun sendQRCode(
        player: ProxyPlayer, 
        qrCodeImage: BufferedImage, 
        title: String, 
        description: String
    ): Boolean
    
    /**
     * 获取发送器名称
     */
    fun getSenderName(): String
    
    /**
     * 检查发送器是否可用
     * @param player 目标玩家（某些发送器可能需要检查玩家状态）
     * @return 是否可用
     */
    fun isAvailable(player: ProxyPlayer? = null): Boolean
}