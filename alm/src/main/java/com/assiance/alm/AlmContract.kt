package com.assiance.alm

/**
 * alm 模块的常量契约。
 *
 * 这些常量原本写在 `MainActivityAlm.companion` 里；因为 4 个 Activity 被删除，常量集中搬到这里。
 * **取值一个字都没有改动**——尤其是 Receiver 的 action 字符串，属于模块对外契约。
 */
object AlmContract {

    // ------------------------------------------------------------ 闹钟
    internal const val ALARM_REQUEST_CODE = 100
    internal const val CHANNEL_ID = "AlarmChannel"
    internal const val NOTIFICATION_ID = 1
    internal const val ALARM_PREFS = "alarm_prefs"
    internal const val ALARM_LIST_KEY = "alarm_list"

    const val ALARM_ACTION = "com.assiance.alm.ALARM_TRIGGER"
    const val ALARM_STATUS_CHANGED_ACTION = "com.assiance.alm.ALARM_STATUS_CHANGED"
    const val ALARM_STOP_ACTION = "com.assiance.alm.ALARM_STOP"

    // ------------------------------------------------------------ 待办
    internal const val TODO_PREFS = "todo_prefs"
    internal const val TODO_LIST_KEY = "todo_list"

    const val TODO_REMINDER_ACTION = "com.assiance.alm.TODO_REMINDER"
    const val TODO_REMINDER_STOP_ACTION = "com.assiance.alm.TODO_REMINDER_STOP"
    const val TODO_CHANNEL_ID = "TodoChannel"
    const val TODO_NOTIFICATION_ID = 2

    // ------------------------------------------------------------ 法定假期
    /** 假期缓存（每年一个键，存原始 JSON 响应体）。见 `HolidayRepository`。 */
    internal const val HOLIDAY_PREFS = "holiday_prefs"
}
