package com.assiance.alm.ui.ds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import top.yukonga.miuix.kmp.basic.NumberPicker
import top.yukonga.miuix.kmp.basic.NumberPickerDefaults

/**
 * alm 的滚轮选择器。
 *
 * **整段移植自 LoveScore 的 `ui/ds/DsWheelPicker.kt`**（用户要求 alm 的时间选择器
 * 全部复用 LoveScore 那一套，两边保持同一手感）。与原件唯一的差别是本模块没有
 * DsTokens 那套设计系统令牌，于是把原件引用的两个值就地内联：
 * `DsAlpha.prominent` → [DISABLED_ALPHA]、`DsSpacing.sm` / `DsSpacing.md` →
 * [SELECTION_LINE_INSET] / [TIME_COLUMN_GAP]。除这两个值外逐行一致，原件里那些
 * 「为什么这么做」的注释一并保留 —— 它们解释的坑在这里同样成立。
 *
 * 内部是 miuix 的 `NumberPicker`（HyperOS 滚轮：拖动跟手、松手吸附、跨格轻震、
 * 离选中线越远越小越淡），外面补两条选中指示线。包一层的原因是这几件事
 * 不该由调用方操心：
 *
 * 1. **配色**。[NumberPickerDefaults.colors] 的四个默认值都读 miuix 自己的
 *    `MiuixTheme.colorScheme`，而本模块从不套 `MiuixTheme` —— 不显式传的话，
 *    深色模式下滚轮会拿 miuix 的浅色默认值，亮出一段不属于当前主题颜色的数字。
 *    这里四个槽位全部按当前 `MaterialTheme.colorScheme` 传死，所以
 *    **调用方不需要、也不应该在外层包任何 miuix 主题**
 *    （`MiuixTheme` 会顺带覆盖 `LocalIndication`，波及整棵子树的按压反馈）。
 * 2. **行高**。行高按选中数字的 lineHeight 折算，跟着系统字体缩放一起长；
 *    写死 dp 会把大字用户的滚轮数字切成两半。
 * 3. **选中指示**。miuix 只靠颜色插值 + 缩放表达选中，没有独立的选中标识，
 *    这里补上上下两条细线框出选中行的范围。**只画线、不填充** —— miuix 已经把
 *    选中数字染成主色，再垫一块主色填充等于同一处强调两次，滚轮会糊成一块色斑。
 *
 * @param value 当前选中值，必须落在 [range] 内。
 * @param range 可选值区间。**不启用环绕**：0..23 滚到头就停，与手写滚轮的行为一致。
 * @param visibleItemCount 同时可见的行数。**请取奇数** —— 指示线恒画在垂直正中，
 *   而 `NumberPicker` 也是把选中行排在正中的，偶数行数下两者会对不齐半行。
 * @param label 每行的显示文案，默认直接显示数字。
 * @param showSelectionBand 是否画本列自己的指示线。几列要共用**同一对**线时
 *   （见 [wheelSelectionBand]）才需要关掉。
 */
@Composable
fun AlmWheelPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    visibleItemCount: Int = 3,
    label: (Int) -> String = { it.toString() },
    showSelectionBand: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    // 禁用态按 M3 惯例取本色淡出；miuix 把它做成独立槽位，口径统一收在这里
    val disabled = scheme.onSurface.copy(alpha = DISABLED_ALPHA)
    val itemHeight = wheelItemHeight()
    val lineColor = if (showSelectionBand) rememberSelectionLineColor() else null

    Box(
        // drawBehind 而不是往里塞一个 Box：指示线纯绘制、不参与测量，
        // 滚轮该多高还是多高，不会因为多盖一层把行距顶歪。
        modifier = modifier.drawBehind {
            lineColor?.let { drawSelectionBand(it, itemHeight.toPx()) }
        },
    ) {
        NumberPicker(
            value = value,
            onValueChange = onValueChange,
            range = range,
            label = label,
            visibleItemCount = visibleItemCount,
            // 关掉环绕：24 小时与 0..59 分钟滚到头即止，避免误触一格就跳回另一端
            wrapAround = false,
            itemHeight = itemHeight,
            // 数字一律走 tnum 等宽：否则 21 → 9 这种位数变化会让整列左右晃
            textStyle = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
            colors = NumberPickerDefaults.colors(
                selectedTextColor = scheme.primary,
                unselectedTextColor = scheme.onSurfaceVariant,
                disabledSelectedTextColor = disabled,
                disabledUnselectedTextColor = disabled,
            ),
        )
    }
}

