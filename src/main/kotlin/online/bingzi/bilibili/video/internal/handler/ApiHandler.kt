package online.bingzi.bilibili.video.internal.handler

import online.bingzi.bilibili.video.internal.helper.debug
import online.bingzi.bilibili.video.internal.helper.debugStatus
import taboolib.common.platform.ProxyPlayer

/**
 * ApiHandler 抽象类
 * 该类作为处理 API 请求的基类，负责定义处理链中的下一个处理器，并提供处理请求的方法。
 * 具体的处理逻辑需要在子类中实现。
 */
abstract class ApiHandler {
    /**
     * 下一个处理器
     * 用于存储链中的下一个 ApiHandler 对象，以便在处理请求时调用。
     */
    private var nextHandler: ApiHandler? = null

    /**
     * 设置下一个处理器
     * @param nextHandler 下一个处理器的实例
     * @return 返回下一个处理器的实例
     *
     * 将下一个处理器设置为当前处理器的下一个处理器，并返回该处理器。
     */
    fun setNextHandler(nextHandler: ApiHandler): ApiHandler {
        this.nextHandler = nextHandler
        return nextHandler
    }

    /**
     * 处理请求
     * @param player 玩家对象，表示请求的发起者
     * @param bvid 视频的唯一标识符
     * @param sessData 会话数据，用于保持会话状态
     * @return 返回布尔值，表示请求是否被成功处理
     *
     * 此方法为抽象方法，具体的处理逻辑需要在子类中实现。
     */
    abstract fun handle(player: ProxyPlayer, bvid: String, sessData: String): Boolean

    /**
     * 调用下一个处理器
     * @param player 玩家对象，表示请求的发起者
     * @param bvid 视频的唯一标识符
     * @param sessData 会话数据，用于保持会话状态
     * @return 返回布尔值，表示请求是否被成功处理
     *
     * 该方法用于调用链中的下一个处理器进行请求处理。如果没有下一个处理器，则返回处理成功的状态。
     * 如果调试状态开启，则输出调试信息。
     */
    protected fun callNextHandler(player: ProxyPlayer, bvid: String, sessData: String): Boolean {
        return nextHandler?.handle(player, bvid, sessData) ?: let {
            // 当没有下一个处理器时，输出调试信息
            if (debugStatus) {
                debug("最终处理器 > 玩家: ${player.name} | 视频: $bvid | 通过")
            }
            // 返回处理成功的状态
            true
        }
    }
}