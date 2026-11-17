package com.assiance.convert.ui.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.assiance.convert.logic.lunarDayLabel
import com.assiance.convert.logic.lunarLeapMonth
import com.assiance.convert.logic.lunarMonthLabel
import com.assiance.convert.logic.lunarToSolar
import com.assiance.convert.logic.solarToLunar
import com.assiance.convert.ui.GLASS_TINT_ALPHA
import com.assiance.convert.ui.glassGrainFor
import com.assiance.convert.ui.rememberConvertGlassState
import io.github.nadeemiqbal.liquidglass.liquidGlass
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * 农历 ↔ 公历双向转换（tyme4kt 1.5.0）。
 *
 * 两个方向各自是独立的一组选择器：
 * - 公历 → 农历：DatePicker 选公历日期，结果卡显示完整农历（年月日 + 干支年 + 生肖）；
 * - 农历 → 公历：年 / 月 / 日 三行选择器（有闰月的年份会多出「闰X月」一项），
 *   结果卡显示对应公历日期；不存在的日期（如小月三十）给出提示而不是崩溃。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LunarScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val today = remember { Calendar.getInstance() }
    val todayLunar = remember {
        solarToLunar(
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH) + 1,
            today.get(Calendar.DAY_OF_MONTH),
        )
    }

    // ---------------------------------------------------------------- 选择状态
    var solarYear by rememberSaveable { mutableStateOf(today.get(Calendar.YEAR)) }
    var solarMonth by rememberSaveable { mutableStateOf(today.get(Calendar.MONTH) + 1) }
    var solarDay by rememberSaveable { mutableStateOf(today.get(Calendar.DAY_OF_MONTH)) }

    var lunarYear by rememberSaveable { mutableStateOf(todayLunar?.lunarYear ?: 2000) }
    var lunarMonth by rememberSaveable { mutableStateOf(todayLunar?.lunarMonthWithLeap ?: 1) }
    var lunarDay by rememberSaveable { mutableStateOf(todayLunar?.lunarDay ?: 1) }

    // ---------------------------------------------------------------- 弹窗状态
    var showSolarDatePicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showDayPicker by remember { mutableStateOf(false) }

    // ---------------------------------------------------------------- 结果
    val lunarResult = solarToLunar(solarYear, solarMonth, solarDay)
    val solarResult = lunarToSolar(lunarYear, lunarMonth, lunarDay)
    val solarResultText = solarResult?.let { (year, month, day) ->
        String.format(Locale.ROOT, "%04d-%02d-%02d", year, month, day)
    }

    val leapMonth = lunarLeapMonth(lunarYear)
    val monthOptions = remember(leapMonth) {
        buildList {
            for (month in 1..12) {
                add(month)
                if (month == leapMonth) add(-leapMonth)
            }
        }
    }

    fun copyText(text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "$label 已复制", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("农历公历") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionLabel("公历 → 农历")
            SettingRow(
                label = "公历日期",
                value = formatSolar(solarYear, solarMonth, solarDay),
                onClick = { showSolarDatePicker = true },
            )
            GlassCard {
                if (lunarResult != null) {
                    ResultHeader(
                        title = "农历",
                        onCopy = { copyText(lunarResult.fullText, "农历日期") },
                    )
                    Text(
                        text = lunarResult.fullText,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${lunarResult.ganZhiYear}年 · 属${lunarResult.zodiac}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = "该公历日期无法转换",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            SectionLabel("农历 → 公历")
            SettingRow(
                label = "农历年",
                value = "$lunarYear 年",
                onClick = { showYearPicker = true },
            )
            SettingRow(
                label = "农历月",
                value = lunarMonthLabel(lunarMonth),
                onClick = { showMonthPicker = true },
            )
            SettingRow(
                label = "农历日",
                value = lunarDayLabel(lunarDay),
                onClick = { showDayPicker = true },
            )
            GlassCard {
                if (solarResultText != null) {
                    ResultHeader(
                        title = "公历",
                        onCopy = { copyText(solarResultText, "公历日期") },
                    )
                    Text(
                        text = solarResultText,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                } else {
                    Text(
                        text = "该农历日期不存在（例如小月没有三十）",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showSolarDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = utcMillisOf(solarYear, solarMonth, solarDay),
        )
        DatePickerDialog(
            onDismissRequest = { showSolarDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val (year, month, day) = readUtcDate(millis)
                        solarYear = year
                        solarMonth = month
                        solarDay = day
                    }
                    showSolarDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showSolarDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showYearPicker) {
        PickerDialog(
            title = "选择农历年",
            items = (1900..2100).toList(),
            selected = lunarYear,
            label = { "$it 年" },
            onPick = { year ->
                lunarYear = year
                // 换年后原先选中的闰月可能不存在了，退回正月
                val leap = lunarLeapMonth(year)
                if (lunarMonth < 0 && -lunarMonth != leap) {
                    lunarMonth = 1
                }
                showYearPicker = false
            },
            onDismiss = { showYearPicker = false },
        )
    }

    if (showMonthPicker) {
        PickerDialog(
            title = "选择农历月",
            items = monthOptions,
            selected = lunarMonth,
            label = { lunarMonthLabel(it) },
            onPick = { month ->
                lunarMonth = month
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false },
        )
    }

    if (showDayPicker) {
        PickerDialog(
            title = "选择农历日",
            items = (1..30).toList(),
            selected = lunarDay,
            label = { lunarDayLabel(it) },
            onPick = { day ->
                lunarDay = day
                showDayPicker = false
            },
            onDismiss = { showDayPicker = false },
        )
    }
}

// ---------------------------------------------------------------------------- 组件

@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = CardDefaults.shape
    val glassState = rememberConvertGlassState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .liquidGlass(
                state = glassState,
                shape = shape,
                tint = MaterialTheme.colorScheme.surfaceContainer
                    .copy(alpha = GLASS_TINT_ALPHA),
                grain = glassGrainFor(glassState.quality),
            )
            .clip(shape)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        content = content,
    )
}

@Composable
private fun ResultHeader(title: String, onCopy: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "复制$title",
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** 一行「标签 —— 当前值 ›」，点击打开对应的选择器。 */
@Composable
private fun SettingRow(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 通用单选列表弹窗（年 / 月 / 日共用）。 */
@Composable
private fun <T> PickerDialog(
    title: String,
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                items(items) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onPick(item) }
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = label(item),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (item == selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

// ---------------------------------------------------------------------------- 日期工具

private fun formatSolar(year: Int, month: Int, day: Int): String =
    String.format(Locale.ROOT, "%04d-%02d-%02d", year, month, day)

/** DatePicker 的 selectedDateMillis 约定为 UTC 午夜，用本地时区构造会偏一天。 */
private fun utcMillisOf(year: Int, month: Int, day: Int): Long =
    Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(year, month - 1, day)
    }.timeInMillis

private fun readUtcDate(millis: Long): Triple<Int, Int, Int> {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
    return Triple(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH),
    )
}
