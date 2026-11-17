package com.assiance.convert.cron

/**
 * cron 表达式生成逻辑（纯 Kotlin，不依赖 Android，便于直接在 JVM 上单测）。
 *
 * 三种方言的段数与字段顺序不同，这是本文件里所有分支的根源：
 *
 * | 方言     | 段数 | 字段顺序                     | 周日取值 | 日/周互斥 |
 * |----------|------|------------------------------|----------|-----------|
 * | crontab  | 5    | 分 时 日 月 周                | 0        | 否        |
 * | Spring   | 6    | 秒 分 时 日 月 周             | 0 或 7   | 需要 `?`  |
 * | Quartz   | 7    | 秒 分 时 日 月 周 年          | 1        | 需要 `?`  |
 *
 * 两条不能含糊的语义限制（界面也要原样告诉用户，不假装三种方言等价）：
 * 1. **crontab 没有秒字段**，生成结果只能按整分钟触发；
 * 2. **crontab / Spring 都没有年份字段**，「单次执行」在它们那里只是「每年同月同日同时刻
 *    触发」，只有 Quartz 能真正表达「只执行一次」。
 *
 * 秒字段固定取 0：界面只让用户选到分钟。这是本功能的既定口径，不是遗漏。
 */

/** 三种 cron 方言。[fieldHint] / [sample] 直接展示在界面上，帮助用户对照字段顺序。 */
enum class CronDialect(
    val label: String,
    val fieldHint: String,
    val sample: String,
) {
    SPRING("Spring", "秒 分 时 日 月 周", "0 30 14 * * ?"),
    CRONTAB("crontab", "分 时 日 月 周", "30 14 * * *"),
    QUARTZ("Quartz", "秒 分 时 日 月 周 年", "0 30 14 * * ? 2026"),
}

/**
 * 星期。三种方言里同一个星期几的编号并不相同（尤其 Quartz 把周日记作 1），
 * 映射集中放在这里，界面与生成逻辑都不要自己再算一遍。
 */
enum class WeekDay(
    val label: String,
    /** crontab：0=周日 … 6=周六 */
    val crontabValue: Int,
    /** Spring：0 或 7=周日，1=周一 … 6=周六 */
    val springValue: Int,
    /** Quartz：1=周日 … 7=周六 */
    val quartzValue: Int,
) {
    SUNDAY("周日", 0, 0, 1),
    MONDAY("周一", 1, 1, 2),
    TUESDAY("周二", 2, 2, 3),
    WEDNESDAY("周三", 3, 3, 4),
    THURSDAY("周四", 4, 4, 5),
    FRIDAY("周五", 5, 5, 6),
    SATURDAY("周六", 6, 6, 7),
}

/** 触发方式。前三项是重复执行，[ONCE] 是单次执行。 */
enum class Recurrence(val label: String) {
    DAILY("每天"),
    WEEKLY("每周"),
    MONTHLY("每月"),
    ONCE("单次"),
}

/**
 * 生成表达式所需的全部输入。
 *
 * @param weekDays 每周执行的星期，可多选；字段按逗号拼接（如 `1,3,5`）
 * @param daysOfMonth 每月执行的日期，可多选；[Recurrence.ONCE] 时只放一项，作为「日」字段
 * @param month 仅 [Recurrence.ONCE] 使用，取值 1-12
 * @param year 仅 [Recurrence.ONCE] 使用，只有 Quartz 会把它写进表达式
 */
data class CronSpec(
    val dialect: CronDialect = CronDialect.SPRING,
    val recurrence: Recurrence = Recurrence.DAILY,
    val hour: Int = 9,
    val minute: Int = 0,
    val weekDays: List<WeekDay> = listOf(WeekDay.MONDAY),
    val daysOfMonth: List<Int> = listOf(1),
    val month: Int = 1,
    val year: Int = 2026,
)

/** 多选的星期 → cron 字段值，按界面顺序（周日→周六）稳定输出。 */
private fun List<WeekDay>.cronValues(selector: (WeekDay) -> Int): String =
    sortedBy { it.ordinal }.joinToString(",") { selector(it).toString() }

