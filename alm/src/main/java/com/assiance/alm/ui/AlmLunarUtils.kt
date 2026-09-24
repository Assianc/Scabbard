package com.assiance.alm.ui

import com.tyme.lunar.LunarDay
import com.tyme.lunar.LunarMonth
import com.tyme.solar.SolarDay
import java.util.Calendar
import java.util.TimeZone

/**
 * 农历显示工具（基于 tyme4kt 1.5.0）。
 *
 * **移植自 LoveScore 的 `ui/screens/agreement/LunarUtils.kt`**（用户要求 alm 的日历页
 * 照约定页重做，农历 + 干支是其中一部分）。与原件唯一的差别是入参：原件在 KMP 工程里
 * 收 `kotlinx.datetime.LocalDate`，本模块是 Android-only、没有引 kotlinx-datetime，
 * 所以改成直接收 `year / month / day` 三个 Int。函数体逐行一致，原件里那些
 * 「为什么这么算」的注释一并保留 —— 它们解释的坑在这里同样成立。
 *
 * 设计目标：
 * - 提供安全的农历转换（tyme4kt 抛越界异常时降级为空串，避免整页崩溃）
 * - 节日 / 节气优先显示（春节、中秋、立春、清明等）
 * - 「初一」显示月份（正月、闰六月等），其它日子显示「初一/十五/廿三」等
 *
 * API 速记（1.5.0）：
 * - SolarDay(year, month, day).getLunarDay() → LunarDay
 * - SolarDay.getTerm() → SolarTerm?（注意：返回的是「当前所属节气」的指针，
 *   整天持续返回同一个节气名；要判断「今天是不是节气日」，必须看 SolarTerm.day 是否 == solar）
 * - LunarDay.getFestival() → LunarFestival?
 * - LunarDay.day == 1 时显示「月份」；其余显示「日名」（"初一"/"十五"/"廿三"）
 * - 所有 Culture 子类（LunarMonth / LunarFestival / SolarTerm 等）的 getName() 返回中文名
 */
object AlmLunarUtils {

    /**
     * 把公历日期转成农历显示字符串。
     *
     * 给**日历格子**用（空间小）：初一显示月份（如「正月」「闰六月」），
     * 其它日子只显示日名（如「十五」「廿三」）。
     *
     * @return 农历文字，转换失败时返回 ""（调用方应 fallback 到空，不影响 UI）
     */
    fun formatLunar(year: Int, month: Int, day: Int): String {
        return try {
            val solar = SolarDay(year, month, day)

            // 节气：只有真正节气日才显示（tyme4kt getTerm() 是「当前节气段」指针，
            // 整段时间内每天都返回同一个节气名；要判断「今天是不是节气日」，
            // 比较 SolarTerm.getSolarDay() 与当前 solar 是否同一天）
            val term = solar.getTerm()
            if (term != null && term.getSolarDay().equals(solar)) {
                return term.getName()
            }

            // 农历日（含节日属性）
            val lunar: LunarDay = solar.getLunarDay()
            lunar.getFestival()?.getName()?.let { return it }

            // 「初一」显示月份（如「正月」「闰六月」），其它日子显示日名（如「十五」「廿三」）
            if (lunar.day == 1) {
                // LunarMonth.getName() 是 protected，外部不可调用；
                // LunarMonth.toString() 默认会拼「农历+干支+年」前缀，而
                // LunarDay.toString() 又会再拼年份前缀，都不适合直接显示。
                // 这里走 getMonthWithLeap()（负数 = 闰月）查 NAMES 表，手动加「闰」前缀。
                lunarMonthName(lunar.getLunarMonth())
            } else {
                lunar.getName()
            }
        } catch (_: Throwable) {
            // 越界等情况（tyme4kt 支持公元前 2500 ~ 公元 9999，正常日期不会触发）
            ""
        }
    }

