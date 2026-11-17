package com.assiance.scabbard.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.assiance.scabbard.utils.GradientAnimManager

/**
 * 带流动渐变填充的标题文字。
 *
 * 对应改造前的 `setupGradientEffect()`：那时往 TextView 的 Paint 上挂 LinearGradient，
 * 再用 ValueAnimator 推 localMatrix 平移。这里换成 Compose 的 Brush + 无限动画。
 * 样式表仍复用 [GradientAnimManager.GradientStyle]，5 种样式、色标位置、2100ms 时长
 * 与原实现一致。
 *
 * 原实现用 `TileMode.MIRROR` 让色带首尾相接，这里保持一致；渐变色带跨度 RAINBOW 取文本
 * 宽度的 3 倍、其余样式 2 倍，也照搬原来的取值。
 *
 * 首帧文本宽度还没测出来，此时不挂 brush，按普通纯色文本渲染，测完下一帧即生效。
 */
@Composable
fun GradientTitle(
    text: String,
    style: GradientAnimManager.GradientStyle,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.headlineSmall,
    textAlign: TextAlign? = null,
) {
    var textWidth by remember(text, textStyle) { mutableFloatStateOf(0f) }

    val transition = rememberInfiniteTransition(label = "gradientTitle")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = ANIM_DURATION_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "gradientTranslate",
    )

    val brush = if (textWidth <= 0f) {
        null
    } else {
        val colors = style.colors.map { Color(it) }
        val gradientWidth =
            textWidth * if (style == GradientAnimManager.GradientStyle.RAINBOW) 3f else 2f
        // 一个周期正好平移一个文本宽度，与原来 ValueAnimator.ofFloat(0f, width) 的行程一致
        val startX = -textWidth + textWidth * progress
        val start = Offset(startX, 0f)
        val end = Offset(startX + gradientWidth, 0f)
        val stops = style.positions
        if (stops == null) {
            Brush.linearGradient(
                colors = colors,
                start = start,
                end = end,
                tileMode = TileMode.Mirror,
            )
        } else {
            val colorStops = stops.mapIndexed { index, stop -> stop to colors[index] }.toTypedArray()
            Brush.linearGradient(
                *colorStops,
                start = start,
                end = end,
                tileMode = TileMode.Mirror,
            )
        }
    }

    Text(
        text = text,
        modifier = modifier,
        style = if (brush == null) textStyle else textStyle.copy(brush = brush),
        textAlign = textAlign,
        onTextLayout = { result -> textWidth = result.size.width.toFloat() },
    )
}

private const val ANIM_DURATION_MILLIS = 2100
