package com.assiance.convert.cron

import java.util.Calendar

/**
 * 由界面当前选择的 [CronSpec] 推算接下来的触发时刻。
 *
 * 刻意**不做 cron 字符串解析**：页面本来就持有结构化的 spec，再去解析一条自己刚生成的
 * 表达式，只会多出一层可能出错的中间环节。语义以 spec 为准 —— crontab / Spring 的
 * 「单次」在这里只显示一次，它们在表达式层面的「每年重复」已由 [dialectNotes] 另行说明。
 *
 * 秒固定 0，与 [buildCronExpression] 的既定口径一致。
 */
fun nextRuns(spec: CronSpec, fromMillis: Long, count: Int = 5): List<Long> {
    if (count <= 0) return emptyList()
    return when (spec.recurrence) {
        Recurrence.DAILY -> scanByDay(spec, fromMillis, count) { _, _ -> true }

        Recurrence.WEEKLY -> {
            val days = spec.weekDays.map { it.crontabValue }.toSet()
            scanByDay(spec, fromMillis, count) { _, cronDayOfWeek -> cronDayOfWeek in days }
        }

        Recurrence.MONTHLY -> {
            val days = spec.daysOfMonth.toSet()
            scanByDay(spec, fromMillis, count) { dayOfMonth, _ -> dayOfMonth in days }
        }

        Recurrence.ONCE -> onceRun(spec, fromMillis)
    }
}

/**
 * 从今天的目标时刻开始按天推进，把命中的日子收进结果。
 *
 * [hit] 的第二个参数是 **crontab 口径**的星期（0=周日 … 6=周六）：
 * `Calendar.DAY_OF_WEEK` 是 1=周日 … 7=周六，这里减 1 换算，避免调用方各自记两套编号。
 *
 * 31 号落在小月时该月自然扫不到，等价于 cron「跳过该月」的行为。
 */
private inline fun scanByDay(
    spec: CronSpec,
    fromMillis: Long,
    count: Int,
    hit: (dayOfMonth: Int, cronDayOfWeek: Int) -> Boolean,
): List<Long> {
    val result = ArrayList<Long>(count)
    val day = Calendar.getInstance().apply {
        timeInMillis = fromMillis
        set(Calendar.HOUR_OF_DAY, spec.hour)
        set(Calendar.MINUTE, spec.minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    // 今天的目标时刻已过，从明天开始找
    if (day.timeInMillis <= fromMillis) day.add(Calendar.DAY_OF_MONTH, 1)

    // 上限约 10 年：正常选择不可能扫不到 count 次，纯属防死循环
    var guard = 0
    while (result.size < count && guard < 3660) {
        val dayOfMonth = day.get(Calendar.DAY_OF_MONTH)
        val cronDayOfWeek = day.get(Calendar.DAY_OF_WEEK) - 1
        if (hit(dayOfMonth, cronDayOfWeek)) result.add(day.timeInMillis)
        day.add(Calendar.DAY_OF_MONTH, 1)
        guard++
    }
    return result
}

/** 单次执行：只返回该时刻；已过去则返回空列表，由界面显示「该时刻已过」。 */
private fun onceRun(spec: CronSpec, fromMillis: Long): List<Long> {
    val once = Calendar.getInstance().apply {
        clear()
        set(
            spec.year,
            spec.month - 1,
            spec.daysOfMonth.firstOrNull() ?: 1,
            spec.hour,
            spec.minute,
            0,
        )
    }
    return if (once.timeInMillis > fromMillis) listOf(once.timeInMillis) else emptyList()
}
