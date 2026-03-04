package online.bingzi.bilibili.video.internal.entity

import org.ktorm.entity.Entity

/**
 * Ktorm DSL-only 哑元实体类型。
 *
 * 目的：避免使用 Table<Nothing> 触发 Ktorm 在 BaseTable 构造阶段
 * 对泛型做 Kotlin 反射解析（隔离类加载场景下会导致反射依赖问题）。
 */
internal interface NoEntity : Entity<NoEntity>
