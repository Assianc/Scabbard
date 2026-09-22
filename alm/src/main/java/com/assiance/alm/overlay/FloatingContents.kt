package com.assiance.alm.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 闹钟悬浮窗内容（原 `layout_alarm_floating.xml`）。
 *
 * 颜色全部取自 [MaterialTheme.colorScheme]；原 XML 里的渐变背景 / 硬编码色已换成 M3 语义色。
 */
@Composable
internal fun AlarmFloatingContent(
    timeText: String,
    onStop: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "冬去，已至鸟鸣时",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = timeText,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onStop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(
                    text = "停止闹钟",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

/**
 * 待办悬浮窗内容（原 `layout_todo_floating.xml`）。
 *
 * 按钮保留了原来的「按下缩小 100ms → 弹回 100ms → 置灰防重复点击」行为
 * （原实现是 `View.animate().scaleX(0.95f)` 两次）。
 */
@Composable
internal fun TodoFloatingContent(
    title: String,
    description: String,
    onStop: () -> Unit,
) {
    var enabled by remember { mutableStateOf(true) }
    val pressScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (!enabled) return@Button
                    // 禁用按钮，防止重复点击（与原实现一致）
                    enabled = false

                    // 按下反馈：0.95 → 1.0（与原实现的两次 100ms 动画一致）
                    scope.launch {
                        pressScale.animateTo(0.95f, tween(durationMillis = 100))
                        pressScale.animateTo(1f, tween(durationMillis = 100))
                    }

                    // 立即发送停止提醒的广播（由 Service 负责）
                    onStop()
                },
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .graphicsLayer {
                        scaleX = pressScale.value
                        scaleY = pressScale.value
                    },
            ) {
                Text(
                    text = "我已知晓",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
