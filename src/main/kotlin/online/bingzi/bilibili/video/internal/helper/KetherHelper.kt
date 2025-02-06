package online.bingzi.bilibili.video.internal.helper

import taboolib.common.platform.ProxyCommandSender
import taboolib.library.kether.ArgTypes.listOf
import taboolib.library.kether.LocalizedException
import taboolib.module.kether.KetherShell.eval
import taboolib.module.kether.ScriptOptions

/**
 * KetherEval 扩展函数
 * 该扩展函数为 String 类型提供 Kether 脚本的执行功能。
 * 主要用于通过指定的执行者来评估 Kether 脚本。
 *
 * @param sender 执行者，类型为 ProxyCommandSender，表示执行脚本的命令发送者。
 */
fun String.ketherEval(sender: ProxyCommandSender) {
    // 调用 evalScript 函数执行当前字符串作为 Kether 脚本
    evalScript(listOf(this), sender)
}

/**
 * KetherEval 扩展函数
 * 该扩展函数为 List<String> 类型提供 Kether 脚本的执行功能。
 * 主要用于通过指定的执行者来评估多个 Kether 脚本。
 *
 * @param sender 执行者，类型为 ProxyCommandSender，表示执行脚本的命令发送者。
 */
fun List<String>.ketherEval(sender: ProxyCommandSender) {
    // 调用 evalScript 函数执行当前列表中的 Kether 脚本
    evalScript(this, sender)
}

/**
 * evalScript 函数
 * 该函数用于执行传入的 Kether 脚本。
 * 在执行过程中会捕获可能出现的异常并进行处理。
 *
 * @param script 脚本，类型为 List<String>，表示要执行的 Kether 脚本列表。
 * @param sender 执行者，类型为 ProxyCommandSender，表示执行脚本的命令发送者。
 */
private fun evalScript(script: List<String>, sender: ProxyCommandSender) {
    try {
        // 使用 KetherShell 的 eval 方法执行脚本，并构建脚本选项
        eval(script, ScriptOptions.builder().sender(sender).build())
    } catch (e: LocalizedException) {
        // 捕获 LocalizedException，记录警告信息
        e.message?.let { sender.warningAsLang("KetherLocalizedException", it) }
    } catch (e: Throwable) {
        // 捕获其他异常类型，记录警告信息
        e.message?.let { sender.warningAsLang("KetherThrowable", it) }
    }
}