    /**
     * 农历完整显示：「月份 + 日名」（如「正月十五」「闰六月初二」「正月初一」）。
     * 与 [formatLunar] 的区别：
     * - [formatLunar] 仅在初一显示月份（如「正月」），其它日子只显示日名（「十五」「廿三」），
     *   给日历格子（空间小）使用。
     * - [formatLunarFull] 始终带月名 + 日名（初一也显示「正月初一」），给
     *   当日面板标题区这种空间充足的场景使用，便于和干支字段对照阅读。
     */
    fun formatLunarFull(year: Int, month: Int, day: Int): String {
        return try {
            val solar = SolarDay(year, month, day)

            // 节气：节气当天显示节气名（如「立春」「清明」），优先于农历月日
            val term = solar.getTerm()
            if (term != null && term.getSolarDay().equals(solar)) {
                return term.getName()
            }

            // 农历日
            val lunar: LunarDay = solar.getLunarDay()

            // 节日优先（如「春节」「中秋」），节日自带上下文，不需要再拼月份
            lunar.getFestival()?.getName()?.let { return it }

            // 始终显示「月份 + 日名」（如「正月十五」「闰六月初二」「正月初一」）。
            // 与日历格子的规则正好相反：格子空间小，只在初一显示月份（「正月」）；
            // 当日面板标题区空间足，始终完整带月名 + 日名，便于和干支字段对照阅读。
            val monthName: String = lunarMonthName(lunar.getLunarMonth())
            val dayName: String = lunar.getName()
            "$monthName$dayName"
        } catch (_: Throwable) {
            ""
        }
    }

    /**
     * 把 LunarMonth 格式化成中文月名（"正月" / "七月" / "闰六月"）。
     *
     * 实现要点：tyme4kt 1.5.0 的 LunarMonth.getName() 是 protected，外部不能直接调用，
     * 而 LunarMonth.toString() 会拼上农历干支年前缀（如「农历壬午年正月」），
     * LunarDay.toString() 又会再拼年份（变成「农历壬午年正月初一」）。
     *
     * 改走 public 的 [LunarMonth.getMonthWithLeap]（返回带闰月标记的 int，闰月为负），
     * 配合 [LunarMonth.Companion.NAMES] 静态月名表，手动拼出干净的中文月名。
     */
    private fun lunarMonthName(month: LunarMonth): String {
        val monthWithLeap: Int = month.getMonthWithLeap()
        val idx: Int = if (monthWithLeap < 0) -monthWithLeap else monthWithLeap
        // NAMES 是 0-based，索引 = 月份 - 1
        val base = LunarMonth.NAMES[idx - 1]
        return if (monthWithLeap < 0) "闰$base" else base
    }

    /**
     * 取公历日所在农历年的天干地支（如「甲子」「乙丑」）。
     * 用于当日面板标题区，与公历日期、农历月日并列显示。
     *
     * @return 干支年字符串；转换失败时返回 ""
     */
    fun formatGanZhi(year: Int, month: Int, day: Int): String {
        return try {
            val solar = SolarDay(year, month, day)
            val lunar: LunarDay = solar.getLunarDay()
            // lunar.year 在 tyme4kt 1.5.0 LunarDay 中已经是 Int（农历年数字）。
            // 干支 = Heavenly Stem + Earthly Branch，按农历年计算：
            //   甲子年对应公元 4 年（农历新年起算）
            //   Heavenly Stem index = (year - 4) % 10
            //   Earthly Branch index = (year - 4) % 12
            // 注意：tyme4kt 的 LunarDay.year 是「农历年」，春节前还属上一年。
            // 干支年与之完全对应（春节换干支）。
            val yearValue: Int = lunar.year
            val stem = STEMS[floorMod(yearValue - 4, 10)]
            val branch = BRANCHES[floorMod(yearValue - 4, 12)]
            "$stem$branch"
        } catch (_: Throwable) {
            ""
        }
    }

