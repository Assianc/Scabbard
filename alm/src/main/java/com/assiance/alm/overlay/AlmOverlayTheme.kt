package com.assiance.alm.overlay

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * # 只给两个悬浮窗用的极小主题包装
 *
 * 悬浮窗是 WindowManager overlay，拿不到宿主的 `ScabbardTheme`，所以这里必须自带一层。
 *
 * - API 31+：Material You 动态取色（[dynamicDarkColorScheme] / [dynamicLightColorScheme]），
 *   与宿主观感保持一致。
 * - API 31 以下：回落到 Material 3 的基线色板（[darkColorScheme] / [lightColorScheme] 默认值），
 *   即「兜底色」——不自己编造一套品牌色。
 *
 * **不要把这个包装用到 NavHost 里的屏幕上**：那些屏幕由宿主的 `ScabbardTheme` 负责，
 * 再包一层会让动态取色断链。
 */
@Composable
internal fun AlmOverlayTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()

    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> darkColorScheme()

        else -> lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
