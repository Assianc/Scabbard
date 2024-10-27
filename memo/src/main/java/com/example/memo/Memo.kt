package com.example.memo

data class Memo(
    val id: Int,
    val title: String,
    val content: String,
    val timestamp: String,
    var updateTime: String // 新增字段
)
