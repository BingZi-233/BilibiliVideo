package online.bingzi.bilibili.video.internal.handler

/**
 * ApiType 枚举类
 *
 * 此枚举类定义了不同类型的 API 处理器。每种 API 类型都与一个特定的处理器关联。
 * 处理器的作用是处理特定的 API 请求，并将请求传递给下一个处理器（如果有的话）。
 *
 * @constructor 创建一个空的 ApiType 实例
 * @property apiHandler 该 API 类型的处理器
 */
enum class ApiType(val apiHandler: ApiHandler) {
    COINS(CoinsHandler()),          // 处理金币相关的 API 请求
    FAVOURED(FavouredHandler()),    // 处理收藏相关的 API 请求
    FOLLOWING(FollowingHandler()),  // 处理关注相关的 API 请求
    LIKE(LikeHandler());            // 处理点赞相关的 API 请求

    companion object {
        /**
         * 构建处理器
         * 根据指定的 API 类型顺序构建一个处理器链。
         * 第一个 API 类型的处理器将作为链的起始处理器。
         *
         * @param apiType 可变参数，包含多个 ApiType，代表处理器的顺序
         * @return 返回构建好的处理器链的起始处理器
         */
        fun buildHandler(vararg apiType: ApiType): ApiHandler {
            // 遍历传入的 apiType 数组，构建处理器链
            for (i in 0 until apiType.size - 1) {
                // 将当前处理器的下一个处理器设置为下一个 apiHandler
                apiType[i].apiHandler.setNextHandler(apiType[i + 1].apiHandler)
            }
            // 返回链的起始处理器
            return apiType[0].apiHandler
        }
    }
}