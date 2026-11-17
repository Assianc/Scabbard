package com.assiance.convert.logic

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * 编码转换的统一返回。 [error] 非空时界面只显示错误，不展示 [text]。
 */
data class ConvertOutcome(val text: String, val error: String?)

// ---------------------------------------------------------------------------- URL

fun urlEncode(input: String): ConvertOutcome = try {
    // URLEncoder 的空格会编成 '+'，这是 application/x-www-form-urlencoded 的标准行为，
    // 与常见在线工具一致，不做额外交替换。
    ConvertOutcome(URLEncoder.encode(input, Charsets.UTF_8.name()), null)
} catch (e: Exception) {
    ConvertOutcome("", "URL 编码失败：${e.message ?: "未知原因"}")
}

fun urlDecode(input: String): ConvertOutcome = try {
    ConvertOutcome(URLDecoder.decode(input, Charsets.UTF_8.name()), null)
} catch (e: Exception) {
    ConvertOutcome("", "URL 解码失败，请检查输入格式")
}

// ---------------------------------------------------------------------------- Base64

fun base64Encode(input: String): ConvertOutcome = try {
    val encoded = Base64.encodeToString(input.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    ConvertOutcome(encoded, null)
} catch (e: Exception) {
    ConvertOutcome("", "Base64 编码失败：${e.message ?: "未知原因"}")
}

fun base64Decode(input: String): ConvertOutcome = try {
    val decoded = Base64.decode(input.trim(), Base64.DEFAULT)
    ConvertOutcome(String(decoded, Charsets.UTF_8), null)
} catch (e: Exception) {
    ConvertOutcome("", "Base64 解码失败，请检查输入是否为合法 Base64")
}

// ---------------------------------------------------------------------------- JSON

/**
 * JSON 美化 / 压缩。
 *
 * 顶层既可能是对象也可能是数组，所以先按首字符判断用哪个解析器；
 * 其余输入（纯数字、裸字符串）不按 JSON 处理，直接提示。
 */
private fun parseJson(input: String): Any {
    val text = input.trim()
    return if (text.startsWith("[")) JSONArray(text) else JSONObject(text)
}

fun prettyJson(input: String): ConvertOutcome {
    val text = input.trim()
    if (text.isEmpty()) return ConvertOutcome("", null)
    return try {
        val pretty = when (val parsed = parseJson(text)) {
            is JSONArray -> parsed.toString(2)
            else -> (parsed as JSONObject).toString(2)
        }
        ConvertOutcome(pretty, null)
    } catch (e: Exception) {
        ConvertOutcome("", "不是合法的 JSON")
    }
}

fun compactJson(input: String): ConvertOutcome {
    val text = input.trim()
    if (text.isEmpty()) return ConvertOutcome("", null)
    return try {
        val compact = when (val parsed = parseJson(text)) {
            is JSONArray -> parsed.toString()
            else -> (parsed as JSONObject).toString()
        }
        ConvertOutcome(compact, null)
    } catch (e: Exception) {
        ConvertOutcome("", "不是合法的 JSON")
    }
}
