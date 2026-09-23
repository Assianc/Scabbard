package com.assiance.alm.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import com.assiance.alm.AlarmData
import com.assiance.alm.AlmRepository
import com.assiance.alm.HistoryItem
import com.assiance.alm.TodoData
import java.util.Calendar

/**
 * 日历页（原 `CalendarActivity`）。
 *
 * 2026-09-23 **整页照 LoveScore 的约定页（`AgreementScreen.kt`）重做**（用户拍板），
 * 页面结构从「系统 CalendarView + 一个历史记录列表」变成
 * 「月历卡 + 当日摘要卡 + 当日面板」三块纵向排布，左右滑动切月份。
 * 三块各自实现在 [CalendarViews]，本文件只负责持有状态、串数据、处理删除与跳转。
 *
 * 相对原实现的保留与变化：
 * - **日期过滤口径一字未改** —— 原 `updateHistoryForDate()` 那套判断原样搬进了
 *   [filterDay]（含几处绕但有意为之的分支），所以「某天能看到哪些待办/闹钟」与旧版一致。
 * - **数据来源不变**，仍是 `AlmRepository.loadHistoryItems`（闹钟展开未来 7 天 + 全量待办）。
 *   月历往过去/未来滑得更远时，闹钟标记会消失——那是这个数据源的既有边界，不是本次引入的。
 * - **长按删除保留**，含「当天记录不能删除」的 Toast 与确认框文案（逐字照搬）。
 * - **新增**：点列表项进编辑页、右上角 Add 进新增页（照约定页加的入口，原实现点击无反应）；
 *   「跳转到指定日期」不再单设按钮，改为点月历卡的年月标题弹三列滚轮。
 * - 删除后**直接从 `historyItems` 里移除**（旧版是从 Adapter 的私有副本里移除、源数据不动，
 *   所以切走再切回被删的项会回来）。现在列表直接由 `historyItems` 渲染，源数据就是唯一副本，
 *   这个旧缺陷随之消失——不是专门修的，是数据流简化后的自然结果。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarScreen(navController: NavController) {
    val context = LocalContext.current

    /** 全量数据（闹钟 + 待办）。 */
    var historyItems by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }

    /** 当前选中的日期。 */
    var selectedDay by rememberSaveable(stateSaver = calendarDaySaver) {
        mutableStateOf(currentCalendarDay())
    }

    /** 月历当前显示的年 / 月 —— 与 [selectedDay] 独立：翻月不改选中日期。 */
    var viewYear by rememberSaveable { mutableIntStateOf(selectedDay.year) }
    var viewMonth by rememberSaveable { mutableIntStateOf(selectedDay.month) }

    /** 待确认删除的项。 */
    var pendingDelete by remember { mutableStateOf<DeleteTarget?>(null) }

    fun loadData() {
        historyItems = AlmRepository.loadHistoryItems(context)
    }

    LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        // 从新增 / 编辑页返回时要重新读一遍，否则改完看不到变化
        // （原实现只在 onCreate 里 load 一次）。与 AlmHomeScreen 同一处理。
        loadData()
    }

    /** 选中日期当天的待办与闹钟。 */
    val dayItems = remember(historyItems, selectedDay) {
        filterDay(historyItems, selectedDay.toMillis())
    }

    // 删除回调：当天（"今天"是选中日）不允许删，其余进确认框。
    // 判据用 isSameDay 而不是比较毫秒：选中日期可能来自滚轮（当天 0 点），
    // 而当前时间带时分秒，直接比毫秒永远不等。
    fun requestDelete(target: DeleteTarget) {
        if (isSameDay(selectedDay.toMillis(), System.currentTimeMillis())) {
            Toast.makeText(context, "当天记录不能删除", Toast.LENGTH_SHORT).show()
        } else {
            pendingDelete = target
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除历史记录") },
            text = { Text("确定删除该历史记录吗？(对应存在的待办和闹钟也会删除)") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    when (target) {
                        is DeleteTarget.Alarm -> {
                            AlmRepository.deleteHistoryAlarm(context, target.alarm.id)
                            historyItems = historyItems.filterNot {
                                it is HistoryItem.AlarmItem &&
                                    it.alarm.id == target.alarm.id
                            }
                        }

                        is DeleteTarget.Todo -> {
                            AlmRepository.deleteHistoryTodo(context, target.todo.id)
                            historyItems = historyItems.filterNot {
                                it is HistoryItem.TodoItem &&
                                    it.todo.id == target.todo.id
                            }
                        }
                    }
                    Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show()
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("日历") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                CalendarCard(
                    viewYear = viewYear,
                    viewMonth = viewMonth,
                    selectedDate = selectedDay,
                    today = currentCalendarDay(),
                    items = historyItems,
                    onSelectDate = { day ->
                        selectedDay = day
                        // 选中的日期可能落在相邻月份（格子里灰显的那几天），
                        // 月历要跟着切过去，否则会停在旧月份、看不到自己选的那天。
                        viewYear = day.year
                        viewMonth = day.month
                    },
                    onJumpToMonth = { year, month ->
                        viewYear = year
                        viewMonth = month
                    },
                    onJumpToToday = {
                        val today = currentCalendarDay()
                        selectedDay = today
                        viewYear = today.year
                        viewMonth = today.month
                    },
                )
            }

            item {
                TodaySummaryCard(day = selectedDay, dayItems = dayItems)
            }

            item {
                DayPanelCard(
                    day = selectedDay,
                    today = currentCalendarDay(),
                    dayItems = dayItems,
                    onAddTodo = { navController.navigate(AlmRoutes.todoSetting()) },
                    onAddAlarm = { navController.navigate(AlmRoutes.alarmSetting()) },
                    onTodoClick = { todo -> navController.navigate(todo.toEditRoute()) },
                    onAlarmClick = { alarm -> navController.navigate(alarm.toEditRoute()) },
                    onDeleteTodo = { requestDelete(DeleteTarget.Todo(it)) },
                    onDeleteAlarm = { requestDelete(DeleteTarget.Alarm(it)) },
                )
            }
        }
    }
}

