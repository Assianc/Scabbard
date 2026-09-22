package com.assiance.memo.ui

import android.graphics.Typeface
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.assiance.memo.R

/**
 * 字体名清单，顺序与旧 MemoDetailActivity.FONT_NAMES / FONTS 一一对应：
 * 下标 0 默认字体、1 宋体、2 仿宋、3 黑体、4 楷体。
 */
val MEMO_FONT_NAMES = arrayOf("默认字体", "宋体", "仿宋", "黑体", "楷体")

private val MemoFontDefault = FontFamily.Default
private val MemoFontSong = FontFamily(Font(R.font.simsunch))
private val MemoFontFangSong = FontFamily(Font(R.font.fasimsunch))
private val MemoFontHei = FontFamily(Font(R.font.simheich))
private val MemoFontKai = FontFamily(Font(R.font.simkaich))

/**
 * 字体名 -> FontFamily。
 * 数据库里默认存的是 "DEFAULT"（不是上面清单里的名字），一并落到默认字体。
 */
fun memoFontFamily(fontName: String): FontFamily = when (fontName) {
    "宋体" -> MemoFontSong
    "仿宋" -> MemoFontFangSong
    "黑体" -> MemoFontHei
    "楷体" -> MemoFontKai
    else -> MemoFontDefault
}

/**
 * 字号预设，与旧 MemoDetailActivity.FONT_SIZES 完全一致。
 * 数值含义同旧实现（EditText.setTextSize(Float) 走 SP 单位），这里同样按 sp 使用。
 */
val MEMO_FONT_SIZES = listOf(
    "初号" to 42f,
    "小初" to 36f,
    "一号" to 26f,
    "小一" to 24f,
    "二号" to 22f,
    "小二" to 18f,
    "三号" to 16f,
    "小三" to 15f,
    "四号" to 14f,
    "小四" to 12f,
    "五号" to 10.5f,
    "小五" to 9f,
    "六号" to 7.5f,
    "小六" to 6.5f,
)

const val MEMO_FONT_SIZE_MIN = 6.5f
const val MEMO_FONT_SIZE_MAX = 42f

const val MEMO_TITLE_FONT_SIZE_DEFAULT = 32f
const val MEMO_CONTENT_FONT_SIZE_DEFAULT = 16f

/**
 * 样式位与 android.graphics.Typeface 对齐：NORMAL=0、BOLD=1、ITALIC=2，
 * 落库的 title_style / content_style 就是这套位。
 */
fun memoFontWeight(style: Int): FontWeight =
    if (style and Typeface.BOLD != 0) FontWeight.Bold else FontWeight.Normal

fun memoFontStyle(style: Int): FontStyle =
    if (style and Typeface.ITALIC != 0) FontStyle.Italic else FontStyle.Normal

fun memoTextDecoration(underline: Boolean): TextDecoration =
    if (underline) TextDecoration.Underline else TextDecoration.None

/** 由加粗 / 斜体开关拼出与旧实现一致的 Typeface 样式位。 */
fun memoStyleBits(bold: Boolean, italic: Boolean): Int {
    var style = Typeface.NORMAL
    if (bold) style = style or Typeface.BOLD
    if (italic) style = style or Typeface.ITALIC
    return style
}
