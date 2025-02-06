package online.bingzi.bilibili.video.internal.helper

import taboolib.common.platform.function.info
import taboolib.module.chat.colored

/**
 * DebugHelper 类
 * 该类提供了调试工具，用于在控制台输出调试信息。
 * 主要功能是格式化调试信息并通过特定的方式输出。
 */
internal fun debug(message: String) {
    /**
     * debug 方法
     * 该方法用于打印调试信息到控制台。
     *
     * @param message 要输出的调试信息，类型为 String。可以是任何需要调试的文本。
     *                例如："连接成功" 或 "数据加载错误"。
     *
     * 返回值：无，方法执行后不返回任何值。
     *
     * 该方法使用了 taboolib 的 info 函数来输出信息，并对信息进行了着色处理。
     */
    info("&7&l[&a&ldebug&7&l] > &f&l$message".colored())
}