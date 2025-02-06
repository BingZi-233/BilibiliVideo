package online.bingzi.bilibili.video.internal.cache

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import online.bingzi.bilibili.video.internal.database.Database.Companion.getPlayerDataContainer
import online.bingzi.bilibili.video.internal.entity.CookieData
import java.util.*
import java.util.concurrent.TimeUnit

// GSON序列化组件，用于将对象转换为JSON格式
val gson: Gson = Gson()

// 定义一个TypeToken，用于表示List<String>的类型
private val listStringType = object : TypeToken<List<String>>() {}.type

/**
 * Cookie缓存
 * 该缓存用于存储和管理Cookie数据，以便在需要时快速访问。
 */
val cookieCache = Caffeine.newBuilder()
    // 设置缓存的最大大小为100个条目
    .maximumSize(100)
    // 设置在写入后5分钟内可以刷新缓存
    .refreshAfterWrite(5, TimeUnit.MINUTES)
    // 创建缓存，缓存的键为UUID，值为CookieData
    .build<UUID, CookieData> {
        // 创建一个新的CookieData实例
        val cookieData = CookieData()

        // 从数据库中获取名为"SESSDATA"的Cookie数据并赋值给cookieData的sessData属性
        it.getPlayerDataContainer("SESSDATA")?.let { cookieData.sessData = it }
        // 从数据库中获取名为"bili_jct"的Cookie数据并赋值给cookieData的biliJct属性
        it.getPlayerDataContainer("bili_jct")?.let { cookieData.biliJct = it }
        // 从数据库中获取名为"DedeUserID"的Cookie数据并赋值给cookieData的dedeUserID属性
        it.getPlayerDataContainer("DedeUserID")?.let { cookieData.dedeUserID = it }
        // 从数据库中获取名为"DedeUserID__ckMd5"的Cookie数据并赋值给cookieData的dedeUserIDCkMd5属性
        it.getPlayerDataContainer("DedeUserID__ckMd5")?.let { cookieData.dedeUserIDCkMd5 = it }
        // 从数据库中获取名为"sid"的Cookie数据并赋值给cookieData的sid属性
        it.getPlayerDataContainer("sid")?.let { cookieData.sid = it }

        // 返回填充好的cookieData
        cookieData
    }