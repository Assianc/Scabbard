package com.assiance.convert.logic

import java.math.BigDecimal
import java.math.RoundingMode

/*
 * 生活向单位换算（纯 Kotlin，不依赖 Android）。
 *
 * 每个单位都实现 toBase / fromBase 一对函数，加减温度这类非线性换算因此和线性单位
 * 共用同一套 [convertValue]。温度以摄氏为基准，长度以米为基准，重量以克为基准。
 */

enum class UnitCategory(val label: String) {
    TEMPERATURE("温度"),
    LENGTH("长度"),
    WEIGHT("重量"),
    ;

    val units: List<UnitDef>
        get() = when (this) {
            TEMPERATURE -> TEMPERATURE_UNITS
            LENGTH -> LENGTH_UNITS
            WEIGHT -> WEIGHT_UNITS
        }
}

data class UnitDef(
    val label: String,
    val symbol: String,
    val toBase: (Double) -> Double,
    val fromBase: (Double) -> Double,
)

private val TEMPERATURE_UNITS = listOf(
    UnitDef("摄氏度", "℃", { it }, { it }),
    UnitDef("华氏度", "℉", { (it - 32) * 5 / 9 }, { it * 9 / 5 + 32 }),
    UnitDef("开尔文", "K", { it - 273.15 }, { it + 273.15 }),
)

/** 线性单位：factor 表示 1 个该单位等于多少个基准单位。 */
private fun linear(label: String, symbol: String, factor: Double) =
    UnitDef(label, symbol, { it * factor }, { it / factor })

private val LENGTH_UNITS = listOf(
    linear("米", "m", 1.0),
    linear("厘米", "cm", 0.01),
    linear("毫米", "mm", 0.001),
    linear("千米", "km", 1000.0),
    linear("英寸", "in", 0.0254),
    linear("英尺", "ft", 0.3048),
    linear("码", "yd", 0.9144),
    linear("英里", "mi", 1609.344),
)

private val WEIGHT_UNITS = listOf(
    linear("克", "g", 1.0),
    linear("千克", "kg", 1000.0),
    linear("吨", "t", 1_000_000.0),
    linear("斤", "斤", 500.0),
    linear("磅", "lb", 453.59237),
    linear("盎司", "oz", 28.349523125),
)

fun convertValue(value: Double, from: UnitDef, to: UnitDef): Double =
    to.fromBase(from.toBase(value))

/**
 * 数值展示：最多 6 位小数并去掉尾零，整数不带小数点。
 * 用 BigDecimal 而不是 `String.format`，避免 0.1 + 0.2 这类浮点尾数直接显示给用户。
 */
fun formatNumber(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "—"
    return BigDecimal.valueOf(value)
        .setScale(6, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
}
