# OneBot 客户端使用指南

本文档详细介绍如何在 Java/Kotlin 项目中使用 OneBot 协议客户端，实现与 QQ 机器人的通信功能。

## OneBot 协议简介

OneBot 是一个统一的聊天机器人应用接口标准，允许开发者通过标准化的 API 与各种聊天平台的机器人进行交互。它支持 HTTP 请求和 WebSocket 连接两种通信方式，提供私聊、群聊、临时会话等多种消息发送功能。

## 环境准备

### 前置条件

1. **OneBot 服务端**: 需要运行一个 OneBot11 协议兼容的服务器，如：
   - NapCat
   - go-cqhttp
   - Mirai + OneBot 插件
   - 其他 OneBot11 标准实现

2. **Java 环境**: JDK 8 或更高版本

### 依赖引入

#### Maven 项目

在 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>cn.evole</groupId>
    <artifactId>onebot-client</artifactId>
    <version>0.4.3</version>
</dependency>
```

#### Gradle 项目（Kotlin DSL）

在 `build.gradle.kts` 中添加依赖：

```kotlin
dependencies {
    implementation("cn.evole:onebot-client:0.4.3")
}
```

#### Gradle 项目（Groovy DSL）

在 `build.gradle` 中添加依赖：

```groovy
dependencies {
    implementation 'cn.evole:onebot-client:0.4.3'
}
```

## 快速开始

### 1. 创建和配置客户端

#### 基础配置

```java
import cn.evole.onebot.client.OneBotClient;
import cn.evole.onebot.client.core.BotConfig;

// 仅指定 WebSocket URL
BotConfig config = new BotConfig("ws://127.0.0.1:8080");

// 创建客户端并建立连接
OneBotClient client = OneBotClient.create(config).open();
```

#### 带访问令牌的配置

```java
// 指定 WebSocket URL 和访问令牌
BotConfig config = new BotConfig("ws://127.0.0.1:8080", "your_access_token");
OneBotClient client = OneBotClient.create(config).open();
```

#### 指定机器人 ID

```java
// 指定 WebSocket URL 和机器人 ID
BotConfig config = new BotConfig("ws://127.0.0.1:8080", 123456789L);
OneBotClient client = OneBotClient.create(config).open();
```

### 2. 发送消息

#### 发送群聊消息

```java
import cn.evole.onebot.client.util.MsgUtils;

// 发送文本消息
client.getBot().sendGroupMsg(987654321L, "Hello, World!", true);

// 发送复杂消息（使用 MsgUtils 构建）
client.getBot().sendGroupMsg(987654321L, 
    MsgUtils.builder().text("你好！").build(), 
    true
);
```

#### 发送私聊消息

```java
// 发送私聊消息
client.getBot().sendPrivateMsg(123456789L, "私聊消息", true);
```

#### 发送临时会话消息

```java
// 通过群聊发起临时会话
client.getBot().sendPrivateMsg(987654321L, 123456789L, "临时会话消息", true);
```

### 3. 事件监听

#### 创建事件监听器

```java
import cn.evole.onebot.client.listener.Listener;
import cn.evole.onebot.client.annotation.SubscribeEvent;
import cn.evole.onebot.client.event.message.GroupMessageEvent;
import cn.evole.onebot.client.event.message.PrivateMessageEvent;

public class EventListener implements Listener {
    
    @SubscribeEvent
    public void onGroupMessage(GroupMessageEvent event) {
        System.out.println("收到群消息: " + event.getMessage());
        System.out.println("发送者: " + event.getSender().getNickname());
        System.out.println("群号: " + event.getGroupId());
        
        // 回复消息
        event.reply("我收到了你的消息：" + event.getMessage());
    }
    
    @SubscribeEvent
    public void onPrivateMessage(PrivateMessageEvent event) {
        System.out.println("收到私聊消息: " + event.getMessage());
        System.out.println("发送者ID: " + event.getUserId());
        
        // 回复消息
        event.reply("私聊回复：" + event.getMessage());
    }
}
```

#### 注册事件监听器

```java
// 创建客户端并注册事件监听器
OneBotClient client = OneBotClient.create(new BotConfig("ws://127.0.0.1:8080"))
        .open()
        .registerEvents(new EventListener());
```

## 常用 API

### 消息相关

```java
// 撤回消息
client.getBot().deleteMsg(messageId);

// 发送合并转发消息
List<Map<String, Object>> forwardMsg = Arrays.asList(/* 转发消息列表 */);
client.getBot().sendGroupForwardMsg(groupId, forwardMsg);
```

### 群管理功能

```java
// 群禁言（duration 为禁言时长，单位秒，0 为解除禁言）
client.getBot().setGroupBan(groupId, userId, 600);

