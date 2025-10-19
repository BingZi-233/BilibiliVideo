package online.bingzi.bilibili.bilibilivideo.internal.helper

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.function.getProxyPlayer
import taboolib.library.kether.LocalizedException
import taboolib.module.kether.KetherShell.eval
import taboolib.module.kether.ScriptOptions
import taboolib.module.lang.sendWarn

/**
 * Kether eval
 * <p>
 * Kether 脚本
 *
 * @param sender 执行者
 *
 * @author BingZi-233
 * @since 2.0.0
 */
fun String.ketherEval(sender: ProxyPlayer) {
    getProxyPlayer(sender.uniqueId)?.let { evalScript(listOf(this), it) }
}

/**
 * Kether eval
 * <p>
 * Kether 脚本
 *
 * @param sender 执行者
 *
 * @author BingZi-233
 * @since 2.0.0
 */
fun List<String>.ketherEval(sender: ProxyPlayer) {
    getProxyPlayer(sender.uniqueId)?.let { evalScript(this, it) }
}

/**
 * Kether eval
 * <p>
 * Kether 脚本
 *
 * @param sender 执行者
 *
 * @author BingZi-233
 * @since 2.0.0
 */
fun String.ketherEval(sender: ProxyCommandSender) {
    evalScript(listOf(this), sender)
}

/**
 * Kether eval
 * <p>
 * Kether 脚本
 *
 * @param sender 执行者
 *
 * @author BingZi-233
 * @since 2.0.0
 */
fun List<String>.ketherEval(sender: ProxyCommandSender) {
    evalScript(this, sender)
}

/**
 * Eval script
 * <p>
 * 脚本执行
 *
 * @param script 脚本
 * @param sender 执行者
 *
 * @author BingZi-233
 * @since 2.0.0
 */
private fun evalScript(script: List<String>, sender: ProxyCommandSender) {
    try {
        eval(script, ScriptOptions.builder().sender(sender).build())
    } catch (e: LocalizedException) {
        e.message?.let { sender.sendWarn("ketherLocalizedException", it) }
    } catch (e: Throwable) {
        e.message?.let { sender.sendWarn("ketherThrowable", it) }
    }
}

/**
 * Eval script result boolean
 * <p>
 * 脚本执行并返回Boolean结果
 *
 * @param script 脚本
 * @param sender 执行者
 * @return Boolean
 *
 * @author BingZi-233
 * @since 2.0.0
 */
private fun evalScriptResultBoolean(script: List<String>, sender: ProxyCommandSender): Boolean {
    val result: Any? = try {
        eval(script, ScriptOptions.builder().sender(sender).build()).getNow(false)
    } catch (e: LocalizedException) {
        e.message?.let { sender.sendWarn("ketherLocalizedException", it) }
    } catch (e: Throwable) {
        e.message?.let { sender.sendWarn("ketherThrowable", it) }
    }
    return if (result is Boolean) {
        result
    } else {
        false
    }
}