# TabooLib事件系统完整指南

本文档总结了TabooLib事件系统的使用方法，为基于TabooLib开发的插件提供事件驱动架构指导。

## 🎯 事件系统架构

TabooLib采用双层事件系统：
- **内部事件** (`InternalEventBus`) - 用于插件内部事件处理
- **平台事件** (`EventBus`) - 跨平台事件监听（Bukkit/BungeeCord/Velocity等）

### 工作原理

TabooLib的事件系统分为两部分：内部事件和平台事件。

#### 内部事件
内部事件通过`InternalEventBus`接口进行管理。`InternalEventBus`的默认实现维护一个`registeredListeners`的`ConcurrentHashMap`，用于存储不同事件类型及其对应的监听器。

- **监听事件**: 当调用`InternalEventBus.listen()`方法时，会创建一个`RegisteredListener`实例并将其添加到`registeredListeners`中，根据事件类型和优先级进行存储。
- **触发事件**: 调用`InternalEventBus.call(event)`方法时，系统会遍历对应事件类型的所有注册监听器，并根据`ignoreCancelled`属性和事件是否可取消来决定是否执行监听器。

#### 平台事件
`EventBus`类作为`ClassVisitor`，在TabooLib启动时扫描带有`@SubscribeEvent`注解的方法。它会根据当前运行的平台（如Bukkit, Bungee, Velocity, AfyBroker），将事件注册到相应的平台事件管理器中。

## 📝 编写事件监听器

### 1. 使用`@SubscribeEvent`注解

```kotlin
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import org.bukkit.event.player.PlayerJoinEvent

object MyEventListener {
    
    @SubscribeEvent(priority = EventPriority.NORMAL, ignoreCancelled = false)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        event.player.sendMessage("欢迎加入服务器，${event.player.name}!")
    }
    
    @SubscribeEvent
    fun onCustomEvent(event: MyCustomEvent) {
        println("自定义事件被触发: ${event.message}")
    }
}
```

### 2. 注解参数说明

- `priority`: 事件优先级 (`LOWEST`, `LOW`, `NORMAL`, `HIGH`, `HIGHEST`, `MONITOR`)
- `ignoreCancelled`: 是否忽略已取消的事件，默认为`false`

## 🔧 创建自定义事件

### 1. 普通事件

```kotlin
import taboolib.common.event.InternalEvent

class MyCustomEvent(val message: String) : InternalEvent()
```

### 2. 可取消事件

```kotlin
import taboolib.common.event.CancelableInternalEvent

class MyCancelableCustomEvent(val value: Int) : CancelableInternalEvent()
```

## 🚀 触发自定义事件

```kotlin
import taboolib.common.event.InternalEventBus

// 触发普通事件
InternalEventBus.call(MyCustomEvent("Hello TabooLib!"))

// 触发可取消事件
val event = MyCancelableCustomEvent(15)
InternalEventBus.call(event)
if (event.isCancelled) {
    println("事件被取消了")
}
```

## 📋 完整使用示例

```kotlin
import taboolib.common.event.InternalEvent
import taboolib.common.event.CancelableInternalEvent
import taboolib.common.event.InternalEventBus
import taboolib.common.platform.event.SubscribeEvent

// 定义自定义事件
class PlayerCustomActionEvent(
    val player: org.bukkit.entity.Player, 
    var actionMessage: String
) : CancelableInternalEvent()

// 事件监听器
object MyPluginListener {
    
    @SubscribeEvent
    fun onPlayerJoin(event: org.bukkit.event.player.PlayerJoinEvent) {
        event.player.sendMessage("Hello from TabooLib!")
        
        // 触发自定义事件
        val customEvent = PlayerCustomActionEvent(event.player, "玩家加入")
        InternalEventBus.call(customEvent)
    }
    
    @SubscribeEvent
    fun onCustomAction(event: PlayerCustomActionEvent) {
        println("玩家 ${event.player.name} 执行了动作: ${event.actionMessage}")
        
        // 可以取消事件
        if (event.actionMessage.contains("禁止")) {
            event.isCancelled = true
        }
    }
}

// 触发事件的工具类
object EventTrigger {
    
    fun triggerCustomEvent(player: org.bukkit.entity.Player, message: String) {
        val event = PlayerCustomActionEvent(player, message)
        InternalEventBus.call(event)
        
        if (event.isCancelled) {
            player.sendMessage("您的动作被取消了: ${event.actionMessage}")
        }
    }
}
```

## 🎮 内部事件监听

除了注解方式，还可以使用编程方式监听内部事件：

```kotlin
// 注册监听器
InternalEventBus.listen(MyCustomEvent::class.java) { event ->
    println("监听器1收到: ${event.message}")
}

// 带优先级的监听器
InternalEventBus.listen(MyCustomEvent::class.java, priority = 1) { event ->
    println("高优先级监听器收到: ${event.message}")
}
```

## 🔍 特殊注解

使用`@Ghost`注解抑制事件类未找到时的警告：

```kotlin
@SubscribeEvent
@Ghost
fun onOptionalEvent(event: SomeOptionalEvent) {
    // 处理可能不存在的事件
}
```

## 💡 最佳实践

1. **事件监听器组织**: 使用`object`创建监听器类
2. **事件命名**: 自定义事件使用描述性名称
3. **错误处理**: 在事件处理中添加适当的异常处理
4. **性能考虑**: 避免在高频事件中执行耗时操作
5. **取消机制**: 合理使用事件取消功能

## 📚 参考资料

- TabooLib官方文档: https://docs.tabooproject.org
- TabooLib GitHub仓库: https://github.com/TabooLib/taboolib
- 社区文档和资源集合

---

*本文档基于TabooLib 6.2版本编写，适用于2025年最新版本的TabooLib框架。*