package com.assiance.scabbard.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 全 App 背景层：跟随当前主题的柔和渐变 + 两颗反向游移的径向光斑（双色极光）。
 *
 * 位置：铺在 [com.assiance.scabbard.ui.ScabbardApp] 的 NavHost **之下**，z-order 最底层。
 * 因此各页面的 Scaffold 必须用 `containerColor = Color.Transparent`、TopAppBar 必须用
 * `topAppBarColors(containerColor = Color.Transparent)`，背景才透得出来 —— 漏掉任何一处
 * 都会在该页留下一条实色横带。
 *
 * **为什么需要它**：首页入口卡用的是液态玻璃，而玻璃是真去采样背板做模糊（Android 侧走
 * `RenderEffect`）再叠折射（`RuntimeShader`）的。背板若是纯色，模糊与折射都是恒等变换，
 * 玻璃看起来就只是一张半透明卡片。给背景加上空间层次，玻璃才成立。
 *
 * 配色全部取自 [MaterialTheme.colorScheme]，不硬编码：本 App 开着 Material You 动态取色
 * （API 31+ 跟随壁纸），从主题 token 派生才能跟着壁纸走，不会和控件配色打架。
 *
 * 动效是一个 [drawBehind]，没有子节点、不触发重组 —— 每帧只重绘这一层。
 */
@Composable
fun AppBackground(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme

    // 缓慢游移：约 12s 一圈，径向光斑的圆心在背景上画圆，制造"光在流动"的观感。
    val transition = rememberInfiniteTransition(label = "bg-flow")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "bg-flow-angle",
    )
    // 光斑透明度轻微呼吸（0.35 ~ 0.55），避免恒定高光显得死板。
    val breathe by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bg-flow-breathe",
    )
    // 副光斑自己的呼吸（0.16 ~ 0.30）。周期取 8s 而不是跟主光斑一样的 6s：
    // 两者周期相同会同步明暗，看上去还是一颗光斑在忽大忽小；错开才像两层极光各自流动。
    val breatheSecond by transition.animateFloat(
        initialValue = SECOND_BREATHE_MIN,
        targetValue = SECOND_BREATHE_MAX,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bg-flow-breathe-second",
    )

    val rad = angle * (PI / 180.0)
    // 副光斑与主光斑反相（差 180°）：两颗光斑沿对角线此消彼长地游移，
    // 屏幕上任何时刻都有一明一暗两处色相，而不是两颗一起漂。
    val radSecond = rad + PI

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // 用 DrawScope 的实际宽高把归一化游移坐标换算成真实 px 中心，
                // 避免 fraction 被当成 px 用导致光斑偏到左上角。
                val cx = size.width * (0.5f + ORBIT_RADIUS_FRACTION * cos(rad).toFloat())
                val cy = size.height * (0.5f + ORBIT_RADIUS_FRACTION * sin(rad).toFloat())
                // 副光斑的游移半径略大于主光斑，两颗圆心不会长期重合
                val cxSecond =
                    size.width * (0.5f + SECOND_ORBIT_RADIUS_FRACTION * cos(radSecond).toFloat())
                val cySecond =
                    size.height * (0.5f + SECOND_ORBIT_RADIUS_FRACTION * sin(radSecond).toFloat())
                // 底色：surfaceContainer → surface 纵向渐变。
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(scheme.surfaceContainer, scheme.surface),
                    )
                )
                // 主光斑：primaryContainer 径向渐变，圆心游移、浓度呼吸。
                // 半径按短边取，见 GLOW_RADIUS_FRACTION 的说明。
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            scheme.primaryContainer.copy(alpha = breathe),
                            Color.Transparent,
                        ),
                        radius = size.minDimension * GLOW_RADIUS_FRACTION,
                        center = Offset(cx, cy),
                    )
                )
                // 副光斑：tertiaryContainer，比主光斑更大更淡，拉开色相层次。
                // 盖在主光斑之上：两者在圆心附近叠出第三种过渡色，边缘各自淡出。
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            scheme.tertiaryContainer.copy(alpha = breatheSecond),
                            Color.Transparent,
                        ),
                        radius = size.minDimension * SECOND_GLOW_RADIUS_FRACTION,
                        center = Offset(cxSecond, cySecond),
                    )
                )
            }
    )
}

/**
 * 光斑半径占屏幕短边的比例。
 *
 * 取 0.6 的用意：光斑直径略大于屏宽，边缘自然淡出、不切出硬边；而圆心游移幅度
 * （[ORBIT_RADIUS_FRACTION]）与半径同量级，光斑能明显地在屏幕上挪动。
 * 调大 → 越来越像均匀铺色，动效随之消失；调小 → 变成一颗清晰的光球，离"低调背景"的定位就远了。
 */
private const val GLOW_RADIUS_FRACTION = 0.6f

/**
 * 光斑圆心的游移半径，占屏幕宽/高的比例（±0.32 屏）。
 * 与 [GLOW_RADIUS_FRACTION] 同量级时光斑挪动最明显。
 */
private const val ORBIT_RADIUS_FRACTION = 0.32f

/**
 * 副光斑半径占屏幕短边的比例。
 *
 * 比主光斑的 [GLOW_RADIUS_FRACTION] 大得多，是刻意的：副光斑的作用是拉开**色相**层次，
 * 不是再点一颗亮斑。铺得越大、越淡，越像一层漫开的色雾；若跟主光斑同尺寸同浓度，
 * 两颗光斑只会互相抵消成一块更亮的大斑，色相层反而不见了。
 */
private const val SECOND_GLOW_RADIUS_FRACTION = 0.85f

/**
 * 副光斑圆心的游移半径。比主光斑略大（0.38 vs 0.32），保证两颗圆心不会长期重合 ——
 * 重合时两个径向渐变叠在一起，双色的效果就没了。
 */
private const val SECOND_ORBIT_RADIUS_FRACTION = 0.38f

/** 副光斑呼吸区间的下沿。比主光斑更低，避免两层色雾把背景提得过亮。 */
private const val SECOND_BREATHE_MIN = 0.16f

/** 副光斑呼吸区间的上沿。 */
private const val SECOND_BREATHE_MAX = 0.30f
