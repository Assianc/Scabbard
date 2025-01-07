package com.assiance.memo

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat

object FontUtils {
    val FONT_NAMES = arrayOf(
        "默认字体",
        "宋体",
        "仿宋",
        "黑体",
        "楷体",
    )

    val FONT_SIZES = arrayOf(
        Pair("初号", 42f),
        Pair("小初", 36f),
        Pair("一号", 26f),
        Pair("小一", 24f),
        Pair("二号", 22f),
        Pair("小二", 18f),
        Pair("三号", 16f),
        Pair("小三", 15f),
        Pair("四号", 14f),
        Pair("小四", 12f),
        Pair("五号", 10.5f),
        Pair("小五", 9f),
        Pair("六号", 7.5f),
        Pair("小六", 6.5f)
    )

    internal val FONTS = mutableListOf<Typeface>()

    fun initFonts(context: Context) {
        FONTS.clear()
        FONTS.add(Typeface.DEFAULT)
        
        try {
            FONTS.add(ResourcesCompat.getFont(context, R.font.simsunch)!!) // 宋体
            FONTS.add(ResourcesCompat.getFont(context, R.font.fasimsunch)!!) // 仿宋
            FONTS.add(ResourcesCompat.getFont(context, R.font.simheich)!!) // 黑体
            FONTS.add(ResourcesCompat.getFont(context, R.font.simkaich)!!) // 楷体
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getFontByName(fontName: String): Typeface {
        val index = FONT_NAMES.indexOf(fontName)
        return if (index >= 0 && index < FONTS.size) {
            FONTS[index]
        } else {
            Typeface.DEFAULT
        }
    }
} 