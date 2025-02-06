package com.assiance.memo

data class MemoHistory(
    val id: Int,
    val memoId: Int,
    val oldTitle: String,
    val oldContent: String,
    val oldImagePaths: List<String>,
    val modifyTime: String
) 