package com.assiance.alm.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assiance.alm.AlarmData
import com.assiance.alm.R
import com.assiance.alm.TodoData
import io.github.nadeemiqbal.liquidglass.liquidGlass
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 首页「闹钟 / 待办」两个列表，以及历史记录列表。
 *
 * **闹钟 / 待办在 2026-09-23 从 XML RecyclerView 重写成了 Compose**：它们原来是
 * `AndroidView` 把 Compose 状态喂给 `AlarmAdapter` / `TodoAdapter`，而液态玻璃
 * （`Modifier.liquidGlass`）用不到 XML View 上 —— Android 上单个 View 无法模糊"背后的
 * 内容"（那是窗口级的 `setBackgroundBlurRadius`）；把 ComposeView 嵌进每个 item 也不行，
 * 跨 ComposeView 边界拿不到宿主的 `CompositionLocal`，玻璃会静默退化成不模糊。
 * 要真玻璃只有把整个列表搬进同一个 composition。
 *
 * 重写时**逐条对齐原 Adapter 的行为**，每条在下面各自的 KDoc / 行内注释里标了出处。
 * 唯一的有意偏离是文字与状态色改成 `MaterialTheme.colorScheme`：原实现是硬编码的
 * `Color.BLACK` / `Color.GRAY`（XML 里还各写了一套 `#333333` 之类），压在半透明的玻璃面上
 * 深色主题下根本读不出来。
 *
 * 原先这里还有第三个列表「历史记录」([HistoryListView]，走 `HistoryAdapter` 的
 * RecyclerView)。2026-09-23 日历页照约定页重做时，它被替换成了 Compose 渲染的
 * 「当日面板」，`HistoryAdapter` 与它的三个 item 布局一并删除，见 [CalendarScreen]。
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
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = LIST_HORIZONTAL_PADDING_DP.dp,
            vertical = LIST_VERTICAL_PADDING_DP.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(ITEM_VERTICAL_SPACING),
    ) {
        items(alarms, key = { it.id }) { alarm ->
            AlmGlassListItem(
                // 原 Adapter：点击进设置页，长按删除（没有"已完成"那套状态分叉）
                onClick = { onAlarmClick(alarm) },
                onLongClick = { onDeleteClick(alarm) },
                onDelete = { onDeleteClick(alarm) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = alarmTimeFormatter.format(alarm.timeInMillis),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = buildRepeatText(alarm.repeatDays),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = alarm.isEnabled,
                        onCheckedChange = { onToggleClick(alarm, it) },
                        modifier = Modifier.padding(end = 16.dp),
                    )
                }
            }
        }
    }
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
    // 原 Adapter 里"已完成的待办要长按弹框才能恢复"那个 AlertDialog
    var restoring by remember { mutableStateOf<TodoData?>(null) }

    restoring?.let { todo ->
        AlertDialog(
            onDismissRequest = { restoring = null },
            title = { Text("恢复待办") },
            text = { Text("是否将此待办恢复为未完成状态？") },
            confirmButton = {
                TextButton(onClick = {
                    restoring = null
                    onToggleClick(todo, false)
                }) { Text("恢复") }
            },
            dismissButton = {
                TextButton(onClick = { restoring = null }) { Text("取消") }
            },
        )
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = LIST_HORIZONTAL_PADDING_DP.dp,
            vertical = LIST_VERTICAL_PADDING_DP.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(ITEM_VERTICAL_SPACING),
    ) {
        items(todos, key = { it.id }) { todo ->
            AlmGlassListItem(
                onClick = { onTodoClick(todo) },
                // 原 Adapter：未完成的长按是删除，已完成的才有长按（弹恢复框）
                onLongClick = {
                    if (todo.isCompleted) restoring = todo else onDeleteClick(todo)
                },
                onDelete = { onDeleteClick(todo) },
                // 原 ItemTouchHelper.getSwipeDirs：已完成的待办返回 0，即不可侧滑
                swipeEnabled = !todo.isCompleted,
            ) {
                TodoItemContent(
                    todo = todo,
                    onToggleClick = onToggleClick,
                )
            }
        }
    }
}

