package online.bingzi.bilibili.video.internal.cache

import online.bingzi.bilibili.video.internal.config.SettingConfig
import taboolib.common5.Baffle
import java.util.concurrent.TimeUnit

// 定义一个全局变量 baffleCache，用于存储一个 Baffle 实例
// Baffle 是一个用于实现防抖机制的工具类，防止某个操作在短时间内被重复触发
// 通过从 SettingConfig 中读取 cooldown 配置，指定防抖的时间间隔
var baffleCache = Baffle.of(SettingConfig.cooldown, TimeUnit.SECONDS)