package com.assiance.convert.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.assiance.convert.cron.CronDialect
import com.assiance.convert.cron.CronSpec
import com.assiance.convert.cron.Recurrence
import com.assiance.convert.cron.WeekDay
import com.assiance.convert.cron.buildCronExpression
import com.assiance.convert.cron.dialectNotes
import com.assiance.convert.cron.nextRuns
import io.github.nadeemiqbal.liquidglass.liquidGlass
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 定时任务 cron 表达式转换页：用户选时间，页面实时输出表达式。
 *
 * 交互按两条轴展开：
 * - **方言**：Spring（6 段）/ crontab（5 段）/ Quartz（7 段），切换即时重算；
 * - **触发方式**：重复执行（每天 / 每周 / 每月）或单次执行（具体年月日时分）。
 *
 * 三种方言的能力并不等价（crontab 无秒、crontab 与 Spring 无年份），差异不隐藏，
 * 由 [dialectNotes] 原样列在结果卡下方 —— 用户复制走的是哪条语义，必须一眼可见。
 *
 * 秒字段固定 0（界面只选到分钟），这是既定口径，见 [buildCronExpression] 的文件头。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CronConverterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val today = remember { Calendar.getInstance() }

    // ---------------------------------------------------------------- 选择状态
    var dialect by rememberSaveable { mutableStateOf(CronDialect.SPRING) }
    var isOnce by rememberSaveable { mutableStateOf(false) }
    var repeatMode by rememberSaveable { mutableStateOf(Recurrence.DAILY) }
    var hour by rememberSaveable { mutableStateOf(9) }
    var minute by rememberSaveable { mutableStateOf(0) }
    // 每周 / 每月都是多选。保存时压成 Int 列表，避免依赖 Set/List 里枚举的默认序列化。
    var selectedWeekDays by rememberSaveable(
        stateSaver = listSaver(
            save = { days -> days.map { it.crontabValue } },
            restore = { values ->
                values.map { value -> WeekDay.entries.first { it.crontabValue == value } }
            },
        ),
    ) { mutableStateOf(listOf(WeekDay.MONDAY)) }
    var selectedDaysOfMonth by rememberSaveable { mutableStateOf(listOf(1)) }
    var onceYear by rememberSaveable { mutableStateOf(today.get(Calendar.YEAR)) }
    var onceMonth by rememberSaveable { mutableStateOf(today.get(Calendar.MONTH) + 1) }
    var onceDay by rememberSaveable { mutableStateOf(today.get(Calendar.DAY_OF_MONTH)) }

    // ---------------------------------------------------------------- 弹窗状态
    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDayOfMonthPicker by remember { mutableStateOf(false) }

    // ---------------------------------------------------------------- 表达式
    val spec = CronSpec(
        dialect = dialect,
        recurrence = if (isOnce) Recurrence.ONCE else repeatMode,
        hour = hour,
        minute = minute,
        weekDays = selectedWeekDays,
        daysOfMonth = if (isOnce) listOf(onceDay) else selectedDaysOfMonth,
        month = if (isOnce) onceMonth else 1,
        year = if (isOnce) onceYear else today.get(Calendar.YEAR),
    )
    val expression = buildCronExpression(spec)
    val notes = dialectNotes(spec)
    // 预览基准取进入页面时的时刻；选任何选项都会重算，但时间锚点不再漂移。
    val now = remember { System.currentTimeMillis() }
    val upcoming = nextRuns(spec, now)

    fun copyExpression() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("cron", expression))
        Toast.makeText(context, "表达式已复制", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Cron 表达式") },
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
            ExpressionCard(
                expression = expression,
                hint = dialect.fieldHint,
                notes = notes,
                onCopy = ::copyExpression,
            )

            NextRunsCard(times = upcoming)

            SectionLabel("方言")
            ChipRow(
                options = CronDialect.entries,
                selected = dialect,
                label = { it.label },
                onSelect = { dialect = it },
            )

            SectionLabel("执行方式")
            ChipRow(
                options = listOf(false, true),
                selected = isOnce,
                label = { if (it) "单次执行" else "重复执行" },
                onSelect = { isOnce = it },
            )

            if (!isOnce) {
                SectionLabel("重复频率")
                ChipRow(
                    options = listOf(Recurrence.DAILY, Recurrence.WEEKLY, Recurrence.MONTHLY),
                    selected = repeatMode,
                    label = { it.label },
                    onSelect = { repeatMode = it },
                )

                when (repeatMode) {
                    Recurrence.WEEKLY -> {
                        SectionLabel("星期（可多选）")
                        MultiChipRow(
                            options = WeekDay.entries,
                            selected = selectedWeekDays,
                            label = { it.label },
                            onToggle = { day ->
                                selectedWeekDays = selectedWeekDays.toggleKeepingOne(day)
                            },
                        )
                    }

                    Recurrence.MONTHLY -> SettingRow(
                        label = "每月日期（可多选）",
                        value = daysOfMonthLabel(selectedDaysOfMonth),
                        onClick = { showDayOfMonthPicker = true },
                    )

                    else -> Unit
                }
            } else {
                SectionLabel("日期")
                SettingRow(
                    label = "执行日期",
                    value = formatDate(onceYear, onceMonth, onceDay),
                    onClick = { showDatePicker = true },
                )
            }

            SectionLabel("时刻")
            SettingRow(
                label = "执行时刻",
                value = String.format(Locale.ROOT, "%02d:%02d", hour, minute),
                onClick = { showTimePicker = true },
            )

            // 与上方选择项留出间距，免得说明文字贴着最后一行
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialHour = hour,
            initialMinute = minute,
            onDismiss = { showTimePicker = false },
            onConfirm = { h, m ->
                hour = h
                minute = m
                showTimePicker = false
            },
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = utcMillisOf(onceYear, onceMonth, onceDay),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val (y, m, d) = readUtcDate(millis)
                        onceYear = y
                        onceMonth = m
                        onceDay = d
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDayOfMonthPicker) {
        AlertDialog(
            onDismissRequest = { showDayOfMonthPicker = false },
            title = { Text("选择每月日期") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items((1..31).toList()) { day ->
                        val checked = day in selectedDaysOfMonth
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable {
                                    // 弹窗不自动关闭，用户可连续勾选多天
                                    selectedDaysOfMonth = selectedDaysOfMonth.toggleKeepingOne(day)
                                }
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "$day 号",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (checked) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.weight(1f),
                            )
                            if (checked) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDayOfMonthPicker = false }) { Text("确定") }
            },
        )
    }
}