    /**
     * 干支月（如「甲午」）。
     * 干支月不是按农历月份简单循环，而是从「寅月」（农历正月）开始，
     * 每年的「寅月」天干由当年天干决定（甲己之年丙作首）。
     */
    fun formatMonthGanZhi(year: Int, month: Int, day: Int): String {
        return try {
            val solar = SolarDay(year, month, day)
            val lunar: LunarDay = solar.getLunarDay()
            // 农历月序号按公历月近似（误差最多 1 个月），与原件同口径。
            val lunarMonthIdx = (month + 1) % 12    // 寅月索引 0 起点
            val yearStemIdx = floorMod(lunar.year - 4, 10)
            // 「五虎遁」年起月诀：甲己之年丙作首（寅月天干=丙=2），乙庚之岁戊为头（寅=4），
            // 丙辛必定寻庚起（寅=6），丁壬壬位顺行流（寅=8），戊癸之年甲虎头（寅=0）。
            val firstStemOfYear = when (yearStemIdx) {
                0, 5 -> 2   // 甲己 → 丙
                1, 6 -> 4   // 乙庚 → 戊
                2, 7 -> 6   // 丙辛 → 庚
                3, 8 -> 8   // 丁壬 → 壬
                else -> 0   // 戊癸 → 甲
            }
            val stemIdx = floorMod(firstStemOfYear + lunarMonthIdx, 10)
            val branchIdx = floorMod(2 + lunarMonthIdx, 12)   // 寅=2
            "${STEMS[stemIdx]}${BRANCHES[branchIdx]}"
        } catch (_: Throwable) {
            ""
        }
    }

    /**
     * 干支日（如「甲子」）。
     * 干支日从公元 1900-01-01（甲戌日）起算，60 天一循环。
     * 用「日差法」算：把目标日期与基准日期的天数差 % 60 取对应干支。
     *
     * 天数差用 **UTC** 的 `Calendar` 相减 —— 原件是 `kotlinx.datetime.LocalDate.daysUntil`，
     * 本模块没有 kotlinx-datetime；取 UTC 是为了避开夏令时把某天算成 23 / 25 小时。
     */
    fun formatDayGanZhi(year: Int, month: Int, day: Int): String {
        return try {
            // 干支日基准：1900-01-01 是甲戌日（index 10，六十甲子表）
            val base = utcMidnight(1900, 1, 1)
            val target = utcMidnight(year, month, day)
            val days = ((target - base) / MILLIS_PER_DAY).toInt()
            val baseIdx = 10
            val idx = floorMod(baseIdx + days, 60)
            "${STEMS[floorMod(idx, 10)]}${BRANCHES[floorMod(idx, 12)]}"
        } catch (_: Throwable) {
            ""
        }
    }

    /** 该日期在 UTC 时区的零点毫秒值。只用来做日期差，不参与任何本地时间的计算。 */
    private fun utcMidnight(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, day)
        }.timeInMillis
}

// 一天的毫秒数直接用 CalendarDayFilter.kt 里那份 internal 的 MILLIS_PER_DAY：
// Kotlin 的顶层 private 只在文件内可见，但**同包同名仍算重复声明**，两处各写一份编译不过。

private val STEMS = arrayOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")

private val BRANCHES = arrayOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")

/**
 * 向下取整的除法 / 取模。
 *
 * Kotlin 的 `/` 和 `%` 对负数是「向 0 截断」，把「距 0 年 0 月的月数」换算成
 * 年 / 月时会让负 offset（如 2002-08）算错一年 —— 例如 -287 / 12 得 -23，
 * 而向下取整应是 -24。日历的月份 Pager 会往两边各留 2400 个月，必定踩到负数。
 */
internal fun floorDiv(a: Int, b: Int): Int = Math.floorDiv(a, b)

internal fun floorMod(a: Int, b: Int): Int = Math.floorMod(a, b)
