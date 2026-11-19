package com.assiance.convert.logic

import kotlin.math.roundToInt

/*
 * 颜色格式转换（纯 Kotlin，不依赖 Android）。
 *
 * 只处理不透明颜色：输入支持 #RGB / #RRGGBB / #AARRGGBB（8 位时忽略 AA，界面上不提供
 * 透明度编辑），输出统一为 #RRGGBB / rgb() / hsl()。
 */

data class Rgb(val r: Int, val g: Int, val b: Int)

/** 解析 HEX；非法输入返回 null，由界面提示并保留上一次的有效颜色。 */
fun parseHex(input: String): Rgb? {
    val text = input.trim().removePrefix("#")
    if (text.isEmpty() || !text.all { it.isHexDigit() }) return null
    return when (text.length) {
        3 -> Rgb(
            r = text[0].digitToInt(16) * 17,
            g = text[1].digitToInt(16) * 17,
            b = text[2].digitToInt(16) * 17,
        )

        6 -> tripleAt(text, 0)

        // #AARRGGBB：忽略 alpha，只取后六位
        8 -> tripleAt(text, 2)

        else -> null
    }
}

private fun Char.isHexDigit(): Boolean =
    this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

private fun tripleAt(text: String, start: Int): Rgb? {
    val r = text.substring(start, start + 2).toIntOrNull(16) ?: return null
    val g = text.substring(start + 2, start + 4).toIntOrNull(16) ?: return null
    val b = text.substring(start + 4, start + 6).toIntOrNull(16) ?: return null
    return Rgb(r, g, b)
}

fun toHex(rgb: Rgb): String =
    String.format(java.util.Locale.ROOT, "#%02X%02X%02X", rgb.r, rgb.g, rgb.b)

fun toRgbString(rgb: Rgb): String = "rgb(${rgb.r}, ${rgb.g}, ${rgb.b})"

fun toHslString(rgb: Rgb): String {
    val (h, s, l) = toHsl(rgb)
    return "hsl($h, $s%, $l%)"
}

/**
 * HSV 表示。取色器用：h ∈ [0,360)，s / v ∈ [0,1]。
 *
 * 单独于 HSL 存在，是因为取色面板的交互天然是「色相 + 饱和度 + 明度」，
 * 用 HSL 的 l 直接画面板会把纯色挤在中间一窄条。
 */
data class Hsv(val h: Float, val s: Float, val v: Float)

/** 标准 RGB → HSV。黑色（max=0）时色相与饱和度均为 0。 */
fun toHsv(rgb: Rgb): Hsv {
    val r = rgb.r / 255f
    val g = rgb.g / 255f
    val b = rgb.b / 255f
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val d = max - min
    val saturation = if (max == 0f) 0f else d / max
    val hue = when {
        d == 0f -> 0f
        max == r -> 60f * (((g - b) / d) % 6f)
        max == g -> 60f * (((b - r) / d) + 2f)
        else -> 60f * (((r - g) / d) + 4f)
    }.let { if (it < 0f) it + 360f else it }
    return Hsv(hue, saturation, max)
}

/** 标准 HSV → RGB，结果四舍五入并夹到 0–255。 */
fun fromHsv(hsv: Hsv): Rgb {
    val c = hsv.v * hsv.s
    val hh = (hsv.h % 360f) / 60f
    val x = c * (1f - kotlin.math.abs(hh % 2f - 1f))
    val (r1, g1, b1) = when {
        hh < 1f -> Triple(c, x, 0f)
        hh < 2f -> Triple(x, c, 0f)
        hh < 3f -> Triple(0f, c, x)
        hh < 4f -> Triple(0f, x, c)
        hh < 5f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val m = hsv.v - c
    return Rgb(
        r = ((r1 + m) * 255f).roundToInt().coerceIn(0, 255),
        g = ((g1 + m) * 255f).roundToInt().coerceIn(0, 255),
        b = ((b1 + m) * 255f).roundToInt().coerceIn(0, 255),
    )
}

/** 标准 RGB → HSL：h ∈ [0,360)，s / l 为百分数。 */
fun toHsl(rgb: Rgb): Triple<Int, Int, Int> {
    val r = rgb.r / 255.0
    val g = rgb.g / 255.0
    val b = rgb.b / 255.0
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val lightness = (max + min) / 2
    if (max == min) {
        return Triple(0, 0, (lightness * 100).roundToInt())
    }
    val d = max - min
    val saturation = if (lightness > 0.5) d / (2 - max - min) else d / (max + min)
    val hue = when (max) {
        r -> (g - b) / d + (if (g < b) 6 else 0)
        g -> (b - r) / d + 2
        else -> (r - g) / d + 4
    } / 6
    return Triple(
        (hue * 360).roundToInt() % 360,
        (saturation * 100).roundToInt(),
        (lightness * 100).roundToInt(),
    )
}
