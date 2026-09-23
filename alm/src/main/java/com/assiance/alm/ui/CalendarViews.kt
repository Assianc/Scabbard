package com.assiance.alm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.assiance.alm.AlarmData
import com.assiance.alm.HistoryItem
import com.assiance.alm.TodoData
import io.github.nadeemiqbal.liquidglass.liquidGlass
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 日历页的三块内容：月历卡 / 当日摘要卡 / 当日面板。
 *
 * **整体照 LoveScore 约定页（`AgreementScreen.kt`）重做**，是用户 2026-09-23 拍板的
 * 「日历整页照约定页重做」。对齐与偏离逐条记在下面各组件上；下面是相对约定页
 * **有意偏离**的汇总（都是 alm 没有对应数据/入口造成的，不是随手改的）：
 *
 * 1. 约定页的当日面板是三页（待办 / 闹铃 / **打卡**），alm 没有打卡这类数据，
 *    用户拍板做成**两页：待办 / 闹钟**。
 * 2. 约定页的月历格子有三种标记样式可切换（图标 / 横杠 / 圆点），那是为了在一格里
 *    区分三类内容。alm 只有两类，固定用圆点，不做样式切换按钮。
 * 3. 约定页的摘要卡有「打卡」一项，alm 换成「闹钟**条数**」——`AlarmData` 只有
 *    `isEnabled`，没有「今天这条响过了没」的字段，凑不出完成度来，所以不做进度。
 *
 * 其余（月份 Pager 的 PAGE_OFFSET、6×7 固定格、周日开头、农历+干支三行标题、
 * 点年月弹滚轮、回到今天、右上角 Add）都按约定页的口径来。
 */

// ============================================================ 玻璃卡容器

/**
 * 日历页统一的玻璃卡片容器。
 *
 * 材质参数与 alm 其它玻璃面（见 [com.assiance.alm.ui.AlmListViews] 的列表项）同一套：
 * `rememberAlmGlassState()` 拿宿主经 `almGraph` 传进来的共享 state +
 * [GLASS_TINT_ALPHA] / [glassGrainFor]。**不要在这里自己 `rememberLiquidGlassState()`** ——
 * 换了实例玻璃就永远不模糊，原因见 [LocalAlmGlassState] 的注释。
 *
 * 约定页的对应物是 `DsCard(material = Material.Glass)`。
 */
@Composable
private fun AlmGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val glassState = rememberAlmGlassState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CARD_SHAPE)
            .liquidGlass(
                state = glassState,
                shape = CARD_SHAPE,
                tint = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = GLASS_TINT_ALPHA),
                grain = glassGrainFor(glassState.quality),
            )
            // liquidGlass 只裁渲染，触摸命中仍由整条 modifier 链决定，所以这里再裁一次
            // （与 AlmListViews 的列表项同一处理）。
            .clip(CARD_SHAPE)
            .padding(CARD_PADDING),
        content = content,
    )
}

// ============================================================ 月历卡

/**
 * 月历卡：标题行（左右翻月 + 可点年月）+ 周表头 + 月份 Pager + 「回到今天」。
 *
 * 月份 Pager 的页码换算与「程序滚动 / 用户滑动」的防抖处理**逐行照搬约定页**，
 * 那套 `isProgrammaticScroll` 标记是踩过坑的：不加的话，程序滚动过程中的中间帧会被当成
 * 用户操作写回 viewYear/viewMonth，重算 pagerStartIndex，进而让 Pager 又滚回去，
 * 表现为「漂移」甚至死循环。
 *
 * @param viewYear  当前显示的年份（外部状态，由 [CalendarScreen] 持有）
 * @param viewMonth 当前显示的月份（1..12）
 * @param selectedDate 当前选中的日期
 * @param items     全量数据（内部按格子过滤出标记）
 */
