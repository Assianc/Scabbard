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
 * 全 App 背景层：跟随当前主题的柔和渐变 + 一颗缓慢游移的径向光斑。
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

    val rad = angle * (PI / 180.0)

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // 用 DrawScope 的实际宽高把归一化游移坐标换算成真实 px 中心，
                // 避免 fraction 被当成 px 用导致光斑偏到左上角。
                val cx = size.width * (0.5f + ORBIT_RADIUS_FRACTION * cos(rad).toFloat())
                val cy = size.height * (0.5f + ORBIT_RADIUS_FRACTION * sin(rad).toFloat())
                // 底色：surfaceContainer → surface 纵向渐变。
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(scheme.surfaceContainer, scheme.surface),
                    )
                )
                // 光斑：primaryContainer 径向渐变，圆心游移、浓度呼吸。
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
