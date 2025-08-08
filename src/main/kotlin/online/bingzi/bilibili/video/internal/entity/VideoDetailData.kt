package online.bingzi.bilibili.video.internal.entity

import com.google.gson.annotations.SerializedName

/**
 * 视频详情数据
 * 
 * 包含视频的完整信息，从/x/web-interface/view或/x/web-interface/wbi/view接口返回
 * 
 * @author BingZi-233
 * @since 2.0.0
 */
data class VideoDetailData(
    /**
     * 视频BV号
     */
    @SerializedName("bvid")
    val bvid: String,
    
    /**
     * 视频AV号
     */
    @SerializedName("aid")
    val aid: Long,
    
    /**
     * 视频分P数
     */
    @SerializedName("videos")
    val videos: Int,
    
    /**
     * 分区ID
     */
    @SerializedName("tid")
    val tid: Int,
    
    /**
     * 分区名称
     */
    @SerializedName("tname")
    val tname: String,
    
    /**
     * 视频版权
     * 1：原创 2：转载
     */
    @SerializedName("copyright")
    val copyright: Int,
    
    /**
     * 视频封面图URL
     */
    @SerializedName("pic")
    val pic: String,
    
    /**
     * 视频标题
     */
    @SerializedName("title")
    val title: String,
    
    /**
     * 发布时间（Unix时间戳）
     */
    @SerializedName("pubdate")
    val pubdate: Long,
    
    /**
     * 投稿时间（Unix时间戳）
     */
    @SerializedName("ctime")
    val ctime: Long,
    
    /**
     * 视频简介
     */
    @SerializedName("desc")
    val desc: String,
    
    /**
     * 视频简介V2版本
     */
    @SerializedName("desc_v2")
    val descV2: List<DescV2>? = null,
    
    /**
     * 视频状态
     */
    @SerializedName("state")
    val state: Int,
    
    /**
     * 视频时长（秒）
     */
    @SerializedName("duration")
    val duration: Int,
    
    /**
     * UP主信息
     */
    @SerializedName("owner")
    val owner: VideoOwner,
    
    /**
     * 视频统计数据
     */
    @SerializedName("stat")
    val stat: VideoStatData,
    
    /**
     * 动态描述
     */
    @SerializedName("dynamic")
    val dynamic: String? = null,
    
    /**
     * 视频cid（用于播放）
     */
    @SerializedName("cid")
    val cid: Long,
    
    /**
     * 视频维度信息
     */
    @SerializedName("dimension")
    val dimension: VideoDimension,
    
    /**
     * 视频权限信息
     */
    @SerializedName("rights")
    val rights: VideoRights,
    
    /**
     * 分P信息列表
     */
    @SerializedName("pages")
    val pages: List<VideoPage>? = null,
    
    /**
     * 合集信息
     */
    @SerializedName("ugc_season")
    val ugcSeason: UgcSeason? = null
)

/**
 * 视频简介V2
 */
data class DescV2(
    /**
     * 原始文本
     */
    @SerializedName("raw_text")
    val rawText: String,
    
    /**
     * 类型
     * 1：普通文本 2：@用户
     */
    @SerializedName("type")
    val type: Int,
    
    /**
     * @用户时的ID
     */
    @SerializedName("biz_id")
    val bizId: Long? = null
)

/**
 * UP主信息
 */
data class VideoOwner(
    /**
     * UP主UID
     */
    @SerializedName("mid")
    val mid: Long,
    
    /**
     * UP主昵称
     */
    @SerializedName("name")
    val name: String,
    
    /**
     * UP主头像URL
     */
    @SerializedName("face")
    val face: String
)

/**
 * 视频维度信息
 */
data class VideoDimension(
    /**
     * 视频宽度
     */
    @SerializedName("width")
    val width: Int,
    
    /**
     * 视频高度
     */
    @SerializedName("height")
    val height: Int,
    
    /**
     * 是否旋转
     * 0：正常 1：旋转90度
     */
    @SerializedName("rotate")
    val rotate: Int
)

/**
 * 视频权限信息
 */
data class VideoRights(
    /**
     * 是否允许承包
     */
    @SerializedName("bp")
    val bp: Int,
    
    /**
     * 是否支持充电
     */
    @SerializedName("elec")
    val elec: Int,
    
    /**
     * 是否允许下载
     */
    @SerializedName("download")
    val download: Int,
    
    /**
     * 是否为电影
     */
    @SerializedName("movie")
    val movie: Int,
    
    /**
     * 是否为付费视频
     */
    @SerializedName("pay")
    val pay: Int,
    
    /**
     * 是否为高清视频
     */
    @SerializedName("hd5")
    val hd5: Int,
    
    /**
     * 是否禁止转载
     */
    @SerializedName("no_reprint")
    val noReprint: Int,
    
    /**
     * 是否自动播放
     */
    @SerializedName("autoplay")
    val autoplay: Int,
    
    /**
     * 是否为UGC付费视频
     */
    @SerializedName("ugc_pay")
    val ugcPay: Int,
    
    /**
     * 是否为合作视频
     */
    @SerializedName("is_cooperation")
    val isCooperation: Int,
    
    /**
     * UGC付费预览
     */
    @SerializedName("ugc_pay_preview")
    val ugcPayPreview: Int,
    
    /**
     * 是否禁止后台播放
     */
    @SerializedName("no_background")
    val noBackground: Int
)

/**
 * 视频分P信息
 */