// 群全体禁言
client.getBot().setGroupWholeBan(groupId, true);

// 设置群名片
client.getBot().setGroupCard(groupId, userId, "新名片");

// 踢出群成员
client.getBot().setGroupKick(groupId, userId, false);
```

### 信息获取

```java
// 获取登录号信息
client.getBot().getLoginInfo();

// 获取好友列表
client.getBot().getFriendList();

// 获取群列表
client.getBot().getGroupList();

// 获取群成员信息
GroupMemberInfoResp memberInfo = client.getBot()
    .getGroupMemberInfo(groupId, userId, false)
    .getData();

// 获取群信息
client.getBot().getGroupInfo(groupId, false);

// 获取群@全体成员剩余次数
client.getBot().getGroupAtAllRemain(groupId);
```

### 文件操作

```java
// 上传群文件
client.getBot().uploadGroupFile(groupId, "/path/to/file", "filename", "folder");

// 获取群文件信息
client.getBot().getGroupFilesByFolder(groupId, "folder_id");
```

### 请求处理

```java
// 处理加好友请求
client.getBot().setFriendAddRequest("request_flag", true, "备注信息");

// 处理加群请求/邀请
client.getBot().setGroupAddRequest("request_flag", "add", true, "拒绝理由");
```

### 其他功能

```java
// 图片 OCR 识别
client.getBot().ocrImage("image_file_path_or_url");

// 获取图片信息
client.getBot().getImage("image_file_name");
```

## 消息构建

使用 `MsgUtils` 工具类构建复杂消息：

```java
import cn.evole.onebot.client.util.MsgUtils;

// 构建包含文本、图片、@的复杂消息
ArrayMsg message = MsgUtils.builder()
    .at(123456789L)           // @某人
    .text(" 你好！\n")        // 文本
    .image("image_url")       // 图片
    .text("这是一条测试消息")  // 更多文本
    .build();

client.getBot().sendGroupMsg(groupId, message, true);
```

### 消息类型示例

```java
// 纯文本消息
MsgUtils.builder().text("Hello World").build();

// @消息
MsgUtils.builder().at(userId).text(" 你被@了").build();

// 图片消息
MsgUtils.builder().image("http://example.com/image.jpg").build();

// 表情消息
MsgUtils.builder().face(123).build(); // 表情ID

// 语音消息
MsgUtils.builder().record("voice_file_path").build();

// 回复消息
MsgUtils.builder().reply(originalMessageId).text("这是回复").build();
```

## 完整示例

### 简单的机器人示例

```java
import cn.evole.onebot.client.OneBotClient;
import cn.evole.onebot.client.core.BotConfig;
import cn.evole.onebot.client.listener.Listener;
import cn.evole.onebot.client.annotation.SubscribeEvent;
import cn.evole.onebot.client.event.message.GroupMessageEvent;
import cn.evole.onebot.client.event.message.PrivateMessageEvent;
import cn.evole.onebot.client.util.MsgUtils;

public class SimpleBot {
    
    public static void main(String[] args) {
        // 创建机器人客户端
        OneBotClient client = OneBotClient.create(
            new BotConfig("ws://127.0.0.1:8080", "your_token")
        )
        .open()
        .registerEvents(new BotEventListener());
        
        // 发送启动消息
        client.getBot().sendGroupMsg(987654321L, "机器人已启动！", true);
        
        // 保持程序运行
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    public static class BotEventListener implements Listener {
        
        @SubscribeEvent
        public void onGroupMessage(GroupMessageEvent event) {
            String message = event.getMessage();
            long groupId = event.getGroupId();
            long userId = event.getUserId();
            
            // 处理特定指令
            if (message.equals("/hello")) {
                event.reply("Hello! " + event.getSender().getNickname());
            }
            else if (message.equals("/time")) {
                event.reply("当前时间：" + java.time.LocalDateTime.now());
            }
            else if (message.startsWith("/echo ")) {
                String content = message.substring(6);
                event.reply("你说：" + content);
            }
        }
        
        @SubscribeEvent  
        public void onPrivateMessage(PrivateMessageEvent event) {
            // 私聊消息处理
            if (event.getMessage().equals("/info")) {
                event.reply("我是一个简单的机器人！");
            }
        }
    }
}
```

### Kotlin 示例

```kotlin
import cn.evole.onebot.client.OneBotClient
import cn.evole.onebot.client.core.BotConfig
import cn.evole.onebot.client.listener.Listener
import cn.evole.onebot.client.annotation.SubscribeEvent
import cn.evole.onebot.client.event.message.GroupMessageEvent
import cn.evole.onebot.client.util.MsgUtils

class KotlinBot {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val client = OneBotClient.create(
                BotConfig("ws://127.0.0.1:8080")
            )
            .open()
            .registerEvents(BotListener())
            
