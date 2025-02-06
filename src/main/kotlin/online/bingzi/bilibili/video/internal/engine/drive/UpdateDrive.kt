package online.bingzi.bilibili.video.internal.engine.drive

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET

/**
 * UpdateDrive 接口
 *
 * 该接口定义了与版本更新相关的网络请求方法。主要用于获取应用程序的版本信息。
 * 通过 Retrofit 库进行网络调用，返回的结果将用于应用的版本更新逻辑。
 */
interface UpdateDrive {

    /**
     * 获取版本信息的方法
     *
     * 该方法通过发起一个 GET 请求来获取 gradle.properties 文件中的版本信息。
     * 返回一个 Call<ResponseBody> 对象，调用者可以使用这个对象来异步处理网络请求的结果。
     *
     * @return Call<ResponseBody> 返回一个表示请求的 Call 对象，调用者可以通过此对象获取请求的响应内容。
     *
     * @throws IOException 可能在网络请求过程中抛出 IOException，表明网络连接问题。
     */
    @GET("main/gradle.properties")
    fun getVersion(): Call<ResponseBody>
}