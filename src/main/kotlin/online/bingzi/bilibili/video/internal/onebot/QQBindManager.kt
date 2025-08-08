package online.bingzi.bilibili.video.internal.onebot

import online.bingzi.bilibili.video.internal.cache.VerificationCodeCache
import online.bingzi.bilibili.video.internal.database.Database
import online.bingzi.bilibili.video.internal.entity.QQBindData
import online.bingzi.bilibili.video.internal.entity.QQBindResult
import online.bingzi.bilibili.video.internal.entity.VerificationCode
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.module.database.ColumnTypeSQLite
import taboolib.module.database.Table
import taboolib.module.database.getHost
import taboolib.platform.util.bukkitPlugin
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * QQ绑定管理器
 * 
 * 管理玩家与QQ的绑定关系
 * 
 * @author BingZi-233
 * @since 2.0.0
 */
object QQBindManager {
    
    /**
     * 数据库host
     */
    private val host = File(bukkitPlugin.dataFolder, "qq_bind.db").getHost()
    
    /**
     * 数据溓
     */
    private val dataSource = host.createDataSource()
    
    /**
     * 数据库表
     */
    private val table = Table("bilibili_video_qq_bind", host) {
        add { id() }
        add("player_uuid") {
            type(ColumnTypeSQLite.TEXT, 36)
        }
        add("player_name") {
            type(ColumnTypeSQLite.TEXT, 32)
        }
        add("qq_number") {
            type(ColumnTypeSQLite.INTEGER)
        }
        add("bind_time") {
            type(ColumnTypeSQLite.TEXT)
        }
        add("enabled") {
            type(ColumnTypeSQLite.INTEGER)
        }
        add("bilibili_mid") {
            type(ColumnTypeSQLite.TEXT, 32)
        }
        add("last_used") {
            type(ColumnTypeSQLite.TEXT)
        }
    }
    
    /**
     * 内存缓存
     * key: 玩家UUID
     * value: QQ绑定数据
     */
    private val bindingCache = mutableMapOf<UUID, QQBindData>()
    
    /**
     * QQ到玩家的映射
     * key: QQ号
     * value: 玩家UUID
     */
    private val qqToPlayerMap = mutableMapOf<Long, UUID>()
    
    /**
     * 初始化
     */
    fun initialize() {
        // 创建表
        table.workspace(dataSource) { createTable() }.run()
        
        // 加载所有绑定数据到内存
        loadAllBindings()
        
        info("QQ绑定管理器初始化完成，已加载 ${bindingCache.size} 条绑定记录")
    }
    
    /**
     * 申请绑定
     * 
     * @param player 玩家
     * @return 验证码，如果失败则返回null
     */
    fun requestBinding(player: ProxyPlayer): String? {
        // 检查是否已绑定
        if (hasBinding(player.uniqueId)) {
            return null
        }
        
        // 检查冷却时间
        if (VerificationCodeCache.isInCooldown(player.uniqueId)) {
            val remaining = VerificationCodeCache.getCooldownRemaining(player.uniqueId)
            player.sendMessage("§c请等待 $remaining 秒后再次申请验证码")
            return null
        }
        
        // 生成验证码
        return VerificationCodeCache.generateCode(player.uniqueId, player.name)
    }
    
    /**
     * 验证并绑定
     * 
     * @param qqNumber QQ号
     * @param code 验证码
     * @return 绑定结果
     */
    fun verifyAndBind(qqNumber: Long, code: String): QQBindResult {
        // 验证验证码
        val verificationCode = VerificationCodeCache.verifyAndGet(code)
            ?: return QQBindResult.CODE_INVALID
        
        if (!verificationCode.isValid()) {
            return if (verificationCode.isExpired()) {
                QQBindResult.CODE_EXPIRED
            } else {
                QQBindResult.CODE_INVALID
            }
        }
        
        // 检查QQ是否已绑定其他账号
        if (qqToPlayerMap.containsKey(qqNumber)) {
            return QQBindResult.ALREADY_BOUND
        }
        
        // 创建绑定数据
        val bindData = QQBindData(
            playerUuid = verificationCode.playerUuid,
            playerName = verificationCode.playerName,
            qqNumber = qqNumber,
            bindTime = LocalDateTime.now()
        )
        
        // 保存到数据库
        if (!saveBinding(bindData)) {
            return QQBindResult.DATABASE_ERROR
        }
        
        // 更新缓存
        bindingCache[verificationCode.playerUuid] = bindData
        qqToPlayerMap[qqNumber] = verificationCode.playerUuid
        
        info("玩家 ${verificationCode.playerName} (${verificationCode.playerUuid}) 成功绑定QQ: $qqNumber")
        
        return QQBindResult.SUCCESS
    }
    
