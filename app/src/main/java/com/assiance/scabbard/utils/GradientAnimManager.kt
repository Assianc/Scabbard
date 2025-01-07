package com.assiance.scabbard.utils

import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader

object GradientAnimManager {
    // 渐变样式枚举
    enum class GradientStyle(val title: String, val colors: IntArray, val positions: FloatArray? = null) {
        BLUE_PURPLE(
            "蓝紫渐变",
            intArrayOf(
                Color.parseColor("#2196F3"),
                Color.parseColor("#9C27B0"),
                Color.parseColor("#2196F3"),
                Color.parseColor("#9C27B0"),
                Color.parseColor("#2196F3")
            ),
            floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        ),
        
        RAINBOW(
            "彩虹渐变",
            intArrayOf(
                Color.parseColor("#FF0000"),  // 红
                Color.parseColor("#FF7F00"),  // 橙
                Color.parseColor("#FFFF00"),  // 黄
                Color.parseColor("#00FF00"),  // 绿
                Color.parseColor("#0000FF"),  // 蓝
                Color.parseColor("#4B0082"),  // 靛
                Color.parseColor("#9400D3"),  // 紫
                Color.parseColor("#FF0000"),  // 红（重复以实现平滑过渡）
                Color.parseColor("#FF7F00"),  // 橙
                Color.parseColor("#FFFF00"),  // 黄
                Color.parseColor("#00FF00"),  // 绿
                Color.parseColor("#0000FF"),  // 蓝
                Color.parseColor("#4B0082"),  // 靛
                Color.parseColor("#9400D3"),  // 紫
                Color.parseColor("#FF0000")   // 红
            ),
            floatArrayOf(
                0f, 0.0714f, 0.1428f, 0.2143f, 0.2857f, 0.3571f, 0.4286f,
                0.5f, 0.5714f, 0.6428f, 0.7143f, 0.7857f, 0.8571f, 0.9286f, 1f
            )
        ),
        
        SUNSET(
            "日落渐变",
            intArrayOf(
                Color.parseColor("#FF512F"),
                Color.parseColor("#F09819"),
                Color.parseColor("#FF512F"),
                Color.parseColor("#F09819"),
                Color.parseColor("#FF512F")
            ),
            floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        ),
        
        OCEAN(
            "海洋渐变",
            intArrayOf(
                Color.parseColor("#2E3192"),
                Color.parseColor("#1BFFFF"),
                Color.parseColor("#2E3192"),
                Color.parseColor("#1BFFFF"),
                Color.parseColor("#2E3192")
            ),
            floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        ),
        
        NEON(
            "霓虹渐变",
            intArrayOf(
                Color.parseColor("#FF1493"),  // 粉红
                Color.parseColor("#00FF00"),  // 荧光绿
                Color.parseColor("#FF1493"),
                Color.parseColor("#00FF00"),
                Color.parseColor("#FF1493")
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
            .edit()
            .putString("gradient_style", style.name)
            .apply()
    }

    fun createGradient(width: Float, height: Float, style: GradientStyle): LinearGradient {
        return LinearGradient(
            0f, 0f, width * 2, height,
            style.colors,
            style.positions,
            Shader.TileMode.MIRROR
        )
    }
} 