data class VideoPage(
    /**
     * 分P的cid
     */
    @SerializedName("cid")
    val cid: Long,
    
    /**
     * 分P序号（从1开始）
     */
    @SerializedName("page")
    val page: Int,
    
    /**
     * 分P来源
     */
    @SerializedName("from")
    val from: String,
    
    /**
     * 分P标题
     */
    @SerializedName("part")
    val part: String,
    
    /**
     * 分P时长（秒）
     */
    @SerializedName("duration")
    val duration: Int,
    
    /**
     * 站外视频ID
     */
    @SerializedName("vid")
    val vid: String? = null,
    
    /**
     * 站外视频链接
     */
    @SerializedName("weblink")
    val weblink: String? = null,
    
    /**
     * 分P维度信息
     */
    @SerializedName("dimension")
    val dimension: VideoDimension
)

/**
 * 合集信息
 */
data class UgcSeason(
    /**
     * 合集ID
     */
    @SerializedName("id")
    val id: Long,
    
    /**
     * 合集标题
     */
    @SerializedName("title")
    val title: String,
    
    /**
     * 合集封面
     */
    @SerializedName("cover")
    val cover: String,
    
    /**
     * UP主UID
     */
    @SerializedName("mid")
    val mid: Long,
    
    /**
     * 合集简介
     */
    @SerializedName("intro")
    val intro: String,
    
    /**
     * 签名状态
     */
    @SerializedName("sign_state")
    val signState: Int,
    
    /**
     * 属性
     */
    @SerializedName("attribute")
    val attribute: Int,
    
    /**
     * 分节列表
     */
    @SerializedName("sections")
    val sections: List<UgcSection>,
    
    /**
     * 统计信息
     */
    @SerializedName("stat")
    val stat: UgcSeasonStat,
    
    /**
     * 视频总数
     */
    @SerializedName("ep_count")
    val epCount: Int
)

/**
 * 合集分节
 */
data class UgcSection(
    /**
     * 分节ID
     */
    @SerializedName("season_id")
    val seasonId: Long,
    
    /**
     * 分节ID
     */
    @SerializedName("id")
    val id: Long,
    
    /**
     * 分节标题
     */
    @SerializedName("title")
    val title: String,
    
    /**
     * 分节类型
     */
    @SerializedName("type")
    val type: Int,
    
    /**
     * 分节内视频列表
     */
    @SerializedName("episodes")
    val episodes: List<UgcEpisode>
)

/**
 * 合集内视频
 */
data class UgcEpisode(
    /**
     * 合集ID
     */
    @SerializedName("season_id")
    val seasonId: Long,
    
    /**
     * 分节ID
     */
    @SerializedName("section_id")
    val sectionId: Long,
    
    /**
     * 视频ID
     */
    @SerializedName("id")
    val id: Long,
    
    /**
     * 视频AV号
     */
    @SerializedName("aid")
    val aid: Long,
    
    /**
     * 视频CID
     */
    @SerializedName("cid")
    val cid: Long,
    
    /**
     * 视频标题
     */
    @SerializedName("title")
    val title: String,
    
    /**
     * 属性
     */
    @SerializedName("attribute")
    val attribute: Int,
    
    /**
     * 视频封面（分节视频）
     */
    @SerializedName("arc")
    val arc: UgcArc,
    
    /**
     * 分P信息
     */
    @SerializedName("page")
    val page: VideoPage,
    
    /**
     * BV号
     */
    @SerializedName("bvid")
    val bvid: String
)

/**
 * 合集内视频信息
 */
data class UgcArc(
    /**
     * 视频AV号
     */
    @SerializedName("aid")
    val aid: Long,
    
    /**
     * 视频封面
     */
    @SerializedName("pic")
    val pic: String,
    
    /**
     * 视频标题
     */
    @SerializedName("title")
    val title: String,
    
    /**
     * 发布时间
     */
    @SerializedName("pubdate")
    val pubdate: Long,
    
    /**
     * 投稿时间
     */
    @SerializedName("ctime")
    val ctime: Long,
    
    /**
     * 视频简介
     */
    @SerializedName("desc")
    val desc: String,
    
    /**
     * 视频状态
     */
    @SerializedName("state")
    val state: Int,
    
    /**
     * 视频时长
     */
    @SerializedName("duration")
    val duration: Int,
    
    /**
     * 统计信息
     */
    @SerializedName("stat")
    val stat: VideoStatData,
    
    /**
     * 动态文本
     */
    @SerializedName("dynamic")
    val dynamic: String? = null,
    
    /**
     * 维度信息
     */
    @SerializedName("dimension")
    val dimension: VideoDimension,
    
    /**
     * 权限信息
     */
    @SerializedName("rights")
    val rights: VideoRights
)

/**
 * 合集统计信息
 */
data class UgcSeasonStat(
    /**
     * 合集ID
     */
    @SerializedName("season_id")
    val seasonId: Long,
    
    /**
     * 播放量
     */
    @SerializedName("view")
    val view: Int,
    
    /**
     * 弹幕数
     */
    @SerializedName("danmaku")
    val danmaku: Int,
    
    /**
     * 评论数
     */
    @SerializedName("reply")
    val reply: Int,
    
    /**
     * 收藏数
     */
    @SerializedName("fav")
    val fav: Int,
    
    /**
     * 投币数
     */
    @SerializedName("coin")
    val coin: Int,
    
    /**
     * 分享数
     */
    @SerializedName("share")
    val share: Int,
    
    /**
     * 当前排名
     */
    @SerializedName("now_rank")
    val nowRank: Int,
    
    /**
     * 历史最高排名
     */
    @SerializedName("his_rank")
    val hisRank: Int,
    
    /**
     * 点赞数
     */
    @SerializedName("like")
    val like: Int
)