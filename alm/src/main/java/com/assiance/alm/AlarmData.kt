package com.assiance.alm

data class AlarmData(
    val id: Int,
    val timeInMillis: Long,
    val isEnabled: Boolean = true
) 