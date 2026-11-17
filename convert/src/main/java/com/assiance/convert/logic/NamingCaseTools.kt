package com.assiance.convert.logic

/**
 * 命名风格转换的纯逻辑（不依赖 Android）。
 *
 * 分词是这里唯一的难点：输入可能是 `user_name`、`userName`、`UserName`、`user-name`、
 * `USER_NAME`、`user name` 中的任意一种，先按分隔符切块，再按"小写→大写"边界拆词，
 * 连续大写（如 `HTTPResponse` 的 `HTTP`）整体成词。
 */

/** 支持的 5 种命名风格。[label] 直接展示在结果行上。 */
enum class NamingCaseStyle(val label: String) {
    SNAKE("snake_case"),
    CAMEL("camelCase"),
    PASCAL("PascalCase"),
    KEBAB("kebab-case"),
    CONSTANT("CONSTANT_CASE"),
}

/** 把任意命名拆成小写单词列表；空输入返回空列表。 */
fun splitWords(input: String): List<String> =
    input
        .split('_', '-', ' ', '.', '/')
        .flatMap { splitChunk(it) }
        .filter { it.isNotEmpty() }

/**
 * 拆单个词块。
 *
 * - 小写/数字 → 大写：`userName` → user / Name，`user2Name` → user2 / Name；
 * - 连续大写后接小写：`HTTPResponse` → HTTP / Response；
 * - 纯大写块（`USER`）整体保留，最终统一转小写。
 */
private fun splitChunk(chunk: String): List<String> {
    if (chunk.isEmpty()) return emptyList()
    val words = mutableListOf<String>()
    val current = StringBuilder()

    fun flush() {
        if (current.isNotEmpty()) {
            words += current.toString()
            current.clear()
        }
    }

    for (i in chunk.indices) {
        val c = chunk[i]
        val prev = current.lastOrNull()
        if (prev != null && c.isUpperCase() && c.isLetter() && prev.isLetter()) {
            val next = chunk.getOrNull(i + 1)
            val boundary = when {
                // 小写或数字 → 大写：新词开始
                prev.isLowerCase() || prev.isDigit() -> true
                // 连续大写后接小写：最后这个大写属于下一个词
                prev.isUpperCase() && next != null && next.isLowerCase() -> true
                else -> false
            }
            if (boundary) flush()
        }
        current.append(c)
    }
    flush()
    return words
}

/** 把输入按 [style] 重组；输入没有可用字符时返回空串。 */
fun convertNaming(input: String, style: NamingCaseStyle): String {
    val words = splitWords(input)
    if (words.isEmpty()) return ""
    return when (style) {
        NamingCaseStyle.SNAKE -> words.joinToString("_") { it.lowercase() }
        NamingCaseStyle.KEBAB -> words.joinToString("-") { it.lowercase() }
        NamingCaseStyle.CONSTANT -> words.joinToString("_") { it.uppercase() }
        NamingCaseStyle.CAMEL -> words
            .mapIndexed { index, word ->
                if (index == 0) word.lowercase() else word.lowercase().replaceFirstChar { it.uppercaseChar() }
            }
            .joinToString("")
        NamingCaseStyle.PASCAL -> words
            .joinToString("") { word ->
                word.lowercase().replaceFirstChar { it.uppercaseChar() }
            }
    }
}
