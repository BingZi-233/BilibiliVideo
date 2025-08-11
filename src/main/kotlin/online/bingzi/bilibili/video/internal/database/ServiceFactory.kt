package online.bingzi.bilibili.video.internal.database

/**
 * 服务工厂
 */
object ServiceFactory {

    /**
     * 获取分步绑定服务实例（推荐使用）
     */
    fun getStepwiseBindingService(): StepwiseBindingService {
        return StepwiseBindingServiceImpl
    }

    /**
     * 获取数据访问服务实例（高级用法）
     */
    fun getPlayerDataService(): PlayerDataService {
        return PlayerDataServiceImpl
    }
}