package online.bingzi.bilibili.video.internal.helper

import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.function.console
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn

// 定义一组用于发送信息和警告的内部函数，这些函数的目标是通过控制台或玩家的方式发送语言信息。
// 这些函数主要用于日志记录和通知，提供了不同的消息类型和参数化功能。
internal fun infoMessageAsLang(node: String) {
    // 通过控制台发送信息
    // 参数 node: 消息节点，表示要发送的信息标识符
    console().sendInfo(node)
}

internal fun infoMessageAsLang(node: String, vararg args: Any) {
    // 通过控制台发送格式化信息
    // 参数 node: 消息节点，表示要发送的信息标识符
    // 参数 args: 可变参数，表示要插入到消息中的动态数据
    console().sendInfo(node, *args)
}

internal fun warningMessageAsLang(node: String) {
    // 通过控制台发送警告信息
    // 参数 node: 消息节点，表示要发送的警告信息标识符
    console().sendWarn(node)
}

internal fun warningMessageAsLang(node: String, vararg args: Any) {
    // 通过控制台发送格式化的警告信息
    // 参数 node: 消息节点，表示要发送的警告信息标识符
    // 参数 args: 可变参数，表示要插入到警告消息中的动态数据
    console().sendWarn(node, *args)
}

// 扩展函数：为 ProxyCommandSender 接口添加信息发送功能
// 该功能根据发送者的类型（玩家或控制台）选择适当的发送方式。
internal fun ProxyCommandSender.infoAsLang(node: String) {
    // 检查当前发送者是否为 ProxyPlayer（玩家）
    if (this is ProxyPlayer) {
        // 如果是玩家，则通过玩家对象发送信息
        this.sendInfo(node)
    } else {
        // 否则，通过控制台发送信息
        infoMessageAsLang(node)
    }
}

internal fun ProxyCommandSender.infoAsLang(node: String, vararg args: Any) {
    // 扩展函数：为 ProxyCommandSender 接口添加格式化信息发送功能
    // 参数 node: 消息节点，表示要发送的信息标识符
    // 参数 args: 可变参数，表示要插入到消息中的动态数据
    if (this is ProxyPlayer) {
        // 如果是玩家，则通过玩家对象发送格式化信息
        this.sendInfo(node, *args)
    } else {
        // 否则，通过控制台发送格式化信息
        infoMessageAsLang(node, *args)
    }
}

// 扩展函数：为 ProxyCommandSender 接口添加警告发送功能
// 该功能根据发送者的类型（玩家或控制台）选择适当的发送方式。
internal fun ProxyCommandSender.warningAsLang(node: String) {
    // 检查当前发送者是否为 ProxyPlayer（玩家）
    if (this is ProxyPlayer) {
        // 如果是玩家，则通过玩家对象发送警告
        this.sendWarn(node)
    } else {
        // 否则，通过控制台发送警告
        warningMessageAsLang(node)
    }
}

internal fun ProxyCommandSender.warningAsLang(node: String, vararg args: Any) {
    // 扩展函数：为 ProxyCommandSender 接口添加格式化警告发送功能
    // 参数 node: 消息节点，表示要发送的警告信息标识符
    // 参数 args: 可变参数，表示要插入到警告消息中的动态数据
    if (this is ProxyPlayer) {
        // 如果是玩家，则通过玩家对象发送格式化警告
        this.sendWarn(node, *args)
    } else {
        // 否则，通过控制台发送格式化警告
        warningMessageAsLang(node, *args)
    }
}