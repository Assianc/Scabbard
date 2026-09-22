package com.assiance.alm.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.NumberPicker
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.assiance.alm.AlmRepository
import com.assiance.alm.HistoryAdapter
import com.assiance.alm.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 历史记录页（原 `CalendarActivity`）。
 *
 * 逻辑与原实现逐条对齐：
 * - 系统 `CalendarView` 仍然原样使用（只能 XML 声明文字样式，所以经 [R.layout.view_calendar]
 *   载入后挂到 Compose 里）；
 * - `loadData()` 的「闹钟未来 7 天展开 + 待办筛选 + 合并排序」原样搬到
 *   [AlmRepository.loadHistoryItems]；
 * - `updateHistoryForDate()` 的日期过滤、日期标题文案（`yyyy年MM月dd日 EEEE` / `Locale.CHINESE`）
 *   逐行照搬；
 * - 长按删除：当天（`currentDate` 是今天）直接 Toast「当天记录不能删除」，否则弹确认框，
 *   确认后写库 + Toast「删除成功」+ `HistoryAdapter.removeItemAt(position)`。
 *
 * 注意：与原实现一样，被删除的项**只从 Adapter 里移除**，`historyItems` 源数据不变，
 * 所以切到别的日期再切回来，被删的记录可能会重新出现——这是原有缺陷，这里没有"顺手修"。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarScreen(navController: NavController) {
    val context = LocalContext.current

    val historyAdapter = remember { HistoryAdapter() }

    var allHistoryItems by remember {
        mutableStateOf<List<HistoryAdapter.HistoryItem>>(emptyList())
    }
    var currentDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var dateTitle by remember { mutableStateOf("") }
    var pendingDelete by remember {
        mutableStateOf<Pair<HistoryAdapter.HistoryItem, Int>?>(null)
    }
    var showJumpDialog by remember { mutableStateOf(false) }

    // CalendarView 的文字样式只能在 XML 里声明，这里用最小布局承载它
    val calendarView = remember {
        LayoutInflater.from(context).inflate(R.layout.view_calendar, null) as CalendarView
    }

    // ---- 原 updateHistoryForDate()：过滤 + 更新列表 + 更新标题
    fun updateHistoryForDate(date: Long) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val nextDay = Calendar.getInstance().apply {
            timeInMillis = calendar.timeInMillis
            add(Calendar.DAY_OF_YEAR, 1)
        }

        val currentTime = System.currentTimeMillis()
        val currentDay = Calendar.getInstance().apply {
            timeInMillis = currentTime
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 过滤出选定日期的项目
        val filteredItems = allHistoryItems.filter { item ->
            when (item) {
                is HistoryAdapter.HistoryItem.AlarmItem -> {
                    val itemCal = Calendar.getInstance().apply {
                        timeInMillis = item.alarm.timeInMillis
                    }
                    itemCal.timeInMillis >= calendar.timeInMillis &&
                        itemCal.timeInMillis < nextDay.timeInMillis
                }

                is HistoryAdapter.HistoryItem.TodoItem -> {
                    val todo = item.todo
                    val startTime = todo.startTime
                    val dueTime = todo.dueTime
                    val isCompleted = todo.isCompleted
                    val completedTime = todo.completedTime

                    when {
                        // 已完成的待办
                        isCompleted -> {
                            if (startTime != null && dueTime == null) {
                                // 对于只有开始日期的已完成待办，显示从开始当天到完成当天
                                val startCal = Calendar.getInstance().apply {
                                    timeInMillis = startTime
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                val compCal = Calendar.getInstance().apply {
                                    timeInMillis = completedTime ?: System.currentTimeMillis()
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                calendar.timeInMillis in
                                    startCal.timeInMillis..compCal.timeInMillis
                            } else {
                                // 其它已完成待办：只显示完成当天（原逻辑）
                                val actualCompletedTime =
                                    completedTime ?: dueTime ?: System.currentTimeMillis()
                                isSameDay(actualCompletedTime, date)
                            }
                        }
                        // 未完成的待办，且有开始时间或截止时间
                        startTime != null || dueTime != null -> {
                            when {
                                // 只有开始时间：显示从待办设置当天开始到当前日期
                                startTime != null && dueTime == null -> {
                                    val startCal = Calendar.getInstance().apply {
                                        timeInMillis = startTime
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    calendar.timeInMillis >= startCal.timeInMillis &&
                                        calendar.timeInMillis <= currentDay.timeInMillis
                                }
                                // 只有截止时间：使用创建时间判断
                                startTime == null && dueTime != null -> {
                                    date >= todo.createdTime &&
                                        (date <= dueTime || isSameDay(date, dueTime))
                                }
                                // 同时有开始和截止时间：显示在该区间内
                                startTime != null && dueTime != null -> {
                                    date >= startTime && (date <= dueTime || isSameDay(date, dueTime))
                                }

                                else -> false
                            }
                        }
                        // 无时间限制的待办
                        else -> {
                            date >= todo.createdTime &&
                                (date <= currentDay.timeInMillis ||
                                    isSameDay(date, todo.createdTime))
                        }
                    }
                }
            }
        }

        historyAdapter.updateItems(filteredItems)

        // 更新日期标题
        val dateFormat = SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINESE)
        dateTitle = dateFormat.format(date)
    }

    // ---- 原 loadData()：读全量数据 → 过滤当前日期
    fun loadData() {
        allHistoryItems = AlmRepository.loadHistoryItems(context)
        updateHistoryForDate(calendarView.date)
    }

    DisposableEffect(calendarView) {
        // 原实现的 setOnDateChangeListener
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }
            // 更新当前选中的日期
            currentDate = calendar.timeInMillis
            updateHistoryForDate(currentDate)
        }
        onDispose { calendarView.setOnDateChangeListener(null) }
    }

    LaunchedEffect(Unit) {
        // 初始化当前选中日期（默认为日历的当前日期）
        currentDate = calendarView.date
        loadData()
    }

    // 长按删除回调（Adapter 内部持有，这里每次都刷新，保证读到最新的状态）
    historyAdapter.onDeleteClick = { historyItem, position ->
        if (isSameDay(currentDate, System.currentTimeMillis())) {
            Toast.makeText(context, "当天记录不能删除", Toast.LENGTH_SHORT).show()
        } else {
            pendingDelete = historyItem to position
        }
    }

    // ---- 删除确认框
    pendingDelete?.let { (historyItem, position) ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除历史记录") },
            text = { Text("确定删除该历史记录吗？(对应存在的待办和闹钟也会删除)") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    when (historyItem) {
                        is HistoryAdapter.HistoryItem.AlarmItem ->
                            AlmRepository.deleteHistoryAlarm(context, historyItem.alarm.id)

                        is HistoryAdapter.HistoryItem.TodoItem ->
                            AlmRepository.deleteHistoryTodo(context, historyItem.todo.id)
                    }
                    Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show()
                    // 立即移除该项
                    historyAdapter.removeItemAt(position)
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }

    // ---- 跳转到指定日期
    if (showJumpDialog) {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        var yearValue by remember { mutableIntStateOf(currentYear) }
        var monthValue by remember { mutableIntStateOf(1) }

        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("跳转到指定日期") },
            text = {
                Column {
                    AndroidView(
                        factory = { ctx ->
                            NumberPicker(ctx).apply {
                                minValue = currentYear - 10
                                maxValue = currentYear + 10
                                value = currentYear
                                setOnValueChangedListener { _, _, newValue ->
                                    yearValue = newValue
                                }
                            }
                        },
                        update = { picker ->
                            if (picker.value != yearValue) picker.value = yearValue
                        },
                    )
                    AndroidView(
                        factory = { ctx ->
                            NumberPicker(ctx).apply {
                                minValue = 1
                                maxValue = 12
                                displayedValues = MONTH_DISPLAY_VALUES
                                setOnValueChangedListener { _, _, newValue ->
                                    monthValue = newValue
                                }
                            }
                        },
                        update = { picker -> if (picker.value != monthValue) picker.value = monthValue },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showJumpDialog = false
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.YEAR, yearValue)
                        set(Calendar.MONTH, monthValue - 1)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                    calendarView.date = calendar.timeInMillis
                    updateHistoryForDate(calendar.timeInMillis)
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) { Text("取消") }
            },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("历史记录") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showJumpDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = "跳转到指定日期",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { _ ->
                        // AndroidView 重新挂载时，先把它从旧的父容器摘下来
                        (calendarView.parent as? ViewGroup)?.removeView(calendarView)
                        calendarView
                    },
                    update = { },
                )
            }

            Text(
                text = dateTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            HistoryListView(
                adapter = historyAdapter,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }
}

/** 原 `CalendarActivity.showDateJumpDialog()` 的月份显示值。 */
private val MONTH_DISPLAY_VALUES = arrayOf(
    "1月", "2月", "3月", "4月", "5月", "6月",
    "7月", "8月", "9月", "10月", "11月", "12月",
)

/** 原 `CalendarActivity.isSameDay()`。 */
private fun isSameDay(time1: Long, time2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
