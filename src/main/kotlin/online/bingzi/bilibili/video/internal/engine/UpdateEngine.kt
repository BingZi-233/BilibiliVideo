package online.bingzi.bilibili.video.internal.engine

import okhttp3.ResponseBody
import online.bingzi.bilibili.video.internal.engine.drive.UpdateDrive
import online.bingzi.bilibili.video.internal.helper.infoMessageAsLang
import online.bingzi.bilibili.video.internal.helper.warningMessageAsLang
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Schedule
import taboolib.common.util.Version
import taboolib.module.configuration.Configuration
import taboolib.platform.util.bukkitPlugin
import java.util.*

/**
 * UpdateEngine对象负责检查插件的更新版本。
 * 它使用Retrofit库与GitHub上的API进行通信，定期检查是否有新版本可用。
 * 此类在插件启动时激活，并每12小时检查一次更新。
 */
object UpdateEngine {

    // 使用懒加载初始化updateAPI，配置Retrofit以便与更新驱动程序接口进行通信。
    private val updateAPI by lazy {
        Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/BingZi-233/BilibiliVideo/") // 基础URL
            .addConverterFactory(GsonConverterFactory.create()) // 添加Gson转换器
            .build()
            .create(UpdateDrive::class.java) // 创建UpdateDrive接口的实现
    }

    // 配置对象，用于存储插件的配置信息。
    lateinit var configuration: Configuration

    /**
     * 检查更新，每隔12小时检查一次新版本。
     * 此方法异步执行，并在成功响应后比较当前版本与GitHub上的版本。
     * 如果有新版本可用，将通过infoMessageAsLang发送通知。
     */
    @Schedule(async = true, period = 1000 * 60 * 60 * 12) // 定时任务，每12小时执行一次
    @Awake(LifeCycle.ACTIVE) // 在插件活动生命周期内执行
    fun checkUpdate() {
        // 发起对更新API的请求，获取最新版本信息
        updateAPI.getVersion().enqueue(object : retrofit2.Callback<ResponseBody> {
            // 当请求成功时的回调
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                // 检查响应是否成功
                if (response.isSuccessful) {
                    // 获取响应体并加载版本信息
                    response.body()?.let {
                        val properties = Properties() // 创建Properties对象用于加载版本信息
                        properties.load(it.byteStream()) // 从响应体中加载数据
                        val githubVersion = properties.getProperty("version") // 获取GitHub上的版本号
                        // 比较版本号，如果GitHub版本在当前版本之后，则提示更新
                        if (Version(githubVersion).isAfter(Version(bukkitPlugin.description.version))) {
                            githubVersion?.let { version -> infoMessageAsLang("UpdateNewVersion", version) } // 发送更新通知
                        }
                    } ?: warningMessageAsLang("UpdateCheckFailure", "无正文内容") // 响应体为空时的警告
                } else {
                    // 响应不成功时，发送警告信息
                    warningMessageAsLang("UpdateCheckFailure", response.errorBody()?.string() ?: "无信息")
                }
            }

            // 当请求失败时的回调
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                // 发送失败信息的警告
                warningMessageAsLang("UpdateCheckFailure", t.message ?: "无信息")
            }
        })
    }
}