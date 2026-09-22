package com.assiance.alm.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlarm
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 首页右下角的「新增」悬浮按钮。
 *
 * 原实现是自定义 View `MovableFloatingActionButton`（Material FAB 子类 + 自己处理
 * `MotionEvent` 的拖动）。任务允许「保留包一层 AndroidView」或「用 Compose 的 pointerInput 重写」，
 * 这里选择重写，并把原实现的**可观察行为**逐条对齐：
 *
 * - 拖动阈值：原为硬编码 20f（px）；Compose 的 `detectDragGestures` 用系统 touch slop，量级一致。
 * - 拖动范围：原实现要求新位置**完全**落在父容器内（`0 < x < 父宽-按钮宽`）才移动，越界则整体不动，
 *   这里保留同样的判定。
 * - 松手停靠：只停靠 X 轴，按中线判断靠左（0）还是靠右（父宽-按钮宽），200ms 减速；Y 轴不动。
 *   （所以停靠后左右 24dp 的外边距会归零——这是原实现的行为，这里没有"修正"。）
 * - 点击与拖动的区分：原实现是「位移小于阈值即视为点击」；Compose 下拖动会消费事件，
 *   未被消费的抬起就落到 FAB 自己的 onClick 上，效果一致。
 * - 图标：闹钟页 `Icons.Filled.AddAlarm`，待办页 `Icons.Filled.AddTask`（对应原来的
 *   `ic_alarm_add` / `ic_add_task`），切换时交叉淡入淡出；切页时按钮整体自转 360°（原来用
 *   `rotation(360f)` / `rotation(-360f)`，这里用累加角度 + 动画复现同样的方向）。
 *
 * 位置用 `rememberSaveable` 保存，返回首页时不会弹回角落（原实现同样会保留位置）。
 */
@Composable
internal fun DraggableAddFab(
    isAlarmTab: Boolean,
    containerSize: IntSize,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val marginPx = with(density) { FAB_MARGIN_DP.dp.toPx() }

    var fabWidth by remember { mutableIntStateOf(0) }
    var fabHeight by remember { mutableIntStateOf(0) }
    var placed by rememberSaveable { mutableStateOf(false) }

    // 位置：左边缘 / 上边缘在容器里的像素坐标
    val x = rememberSaveable(saver = FloatAnimatableSaver) { Animatable(0f) }
    val y = rememberSaveable(saver = FloatAnimatableSaver) { Animatable(0f) }

    // 首次量到容器与自身尺寸后，落到右下角（对应原布局的 bottom|end + margin 24dp）
    LaunchedEffect(containerSize, fabWidth, fabHeight) {
        if (!placed && containerSize.width > 0 && fabWidth > 0 && fabHeight > 0) {
            x.snapTo((containerSize.width - fabWidth - marginPx).coerceAtLeast(marginPx))
            y.snapTo((containerSize.height - fabHeight - marginPx).coerceAtLeast(marginPx))
            placed = true
        }
    }

    // 切页时自转，方向与原来的 rotation(360f) / rotation(-360f) 一致
    var rotation by remember { mutableFloatStateOf(0f) }
    var rotationInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(isAlarmTab) {
        if (rotationInitialized) {
            rotation += if (isAlarmTab) 360f else -360f
        } else {
            rotationInitialized = true
        }
    }
    val animatedRotation by animateFloatAsState(
        targetValue = rotation,
        animationSpec = tween(durationMillis = FAB_ROTATION_DURATION_MS),
        label = "fabRotation",
    )

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .offset { IntOffset(x.value.roundToInt(), y.value.roundToInt()) }
            // 还没量到尺寸前先不显示，避免在左上角闪一下
            .graphicsLayer {
                rotationZ = animatedRotation
                alpha = if (placed) 1f else 0f
            }
            .onSizeChanged {
                fabWidth = it.width
                fabHeight = it.height
            }
            .pointerInput(containerSize, fabWidth, fabHeight) {
                val limitX = (containerSize.width - fabWidth).coerceAtLeast(0)
                val limitY = (containerSize.height - fabHeight).coerceAtLeast(0)

                fun snapToNearestEdge() {
                    val centerX = x.value + fabWidth / 2f
                    val targetX = if (centerX < containerSize.width / 2f) 0f else limitX.toFloat()
                    scope.launch {
                        x.animateTo(targetX, tween(durationMillis = FAB_SNAP_DURATION_MS))
                    }
                }

                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newX = x.value + dragAmount.x
                        val newY = y.value + dragAmount.y
                        // 与原实现一致：只有完全落在容器内才跟随，越界则整体不动
                        if (newX > 0f && newX < limitX && newY > 0f && newY < limitY) {
                            scope.launch {
                                x.snapTo(newX)
                                y.snapTo(newY)
                            }
                        }
                    },
                    onDragEnd = { snapToNearestEdge() },
                    onDragCancel = { snapToNearestEdge() },
                )
            },
    ) {
        Crossfade(
            targetState = isAlarmTab,
            animationSpec = tween(durationMillis = FAB_ICON_SWAP_DURATION_MS),
            label = "fabIcon",
        ) { alarmTab ->
            Icon(
                imageVector = if (alarmTab) Icons.Filled.AddAlarm else Icons.Filled.AddTask,
                contentDescription = null,
            )
        }
    }
}

/** 让 [Animatable] 能进 `rememberSaveable`（只保存当前值）。 */
private val FloatAnimatableSaver: Saver<Animatable<Float, AnimationVector1D>, Float> =
    Saver(save = { it.value }, restore = { Animatable(it) })

/** 原布局的 `android:layout_margin="24dp"`。 */
private const val FAB_MARGIN_DP = 24

/** 原 `updateFabIcon()` 的缩放时长是 150ms + 150ms，这里用一段交叉淡入淡出近似。 */
private const val FAB_ICON_SWAP_DURATION_MS = 150

/** 原 `fabAdd.animate().rotation(...).setDuration(300)`。 */
private const val FAB_ROTATION_DURATION_MS = 300

/** 原停靠动画 `setDuration(200)` + `DecelerateInterpolator`。 */
private const val FAB_SNAP_DURATION_MS = 200
