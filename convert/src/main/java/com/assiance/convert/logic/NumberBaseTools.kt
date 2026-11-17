package com.assiance.convert.logic

import java.math.BigInteger

/** 进制转换支持的四种进制。[radix] 直接交给 [BigInteger] 解析。 */
enum class NumberBase(val label: String, val radix: Int) {
    BIN("2 进制", 2),
    OCT("8 进制", 8),
    DEC("10 进制", 10),
    HEX("16 进制", 16),
}

/**
 * 一次进制转换的结果。
 *
 * [error] 与 [outputs] 互斥：解析失败时 [outputs] 为空、[error] 给出提示；
 * 输入为空时两者都为空（界面显示空白，不报错）。
 */
data class NumberBaseResult(
    val value: BigInteger?,
    val outputs: Map<NumberBase, String>,
    val error: String?,
)

/**
 * 把 [input] 按 [from] 进制解析，输出四种进制的表示。
 *
 * 用 [BigInteger] 而不是 Long：十六进制 16 位以上就会溢出 Long，而用户粘贴的
 * 哈希/ID 经常超过那个范围。16 进制统一输出大写，避免同一数值出现两种写法。
 */
fun convertNumberBase(input: String, from: NumberBase): NumberBaseResult {
    val text = input.trim()
    if (text.isEmpty()) return NumberBaseResult(null, emptyMap(), null)

    // 16 进制允许带 0x / 0X 前缀；其它进制不剥前缀，"0x10" 按 10 进制解析要报错才对。
    val body = if (from == NumberBase.HEX) {
        text.removePrefix("0x").removePrefix("0X")
    } else {
        text
    }

    return try {
        val value = BigInteger(body, from.radix)
        NumberBaseResult(
            value = value,
            outputs = NumberBase.entries.associateWith { base ->
                val formatted = value.toString(base.radix)
                if (base == NumberBase.HEX) formatted.uppercase() else formatted
            },
            error = null,
        )
    } catch (e: NumberFormatException) {
        NumberBaseResult(null, emptyMap(), "不是有效的 ${from.label}数")
    }
}
