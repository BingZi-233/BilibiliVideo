package online.bingzi.bilibili.video.internal.service

import online.bingzi.bilibili.video.internal.bilibili.TripleActionApi
import online.bingzi.bilibili.video.internal.bilibili.dto.TripleStatusResult

/**
 * 三连检测服务层。
 */
object TripleCheckService {

    /**
     * 使用凭证字段，对某个稿件（bvid）进行三连状态检测。
     */
    fun checkTripleByBvid(
        sessData: String,
        biliJct: String,
        buvid3: String?,
        accessKey: String?,
        bvid: String
    ): TripleStatusResult {
        return TripleActionApi.queryTripleStatusByBvid(
            sessData = sessData,
            biliJct = biliJct,
            buvid3 = buvid3,
            accessKey = accessKey,
            bvid = bvid
        )
    }
}