@Composable
internal fun CalendarCard(
    viewYear: Int,
    viewMonth: Int,
    selectedDate: CalendarDay,
    today: CalendarDay,
    items: List<HistoryItem>,
    onSelectDate: (CalendarDay) -> Unit,
    onJumpToMonth: (Int, Int) -> Unit,
    onJumpToToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 点年月标题弹出的滚轮选择器
    var showDatePicker by remember { mutableStateOf(false) }

    val monthsFromToday = (viewYear - today.year) * 12 + (viewMonth - today.month)
    val pagerStartIndex = PAGE_OFFSET + monthsFromToday
    val pagerState = rememberPagerState(
        initialPage = pagerStartIndex,
        pageCount = { PAGE_TOTAL },
    )
    val scope = rememberCoroutineScope()

    // 是否正在由「外部 viewYear/viewMonth 变化」触发的程序滚动。滚动期间不要把中间帧
    // 当作用户操作回调（否则反向滚动 / 漂移 / 死循环），详见约定页的同名标记。
    val isProgrammaticScroll = remember { mutableStateOf(false) }

    // 外部 viewYear/viewMonth 变化（滚轮确认 / 回到今天）→ 同步 Pager 滚到对应页。
    // 用户左右滑动时 currentPage 也会变，但 pagerStartIndex 跟着重算，此时两者相等，
    // 下面的判断可以避免循环触发。
    LaunchedEffect(pagerStartIndex) {
        if (pagerState.currentPage != pagerStartIndex) {
            isProgrammaticScroll.value = true
            pagerState.animateScrollToPage(pagerStartIndex)
            isProgrammaticScroll.value = false
        }
    }

    // Pager 滑动 → 同步 viewYear/viewMonth（不动 selectedDate，保留用户选过的天）。
    LaunchedEffect(pagerState.currentPage) {
        if (isProgrammaticScroll.value) return@LaunchedEffect
        val offset = pagerState.currentPage - PAGE_OFFSET
        // 必须用 floorDiv/floorMod：Kotlin 的 `/` 对负数是向 0 截断，
        // 会让负 offset 算错一年（如 -287/12 = -23，而向下取整是 -24）。
        val totalMonthsFromYearZero = (today.year * 12 + (today.month - 1)) + offset
        val newYear = floorDiv(totalMonthsFromYearZero, 12)
        val newMonth = floorMod(totalMonthsFromYearZero, 12) + 1
        if (newYear != viewYear || newMonth != viewMonth) {
            onJumpToMonth(newYear, newMonth)
        }
    }

    AlmGlassCard(modifier = modifier) {
        // —— 标题行 ——
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = {
                scope.launch {
                    val target = (pagerState.currentPage - 1).coerceAtLeast(0)
                    pagerState.animateScrollToPage(target)
                }
            }) {
                Icon(
                    imageVector = Icons.Outlined.ChevronLeft,
                    contentDescription = "上个月",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "$viewYear · ${viewMonth}月",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = {
                scope.launch {
                    val target = (pagerState.currentPage + 1).coerceAtMost(PAGE_TOTAL - 1)
                    pagerState.animateScrollToPage(target)
                }
            }) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "下个月",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // —— 周表头：固定在 Pager 外，不跟着月份滚 ——
        Row(modifier = Modifier.fillMaxWidth()) {
            WEEKDAY_LABELS.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        // —— 6×7 日期格子：包进 HorizontalPager，左右滑动切换月份 ——
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            beyondViewportPageCount = 0,
            pageSpacing = 0.dp,
            userScrollEnabled = true,
        ) { pageIndex ->
            val offset = pageIndex - PAGE_OFFSET
            val totalMonthsFromYearZero = (today.year * 12 + (today.month - 1)) + offset
            MonthGrid(
                year = floorDiv(totalMonthsFromYearZero, 12),
                month = floorMod(totalMonthsFromYearZero, 12) + 1,
                today = today,
                selectedDate = selectedDate,
                items = items,
                onSelectDate = onSelectDate,
            )
        }

        Spacer(Modifier.height(8.dp))

        // —— 右下角：跳回今天（约定页左下角那个「标记样式切换」alm 不做，见文件头）——
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onJumpToToday) {
                Text(
                    text = "回到今天",
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    // —— 点年月 → 三列滚轮选日期 ——
    if (showDatePicker) {
        // key(selectedDate)：换一天再打开时重新用当天的值初始化候选日期，而不是留着上次的。
        // 内部维护一个候选值，滑动过程不写回外层，只有「确定」才回调 —— 中途取消等于没选过。
        var pickedYear by remember(selectedDate) { mutableIntStateOf(selectedDate.year) }
        var pickedMonth by remember(selectedDate) { mutableIntStateOf(selectedDate.month) }
        var pickedDay by remember(selectedDate) { mutableIntStateOf(selectedDate.day) }

        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            title = { Text("跳转到指定日期") },
            text = {
                // 年月日是同一次选择的三个字段，共用一条横贯三列的指示带，
                // 所以三列都由 WheelDatePicker 内部置 showSelectionBand = false。
                WheelDatePicker(
                    year = pickedYear,
                    month = pickedMonth,
                    day = pickedDay,
                    onDateChange = { y, m, d ->
                        pickedYear = y
                        pickedMonth = m
                        pickedDay = d
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    onSelectDate(CalendarDay(pickedYear, pickedMonth, pickedDay))
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        )
    }
}

/**
 * 单个月份的 6×7 日期网格。
 *
 * 每格的标记（有无待办 / 闹钟）在这里一次算完：42 格 × 全量数据。放在 `remember` 里
 * 以 `items / year / month` 为键，滑 Pager 时只在月份真的变化的那一页重算。
 */
@Composable
private fun MonthGrid(
    year: Int,
    month: Int,
    today: CalendarDay,
    selectedDate: CalendarDay,
    items: List<HistoryItem>,
    onSelectDate: (CalendarDay) -> Unit,
) {
    val cells = remember(year, month) { buildMonthCells(year, month) }
    val marks = remember(items, year, month) {
        cells.associateWith { marksOn(items, it.toMillis()) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                week.forEach { cell ->
                    DayCell(
                        day = cell,
                        isCurrentMonth = cell.month == month,
                        isToday = cell == today,
                        isSelected = cell == selectedDate,
                        marks = marks[cell] ?: DayMarks(),
                        onClick = { onSelectDate(cell) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * 单日格子：日期数字 + 农历/节日 + 两个标记圆点。
 *
 * 三种状态叠加：选中（描边 + 数字染主题色）/ 今日（浅色底）/ 普通；非当月日期整体淡出。
 * 「今日」用底色、「选中」用描边，两者可以同时命中（今天正好被选中时既有底色又有描边）。
 *
 * 约定页的对应物是 `DayCell`，这里是它的 alm 版（去掉打卡相关的一切）。
 */
@Composable
private fun DayCell(
    day: CalendarDay,
    isCurrentMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    marks: DayMarks,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val bgColor = if (isToday) primary.copy(alpha = 0.12f) else Color.Transparent
    val textColor = if (isCurrentMonth) onSurface else onSurfaceVariant.copy(alpha = 0.4f)
    val selectedTextColor = if (isCurrentMonth) primary else onSurfaceVariant.copy(alpha = 0.6f)
    val borderModifier = if (isSelected) {
        Modifier.border(width = 1.5.dp, color = primary, shape = CELL_SHAPE)
    } else {
        Modifier
    }

    // 农历文字（如「正月」「十五」「立春」「春节」），转换失败时为空串
    val lunarText = remember(day) { AlmLunarUtils.formatLunar(day.year, day.month, day.day) }

    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(CELL_SHAPE)
            .background(bgColor)
            .then(borderModifier)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = day.day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) selectedTextColor else textColor,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            )
            if (lunarText.isNotEmpty()) {
                Text(
                    text = lunarText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCurrentMonth) {
                        onSurfaceVariant.copy(alpha = 0.65f)
                    } else {
                        onSurfaceVariant.copy(alpha = 0.35f)
                    },
                    maxLines = 1,
                )
            }
            if (marks.any) {
                Spacer(Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (marks.hasTodo) MarkerDot(CALENDAR_TODO_ACCENT)
                    if (marks.hasAlarm) MarkerDot(CALENDAR_ALARM_ACCENT)
                }
            }
        }
    }
}

/** 格子底部的小圆点标记。 */
@Composable
private fun MarkerDot(color: Color) {
    Box(
        modifier = Modifier
            .size(5.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(color),
    )
}

// ============================================================ 当日摘要卡

/**
 * 当日摘要卡：所选日期的待办 / 闹钟两项一览。
 *
 * 偏离约定页的一处：约定页的「闹铃」显示待响应数量，alm 的 `AlarmData` 只有 `isEnabled`、
 * 没有「今天这条响过了没」的字段，凑不出完成度，所以闹钟这项只显示**条数**（见文件头）。
 *
 * 这一天两类都空时只显示一句空态，不占多余高度。
 */
@Composable
internal fun TodaySummaryCard(
    day: CalendarDay,
    dayItems: DayItems,
    modifier: Modifier = Modifier,
) {
    AlmGlassCard(modifier = modifier) {
        if (dayItems.isEmpty) {
            Text(
                text = "这一天没有待办和闹钟",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@AlmGlassCard
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SummaryEntry(
                label = "待办",
                value = if (dayItems.todos.isEmpty()) {
                    "无"
                } else {
                    "${dayItems.todos.count { it.isCompleted }} / ${dayItems.todos.size}"
                },
                accent = CALENDAR_TODO_ACCENT,
                icon = Icons.Outlined.Check,
                modifier = Modifier.weight(1f),
            )
            SummaryEntry(
                label = "闹钟",
                value = if (dayItems.alarms.isEmpty()) "无" else "${dayItems.alarms.size} 个",
                accent = CALENDAR_ALARM_ACCENT,
                icon = Icons.Outlined.Alarm,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 摘要卡里的一项：图标 + 名称 + 数值，整项按语义色着色。 */
@Composable
private fun SummaryEntry(
    label: String,
    value: String,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// ============================================================ 当日面板

/**
 * 当日面板：日期标题 + 「待办 / 闹钟」两页 Pager。
 *
 * **两页**是用户 2026-09-23 拍板的（约定页是三页，第三页是 alm 没有的打卡）。
 *
 * 列表项**不再套玻璃**：整张卡已经是玻璃面了，里面每条再叠一层玻璃会糊成一团。
 * 约定页也是这个处理（卡片里是朴素的行）。
 *
 * 右上角 Add 按钮按当前页跳对应的新增页 —— 这是从约定页照搬的入口，原
 * `CalendarActivity` 没有；点列表项进编辑页同理（原实现点击无响应，见 [CalendarScreen]）。
 */
@Composable
internal fun DayPanelCard(
    day: CalendarDay,
    today: CalendarDay,
    dayItems: DayItems,
    onAddTodo: () -> Unit,
    onAddAlarm: () -> Unit,
    onTodoClick: (TodoData) -> Unit,
    onAlarmClick: (AlarmData) -> Unit,
    onDeleteTodo: (TodoData) -> Unit,
    onDeleteAlarm: (AlarmData) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { DAY_PANEL_PAGE_COUNT })
    val scope = rememberCoroutineScope()

    val tabs = listOf(
        DayPanelTab("待办", Icons.Outlined.Check, CALENDAR_TODO_ACCENT, dayItems.todos.isNotEmpty()),
        DayPanelTab("闹钟", Icons.Outlined.Alarm, CALENDAR_ALARM_ACCENT, dayItems.alarms.isNotEmpty()),
    )

    AlmGlassCard(modifier = modifier) {
        // —— 日期标题 ——
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "${day.year} · ${day.month}月${day.day}日",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                val lunarText = remember(day) {
                    AlmLunarUtils.formatLunarFull(day.year, day.month, day.day)
                }
                val ganZhiYear = remember(day) {
                    AlmLunarUtils.formatGanZhi(day.year, day.month, day.day)
                }
                val ganZhiMonth = remember(day) {
                    AlmLunarUtils.formatMonthGanZhi(day.year, day.month, day.day)
                }
                val ganZhiDay = remember(day) {
                    AlmLunarUtils.formatDayGanZhi(day.year, day.month, day.day)
                }
                val lunarLine = buildString {
                    if (lunarText.isNotEmpty()) append(lunarText)
                    if (ganZhiYear.isNotEmpty()) {
                        if (isNotEmpty()) append(" · ")
                        // formatGanZhi() 只返回干支（"丙午"），显示时要带「年」字尾
                        append(ganZhiYear).append("年")
                    }
                    if (ganZhiMonth.isNotEmpty() && ganZhiDay.isNotEmpty()) {
                        if (isNotEmpty()) append(" · ")
                        append(ganZhiMonth).append("月").append(ganZhiDay).append("日")
                    }
                }

                val diff = daysBetween(today.toMillis(), day.toMillis())
                val subText = when {
                    diff == 0 -> "今天"
                    diff > 0 -> "$diff 天后"
                    else -> "${-diff} 天前"
                }

                // 农历解析不出来时（越界等），第二行退回显示距今天数，不留空行
                Text(
                    text = if (lunarLine.isNotEmpty()) lunarLine else subText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (lunarLine.isNotEmpty()) {
                    Text(
                        text = subText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }

            // 右上角新增：按下当前页决定新增待办还是闹钟，图标跟着该页的语义色
            IconButton(
                onClick = { if (pagerState.currentPage == 0) onAddTodo() else onAddAlarm() },
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "新增",
                    tint = tabs[pagerState.currentPage].accent,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // containerColor 必须显式置透明：PrimaryTabRow 的默认值是不透明的 surface，
        // 会在玻璃卡上盖出一条实色横带（AlmHomeScreen 里同一个坑）。
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color.Transparent,
        ) {
            tabs.forEachIndexed { index, spec ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(spec.label) },
                    icon = {
                        Icon(
                            imageVector = spec.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            // 未选中时图标淡显，选中时恢复该页语义色
                            tint = if (pagerState.currentPage == index) {
                                spec.accent
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    },
                    selectedContentColor = spec.accent,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            beyondViewportPageCount = 0,
        ) { page ->
            if (page == 0) {
                DayTodoList(
                    todos = dayItems.todos,
                    onClick = onTodoClick,
                    onDelete = onDeleteTodo,
                )
            } else {
                DayAlarmList(
                    alarms = dayItems.alarms,
                    onClick = onAlarmClick,
                    onDelete = onDeleteAlarm,
                )
            }
        }
    }
}

/** 当日面板的一个页签：名称、图标、语义色、当天是否有内容（有则在标题旁点一个点）。 */
private data class DayPanelTab(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accent: Color,
    val hasItems: Boolean,
)

/**
 * 当天的待办。**只读展示**：不做勾选 —— 原 `CalendarActivity` 的日历页也不提供勾选，
 * 改动状态要去待办设置页。长按删除、点击进编辑页。
 *
 * 空列表时给一句空态而不是留白：两页 Pager 里一片空白会让人以为没加载出来。
 */
@Composable
private fun DayTodoList(
    todos: List<TodoData>,
    onClick: (TodoData) -> Unit,
    onDelete: (TodoData) -> Unit,
) {
    if (todos.isEmpty()) {
        DayPanelEmptyText("这一天没有待办")
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        todos.forEach { todo ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ROW_SHAPE)
                    .combinedClickable(
                        onClick = { onClick(todo) },
                        onLongClick = { onDelete(todo) },
                    )
                    .padding(horizontal = 8.dp, vertical = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = todo.title.ifEmpty { todo.description },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
                        color = if (todo.isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (todo.isCompleted) {
                        Text(
                            text = "已完成",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }

                val timeText = buildDayTodoTimeText(todo)
                if (timeText.isNotEmpty()) {
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

/**
 * 当天的闹钟。
 *
 * 时间用 `HH:mm`：同一天里再显示日期就是废话，且这里一条 = 一个「本周某天的这一时刻」，
 * 与首页闹钟列表的显示口径一致。
 */
@Composable
private fun DayAlarmList(
    alarms: List<AlarmData>,
    onClick: (AlarmData) -> Unit,
    onDelete: (AlarmData) -> Unit,
) {
    if (alarms.isEmpty()) {
        DayPanelEmptyText("这一天没有闹钟")
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        alarms.forEach { alarm ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ROW_SHAPE)
                    .combinedClickable(
                        onClick = { onClick(alarm) },
                        onLongClick = { onDelete(alarm) },
                    )
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = dayAlarmTimeFormatter.format(alarm.timeInMillis),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (alarm.isEnabled) "已启用" else "已禁用",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun DayPanelEmptyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        textAlign = TextAlign.Center,
    )
}

/** 当日面板里一条的时间文案，口径同首页待办列表（`AlmListViews.buildTodoTimeText`）。 */
private fun buildDayTodoTimeText(todo: TodoData): String {
    val sb = StringBuilder()
    todo.startTime?.let {
        sb.append("开始于：").append(dayTodoTimeFormatter.format(it))
    }
    if (todo.startTime != null && todo.dueTime != null) {
        sb.append("\n")
    }
    todo.dueTime?.let {
        sb.append("截止于：").append(dayTodoTimeFormatter.format(it))
    }
    return sb.toString()
}

// ============================================================ 常量

/**
 * 卡片圆角。16dp 是卡片级圆角（列表项那个 8dp 是条目级），与约定页的 `DsCard` 同一档。
 */
private val CARD_SHAPE = RoundedCornerShape(16.dp)

private val CARD_PADDING = 16.dp

/** 日历格子的圆角，对齐约定页的 `DayCell`。 */
private val CELL_SHAPE = RoundedCornerShape(10.dp)

/** 当日面板里每行的圆角（比卡片小一档，读得出「卡里的一条」）。 */
private val ROW_SHAPE = RoundedCornerShape(8.dp)

/**
 * 月历 Pager 的页码基准：以「今天所在月」为 [PAGE_OFFSET] 页，前后各留 [PAGE_OFFSET] 个月。
 *
 * 与约定页同值（±200 年）。alm 实际能滑到的范围比这个小得多——年份滚轮只到 ±10 年——
 * 但页码基准属纯 UI 口径，照搬约定页的值省得两处行为不一致。
 */
private const val PAGE_OFFSET = 2400
private const val PAGE_TOTAL = PAGE_OFFSET * 2 + 1

/** 周表头文案，**周日开头**（与约定页一致）。 */
private val WEEKDAY_LABELS = listOf("日", "一", "二", "三", "四", "五", "六")

/**
 * 待办的语义色（蓝）。取自约定页的 `AgreementTodoAccent`，两边的日历读起来要一致。
 */
private val CALENDAR_TODO_ACCENT = Color(0xFF5C6BC0)

/** 闹钟的语义色（橙）。取自约定页的 `AgreementAlarmAccent`。 */
private val CALENDAR_ALARM_ACCENT = Color(0xFFFFA000)

/** 当日面板的页数：待办 / 闹钟（见文件头第 1 条）。 */
private const val DAY_PANEL_PAGE_COUNT = 2

private val dayAlarmTimeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

private val dayTodoTimeFormatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
