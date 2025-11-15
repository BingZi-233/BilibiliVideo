package online.bingzi.bilibili.video.internal

/**
 * 数据库表名前缀。
 *
 * 默认值需与配置文件 `database.yml` 中的 `database.options.table-prefix` 保持一致。
 * 建议在插件启动时由数据库配置初始化逻辑进行覆盖。
 */
internal var DATABASE_TABLE_PREFIX: String = "bv_"

