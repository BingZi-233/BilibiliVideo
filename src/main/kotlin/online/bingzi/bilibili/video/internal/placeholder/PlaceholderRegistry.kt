package online.bingzi.bilibili.video.internal.placeholder


/**
 * 反射注册 PlaceholderAPI 占位符！
 * @author MaddyJace
 */
internal object PlaceholderRegistry {
    fun init() {
        try {
            val clazz = Class.forName("online.bingzi.bilibili.video.internal.placeholder.PlaceholderExpansion")
            val expansion = clazz.getDeclaredConstructor().newInstance()
            val registerMethod = clazz.getMethod("register")
            registerMethod.invoke(expansion)
        } catch (_: Throwable) { }
    }
}