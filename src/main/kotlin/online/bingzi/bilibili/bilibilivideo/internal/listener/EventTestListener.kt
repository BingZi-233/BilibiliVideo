package online.bingzi.bilibili.bilibilivideo.internal.listener

import online.bingzi.bilibili.bilibilivideo.api.event.BilibiliLoginEvent
import online.bingzi.bilibili.bilibilivideo.api.event.BilibiliLogoutEvent
import online.bingzi.bilibili.bilibilivideo.api.event.UpFollowStatusCheckEvent
import online.bingzi.bilibili.bilibilivideo.api.event.VideoTripleStatusCheckEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.info
import java.text.SimpleDateFormat
import java.util.*

object EventTestListener {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    
    @SubscribeEvent
    fun onBilibiliLogin(event: BilibiliLoginEvent) {
        val timestamp = dateFormat.format(Date())
        
        info("================== Bilibili 登录事件 ==================")
        info("时间: $timestamp")
        info("玩家: ${event.player.name} (UUID: ${event.player.uniqueId})")
        info("MID: ${event.getMid()}")
        info("昵称: ${event.getNickname()}")
        info("登录时间: ${dateFormat.format(Date(event.getLoginTime()))}")
        info("会话信息: ${event.session}")
        info("====================================================")
    }
    
    @SubscribeEvent
    fun onBilibiliLogout(event: BilibiliLogoutEvent) {
        val timestamp = dateFormat.format(Date())
        val sessionDuration = event.getSessionDuration()
        val durationMinutes = sessionDuration / 1000 / 60
        val durationSeconds = (sessionDuration / 1000) % 60
        
        info("================== Bilibili 登出事件 ==================")
        info("时间: $timestamp")
        info("玩家: ${event.player.name} (UUID: ${event.player.uniqueId})")
        info("MID: ${event.getMid()}")
        info("昵称: ${event.getNickname()}")
        info("会话时长: ${durationMinutes}分${durationSeconds}秒")
        info("之前会话: ${event.previousSession}")
        info("====================================================")
    }
    
    @SubscribeEvent
    fun onVideoTripleStatusCheck(event: VideoTripleStatusCheckEvent) {
        val timestamp = dateFormat.format(Date())
        
        info("================ 视频三连状态查询事件 ================")
        info("时间: $timestamp")
        info("玩家: ${event.player.name} (UUID: ${event.player.uniqueId})")
        info("视频BV号: ${event.getBvid()}")
        info("用户MID: ${event.getMid()}")
        info("是否点赞: ${if (event.isLiked()) "是" else "否"}")
        info("投币数量: ${event.getCoinCount()}")
        info("是否收藏: ${if (event.isFavorited()) "是" else "否"}")
        info("是否完成三连: ${if (event.hasTripleAction()) "是" else "否"}")
        info("三连数据: ${event.tripleData}")
        info("====================================================")
    }
    
    @SubscribeEvent
    fun onUpFollowStatusCheck(event: UpFollowStatusCheckEvent) {
        val timestamp = dateFormat.format(Date())
        
        info("================ UP主关注状态查询事件 ================")
        info("时间: $timestamp")
        info("玩家: ${event.player.name} (UUID: ${event.player.uniqueId})")
        info("UP主昵称: ${event.getUpName()}")
        info("UP主MID: ${event.getUpMid()}")
        info("粉丝MID: ${event.getFollowerMid()}")
        info("关注状态: ${if (event.isFollowing()) "已关注" else "未关注"}")
        info("关注数据: ${event.followData}")
        info("====================================================")
    }
}