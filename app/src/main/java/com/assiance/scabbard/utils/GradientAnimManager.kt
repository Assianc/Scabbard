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
                Color.parseColor("#FF0000"),  // 红
                Color.parseColor("#FF7F00"),  // 橙
                Color.parseColor("#FFFF00"),  // 黄
                Color.parseColor("#00FF00"),  // 绿
                Color.parseColor("#0000FF"),  // 蓝
                Color.parseColor("#4B0082"),  // 靛
                Color.parseColor("#9400D3"),  // 紫
                Color.parseColor("#FF0000"),  // 红
                Color.parseColor("#FF7F00"),  // 橙
                Color.parseColor("#FFFF00"),  // 黄
                Color.parseColor("#00FF00"),  // 绿
                Color.parseColor("#0000FF"),  // 蓝
                Color.parseColor("#4B0082"),  // 靛
                Color.parseColor("#9400D3")   // 紫
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
            .edit()
            .putInt("nav_alpha", alpha)
            .apply()
    }
} 