package com.assiance.alm

import android.content.Context
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 某一天的法定假期信息。
 *
 * @param name     假期名称（如「国庆节」「春节」）；调休上班日的名称是该次放假的名称，
 *                 即 2026-10-10 这种补班日的 `name` 也是「国庆节」——数据源就是这么给的，
 *                 所以「休/班」只能看 [isOffDay]，不能靠名称判断。
 * @param isOffDay `true` = 放假（休），`false` = 调休上班（班）
 */
data class HolidayData(val name: String, val isOffDay: Boolean)

/**
 * 法定假期数据源。
 *
 * 用户 2026-09-24 拍板的四条口径：数据走**联网 API**、**休和班都标**、
 * 显示在**月历格子 + 当日面板标题**、拉不到时**用本地缓存兜底、完全没缓存就不显示**。
 *
 * **数据源**：`NateScarlet/holiday-cn`，社区维护的国务院放假安排整理（每年国务院发文后更新一次），
 * 走 jsDelivr 的 GitHub 镜像分发。**选 `fastly.jsdelivr.net` 这个域名是实测结果**：
 * `cdn.jsdelivr.net` / `gcore.jsdelivr.net` 都连不通，而 `timor.tech` 那类常见公益 API
 * 被 Cloudflare 的人机验证挡住（返回挑战页），App 里同样拿不到数据。
 *
 * 每年一个文件，格式：
 * ```json
 * {"year": 2026, "papers": [...], "days": [{"name": "元旦", "date": "2026-01-01", "isOffDay": true}]}
 * ```
 * `date` 是**零填充**的 `yyyy-MM-dd`，与 [holidayKey] 生成的键同格式。
 *
 * **缓存**：按年存进 [AlmContract.HOLIDAY_PREFS]，存的是**原始响应体**，
 * 读的时候再解析。这样不必为缓存另造一套序列化，格式校验也只需要在写入前做一次
 * （[fetch] 解析不通过就不写，避免把错误页/半截响应缓存下来）。
 *
 * **未发布年份的 `days` 是空数组**（实测 2027 年文件即 `{"days": []}`，国务院还没发文）——
 * 这在 UI 上与「拉取失败」是同一种表现：不显示任何标记。这是刻意的，不为它单设状态；
 * 两者唯一的差别在缓存上——**空数据不写缓存**（见 [fetch]），否则那一年会被永久钉成「没有假期」。
 */
object HolidayRepository {

    /** 年度文件的地址模板。`%d` 是年份。 */
    private const val URL_TEMPLATE =
        "https://fastly.jsdelivr.net/gh/NateScarlet/holiday-cn@master/%d.json"

    private const val CONNECT_TIMEOUT_MS = 8_000
    private const val READ_TIMEOUT_MS = 8_000

    private const val TAG = "HolidayRepository"

    /** 缓存键前缀，后面拼年份。 */
    private const val YEAR_KEY_PREFIX = "year_"

    /**
     * 该年是否已有缓存。**不解析内容**，只判键在不在——[fetch] 只会写入解析通过的响应，
     * 所以「有键」就等价于「有可用数据」，UI 拿它来决定要不要发请求。
     */
    fun hasCache(context: Context, year: Int): Boolean =
        prefs(context).contains(yearKey(year))

    /**
     * 读该年的缓存，key 是 [holidayKey] 格式的日期（`yyyy-MM-dd`）。
     *
     * 没有缓存、或缓存内容解析不出来（理论上不会，[fetch] 已把过关）时返回空 map，
     * 调用方按「这一年没有假期数据」处理即可，不需要区分这两种情况。
     */
    fun cached(context: Context, year: Int): Map<String, HolidayData> {
        val raw = prefs(context).getString(yearKey(year), null) ?: return emptyMap()
        return parse(raw).orEmpty()
    }

    /**
     * 联网拉取该年数据并写缓存。成功返回 `true`。
     *
     * **阻塞调用**，必须在 IO 线程执行（调用方见 `CalendarScreen` 的 `LaunchedEffect`）。
     * 任何失败（连不上、非 200、JSON 格式不对）都只记日志并返回 `false`，
     * 不抛异常——假期标记是纯展示的旁路信息，拿不到就不标，不该影响日历页本身。
     */
    fun fetch(context: Context, year: Int): Boolean {
        val url = URL(URL_TEMPLATE.format(year))
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            requestMethod = "GET"
        }
        return try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "拉取 $year 年假期失败：HTTP ${conn.responseCode}")
                return false
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            // 解析得出来才写缓存：否则会把错误页、半截响应永久留在本地。
            val parsed = parse(body)
            if (parsed == null) {
                Log.w(TAG, "拉取 $year 年假期失败：响应不是预期格式")
                return false
            }
            // 空 days 表示国务院还没发这一年的放假通知（实测 2027 年的文件就是 `{"days": []}`），
            // **这种不能写缓存**：写下去这一年就被永久钉成「没有假期」，等通知真发出来，
            // 用户不重装 App 就永远看不到。不缓存的话，下次翻到这一年会再拉一遍——
            // 一年一次请求，代价可以接受；而用户要的「有缓存就用缓存」针对的是**有效数据**。
            if (parsed.isEmpty()) {
                Log.i(TAG, "$year 年的放假通知尚未发布，本次不写缓存")
                return false
            }
            prefs(context).edit().putString(yearKey(year), body).apply()
            true
        } catch (e: Exception) {
            // 联网的失败方式是开放的（超时、DNS、SSL、被中间设备劫持……），
            // 这里不逐一列举，统一降级成「这一年没有假期数据」。
            Log.w(TAG, "拉取 $year 年假期失败", e)
            false
        } finally {
            conn.disconnect()
        }
    }

    /**
     * 日期键，格式与数据源一致：零填充的 `yyyy-MM-dd`。
     *
     * 不用 `String.format` 是因为它在每次调用时都会新建一个 `Formatter`，
     * 而月历一次要算 42 格、每次重组都要重算。
     */
    fun holidayKey(year: Int, month: Int, day: Int): String = buildString(10) {
        append(year)
        append('-')
        if (month < 10) append('0')
        append(month)
        append('-')
        if (day < 10) append('0')
        append(day)
    }

    /**
     * 解析年度文件，取出 `days` 里逐日的假期信息。
     *
     * 返回 `null` 表示这份响应不能用（不是 JSON、或没有 `days` 数组）——
     * 注意**空数组是合法的**，返回的是空 map 而不是 `null`。
     */
    private fun parse(body: String): Map<String, HolidayData>? {
        return try {
            val days = JSONObject(body).optJSONArray("days") ?: return null
            val result = HashMap<String, HolidayData>(days.length())
            for (i in 0 until days.length()) {
                val item = days.optJSONObject(i) ?: continue
                val date = item.optString("date")
                if (date.isEmpty()) continue
                result[date] = HolidayData(
                    name = item.optString("name"),
                    isOffDay = item.optBoolean("isOffDay"),
                )
            }
            result
        } catch (e: JSONException) {
            Log.w(TAG, "假期 JSON 解析失败", e)
            null
        }
    }

    private fun yearKey(year: Int) = "$YEAR_KEY_PREFIX$year"

    private fun prefs(context: Context) =
        context.getSharedPreferences(AlmContract.HOLIDAY_PREFS, Context.MODE_PRIVATE)
}
