package online.bingzi.bilibili.video.internal.entity

/**
 * TripleData 类用于表示三连操作的数据结构，包括点赞、投币和收藏状态，以及硬币数量。
 * 该类主要用于在视频相关的功能中存储用户对视频的互动信息。
 *
 * @property like 表示用户是否点赞，类型为 Boolean，取值范围为 true（点赞）或 false（未点赞）。
 * @property coin 表示用户是否投币，类型为 Boolean，取值范围为 true（投币）或 false（未投币）。
 * @property fav 表示用户是否收藏，类型为 Boolean，取值范围为 true（收藏）或 false（未收藏）。
 * @property multiply 表示用户投币的数量，类型为 Int，取值范围为大于等于 0 的整数。
 * @constructor 创建一个空的 TripleData 对象。
 */
data class TripleData(
    val like: Boolean,  // 用户的点赞状态
    val coin: Boolean,  // 用户的投币状态
    val fav: Boolean,   // 用户的收藏状态
    val multiply: Int   // 用户投币的数量
)