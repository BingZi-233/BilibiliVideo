package online.bingzi.bilibili.video.internal.cache

import java.util.concurrent.ConcurrentHashMap

/**
 * QRCodeCache 是一个用于存储二维码相关信息的缓存类。
 * 该类使用线程安全的 ConcurrentHashMap 来存储二维码的键值对，
 * 键为 String 类型的二维码标识，值为与该二维码相关联的字符串列表。
 * 主要用于提高二维码数据的获取效率，避免重复计算或请求。
 */
val qrCodeKeyCache: ConcurrentHashMap<String, List<String>> = ConcurrentHashMap()