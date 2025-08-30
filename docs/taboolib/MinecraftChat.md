# MinecraftChat 模块

## 概述
MinecraftChat 模块提供强大的聊天消息处理功能，支持 JSON 格式消息、RGB 颜色、悬停文本、点击事件等现代 Minecraft 聊天特性。

## 功能特性
- Component (JSON) 消息构建
- RGB 颜色支持 (1.16+)
- 悬停文本 (HoverEvent)
- 点击事件 (ClickEvent)
- 颜色代码转换
- 文本样式 (粗体、斜体等)

## 安装配置
在 `build.gradle.kts` 中配置：
```kotlin
taboolib {
    env {
        install(MinecraftChat)
        install(Bukkit) // 通常与 Bukkit 模块一起使用
    }
}
```

## 基本用法

### 简单消息
```kotlin
import taboolib.module.chat.*
import net.md_5.bungee.api.chat.TextComponent

// 发送简单彩色消息
player.sendMessage("§a这是一条绿色消息")
player.sendMessage("§c这是一条红色消息")

// 使用 RGB 颜色 (1.16+)
player.sendMessage("&#FF5733这是橙红色消息")
player.sendMessage("&#33FF57这是绿色消息")
```

### Component 消息构建
```kotlin
import taboolib.module.chat.component.*

// 基础 Component
val component = Components.text("Hello World!")
    .color(NamedTextColor.GREEN)
    .bold(true)

player.sendMessage(component)

// 链式构建
val welcomeMessage = Components.text("欢迎来到服务器！")
    .color(NamedTextColor.GOLD)
    .append(Components.newline())
    .append(
        Components.text("点击这里获取帮助")
            .color(NamedTextColor.BLUE)
            .underlined(true)
            .clickEvent(ClickEvent.runCommand("/help"))
            .hoverEvent(HoverEvent.showText(Components.text("执行 /help 命令")))
    )

player.spigot().sendMessage(welcomeMessage.build())
```

## 高级功能

### 悬停和点击事件
```kotlin
// 创建带悬停信息的消息
val hoverMessage = Components.text("悬停查看详情")
    .color(NamedTextColor.YELLOW)
    .hoverEvent(HoverEvent.showText(
        Components.text("这里是详细信息\n")
            .color(NamedTextColor.WHITE)
            .append(Components.text("第二行信息").color(NamedTextColor.GRAY))
    ))

// 创建可点击的链接
val linkMessage = Components.text("访问官网")
    .color(NamedTextColor.BLUE)
    .underlined(true)
    .clickEvent(ClickEvent.openUrl("https://example.com"))
    .hoverEvent(HoverEvent.showText(Components.text("点击打开网页")))

// 创建执行命令的按钮
val commandButton = Components.text("[点击传送]")
    .color(NamedTextColor.GREEN)
    .bold(true)
    .clickEvent(ClickEvent.runCommand("/spawn"))
    .hoverEvent(HoverEvent.showText(Components.text("传送到出生点")))
```

### RGB 颜色渐变
```kotlin
import taboolib.module.chat.colored

// 渐变文本
val gradientText = "彩虹文字效果".colored()
player.sendMessage(gradientText)

// 自定义渐变
fun createGradient(text: String, fromColor: String, toColor: String): String {
    return text.toCharArray().mapIndexed { index, char ->
        val progress = index.toDouble() / text.length
        val color = interpolateColor(fromColor, toColor, progress)
        "&#${color}$char"
    }.joinToString("")
}

val customGradient = createGradient("自定义渐变", "FF0000", "0000FF")
player.sendMessage(customGradient)
```

### 消息格式化工具
```kotlin
object ChatFormatter {
    
    // 格式化玩家消息
    fun formatPlayerMessage(player: Player, message: String): Component {
        return Components.text("[")
            .color(NamedTextColor.GRAY)
            .append(
                Components.text(player.name)
                    .color(getPlayerNameColor(player))
                    .hoverEvent(HoverEvent.showText(
                        Components.text("等级: ${player.level}\n")
                            .append(Components.text("游戏时间: ${getPlayTime(player)}"))
                    ))
            )
            .append(Components.text("] ").color(NamedTextColor.GRAY))
            .append(Components.text(message).color(NamedTextColor.WHITE))
    }
    
    // 格式化系统消息
    fun formatSystemMessage(message: String, type: MessageType): Component {
        val (prefix, color) = when (type) {
            MessageType.INFO -> "[信息]" to NamedTextColor.GREEN
            MessageType.WARNING -> "[警告]" to NamedTextColor.YELLOW  
            MessageType.ERROR -> "[错误]" to NamedTextColor.RED
        }
        
        return Components.text(prefix)
            .color(color)
            .bold(true)
            .append(Components.text(" $message").color(NamedTextColor.WHITE).bold(false))
    }
}

enum class MessageType {
    INFO, WARNING, ERROR
}
```

