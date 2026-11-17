package com.assiance.convert.logic

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 时间戳 / 日期相关的纯逻辑，不依赖 Android。
 *
 * **不用 java.time**：convert 的 minSdk 是 24，且没有开 core library desugaring，
 * java.time 在 24/25 上会直接崩。这里统一用 java.util.Calendar / SimpleDateFormat。
 */

/** 时间戳单位。AUTO 按输入位数猜。 */
enum class TimestampUnit(val label: String) {
    AUTO("自动"),
    SECONDS("秒"),
    MILLIS("毫秒"),
}

/**
 * 解析输入的时间戳文本，返回毫秒。
 *
 * AUTO 的判据是**位数**：12 位及以上按毫秒，其余按秒。
 * 常见场景正好落在两头——10 位秒（2001~2286 年）、13 位毫秒（2001~2286 年）；
 * 11/12 位属于两个口径都可能的时间段，这里按秒处理，界面允许手动切到毫秒。
 *
 * 输入为空、含非数字字符或超出 Long 范围时返回 null。
 */
fun parseTimestampMillis(text: String, unit: TimestampUnit): Long? {
    val raw = text.trim()
    if (raw.isEmpty()) return null
    val negative = raw.startsWith("-")
    val digits = raw.removePrefix("-")
    if (digits.isEmpty() || !digits.all { it.isDigit() }) return null
    val value = digits.toLongOrNull() ?: return null
    val signed = if (negative) -value else value
    return when (unit) {
        TimestampUnit.MILLIS -> signed
        TimestampUnit.SECONDS -> signed * 1000L
        TimestampUnit.AUTO -> if (digits.length >= 12) signed else signed * 1000L
    }
}

/** 把毫秒时间戳格式化到指定时区。 */
fun formatTimestamp(
    millis: Long,
    zone: TimeZone,
    pattern: String = "yyyy-MM-dd HH:mm:ss",
): String = SimpleDateFormat(pattern, Locale.ROOT)
    .apply { timeZone = zone }
    .format(Date(millis))

/** 同一时刻在本机时区与 UTC 下的两种表示。 */
fun formatTimestampBoth(millis: Long): Pair<String, String> =
    formatTimestamp(millis, TimeZone.getDefault()) to
        formatTimestamp(millis, TimeZone.getTimeZone("UTC"))

/** 年月日时分（本地时区）→ 毫秒时间戳。 */
fun localDateTimeMillis(
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int,
): Long = Calendar.getInstance().apply {
    clear()
    set(year, month - 1, day, hour, minute, 0)
}.timeInMillis

/**
 * 两个日期之间的日历日数（结束 - 起始），忽略时分秒，也不受夏令时影响。
 *
 * 用「公历日 → 连续天数」的换算（Howard Hinnant 的 days_from_civil），
 * 避免 Calendar 在夏令时切换日出现 23/25 小时导致差一天。
 */
fun daysBetween(
    startYear: Int,
    startMonth: Int,
    startDay: Int,
    endYear: Int,
    endMonth: Int,
    endDay: Int,
): Long = daysFromCivil(endYear, endMonth, endDay) - daysFromCivil(startYear, startMonth, startDay)

/** 公历日期 → 距 1970-01-01 的天数（可为负）。 */
private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
    val y = if (month <= 2) year - 1 else year
    val era = (if (y >= 0) y else y - 399) / 400
    val yearOfEra = y - era * 400
    val dayOfYear = (153 * (if (month > 2) month - 3 else month + 9) + 2) / 5 + day - 1
    val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
    return era * 146097L + dayOfEra - 719468L
}
