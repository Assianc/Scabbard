package com.assiance.convert.logic

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/**
 * 时区对照的纯逻辑：给一个时间点，算出固定城市列表各自的当地时间。
 *
 * 全部走 `java.util.TimeZone` / `SimpleDateFormat`。**不用 java.time**：
 * 模块 minSdk 24 且没有开 core library desugaring。
 *
 * 时差按**该时刻的实际偏移**计算（而不是写死 UTC+X），所以纽约、伦敦的夏令时
 * 会自动反映在对照结果里。
 */

/** 对照页显示的一座城市。 */
data class TimeZoneCity(val name: String, val zoneId: String)

/** 固定城市清单，按从东到西大致排列，北京固定在第一位。 */
val TIME_ZONE_CITIES = listOf(
    TimeZoneCity("北京", "Asia/Shanghai"),
    TimeZoneCity("东京", "Asia/Tokyo"),
    TimeZoneCity("新加坡", "Asia/Singapore"),
    TimeZoneCity("迪拜", "Asia/Dubai"),
    TimeZoneCity("伦敦", "Europe/London"),
    TimeZoneCity("纽约", "America/New_York"),
    TimeZoneCity("洛杉矶", "America/Los_Angeles"),
    TimeZoneCity("悉尼", "Australia/Sydney"),
)

/** 某城市在某一时刻的显示数据。 */
data class CityClock(
    val city: String,
    /** HH:mm */
    val time: String,
    /** MM-dd HH:mm */
    val dateTime: String,
    /** UTC+8 / UTC-5 / UTC+5:30 */
    val utcOffset: String,
    /** 与北京的时差，如「比北京早 1 小时」 */
    val diffFromBeijing: String,
)

/**
 * 计算所有城市在 [baseMillis] 这一刻的当地时间。
 *
 * @param baseMillis 毫秒时间戳（与设备时区无关，同一时刻全球对照）
 */
fun clocksAt(baseMillis: Long): List<CityClock> {
    val beijingOffset = TimeZone.getTimeZone("Asia/Shanghai").getOffset(baseMillis)
    return TIME_ZONE_CITIES.map { city ->
        val zone = TimeZone.getTimeZone(city.zoneId)
        val offset = zone.getOffset(baseMillis)
        CityClock(
            city = city.name,
            time = SimpleDateFormat("HH:mm", Locale.ROOT)
                .apply { timeZone = zone }
                .format(Date(baseMillis)),
            dateTime = SimpleDateFormat("MM-dd HH:mm", Locale.ROOT)
                .apply { timeZone = zone }
                .format(Date(baseMillis)),
            utcOffset = formatUtcOffset(offset),
            diffFromBeijing = formatDiffFromBeijing(offset - beijingOffset),
        )
    }
}

/** 把时区偏移毫秒格式化成 `UTC+8` / `UTC-5` / `UTC+5:30`。 */
internal fun formatUtcOffset(offsetMillis: Int): String {
    val totalMinutes = offsetMillis / 60_000
    val sign = if (totalMinutes >= 0) "+" else "-"
    val absMinutes = abs(totalMinutes)
    val hours = absMinutes / 60
    val minutes = absMinutes % 60
    return if (minutes == 0) {
        "UTC$sign$hours"
    } else {
        String.format(Locale.ROOT, "UTC%s%d:%02d", sign, hours, minutes)
    }
}

/** 把与北京的偏移差（毫秒）说成人话。 */
internal fun formatDiffFromBeijing(diffMillis: Int): String {
    val diffMinutes = diffMillis / 60_000
    if (diffMinutes == 0) return "与北京相同"
    val absMinutes = abs(diffMinutes)
    val hours = absMinutes / 60
    val minutes = absMinutes % 60
    val amount = when {
        hours > 0 && minutes > 0 -> "$hours 小时 $minutes 分"
        hours > 0 -> "$hours 小时"
        else -> "$minutes 分钟"
    }
    return if (diffMinutes > 0) "比北京早 $amount" else "比北京晚 $amount"
}
