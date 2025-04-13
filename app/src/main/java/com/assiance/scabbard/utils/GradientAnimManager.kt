package com.assiance.scabbard.utils

import android.content.Context
import android.graphics.LinearGradient
import android.graphics.Shader
import androidx.core.content.edit
import androidx.core.graphics.toColorInt

object GradientAnimManager {
    // 渐变样式枚举
    enum class GradientStyle(
        val title: String,
        val colors: IntArray,
        val positions: FloatArray? = null
    ) {
        BLUE_PURPLE(
            "蓝紫渐变",
            intArrayOf(
                "#2196F3".toColorInt(),
                "#9C27B0".toColorInt(),
                "#2196F3".toColorInt(),
                "#9C27B0".toColorInt(),
                "#2196F3".toColorInt()
            ),
            floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        ),

        RAINBOW(
            "彩虹渐变",
            intArrayOf(
                "#FF0000".toColorInt(),  // 红
                "#FF7F00".toColorInt(),  // 橙
                "#FFFF00".toColorInt(),  // 黄
                "#00FF00".toColorInt(),  // 绿
                "#0000FF".toColorInt(),  // 蓝
                "#4B0082".toColorInt(),  // 靛
                "#9400D3".toColorInt(),  // 紫
                "#FF0000".toColorInt(),  // 红（重复以实现平滑过渡）
                "#FF7F00".toColorInt(),  // 橙
                "#FFFF00".toColorInt(),  // 黄
                "#00FF00".toColorInt(),  // 绿
                "#0000FF".toColorInt(),  // 蓝
                "#4B0082".toColorInt(),  // 靛
                "#9400D3".toColorInt(),  // 紫
                "#FF0000".toColorInt(),  // 红
                "#FF7F00".toColorInt(),  // 橙
                "#FFFF00".toColorInt(),  // 黄
                "#00FF00".toColorInt(),  // 绿
                "#0000FF".toColorInt(),  // 蓝
                "#4B0082".toColorInt(),  // 靛
                "#9400D3".toColorInt()   // 紫
            ),
            floatArrayOf(
                0f, 0.05f, 0.1f, 0.15f, 0.2f, 0.25f, 0.3f,
                0.35f, 0.4f, 0.45f, 0.5f, 0.55f, 0.6f, 0.65f,
                0.7f, 0.75f, 0.8f, 0.85f, 0.9f, 0.95f, 1f
            )
        ),

        SUNSET(
            "日落渐变",
            intArrayOf(
                "#FF512F".toColorInt(),
                "#F09819".toColorInt(),
                "#FF512F".toColorInt(),
                "#F09819".toColorInt(),
                "#FF512F".toColorInt()
            ),
            floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        ),

        OCEAN(
            "海洋渐变",
            intArrayOf(
                "#2E3192".toColorInt(),
                "#1BFFFF".toColorInt(),
                "#2E3192".toColorInt(),
                "#1BFFFF".toColorInt(),
                "#2E3192".toColorInt()
            ),
            floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        ),

        NEON(
            "霓虹渐变",
            intArrayOf(
                "#FF1493".toColorInt(),  // 粉红
                "#00FF00".toColorInt(),  // 荧光绿
                "#FF1493".toColorInt(),
                "#00FF00".toColorInt(),
                "#FF1493".toColorInt()
            ),
            floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        )
    }

    fun getCurrentStyle(context: Context): GradientStyle {
        val prefName = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getString("gradient_style", GradientStyle.BLUE_PURPLE.name)
        return GradientStyle.valueOf(prefName ?: GradientStyle.BLUE_PURPLE.name)
    }

    fun setCurrentStyle(context: Context, style: GradientStyle) {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit {
                putString("gradient_style", style.name)
            }
    }

    fun createGradient(width: Float, height: Float, style: GradientStyle): LinearGradient {
        val gradientWidth = if (style == GradientStyle.RAINBOW) {
            width * 3f
        } else {
            width * 2f
        }

        return LinearGradient(
            0f, 0f, gradientWidth, height,
            style.colors,
            style.positions,
            Shader.TileMode.MIRROR
        )
    }

    fun getCurrentNavAlpha(context: Context): Int {
        return context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getInt("nav_alpha", 230) // 默认值为230，约90%不透明度
    }

    fun setCurrentNavAlpha(context: Context, alpha: Int) {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit {
                putInt("nav_alpha", alpha)
            }
    }
} 