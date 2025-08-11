package online.bingzi.bilibili.video.internal.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

/**
 * Bilibili Cookie 管理器
 * 支持按 Player/用户分离的 Cookie 存储和管理
 */
object BilibiliCookieJar : CookieJar {
    
    // 每个Player的Cookie存储 Map<PlayerUUID, Map<CookieName, Cookie>>
    private val playerCookieStores = ConcurrentHashMap<String, ConcurrentHashMap<String, Cookie>>()
    
    // 当前活动的Player UUID（线程本地存储）
    private val currentPlayerUuid = ThreadLocal<String>()
    
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val playerUuid = getCurrentPlayerUuid() ?: return
        val cookieStore = getOrCreateCookieStore(playerUuid)
        
        for (cookie in cookies) {
            cookieStore[cookie.name] = cookie
        }
    }
    
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val playerUuid = getCurrentPlayerUuid() ?: return emptyList()
        val cookieStore = getCookieStore(playerUuid) ?: return emptyList()
        
        return cookieStore.values.filter { cookie ->
            cookie.matches(url)
        }
    }
    
    /**
     * 设置当前活动的 Player
     * @param playerUuid Player的UUID字符串
     */
    fun setCurrentPlayer(playerUuid: String?) {
        if (playerUuid != null) {
            currentPlayerUuid.set(playerUuid)
        } else {
            currentPlayerUuid.remove()
        }
    }
    
    /**
     * 获取当前活动的 Player UUID
     */
    fun getCurrentPlayerUuid(): String? {
        return currentPlayerUuid.get()
    }
    
    /**
     * 为指定 Player 设置 Cookie
     * @param playerUuid Player的UUID
     * @param cookies Cookie 键值对
     */
    fun setCookies(playerUuid: String, cookies: Map<String, String>) {
        val cookieStore = getOrCreateCookieStore(playerUuid)
        cookies.forEach { (name, value) ->
            val cookie = Cookie.Builder()
                .name(name)
                .value(value)
                .domain(".bilibili.com")
                .path("/")
                .build()
            cookieStore[name] = cookie
        }
    }
    
    /**
     * 为当前活动 Player 设置 Cookie
     * @param cookies Cookie 键值对
     */
    fun setCookies(cookies: Map<String, String>) {
        val playerUuid = getCurrentPlayerUuid() ?: return
        setCookies(playerUuid, cookies)
    }
    
    /**
     * 获取指定 Player 的所有 Cookie
     * @param playerUuid Player的UUID
     * @return Cookie 键值对
     */
    fun getCookies(playerUuid: String): Map<String, String> {
        val cookieStore = getCookieStore(playerUuid) ?: return emptyMap()
        return cookieStore.mapValues { it.value.value }
    }
    
    /**
     * 获取当前活动 Player 的所有 Cookie
     * @return Cookie 键值对
     */
    fun getCookies(): Map<String, String> {
        val playerUuid = getCurrentPlayerUuid() ?: return emptyMap()
        return getCookies(playerUuid)
    }
    
    /**
     * 获取指定 Player 的指定名称的 Cookie
     * @param playerUuid Player的UUID
     * @param name Cookie 名称
     * @return Cookie 值，不存在则返回 null
     */
    fun getCookie(playerUuid: String, name: String): String? {
        val cookieStore = getCookieStore(playerUuid) ?: return null
        return cookieStore[name]?.value
    }
    
    /**
     * 获取当前活动 Player 的指定名称的 Cookie
     * @param name Cookie 名称
     * @return Cookie 值，不存在则返回 null
     */
    fun getCookie(name: String): String? {
        val playerUuid = getCurrentPlayerUuid() ?: return null
        return getCookie(playerUuid, name)
    }
    
    /**
     * 为指定 Player 设置单个 Cookie
     * @param playerUuid Player的UUID
     * @param name Cookie 名称
     * @param value Cookie 值
     */
    fun setCookie(playerUuid: String, name: String, value: String) {
        val cookieStore = getOrCreateCookieStore(playerUuid)
        val cookie = Cookie.Builder()
            .name(name)
            .value(value)
            .domain(".bilibili.com")
            .path("/")
            .build()
        cookieStore[name] = cookie
    }
    
    /**
     * 为当前活动 Player 设置单个 Cookie
     * @param name Cookie 名称
     * @param value Cookie 值
     */
    fun setCookie(name: String, value: String) {
        val playerUuid = getCurrentPlayerUuid() ?: return
        setCookie(playerUuid, name, value)
    }
    
    /**
     * 清除指定 Player 的所有 Cookie
     * @param playerUuid Player的UUID
     */
    fun clearCookies(playerUuid: String) {
        playerCookieStores.remove(playerUuid)
    }
    
    /**
     * 清除当前活动 Player 的所有 Cookie
     */
    fun clearCookies() {
        val playerUuid = getCurrentPlayerUuid() ?: return
        clearCookies(playerUuid)
    }
    
    /**
     * 移除指定 Player 的指定名称的 Cookie
     * @param playerUuid Player的UUID
     * @param name Cookie 名称
     */
    fun removeCookie(playerUuid: String, name: String) {
        val cookieStore = getCookieStore(playerUuid) ?: return
        cookieStore.remove(name)
    }
    
    /**
     * 移除当前活动 Player 的指定名称的 Cookie
     * @param name Cookie 名称
     */
    fun removeCookie(name: String) {
        val playerUuid = getCurrentPlayerUuid() ?: return
        removeCookie(playerUuid, name)
    }
    
    /**
     * 检查指定 Player 是否已登录
     * @param playerUuid Player的UUID
     * @return 是否已登录
     */
    fun isLoggedIn(playerUuid: String): Boolean {
        val cookieStore = getCookieStore(playerUuid) ?: return false
        return cookieStore.containsKey("DedeUserID") && 
               cookieStore.containsKey("SESSDATA") && 
               cookieStore.containsKey("bili_jct")
    }
    
    /**
     * 检查当前活动 Player 是否已登录
     * @return 是否已登录
     */
    fun isLoggedIn(): Boolean {
        val playerUuid = getCurrentPlayerUuid() ?: return false
        return isLoggedIn(playerUuid)
    }
    
    /**
     * 获取指定 Player 的用户 ID
     * @param playerUuid Player的UUID
     * @return 用户 ID，未登录则返回 null
     */
    fun getUserId(playerUuid: String): String? {
        return getCookie(playerUuid, "DedeUserID")
    }
    
    /**
     * 获取当前活动 Player 的用户 ID
     * @return 用户 ID，未登录则返回 null
     */
    fun getUserId(): String? {
        val playerUuid = getCurrentPlayerUuid() ?: return null
        return getUserId(playerUuid)
    }
    
    /**
     * 获取指定 Player 的 CSRF Token
     * @param playerUuid Player的UUID
     * @return CSRF Token，未登录则返回 null
     */
    fun getCsrfToken(playerUuid: String): String? {
        return getCookie(playerUuid, "bili_jct")
    }
    
    /**
     * 获取当前活动 Player 的 CSRF Token
     * @return CSRF Token，未登录则返回 null
     */
    fun getCsrfToken(): String? {
        val playerUuid = getCurrentPlayerUuid() ?: return null
        return getCsrfToken(playerUuid)
    }
    
    /**
     * 获取所有已存储Cookie的Player列表
     * @return Player UUID 列表
     */
    fun getAllPlayerUuids(): Set<String> {
        return playerCookieStores.keys.toSet()
    }
    
    /**
     * 获取或创建指定 Player 的 Cookie 存储
     */
    private fun getOrCreateCookieStore(playerUuid: String): ConcurrentHashMap<String, Cookie> {
        return playerCookieStores.getOrPut(playerUuid) { ConcurrentHashMap() }
    }
    
    /**
     * 获取指定 Player 的 Cookie 存储
     */
    private fun getCookieStore(playerUuid: String): ConcurrentHashMap<String, Cookie>? {
        return playerCookieStores[playerUuid]
    }
    
    /**
     * 清理所有 Cookie 数据（慎用）
     */
    fun clearAllCookies() {
        playerCookieStores.clear()
        currentPlayerUuid.remove()
    }
}