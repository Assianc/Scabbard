package com.assiance.scabbard.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 双色极光背景的配色预设。
 *
 * 与 [ThemePalette] **解耦**：主题色板决定控件配色，这里只决定背景那两颗光斑的颜色。
 * 拆开是因为两者的诉求不同 —— 色板要成套（控件、容器、正文都得协调），而背景只是
 * 一层氛围，用户完全可能想要「薄荷绿的控件 + 暖色背景」。
 *
 * [FOLLOW_THEME] 是默认，也是本项目原来的行为：光斑直接取当前色板的
 * `primaryContainer` 与 `tertiaryContainer`，所以换色板背景跟着换。
 * 其余几套是写死的两色，与色板无关。
 *
 * 取值都是**中间偏浅**的色调而不是饱和重色：它们在背景上是以 0.16~0.55 的 alpha
 * 叠在 `surfaceContainer → surface` 渐变之上的，用重色会在浅色主题下糊成一块脏斑。
 * 同一个色值在深浅两种主题下都能用 —— 深底上是一层发光，浅底上是一层薄雾。
 */
enum class AuroraPreset(
    val title: String,
    /** 设置页里列在标题下的一行说明。 */
    val subtitle: String,
    /** 第一道光斑的颜色。null = 跟随当前色板的 `primaryContainer`。 */
    val primaryGlow: Color?,
    /** 第二道光斑的颜色。null = 跟随当前色板的 `tertiaryContainer`。 */
    val secondaryGlow: Color?,
) {
    /** 跟随主题：两道光斑都取自当前色板。 */
    FOLLOW_THEME(
        title = "跟随主题",
        subtitle = "两道光斑取当前色板的容器色，换色板背景跟着换",
        primaryGlow = null,
        secondaryGlow = null,
    ),

    DAWN(
        title = "晨曦",
        subtitle = "玫粉 → 暖橙",
        primaryGlow = Color(0xFFFFC2D1),
        secondaryGlow = Color(0xFFFFD9A0),
    ),

    DEEP_SEA(
        title = "深海",
        subtitle = "青 → 蓝紫",
        primaryGlow = Color(0xFF9BE7FF),
        secondaryGlow = Color(0xFFB8C6FF),
    ),

    DUSK(
        title = "暮光",
        subtitle = "紫 → 青",
        primaryGlow = Color(0xFFD6BBFB),
        secondaryGlow = Color(0xFF9ED8EA),
    ),

    FOREST(
        title = "森林",
        subtitle = "绿 → 黄绿",
        primaryGlow = Color(0xFFB5E8B0),
        secondaryGlow = Color(0xFFF2ECA8),
    ),

    SAKURA(
        title = "樱雾",
        subtitle = "樱粉 → 藕紫",
        primaryGlow = Color(0xFFFFC9E0),
        secondaryGlow = Color(0xFFDCC6F5),
    ),
}
