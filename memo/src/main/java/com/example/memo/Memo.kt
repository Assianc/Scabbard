package com.example.memo

data class Memo(
    val id: Int,
    val title: String,
    val content: String,
    val timestamp: String,  // 创建时间
    var updateTime: String  // 更新时间
)