/** 多选的日期 → cron 字段值，升序输出。 */
private fun List<Int>.cronValues(): String = sorted().joinToString(",")

/** 按方言生成表达式。界面上的预览与「复制」都直接用它。 */
fun buildCronExpression(spec: CronSpec): String = when (spec.dialect) {
    CronDialect.SPRING -> buildSpring(spec)
    CronDialect.CRONTAB -> buildCrontab(spec)
    CronDialect.QUARTZ -> buildQuartz(spec)
}

/**
 * crontab：分 时 日 月 周。没有秒、没有年。
 *
 * 单次执行只能落到「日 月」两个字段上，cron 本身没有年份，于是它的实际语义是
 * **每年同月同日同时刻触发** —— 界面必须把这点讲出来（见 [dialectNotes]）。
 */
private fun buildCrontab(spec: CronSpec): String {
    val minute = spec.minute
    val hour = spec.hour
    return when (spec.recurrence) {
        Recurrence.DAILY -> "$minute $hour * * *"
        Recurrence.WEEKLY -> "$minute $hour * * ${spec.weekDays.cronValues { it.crontabValue }}"
        Recurrence.MONTHLY -> "$minute $hour ${spec.daysOfMonth.cronValues()} * *"
        Recurrence.ONCE -> "$minute $hour ${spec.daysOfMonth.first()} ${spec.month} *"
    }
}

/**
 * Spring：秒 分 时 日 月 周。
 *
 * 日与周必须有一个是 `?`（Spring 的解析器虽然允许两栏都有，但同时指定只会取交集，
 * 对「每周」或「每月」都是意外行为，所以这里一律显式互斥）。
 */
private fun buildSpring(spec: CronSpec): String {
    val second = 0
    val minute = spec.minute
    val hour = spec.hour
    return when (spec.recurrence) {
        Recurrence.DAILY -> "$second $minute $hour * * ?"
        Recurrence.WEEKLY -> "$second $minute $hour ? * ${spec.weekDays.cronValues { it.springValue }}"
        Recurrence.MONTHLY -> "$second $minute $hour ${spec.daysOfMonth.cronValues()} * ?"
        Recurrence.ONCE -> "$second $minute $hour ${spec.daysOfMonth.first()} ${spec.month} ?"
    }
}

/**
 * Quartz：秒 分 时 日 月 周 年。
 *
 * 与 Spring 同样日/周互斥；多出的年字段让[Recurrence.ONCE] 能真正表达「只执行一次」。
 */
private fun buildQuartz(spec: CronSpec): String {
    val second = 0
    val minute = spec.minute
    val hour = spec.hour
    return when (spec.recurrence) {
        Recurrence.DAILY -> "$second $minute $hour * * ? *"
        Recurrence.WEEKLY -> "$second $minute $hour ? * ${spec.weekDays.cronValues { it.quartzValue }} *"
        Recurrence.MONTHLY -> "$second $minute $hour ${spec.daysOfMonth.cronValues()} * ? *"
        Recurrence.ONCE ->
            "$second $minute $hour ${spec.daysOfMonth.first()} ${spec.month} ? ${spec.year}"
    }
}

/**
 * 当前方言 + 触发方式下，表达式「表达不了」的部分。
 *
 * 返回空列表表示方言能力完整覆盖当前选择。界面把每条原样展示在结果下方 ——
 * 三种方言并不等价，不能让用户以为「单次」在 crontab 里也只跑一次。
 */
fun dialectNotes(spec: CronSpec): List<String> = buildList {
    when (spec.dialect) {
        CronDialect.CRONTAB -> {
            add("crontab 没有秒字段，按整分钟触发")
            if (spec.recurrence == Recurrence.ONCE) {
                add("crontab 没有年份字段，这条「单次」实际会每年同一天同一时刻触发")
            }
        }

        CronDialect.SPRING -> {
            if (spec.recurrence == Recurrence.ONCE) {
                add("Spring 没有年份字段，这条「单次」实际会每年同一天同一时刻触发")
            }
        }

        CronDialect.QUARTZ -> Unit
    }
}
