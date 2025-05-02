package online.bingzi.bilibili.video.internal.cache

import online.bingzi.bilibili.video.internal.engine.NetworkEngine

val buvid3Cache: String by lazy {
    NetworkEngine.bilibiliAPI.getBuvid3().execute().body()?.data?.buVid ?: ""
}