/**
 * 横跨整宽的选中指示线，画在**容器**上而不是单列上。
 *
 * 用于几列共同表达一次选择的场合 —— 日历的年 / 月 / 日 是一个日期的三个字段，
 * 共用一对线才读得出「这是一次选择」，各画一对反而像三个独立开关。
 * 时间滚轮相反：时和分中间隔着冒号，各画一对线才不会被冒号穿过去，
 * 所以那边用 [AlmWheelPicker] 自带的指示线即可。
 *
 * 容器高度必须是 `itemHeight * visibleItemCount`（即若干个 [AlmWheelPicker] 摞出来的高度），
 * 两条线按 `高度 / visibleItemCount` 反推行高并居中，不用外部传行高进来。
 *
 * 用它的容器里，各列要记得 `showSelectionBand = false`，否则会叠出三对并排的短线。
 */
@Composable
fun Modifier.wheelSelectionBand(visibleItemCount: Int): Modifier {
    val lineColor = rememberSelectionLineColor()
    return drawBehind { drawSelectionBand(lineColor, size.height / visibleItemCount) }
}

/** 指示线的颜色：从当前 M3 主题现算。 */
@Composable
private fun rememberSelectionLineColor(): Color =
    MaterialTheme.colorScheme.primary.copy(alpha = SELECTION_LINE_ALPHA)

/**
 * 指示线的透明度。
 *
 * 去掉填充后这就是它**唯一**的视觉重量旋钮 —— 嫌抢眼就压这个值，
 * 而不是换颜色：两条线的作用是框出范围，不该和数字争注意力。
 */
private const val SELECTION_LINE_ALPHA = 0.4f

/** 禁用态淡出值。原件取 `DsAlpha.prominent`。 */
private const val DISABLED_ALPHA = 0.6f

/** 指示线左右内缩量。原件取 `DsSpacing.sm`。 */
private val SELECTION_LINE_INSET = 8.dp

/** 时间滚轮两列之间留的横向间距。原件取 `DsSpacing.md`。 */
val TIME_COLUMN_GAP = 12.dp

/**
 * 滚轮行高的基准：选中数字的 lineHeight（titleLarge 的 28sp）+ 上下呼吸。
 *
 * 用 sp 折算而不是写死 36dp，是为了让行高跟着系统字体缩放一起长：
 * 否则大字用户的滚轮数字会被相邻行切掉。字体缩放 1.0 时正好是 36dp。
 */
private val WHEEL_ITEM_HEIGHT_SP = 28.sp
private val WHEEL_ITEM_BREATH = 8.dp

/** 滚轮行高：日历滚轮与时间滚轮共用，保证两种滚轮的手感一致。 */
@Composable
private fun wheelItemHeight(): Dp =
    with(LocalDensity.current) { WHEEL_ITEM_HEIGHT_SP.toDp() } + WHEEL_ITEM_BREATH

/**
 * 选中指示：上下两条两端淡出的细线，**不填充**。
 *
 * 只框范围不上底色 —— miuix 已经把选中数字染成主色，再垫一块主色填充
 * 等于在同一处强调两次，滚轮会糊成一块色斑；两条线不抢数字的视线。
 *
 * 两条线按垂直中心对称、间距取一整格行高 —— `NumberPicker` 内部是
 * `itemHeightPx = height / visibleItemCount` 且把选中行排在正中，
 * 所以这个位置正好框住选中行，不用去问它滚到哪了。
 *
 * 左右各内缩 [SELECTION_LINE_INSET]：线铺满整列会顶到相邻列，并排时糊成一片。
 */
