package com.assiance.memo

data class Memo(
    val id: Int,
    val title: String,
    val content: String,
    val timestamp: String,  // 创建时间
    var updateTime: String,  // 更新时间
    var imagePaths: List<String> = emptyList(),  // 改为图片路径列表
    var fontName: String = "DEFAULT",  // 添加字体字段
    var titleFontName: String = "DEFAULT",
    var titleStyle: Int = 0,
    var contentStyle: Int = 0,
    var titleUnderline: Boolean = false,
    var contentUnderline: Boolean = false,
    var titleFontSize: Float = 32f,
    var contentFontSize: Float = 16f
)
