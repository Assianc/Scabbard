package com.assiance.alm.ui

import com.assiance.alm.AlarmData
import com.assiance.alm.HistoryItem
import com.assiance.alm.TodoData
import java.util.Calendar

/**
 * 日历页的「某一天上有什么」。
 *
 * 从原 `CalendarActivity.updateHistoryForDate()` 里那段过滤逻辑抽出来，拆成两桶 ——
 * 日历页的当日面板是「待办 / 闹钟」两页（用户 2026-09-23 拍板），正好一类一页，
 * 不用先合并再按类型二次筛选。
 */
internal data class DayItems(
    val todos: List<TodoData> = emptyList(),
    val alarms: List<AlarmData> = emptyList(),
) {
    val isEmpty: Boolean get() = todos.isEmpty() && alarms.isEmpty()
}

/**
 * 月历格子上的标记：这一天有没有待办 / 闹钟。
 *
 * 与 [DayItems] 的区别是只关心「有没有」—— 月历一次要算 42 个格子，
 * 没必要为每个格子都建出列表来。
 */
internal data class DayMarks(
    val hasTodo: Boolean = false,
    val hasAlarm: Boolean = false,
) {
    val any: Boolean get() = hasTodo || hasAlarm
}

/**
 * 过滤出 [dateMillis] 这一天的待办与闹钟。
 *
 * **逐条对齐原 `CalendarActivity.updateHistoryForDate()` 的判断逻辑**，一处口径都没改
 * （包括几处看起来绕的地方，见下面各分支的注释）。原来的实现是「先合并成
 * `List<HistoryItem>`，再按 item 类型走不同分支」，这里换成先把基准时间算好、
 * 再分类收集，判断条件本身逐个照搬。
 *
 * 注意：`date` 参数用的是**传进来的原始毫秒值**，不是归一化到 0 点的那个。
 * 原实现就是拿未归一化的 `date` 去比 `todo.startTime` / `todo.dueTime` 的，
 * 调用方传进来的又恰好是 `CalendarView.date`（本来就是当天 0 点），所以两者等价 ——
 * 这里保持原样，不做"顺手统一"，免得行为有差异。
 */
internal fun filterDay(
    items: List<HistoryItem>,
    dateMillis: Long,
): DayItems {
    val dayStart = startOfDay(dateMillis)
    val todayStart = startOfDay(System.currentTimeMillis())
    val nextDayStart = startOfDayPlusOneDay(dateMillis)

    val todos = mutableListOf<TodoData>()
    val alarms = mutableListOf<AlarmData>()

    items.forEach { item ->
        when (item) {
            is HistoryItem.AlarmItem -> {
                // 原实现：闹钟时间落在 [当天 0 点, 次日 0 点) 就算这一天
                val time = item.alarm.timeInMillis
                if (time >= dayStart && time < nextDayStart) {
                    alarms.add(item.alarm)
                }
            }

            is HistoryItem.TodoItem -> {
                if (todoAppearsOn(item.todo, dateMillis, dayStart, todayStart)) {
                    todos.add(item.todo)
                }
            }
        }
    }

    return DayItems(todos = todos, alarms = alarms)
}

/**
 * 月历格子用的轻量版 [filterDay]：只回答「这一天有没有」，不建列表。
 *
 * 一次要算 42 个格子（6×7 固定格），逐格建列表的代价没必要付。两个标记都凑齐就提前
 * 结束遍历。
 */
internal fun marksOn(
    items: List<HistoryItem>,
    dateMillis: Long,
): DayMarks {
    val dayStart = startOfDay(dateMillis)
    val todayStart = startOfDay(System.currentTimeMillis())
    val nextDayStart = startOfDayPlusOneDay(dateMillis)

    var hasTodo = false
    var hasAlarm = false
    for (item in items) {
        when (item) {
            is HistoryItem.AlarmItem -> {
                if (!hasAlarm) {
                    val time = item.alarm.timeInMillis
                    if (time >= dayStart && time < nextDayStart) hasAlarm = true
                }
            }

            is HistoryItem.TodoItem -> {
                if (!hasTodo && todoAppearsOn(item.todo, dateMillis, dayStart, todayStart)) {
                    hasTodo = true
                }
            }
        }
        if (hasTodo && hasAlarm) break
    }
    return DayMarks(hasTodo = hasTodo, hasAlarm = hasAlarm)
}

/**
 * 单条待办是否出现在 [dateMillis] 这一天。原 `updateHistoryForDate()` 里
 * `is HistoryItem.TodoItem -> { ... }` 那个大 `when` 的逐分支照搬。
 *
 * @param date      选中日期的原始毫秒（未归一化，见 [filterDay] 的说明）
 * @param dayStart  选中日期的 0 点
 * @param todayStart 今天的 0 点
 */
