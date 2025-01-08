package com.assiance.alm

data class TodoData(
    val id: Int,
    val title: String,
    val description: String = "",
    val dueTime: Long? = null,
    val isCompleted: Boolean = false
) 