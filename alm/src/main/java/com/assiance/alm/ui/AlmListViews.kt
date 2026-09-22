package com.assiance.alm.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.assiance.alm.AlarmAdapter
import com.assiance.alm.AlarmData
import com.assiance.alm.HistoryAdapter
import com.assiance.alm.TodoAdapter
import com.assiance.alm.TodoData

/**
 * 三个列表仍然复用**原有的 RecyclerView Adapter**（`AlarmAdapter` / `TodoAdapter` /
 * `HistoryAdapter`，任务要求「逻辑不动」），所以这里只做一层 `AndroidView` 桥接，
 * 把 Compose 的状态喂给 Adapter，并把 Adapter 的回调转回 Compose。
 *
 * 桥接对外只暴露 Compose 形态的入参/回调，屏幕里看不到任何 View 类型。
 */

/** 首页「闹钟」列表。原 `activity_main_alm.xml` 里的 `alarmListView`。 */
@Composable
internal fun AlarmListView(
    alarms: List<AlarmData>,
    onDeleteClick: (AlarmData) -> Unit,
    onToggleClick: (AlarmData, Boolean) -> Unit,
    onAlarmClick: (AlarmData) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentDelete by rememberUpdatedState(onDeleteClick)
    val currentToggle by rememberUpdatedState(onToggleClick)
    val currentClick by rememberUpdatedState(onAlarmClick)
    val horizontalPadding = with(LocalDensity.current) { LIST_HORIZONTAL_PADDING_DP.dp.roundToPx() }

    // 只有数据真的变了才 notifyDataSetChanged，避免每次重组都整表刷新
    var applied: List<AlarmData> by remember { mutableStateOf(emptyList()) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            RecyclerView(context).apply {
                layoutManager = LinearLayoutManager(context)
                setPadding(horizontalPadding, 0, horizontalPadding, 0)
                clipToPadding = false
                adapter = AlarmAdapter(
                    alarms = emptyList(),
                    onDeleteClick = { currentDelete(it) },
                    onToggleClick = { alarm, isEnabled -> currentToggle(alarm, isEnabled) },
                    onAlarmClick = { currentClick(it) },
                )
                (adapter as AlarmAdapter).attachToRecyclerView(this)
            }
        },
        update = { recyclerView ->
            if (applied != alarms) {
                (recyclerView.adapter as AlarmAdapter).updateAlarms(alarms)
                applied = alarms
            }
        },
    )
}

/** 首页「待办」列表。原 `activity_main_alm.xml` 里的 `todoListView`。 */
@Composable
internal fun TodoListView(
    todos: List<TodoData>,
    onDeleteClick: (TodoData) -> Unit,
    onToggleClick: (TodoData, Boolean) -> Unit,
    onTodoClick: (TodoData) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentDelete by rememberUpdatedState(onDeleteClick)
    val currentToggle by rememberUpdatedState(onToggleClick)
    val currentClick by rememberUpdatedState(onTodoClick)
    val horizontalPadding = with(LocalDensity.current) { LIST_HORIZONTAL_PADDING_DP.dp.roundToPx() }

    var applied: List<TodoData> by remember { mutableStateOf(emptyList()) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            RecyclerView(context).apply {
                layoutManager = LinearLayoutManager(context)
                setPadding(horizontalPadding, 0, horizontalPadding, 0)
                clipToPadding = false
                adapter = TodoAdapter(
                    todos = emptyList(),
                    onDeleteClick = { currentDelete(it) },
                    onToggleClick = { todo, isCompleted -> currentToggle(todo, isCompleted) },
                    onTodoClick = { currentClick(it) },
                )
                (adapter as TodoAdapter).attachToRecyclerView(this)
            }
        },
        update = { recyclerView ->
            if (applied != todos) {
                (recyclerView.adapter as TodoAdapter).updateTodos(todos)
                applied = todos
            }
        },
    )
}

/**
 * 历史记录列表。
 *
 * 与原实现一样，**数据由 `HistoryAdapter` 自己持有**（`updateItems` / `removeItemAt` 都由
 * 屏幕侧的过滤函数直接调用），所以这里只负责把 Adapter 绑到 RecyclerView 上，
 * 不在 `update` 里喂数据——这样「删除后 `removeItemAt` 的即时生效、以及源数据未变导致
 * 切换日期后可能复现」这两个原有行为都能原封不动保留。
 */
@Composable
internal fun HistoryListView(
    adapter: HistoryAdapter,
    modifier: Modifier = Modifier,
) {
    val verticalPadding = with(LocalDensity.current) { LIST_VERTICAL_PADDING_DP.dp.roundToPx() }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            RecyclerView(context).apply {
                layoutManager = LinearLayoutManager(context)
                setPadding(verticalPadding, verticalPadding, verticalPadding, verticalPadding)
                clipToPadding = false
                isVerticalScrollBarEnabled = true
                this.adapter = adapter
            }
        },
        update = { recyclerView ->
            if (recyclerView.adapter !== adapter) {
                recyclerView.adapter = adapter
            }
        },
    )
}

private const val LIST_HORIZONTAL_PADDING_DP = 8
private const val LIST_VERTICAL_PADDING_DP = 8