// ---------------------------------------------------------------------------- 组件

/**
 * 结果卡：表达式本身 + 字段提示 + 方言能力差异。
 *
 * 用液态玻璃与全 App 的卡片材质保持一致；表达式用等宽字体，避免 0/O 看混。
 */
@Composable
private fun ExpressionCard(
    expression: String,
    hint: String,
    notes: List<String>,
    onCopy: () -> Unit,
) {
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
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "表达式",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "复制表达式",
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Text(
            text = expression,
            style = MaterialTheme.typography.titleLarge,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "字段顺序：$hint",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        notes.forEach { note ->
            Text(
                text = "· $note",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 接下来 5 次触发预览。
 *
 * 时刻直接由结构化的 [CronSpec] 推算（见 `CronNextRun.kt`），不解析表达式字符串；
 * 单次执行已过去时列表为空，这里显示「该时刻已过」而不是留一张空卡。
 */
@Composable
private fun NextRunsCard(times: List<Long>) {
    val shape = CardDefaults.shape
    val glassState = rememberConvertGlassState()
    val formatter = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm EEEE", Locale.CHINESE)
    }

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
    ) {
        Text(
            text = "接下来 5 次",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (times.isEmpty()) {
            Text(
                text = "该时刻已过",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            times.forEachIndexed { index, millis ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = formatter.format(Date(millis)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
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

/**
 * 一排可横向换行的单选 chip。7 个星期在窄屏上放不下一行，用 [FlowRow] 自动折行，
 * 不做横向滚动 —— 选项必须全部可见。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

/** 多选版本：每个 chip 独立开关，选中集合由调用方维护。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> MultiChipRow(
    options: List<T>,
    selected: List<T>,
    label: (T) -> String,
    onToggle: (T) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option in selected,
                onClick = { onToggle(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

/**
 * 多选切换；取消最后一个选中项时不响应 —— cron 的星期 / 日期字段不能为空，
 * 与其让表达式退化成非法字符串，不如让最后一项取消不掉。
 */
private fun <T> List<T>.toggleKeepingOne(item: T): List<T> = when {
    item !in this -> this + item
    size == 1 -> this
    else -> this - item
}

/** 每月日期行的摘要：3 天以内列具体日期，超过就只给数量，避免把这一行挤爆。 */
private fun daysOfMonthLabel(days: List<Int>): String =
    if (days.size <= 3) {
        days.sorted().joinToString("、") + " 号"
    } else {
        "已选 ${days.size} 天"
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

/**
 * 时刻选择弹窗。
 *
 * material3 的 [TimePicker] 宽约 328dp，塞进默认宽度的 AlertDialog 会被裁掉，
 * 所以用 [Dialog] + [Surface] 自己铺一层（AOSP 的 TimePickerDialog 示例同样是这个写法）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true,
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "选择时刻",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(16.dp))
                TimePicker(state = state)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("确定") }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------- 日期工具

/**
 * 把年月日转成 DatePicker 使用的 UTC 午夜毫秒。
 *
 * DatePicker 的 `selectedDateMillis` 约定为 UTC 零点，用本地时区构造会整体偏一天。
 */
private fun utcMillisOf(year: Int, month: Int, day: Int): Long =
    Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(year, month - 1, day)
    }.timeInMillis

/** 把 DatePicker 回传的 UTC 毫秒还原成年月日。 */
private fun readUtcDate(millis: Long): Triple<Int, Int, Int> {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
    return Triple(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH),
    )
}

private fun formatDate(year: Int, month: Int, day: Int): String =
    String.format(Locale.ROOT, "%04d-%02d-%02d", year, month, day)