@Composable
private fun TodoItemContent(
    todo: TodoData,
    onToggleClick: (TodoData, Boolean) -> Unit,
) {
    val contentColor = if (todo.isCompleted) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val strike = if (todo.isCompleted) TextDecoration.LineThrough else null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = todo.isCompleted,
            // 原 Adapter：已完成的条目点复选框不响应（它把状态按回 true），恢复只能靠长按
            onCheckedChange = { if (!todo.isCompleted) onToggleClick(todo, it) },
            modifier = Modifier.size(48.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        ) {
            // 原 Adapter：标题为空时它整条隐藏，描述顶上来变成 24sp 粗体
            if (todo.title.isNotEmpty()) {
                Text(
                    text = todo.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = strike,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (todo.description.isNotEmpty()) {
                    Text(
                        text = todo.description,
                        fontSize = 16.sp,
                        textDecoration = strike,
                        color = contentColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            } else if (todo.description.isNotEmpty()) {
                Text(
                    text = todo.description,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = strike,
                    color = contentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val timeText = buildTodoTimeText(todo)
            if (timeText.isNotEmpty()) {
                Text(
                    text = timeText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        if (todo.isCompleted) {
            Text(
                text = "已完成",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/**
 * 一条玻璃列表项：液态玻璃底 + 点击/长按 + 左滑删除。
 *
 * 侧滑那套参数逐条对齐原 `ItemTouchHelper` 的自定义 `onChildDraw`：
 * - 最多只能滑出条目宽度的 **1/3**（`maxSwipe`）；
 * - 滑过这个上限的 **75%** 就触发删除（[SWIPE_DELETE_TRIGGER]），且**只触发一次**；
 * - 条目本身随滑动渐隐（`alpha = 1 - |dX| / maxSwipe`），底下露出红底与删除图标；
 * - [swipeEnabled] 为 false 时完全不可滑（原实现里"已完成的待办不能侧滑"走的就是这条）。
 *
 * 有一处**原实现没写、这里补上**的：松手时若没滑过触发线，条目**弹回原位**。
 * 原实现只重置了 `alpha`，没重置 `translationX`，松手后条目会停在半路。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlmGlassListItem(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    swipeEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    // 玻璃面必须与宿主背景层共用同一个 state，否则 backdrop 那个 GraphicsLayer
    // 永远是空的、玻璃不会模糊 —— 宿主经 almGraph 传进来，见 AlmGlassContract。
    val glassState = rememberAlmGlassState()

    var itemWidth by remember { mutableIntStateOf(0) }
    val maxSwipe = (itemWidth / 3f).coerceAtLeast(1f)

    var dragOffset by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var deleteTriggered by remember { mutableStateOf(false) }

    // 拖动中要跟手（snap），松手才回弹（spring）—— 两者共用一个目标值，
    // 靠 dragging 切换动画规格。
    val offset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = if (dragging) snap() else spring(stiffness = Spring.StiffnessMediumLow),
        label = "almSwipeOffset",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { itemWidth = it.width },
    ) {
        // 露出来的那一层：原实现画在 itemView 右侧，这里做成整块背景 + 前景滑动，
        // 露出的部分与原来一致。
        //
        // 红底**必须裁到"已经滑开的那一条"**：原实现的条目是不透明的 CardView，红底只在
        // 条目滑走后才露出来；而这里的玻璃面是半透明的（tint alpha 0.3），整块铺红会让它
        // 从玻璃下面透上来，静止时整条泛红 —— 看着就像玻璃没生效。左边界跟着拖动偏移走，
        // 静止（offset = 0）时宽度为 0，与原来的观感一致。
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(ITEM_SHAPE)
                // 裁的是**整个 Box 的内容**（红底 + 删除图标），不是单画一个红矩形 ——
                // `drawBehind` 的裁剪只作用于它自己 draw 的东西，图标是在其后绘制的，
                // 不跟着裁就会留在原地、隔着半透明玻璃露出来。
                .drawWithContent {
                    clipRect(left = (size.width + offset).coerceAtMost(size.width)) {
                        this@drawWithContent.drawContent()
                    }
                }
                .background(Color.Red),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = "删除",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = DELETE_ICON_END_PADDING),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offset.roundToInt(), 0) }
                // 原实现：itemView.alpha = 1 - |limitedDX| / maxSwipe
                .alpha((1f - abs(offset) / maxSwipe).coerceIn(0f, 1f))
                .clip(ITEM_SHAPE)
                .liquidGlass(
                    state = glassState,
                    shape = ITEM_SHAPE,
                    tint = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = GLASS_TINT_ALPHA),
                    grain = glassGrainFor(glassState.quality),
                )
                // liquidGlass 只裁渲染，触摸命中仍由整条 modifier 链决定，所以这里再裁一次。
                .clip(ITEM_SHAPE)
                .pointerInput(swipeEnabled, maxSwipe) {
                    if (!swipeEnabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            dragging = false
                            // 没滑过触发线就弹回；已经触发删除的条目不用管
                            if (!deleteTriggered) dragOffset = 0f
                        },
                        onDragCancel = {
                            dragging = false
                            if (!deleteTriggered) dragOffset = 0f
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        dragging = true
                        val next = (dragOffset + dragAmount).coerceIn(-maxSwipe, 0f)
                        dragOffset = next
                        if (!deleteTriggered && -next >= maxSwipe * SWIPE_DELETE_TRIGGER) {
                            deleteTriggered = true
                            onDelete()
                        }
                    }
                }
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        ) {
            content()
        }
    }
}

private val alarmTimeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

private val todoTimeFormatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

/** 原 `AlarmAdapter.buildRepeatText`，逐个分支照搬。 */
private fun buildRepeatText(repeatDays: BooleanArray): String {
    val days = arrayOf("一", "二", "三", "四", "五", "六", "日")
    val selectedDays = repeatDays.mapIndexed { index, selected ->
        if (selected) days[index] else null
    }.filterNotNull()

    return when {
        selectedDays.isEmpty() -> "不重复"
        selectedDays.size == 7 -> "每天"
        selectedDays.size == 5 && !repeatDays[5] && !repeatDays[6] -> "工作日"
        selectedDays.size == 2 && repeatDays[5] && repeatDays[6] -> "周末"
        else -> "每周${selectedDays.joinToString("、")}"
    }
}

/** 原 `TodoAdapter.buildTimeText`，逐个分支照搬（含"两个时间都有时才换行"）。 */
private fun buildTodoTimeText(todo: TodoData): String {
    val sb = StringBuilder()

    todo.startTime?.let {
        sb.append("开始于：")
        sb.append(todoTimeFormatter.format(it))
    }

    if (todo.startTime != null && todo.dueTime != null) {
        sb.append("\n")
    }

    todo.dueTime?.let {
        sb.append("截止于：")
        sb.append(todoTimeFormatter.format(it))
    }

    return sb.toString()
}

/** 卡片圆角，对齐原 item 布局的 `app:cardCornerRadius="8dp"`。 */
private val ITEM_SHAPE = RoundedCornerShape(8.dp)

/** 相邻条目间距：原布局每项上下各 4dp 的 `layout_marginVertical`。 */
private val ITEM_VERTICAL_SPACING = 8.dp

/**
 * 删除图标距条目右边缘的距离。
 *
 * 原实现取 `(itemHeight - iconHeight) / 2`，也就是跟着条目高度浮动；Compose 里条目高度
 * 由内容决定、拿不到那个值，这里按条目的常见高度取等值近似。
 */
private val DELETE_ICON_END_PADDING = 16.dp

/** 原 `ItemTouchHelper` 里"滑过上限的 75% 就触发删除"的阈值。 */
private const val SWIPE_DELETE_TRIGGER = 0.75f

/**
 * 列表左右内边距。原实现是 RecyclerView 的 8dp padding **加上** item 自己 8dp 的
 * `layout_marginHorizontal`，合起来 16dp。
 */
private const val LIST_HORIZONTAL_PADDING_DP = 16

private const val LIST_VERTICAL_PADDING_DP = 8
