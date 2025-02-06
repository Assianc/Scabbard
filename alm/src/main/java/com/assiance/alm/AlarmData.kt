package com.assiance.alm

data class AlarmData(
    val id: Int,
    val timeInMillis: Long,
    val isEnabled: Boolean = true,
    val ringtoneUri: String? = null,
    val repeatDays: BooleanArray = BooleanArray(7) { true } // 存储周一到周日是否重复
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AlarmData

        if (id != other.id) return false
        if (timeInMillis != other.timeInMillis) return false
        if (isEnabled != other.isEnabled) return false
        if (ringtoneUri != other.ringtoneUri) return false
        if (!repeatDays.contentEquals(other.repeatDays)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + timeInMillis.hashCode()
        result = 31 * result + isEnabled.hashCode()
        result = 31 * result + (ringtoneUri?.hashCode() ?: 0)
        result = 31 * result + repeatDays.contentHashCode()
        return result
    }
} 