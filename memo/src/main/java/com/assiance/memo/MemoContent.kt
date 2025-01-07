package com.assiance.memo

sealed class MemoContent {
    data class TextContent(
        val text: String,
        var fontName: String = "DEFAULT",
        var style: Int = 0,
        var fontSize: Float = 16f
    ) : MemoContent()
    
    data class ImageContent(val imagePath: String) : MemoContent()
} 