    /**
     * 解除绑定
     * 
     * @param playerUuid 玩家UUID
     * @return 是否成功
     */
    fun unbind(playerUuid: UUID): Boolean {
        val binding = bindingCache[playerUuid] ?: return false
        
        // 从数据库删除
        val result = table.workspace(dataSource) {
            delete { where { "player_uuid" eq playerUuid.toString() } }
        }.run()
        
        if (result > 0) {
            // 更新缓存
            bindingCache.remove(playerUuid)
            qqToPlayerMap.remove(binding.qqNumber)
            
            info("玩家 ${binding.playerName} 解除QQ绑定: ${binding.qqNumber}")
            return true
        }
        
        return false
    }
    
    /**
     * 解除QQ绑定
     * 
     * @param qqNumber QQ号
     * @return 是否成功
     */
    fun unbindByQQ(qqNumber: Long): Boolean {
        val playerUuid = qqToPlayerMap[qqNumber] ?: return false
        return unbind(playerUuid)
    }
    
    /**
     * 获取玩家的绑定信息
     * 
     * @param playerUuid 玩家UUID
     * @return 绑定信息，如果未绑定则返回null
     */
    fun getBinding(playerUuid: UUID): QQBindData? {
        return bindingCache[playerUuid]
    }
    
    /**
     * 通过QQ号获取绑定信息
     * 
     * @param qqNumber QQ号
     * @return 绑定信息，如果未绑定则返回null
     */
    fun getBindingByQQ(qqNumber: Long): QQBindData? {
        val playerUuid = qqToPlayerMap[qqNumber] ?: return null
        return bindingCache[playerUuid]
    }
    
    /**
     * 检查玩家是否已绑定
     * 
     * @param playerUuid 玩家UUID
     * @return 是否已绑定
     */
    fun hasBinding(playerUuid: UUID): Boolean {
        return bindingCache.containsKey(playerUuid)
    }
    
    /**
     * 检查QQ是否已绑定
     * 
     * @param qqNumber QQ号
     * @return 是否已绑定
     */
    fun hasBindingByQQ(qqNumber: Long): Boolean {
        return qqToPlayerMap.containsKey(qqNumber)
    }
    
    /**
     * 更新Bilibili MID
     * 
     * @param playerUuid 玩家UUID
     * @param mid Bilibili MID
     * @return 是否成功
     */
    fun updateBilibiliMid(playerUuid: UUID, mid: String): Boolean {
        val binding = bindingCache[playerUuid] ?: return false
        
        // 更新数据库
        val result = table.workspace(dataSource) {
            update {
                set("bilibili_mid", mid)
                where { "player_uuid" eq playerUuid.toString() }
            }
        }.run()
        
        if (result > 0) {
            // 更新缓存
            bindingCache[playerUuid] = binding.copy(bilibiliMid = mid)
            return true
        }
        
        return false
    }
    
    /**
     * 更新最后使用时间
     * 
     * @param playerUuid 玩家UUID
     */
    fun updateLastUsed(playerUuid: UUID) {
        val binding = bindingCache[playerUuid] ?: return
        
        val now = LocalDateTime.now()
        
        // 更新数据库
        table.workspace(dataSource) {
            update {
                set("last_used", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                where { "player_uuid" eq playerUuid.toString() }
            }
        }.run()
        
        // 更新缓存
        bindingCache[playerUuid] = binding.copy(lastUsed = now)
    }
    
    /**
     * 保存绑定到数据库
     * 
     * @param bindData 绑定数据
     * @return 是否成功
     */
    private fun saveBinding(bindData: QQBindData): Boolean {
        return try {
            table.workspace(dataSource) {
                insert(
                    "player_uuid", "player_name", "qq_number", "bind_time", "enabled", "bilibili_mid", "last_used"
                ) {
                    value(
                        bindData.playerUuid.toString(),
                        bindData.playerName,
                        bindData.qqNumber,
                        bindData.bindTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        if (bindData.enabled) 1 else 0,
                        bindData.bilibiliMid,
                        bindData.lastUsed?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    )
                }
            }.run()
            true
        } catch (e: Exception) {
            warning("保存QQ绑定失败: ${e.message}")
            false
        }
    }
    
    /**
     * 从数据库加载所有绑定
     */
    private fun loadAllBindings() {
        bindingCache.clear()
        qqToPlayerMap.clear()
        
        table.select(dataSource) {
            // 选择所有记录
        }.forEach {
            val bindData = QQBindData(
                playerUuid = UUID.fromString(getString("player_uuid")),
                playerName = getString("player_name"),
                qqNumber = getLong("qq_number"),
                bindTime = LocalDateTime.parse(getString("bind_time"), DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                enabled = getInt("enabled") == 1,
                bilibiliMid = getString("bilibili_mid"),
                lastUsed = getString("last_used")?.let { lastUsedStr ->
                    LocalDateTime.parse(lastUsedStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                }
            )
            
            bindingCache[bindData.playerUuid] = bindData
            qqToPlayerMap[bindData.qqNumber] = bindData.playerUuid
        }
    }
    
    /**
     * 重新加载绑定数据
     */
    fun reload() {
        loadAllBindings()
        info("重新加载QQ绑定数据，共 ${bindingCache.size} 条记录")
    }
}