private fun DrawScope.drawSelectionBand(lineColor: Color, rowHeight: Float) {
    val inset = SELECTION_LINE_INSET.toPx()
    val lineWidth = size.width - inset * 2f
    if (lineWidth <= 0f || rowHeight <= 0f) return

    val top = (size.height - rowHeight) / 2f
    // 用横向渐变而不是纯色：两端淡出，免得在线的两端切出一刀硬边。
    // 起止点钉死在两端，这样渐变比例不随列宽变化。
    val lineBrush = Brush.horizontalGradient(
        0f to Color.Transparent,
        0.15f to lineColor,
        0.85f to lineColor,
        1f to Color.Transparent,
        startX = inset,
        endX = inset + lineWidth,
    )
    val lineHeight = 1.dp.toPx()
    drawRect(lineBrush, Offset(inset, top), Size(lineWidth, lineHeight))
    drawRect(lineBrush, Offset(inset, top + rowHeight - lineHeight), Size(lineWidth, lineHeight))
}

/**
 * 时间滚轮选择器：小时 + 分钟两列，中间以冒号分隔。
 *
 * 同为 [AlmWheelPicker] 摞出来的，与日期滚轮（各屏自己组装）保证原生一致的手感。
 * 进来就停在调用方传入的 [hour] / [minute] 上，用户通常只需微调。
 *
 * 两列**各画各的**选中指示线：中间隔着冒号，共用一条横贯的带子会被冒号穿过去。
 */
@Composable
fun AlmTimeWheel(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlmWheelPicker(
            value = hour,
            onValueChange = onHourChange,
            range = WHEEL_HOURS,
            visibleItemCount = WHEEL_TIME_VISIBLE_ITEMS,
            label = { it.twoDigits() },
            // 左右各留 TIME_COLUMN_GAP：两列不贴在一起，冒号才有位置站住
            modifier = Modifier.weight(1f).padding(horizontal = TIME_COLUMN_GAP),
        )
        TimeColon()
        AlmWheelPicker(
            value = minute,
            onValueChange = onMinuteChange,
            range = WHEEL_MINUTES,
            visibleItemCount = WHEEL_TIME_VISIBLE_ITEMS,
            label = { it.twoDigits() },
            modifier = Modifier.weight(1f).padding(horizontal = TIME_COLUMN_GAP),
        )
    }
}

/**
 * 时间两列之间的冒号。
 *
 * 没用文字「:」：字体的冒号在不同字号下粗细和位置都不好对齐，
 * 画两个圆点更容易和 20sp 的数字对上视觉重量，间距也能精确控制。
 */
@Composable
private fun TimeColon(modifier: Modifier = Modifier) {
    val dotColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DISABLED_ALPHA)
    Canvas(modifier = modifier.size(width = 10.dp, height = 22.dp)) {
        val radius = 2.5.dp.toPx()
        drawCircle(
            color = dotColor,
            radius = radius,
            center = Offset(size.width / 2f, size.height * 0.3f),
        )
        drawCircle(
            color = dotColor,
            radius = radius,
            center = Offset(size.width / 2f, size.height * 0.7f),
        )
    }
}

/** 时间滚轮的可见行数：弹窗里时间只是其中一个字段，3 行既看得清相邻项又给下方留出空间。 */
private const val WHEEL_TIME_VISIBLE_ITEMS = 3

/** 小时列取值 0..23：闹钟与待办存的是 24 小时制，与之一致。 */
private val WHEEL_HOURS = 0..23

/** 分钟列取值 0..59：不按 5 分钟步进，否则设不了 22:07 这类时间。 */
private val WHEEL_MINUTES = 0..59

/** 两位补零：滚轮里 9 点显示成 09。 */
private fun Int.twoDigits(): String = toString().padStart(2, '0')

/**
 * 年份滚轮的可选跨度：当前年 ±[WHEEL_YEAR_SPAN]。
 *
 * 待办的开始/截止时间与日历页的「跳转到指定日期」都是年份滚轮，可达范围收在这里
 * 定义一次，两处不会各走各的。
 */
const val WHEEL_YEAR_SPAN = 10

/** 年份可选范围：当前年 ±[WHEEL_YEAR_SPAN]。 */
@Composable
fun rememberWheelYearRange(): IntRange = remember {
    val current = Calendar.getInstance().get(Calendar.YEAR)
    (current - WHEEL_YEAR_SPAN)..(current + WHEEL_YEAR_SPAN)
}