### 交互式菜单
```kotlin
object InteractiveMenu {
    
    fun showMainMenu(player: Player) {
        val menu = Components.text("=== 服务器菜单 ===")
            .color(NamedTextColor.GOLD)
            .bold(true)
            .append(Components.newline())
            .append(createMenuButton("🏠 回到主城", "/spawn", "传送到主城"))
            .append(Components.newline())
            .append(createMenuButton("💰 查看余额", "/money", "查看你的金币余额"))
            .append(Components.newline())
            .append(createMenuButton("🎒 打开背包", "/backpack", "打开额外背包"))
            .append(Components.newline())
            .append(createMenuButton("📊 查看统计", "/stats", "查看个人统计信息"))
        
        player.spigot().sendMessage(menu.build())
    }
    
    private fun createMenuButton(text: String, command: String, description: String): Component {
        return Components.text("  $text")
            .color(NamedTextColor.YELLOW)
            .clickEvent(ClickEvent.runCommand(command))
            .hoverEvent(HoverEvent.showText(
                Components.text(description)
                    .color(NamedTextColor.WHITE)
                    .append(Components.newline())
                    .append(Components.text("点击执行").color(NamedTextColor.GRAY).italic(true))
            ))
    }
}
```

### 聊天频道系统
```kotlin
object ChatChannels {
    
    enum class Channel(val prefix: String, val color: NamedTextColor, val permission: String?) {
        GLOBAL("全服", NamedTextColor.WHITE, null),
        LOCAL("本地", NamedTextColor.YELLOW, null),
        ADMIN("管理", NamedTextColor.RED, "chat.admin"),
        VIP("VIP", NamedTextColor.GOLD, "chat.vip")
    }
    
    fun sendChannelMessage(player: Player, channel: Channel, message: String) {
        // 检查权限
        if (channel.permission != null && !player.hasPermission(channel.permission)) {
            player.sendMessage("§c你没有权限使用此频道！")
            return
        }
        
        val formattedMessage = Components.text("[${channel.prefix}]")
            .color(channel.color)
            .bold(true)
            .append(Components.text(" "))
            .append(
                Components.text(player.name)
                    .color(NamedTextColor.BLUE)
                    .hoverEvent(HoverEvent.showText(
                        Components.text("玩家: ${player.name}\n")
                            .append(Components.text("等级: ${player.level}\n"))
                            .append(Components.text("点击私聊"))
                    ))
                    .clickEvent(ClickEvent.suggestCommand("/msg ${player.name} "))
            )
            .append(Components.text(": $message").color(channel.color))
        
        // 广播消息给对应范围的玩家
        broadcastToChannel(channel, formattedMessage, player)
    }
    
    private fun broadcastToChannel(channel: Channel, message: Component, sender: Player) {
        when (channel) {
            Channel.GLOBAL -> server.onlinePlayers.forEach { it.spigot().sendMessage(message.build()) }
            Channel.LOCAL -> getNearbyPlayers(sender, 50.0).forEach { it.spigot().sendMessage(message.build()) }
            Channel.ADMIN -> server.onlinePlayers.filter { it.hasPermission("chat.admin") }
                .forEach { it.spigot().sendMessage(message.build()) }
            Channel.VIP -> server.onlinePlayers.filter { it.hasPermission("chat.vip") }
                .forEach { it.spigot().sendMessage(message.build()) }
        }
    }
}
```

## 实用工具

### 消息模板系统
```kotlin
object MessageTemplates {
    
    private val templates = mapOf(
        "join" to "&#55FF55欢迎 {player} 加入服务器！",
        "quit" to "&#FF5555{player} 离开了服务器",
        "levelup" to "&#FFD700恭喜 {player} 升到了 {level} 级！",
        "achievement" to "&#00AAFF{player} 获得了成就：{achievement}！"
    )
    
    fun sendTemplate(templateId: String, placeholders: Map<String, String>) {
        val template = templates[templateId] ?: return
        var message = template
        
        placeholders.forEach { (key, value) ->
            message = message.replace("{$key}", value)
        }
        
        server.onlinePlayers.forEach { it.sendMessage(message) }
    }
    
    fun sendJoinMessage(player: Player) {
        sendTemplate("join", mapOf("player" to player.name))
    }
    
    fun sendLevelUpMessage(player: Player, level: Int) {
        sendTemplate("levelup", mapOf(
            "player" to player.name,
            "level" to level.toString()
        ))
    }
}
```

### 颜色代码转换
```kotlin
import taboolib.module.chat.colored

// 转换传统颜色代码到 RGB
fun convertLegacyColors(text: String): String {
    return text.replace("&", "§").colored()
}

// 移除所有颜色代码
fun stripColors(text: String): String {
    return text.replace(Regex("§[0-9a-fk-or]"), "")
        .replace(Regex("&#[0-9A-Fa-f]{6}"), "")
}

// 获取消息的纯文本长度
fun getPlainTextLength(text: String): Int {
    return stripColors(text).length
}
```

## 注意事项
- RGB 颜色需要 Minecraft 1.16+ 支持
- 某些客户端可能不支持所有特性
- 过多的特殊效果可能影响可读性
- Component 消息比普通字符串消息消耗更多资源
- 建议缓存常用的 Component 对象以提高性能