            // 发送启动消息
            client.bot.sendGroupMsg(987654321L, "Kotlin 机器人启动！", true)
        }
    }
    
    class BotListener : Listener {
        @SubscribeEvent
        fun onGroupMessage(event: GroupMessageEvent) {
            when (event.message) {
                "/kotlin" -> {
                    event.reply("Hello from Kotlin!")
                }
                "/info" -> {
                    val msg = MsgUtils.builder()
                        .at(event.userId)
                        .text(" 这是一个 Kotlin 机器人")
                        .build()
                    event.reply(msg)
                }
            }
        }
    }
}
```

## 最佳实践

### 1. 连接管理

```java
public class BotManager {
    private OneBotClient client;
    private final BotConfig config;
    
    public BotManager(String url, String token) {
        this.config = new BotConfig(url, token);
    }
    
    public void connect() {
        try {
            client = OneBotClient.create(config).open();
            System.out.println("机器人连接成功");
        } catch (Exception e) {
            System.err.println("连接失败: " + e.getMessage());
        }
    }
    
    public void disconnect() {
        if (client != null) {
            client.close();
            client = null;
        }
    }
    
    public boolean isConnected() {
        return client != null && client.getBot() != null;
    }
}
```

### 2. 异常处理

```java
@SubscribeEvent
public void onGroupMessage(GroupMessageEvent event) {
    try {
        // 消息处理逻辑
        processMessage(event);
    } catch (Exception e) {
        System.err.println("处理消息时出错: " + e.getMessage());
        e.printStackTrace();
        
        // 可选：发送错误提示
        try {
            event.reply("处理消息时出现错误，请稍后再试");
        } catch (Exception ignored) {
            // 忽略回复失败的异常
        }
    }
}
```

### 3. 配置管理

```java
import java.util.Properties;
import java.io.FileInputStream;

public class BotConfig {
    private final Properties props;
    
    public BotConfig(String configFile) throws Exception {
        props = new Properties();
        props.load(new FileInputStream(configFile));
    }
    
    public String getWebSocketUrl() {
        return props.getProperty("onebot.url", "ws://127.0.0.1:8080");
    }
    
    public String getAccessToken() {
        return props.getProperty("onebot.token", "");
    }
    
    public long getBotId() {
        return Long.parseLong(props.getProperty("onebot.bot_id", "0"));
    }
}
```

### 4. 日志记录

```java
import java.util.logging.Logger;
import java.util.logging.Level;

public class BotEventListener implements Listener {
    private static final Logger logger = Logger.getLogger(BotEventListener.class.getName());
    
    @SubscribeEvent
    public void onGroupMessage(GroupMessageEvent event) {
        logger.info(String.format("收到群消息 [%d] %s: %s", 
            event.getGroupId(), 
            event.getSender().getNickname(), 
            event.getMessage()));
            
        // 处理逻辑...
    }
}
```

## 常见问题

### 1. 连接失败

**问题**: WebSocket 连接失败或超时

**解决方案**:
- 检查 OneBot 服务是否正常运行
- 确认 URL 和端口号是否正确
- 检查防火墙设置
- 验证访问令牌是否正确

### 2. 消息发送失败

**问题**: 调用发送消息 API 没有效果

**解决方案**:
- 确认机器人是否在目标群中
- 检查机器人是否被禁言
- 验证群号和用户 ID 是否正确
- 查看 OneBot 服务端日志

### 3. 事件监听不生效

**问题**: `@SubscribeEvent` 注解的方法没有被调用

**解决方案**:
- 确认事件监听器已通过 `registerEvents()` 注册
- 检查方法签名是否正确
- 确认事件类型是否匹配
- 验证监听器类是否实现了 `Listener` 接口

### 4. 内存泄漏

**问题**: 长时间运行后内存占用过高

**解决方案**:
- 及时处理和释放大型消息对象
- 避免在事件处理器中持有大量对象引用
- 定期清理缓存数据
- 合理设置 JVM 内存参数

## 参考资源

- [OneBot 官方文档](https://onebot.dev/)
- [OneBot Client GitHub 仓库](https://github.com/cnlimiter/onebot-client)
- [OneBot 11 标准 API](https://11.onebot.dev/)
- [NapCat 文档](https://napneko.github.io/NapCatQQ/)

---

*本文档会持续更新，欢迎提交问题和改进建议。*