package online.bingzi.bilibili.video.internal.cache

import online.bingzi.bilibili.video.api.qrcode.QRCodeSenderRegistry
import online.bingzi.bilibili.video.internal.qrcode.QRCodeSendService
import taboolib.common.platform.Awake
import taboolib.common.platform.ProxyPlayer
import java.util.concurrent.CompletableFuture

/**
 * 二维码发送器建议服务
 * 提供可用发送器的动态建议，用于命令参数自动补全
 * 
 * 主要功能：
 * - 获取当前可用的二维码发送器列表
 * - 根据玩家状态过滤可用的发送器
 * - 提供缓存机制优化响应速度
 */
@Awake
object QRCodeSenderSuggestionService {
    
    /**
     * 获取可用的二维码发送器建议
     * 
     * @param player 玩家对象，可为null（用于控制台）
     * @return 可用发送器名称列表的Future
     */
    fun getAvailableSenders(player: ProxyPlayer? = null): CompletableFuture<List<String>> {
        return CompletableFuture.supplyAsync {
            try {
                val registeredSenders = QRCodeSenderRegistry.getRegisteredSenderNames()
                
                // 过滤可用的发送器
                registeredSenders
                    .filter { senderName ->
                        QRCodeSendService.isSenderAvailable(senderName, player)
                    }
                    .sorted() // 按名称排序
                    .take(10) // 限制建议数量
                    
            } catch (e: Exception) {
                // 出现异常时返回默认的发送器选项
                getDefaultSenders()
            }
        }
    }
    
    /**
     * 获取所有注册的发送器建议（不检查可用性）
     * 用于管理员命令或调试场景
     * 
     * @return 所有发送器名称列表的Future
     */
    fun getAllRegisteredSenders(): CompletableFuture<List<String>> {
        return CompletableFuture.supplyAsync {
            try {
                QRCodeSenderRegistry.getRegisteredSenderNames()
                    .sorted()
                    .take(10)
                    
            } catch (e: Exception) {
                getDefaultSenders()
            }
        }
    }
    
    /**
     * 检查指定发送器是否可用
     * 
     * @param senderName 发送器名称
     * @param player 玩家对象
     * @return 是否可用的Future
     */
    fun isSenderAvailable(senderName: String, player: ProxyPlayer? = null): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                QRCodeSendService.isSenderAvailable(senderName, player)
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * 获取发送器的详细信息建议
     * 返回包含描述的发送器信息
     * 
     * @param player 玩家对象
     * @return 发送器详细信息列表的Future
     */
    fun getDetailedSenderSuggestions(player: ProxyPlayer? = null): CompletableFuture<List<String>> {
        return CompletableFuture.supplyAsync {
            try {
                val registeredSenders = QRCodeSenderRegistry.getRegisteredSenderNames()
                
                registeredSenders
                    .mapNotNull { senderName ->
                        if (QRCodeSendService.isSenderAvailable(senderName, player)) {
                            "$senderName (${getSenderDescription(senderName)})"
                        } else {
                            null
                        }
                    }
                    .sorted()
                    .take(10)
                    
            } catch (e: Exception) {
                getDefaultSenders().map { "$it (默认)" }
            }
        }
    }
    
    /**
     * 获取发送器的简短描述
     * 
     * @param senderName 发送器名称
     * @return 发送器描述
     */
    private fun getSenderDescription(senderName: String): String {
        return when (senderName.lowercase()) {
            "chat", "聊天" -> "游戏内聊天显示"
            "map", "地图" -> "游戏内地图显示"  
            "onebot", "qq" -> "QQ机器人发送"
            else -> "自定义发送器"
        }
    }
    
    /**
     * 获取默认的发送器选项
     * 当无法获取注册信息时的降级方案
     * 
     * @return 默认发送器名称列表
     */
    private fun getDefaultSenders(): List<String> {
        return listOf("chat", "map", "onebot").take(10)
    }
    
    /**
     * 缓存清理方法
     * 当发送器注册状态变更时可调用
     */
    fun invalidateCache() {
        // 清理相关缓存
        CommandSuggestionCache.invalidate("qrcode_senders")
        CommandSuggestionCache.invalidate("available_senders")
    }
    
    /**
     * 获取发送器统计信息
     * 用于调试和监控
     * 
     * @return 统计信息
     */
    fun getSenderStats(): Map<String, Any> {
        return try {
            val totalSenders = QRCodeSenderRegistry.getRegisteredSenderCount()
            val registeredNames = QRCodeSenderRegistry.getRegisteredSenderNames()
            
            mapOf(
                "totalSenders" to totalSenders,
                "registeredSenders" to registeredNames.toList(),
                "defaultSenders" to getDefaultSenders()
            )
        } catch (e: Exception) {
            mapOf(
                "error" to (e.message ?: "Unknown error"),
                "defaultSenders" to getDefaultSenders()
            )
        }
    }
}