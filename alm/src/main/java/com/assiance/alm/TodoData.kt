package com.assiance.alm

data class TodoData(
    val id: Int,
    val title: String,
    val description: String = "",
    val startTime: Long? = null,
    val dueTime: Long? = null,
    val isCompleted: Boolean = false,
    val startRingtoneUri: String? = null,
    val dueRingtoneUri: String? = null
) 