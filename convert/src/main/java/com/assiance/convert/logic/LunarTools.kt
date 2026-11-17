package com.assiance.convert.logic

import com.tyme.lunar.LunarDay
import com.tyme.lunar.LunarMonth
import com.tyme.lunar.LunarYear
import com.tyme.solar.SolarDay
import java.util.Locale

/**
 * 农历 ↔ 公历转换（基于 tyme4kt 1.5.0，与 alm 日历页同库同版本）。
 *
 * 只暴露本页需要的最小 API：
 * - 公历 → 农历：年、月名、日名、干支年、生肖；
 * - 农历 → 公历：反过来给出 y/m/d；
 * - 年份的闰月查询，供农历月选择器列出「闰X月」。
 *
 * 越界 / 不存在的日期（如小月没有三十）一律返回 null，由界面提示，
 * 不让 tyme4kt 的异常冒到 Compose 里。
 */

/** 公历日期对应的农历信息。 */
data class LunarInfo(
    val lunarYear: Int,
    /** 农历月，负数表示闰月（可直接回传给 [lunarToSolar]） */
    val lunarMonthWithLeap: Int,
    /** 农历日 1-30 */
    val lunarDay: Int,
    /** 如「二〇二六」 */
    val yearText: String,
    /** 如「十月」「闰六月」 */
    val monthName: String,
    /** 如「初八」「廿三」 */
    val dayName: String,
    /** 如「二〇二六年十月初八」 */
    val fullText: String,
    /** 农历年的干支，如「丙午」 */
    val ganZhiYear: String,
    /** 当年生肖，如「马」 */
    val zodiac: String,
    /** 公历回显，如「2026-11-17」 */
    val solarText: String,
)

/** 公历 → 农历；日期非法时返回 null。 */
fun solarToLunar(year: Int, month: Int, day: Int): LunarInfo? = try {
    val solar = SolarDay(year, month, day)
    val lunar = solar.getLunarDay()
    val lunarYear = lunar.year
    val monthWithLeap = lunar.getLunarMonth().getMonthWithLeap()
    val monthName = lunarMonthName(lunar.getLunarMonth())
    LunarInfo(
        lunarYear = lunarYear,
        lunarMonthWithLeap = monthWithLeap,
        lunarDay = lunar.day,
        yearText = chineseYear(lunarYear),
        monthName = monthName,
        dayName = lunar.getName(),
        fullText = "${chineseYear(lunarYear)}年$monthName${lunar.getName()}",
        ganZhiYear = LunarYear(lunarYear).getSixtyCycle().getName(),
        zodiac = zodiacOf(lunarYear),
        solarText = String.format(Locale.ROOT, "%04d-%02d-%02d", year, month, day),
    )
} catch (_: Throwable) {
    null
}

/**
 * 农历 → 公历。
 *
 * @param monthWithLeap 农历月，负数表示闰月（tyme4kt 的约定，与 [LunarMonth.getMonthWithLeap] 一致）
 * @return (公历年, 公历月, 公历日)；日期不存在时返回 null
 */
fun lunarToSolar(lunarYear: Int, monthWithLeap: Int, day: Int): Triple<Int, Int, Int>? = try {
    val solar = LunarDay(lunarYear, monthWithLeap, day).getSolarDay()
    Triple(solar.year, solar.month, solar.day)
} catch (_: Throwable) {
    null
}

/** 某农历年的闰月月份；没有闰月返回 0。 */
fun lunarLeapMonth(lunarYear: Int): Int = try {
    LunarYear(lunarYear).getLeapMonth()
} catch (_: Throwable) {
    0
}

/** 农历月的中文名（`monthWithLeap` 负数 = 闰月）。 */
fun lunarMonthLabel(monthWithLeap: Int): String {
    val index = kotlin.math.abs(monthWithLeap)
    val base = LunarMonth.NAMES[index - 1]
    return if (monthWithLeap < 0) "闰$base" else base
}

/** 农历日的中文名（初一 … 三十）。 */
fun lunarDayLabel(day: Int): String = LunarDay.NAMES[day - 1]

/** 农历年的生肖。 */
fun zodiacOf(lunarYear: Int): String = try {
    val branch = LunarYear(lunarYear).getSixtyCycle().getEarthBranch().getName()
    ZODIAC[branch].orEmpty()
} catch (_: Throwable) {
    ""
}

/** LunarMonth.getName() 是 protected，走公开的 getMonthWithLeap + 静态月名表拼。 */
private fun lunarMonthName(month: LunarMonth): String =
    lunarMonthLabel(month.getMonthWithLeap())

/** 2026 → 二〇二六。 */
private fun chineseYear(year: Int): String = buildString {
    for (ch in year.toString()) {
        append(CHINESE_DIGITS[ch - '0'])
    }
}

private val CHINESE_DIGITS = charArrayOf('〇', '一', '二', '三', '四', '五', '六', '七', '八', '九')

private val ZODIAC = mapOf(
    "子" to "鼠",
    "丑" to "牛",
    "寅" to "虎",
    "卯" to "兔",
    "辰" to "龙",
    "巳" to "蛇",
    "午" to "马",
    "未" to "羊",
    "申" to "猴",
    "酉" to "鸡",
    "戌" to "狗",
    "亥" to "猪",
)