/** 待确认删除的目标。 */
private sealed interface DeleteTarget {
    data class Alarm(val alarm: AlarmData) : DeleteTarget
    data class Todo(val todo: TodoData) : DeleteTarget
}

/** 进编辑页的路由，字段与 `MainActivityAlm.editTodo()` / `editAlarm()` 传的那批一致。 */
private fun TodoData.toEditRoute(): String = AlmRoutes.todoSetting(
    todoId = id,
    title = title,
    description = description,
    startTime = startTime ?: -1L,
    dueTime = dueTime ?: -1L,
    startRingtoneUri = startRingtoneUri,
    dueRingtoneUri = dueRingtoneUri,
)

private fun AlarmData.toEditRoute(): String = AlmRoutes.alarmSetting(
    alarmId = id,
    alarmTime = timeInMillis,
    ringtoneUri = ringtoneUri,
)

/** 今天（本地时区）。 */
private fun currentCalendarDay(): CalendarDay = Calendar.getInstance().let {
    CalendarDay(
        year = it.get(Calendar.YEAR),
        month = it.get(Calendar.MONTH) + 1,
        day = it.get(Calendar.DAY_OF_MONTH),
    )
}

/**
 * [CalendarDay] 的 `rememberSaveable` 存取器。
 *
 * 存成三个 Int 而不是让 CalendarDay 实现 Parcelable：它是个纯数据的小结构，
 * 为一次旋转去引 parcelize 插件不划算。
 */
private val calendarDaySaver = listSaver<CalendarDay, Int>(
    save = { listOf(it.year, it.month, it.day) },
    restore = { CalendarDay(year = it[0], month = it[1], day = it[2]) },
)
