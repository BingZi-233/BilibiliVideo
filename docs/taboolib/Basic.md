# Basic 模块

## 概述
Basic 模块是 TabooLib 的基础模块，提供框架的核心功能和基本工具。这是任何 TabooLib 项目必须安装的第一个模块。

## 功能特性
- 提供基础的平台抽象
- 核心工具类和实用程序
- 插件生命周期管理
- 基础事件系统

## 安装配置
在 `build.gradle.kts` 中配置：
```kotlin
taboolib {
    env {
        install(Basic)
    }
}
```

## 基本用法

### 插件主类
使用 TabooLib 的插件对象模式：
```kotlin
import taboolib.common.platform.Plugin

object MyPlugin : Plugin() {
    override fun onEnable() {
        info("插件启动成功！")
    }
    
    override fun onDisable() {
        info("插件已关闭！")
    }
}
```

### 日志输出
```kotlin
import taboolib.common.platform.function.*

// 信息日志
info("这是信息日志")

// 警告日志
warning("这是警告日志")

// 错误日志
severe("这是错误日志")
```

## 注意事项
- Basic 模块必须最先安装
- 提供跨平台兼容性的基础支持
- 所有其他模块都依赖于 Basic 模块