private fun todoAppearsOn(
    todo: TodoData,
    date: Long,
    dayStart: Long,
    todayStart: Long,
): Boolean {
    val startTime = todo.startTime
    val dueTime = todo.dueTime
    val completedTime = todo.completedTime

    return when {
        // ---- 已完成的待办
        todo.isCompleted -> {
            if (startTime != null && dueTime == null) {
                // 只有开始日期的已完成待办：显示从开始当天到完成当天这一整段
                val startDay = startOfDay(startTime)
                val completedDay = startOfDay(completedTime ?: System.currentTimeMillis())
                dayStart in startDay..completedDay
            } else {
                // 其它已完成待办：只显示完成当天（原逻辑）
                val actualCompletedTime = completedTime ?: dueTime ?: System.currentTimeMillis()
                isSameDay(actualCompletedTime, date)
            }
        }

        // ---- 未完成、且有开始时间或截止时间
        startTime != null || dueTime != null -> when {
            // 只有开始时间：从开始当天一直显示到今天
            startTime != null && dueTime == null ->
                dayStart >= startOfDay(startTime) && dayStart <= todayStart

            // 只有截止时间：从创建时间起，到截止时间为止
            startTime == null && dueTime != null ->
                date >= todo.createdTime && (date <= dueTime || isSameDay(date, dueTime))

            // 同时有开始和截止：显示在该区间内
            else ->
                date >= startTime!! && (date <= dueTime!! || isSameDay(date, dueTime))
        }

        // ---- 无时间限制的待办：从创建时间起，到今天为止
        else ->
            date >= todo.createdTime &&
                (date <= todayStart || isSameDay(date, todo.createdTime))
    }
}

/**
 * 日历格子里的一天（只有年月日，没有时间）。
 *
 * 用它而不是毫秒做 Map 的键：毫秒值要落到同一个 0 点才对得上，一旦哪处忘了归一化就会
 * 静默错配；年月日相等就是相等。实现 `data class` 因此自带可用的 `equals` / `hashCode`。
 */
internal data class CalendarDay(
    val year: Int,
    val month: Int,
    val day: Int,
) {
    /** 这一天的 0 点毫秒值。 */
    fun toMillis(): Long = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, day)
    }.timeInMillis
}

/**
 * 某年某月的 6×7 日期格子（**周日开头**，与原约定页一致）。
 *
 * 固定 42 格而不是按当月天数算到几行：格子数随月份变化的话，月份 Pager 每滑一页高度
 * 都会跳一下。多出来的格子用相邻月份补齐（灰显），也是常见日历的做法。
 *
 * 用 `Calendar` 逐日推进而不是手写月份天数表：闰年、大小月都由它兜着，且能正确处理
 * 「上个月的日期」这类跨年情况。
 */
internal fun buildMonthCells(year: Int, month: Int): List<CalendarDay> {
    val cursor = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, 1)
        // Calendar.SUNDAY == 1，减 1 后周日 = 0，正好是「本周第几天」的 0 基索引。
        // 退到本周第一格（可能是上个月的日期）。
        add(Calendar.DAY_OF_MONTH, -(get(Calendar.DAY_OF_WEEK) - 1))
    }
    return List(MONTH_CELL_COUNT) {
        val cell = CalendarDay(
            year = cursor.get(Calendar.YEAR),
            month = cursor.get(Calendar.MONTH) + 1,
            day = cursor.get(Calendar.DAY_OF_MONTH),
        )
        cursor.add(Calendar.DAY_OF_MONTH, 1)
        cell
    }
}

/**
 * 把毫秒折算成「距 1970-01-01 的天数」，用于算日期差（如"3 天前"）。
 *
 * 取本地 0 点再相减，而不是直接除 86400000：直接除会把时区偏移算进去，
 * 东八区的 1970-01-01 00:00 本地时间除以一天是负数。
 */
internal fun daysBetween(fromMillis: Long, toMillis: Long): Int {
    val from = startOfDay(fromMillis)
    val to = startOfDay(toMillis)
    return ((to - from) / MILLIS_PER_DAY).toInt()
}

/** 原 `CalendarActivity.isSameDay()`。 */
internal fun isSameDay(time1: Long, time2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

/** 该时刻所在日期的 0 点。 */
internal fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/**
 * 次日 0 点。
 *
 * 走 `Calendar.add(DAY_OF_YEAR, 1)` 而不是 `+ 86_400_000`：有夏令时的时区里某一天是
 * 23 或 25 小时，直接加一天毫秒数会落到次日 1 点或 23 点。
 */
internal fun startOfDayPlusOneDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = startOfDay(millis)
    add(Calendar.DAY_OF_YEAR, 1)
}.timeInMillis

internal const val MILLIS_PER_DAY = 86_400_000L

/** 月历固定 6 行 × 7 列。 */
private const val MONTH_CELL_COUNT = 42
