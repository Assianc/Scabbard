package com.assiance.alm

/**
 * 日历页的一条数据：闹钟或待办。
 *
 * 原先是 `HistoryAdapter.HistoryItem` —— 一个 RecyclerView Adapter 的嵌套类型。
 * 2026-09-23 日历页照约定页重做后，那条历史记录列表改由 Compose 直接渲染
 * （见 [com.assiance.alm.ui.CalendarScreen]），`HistoryAdapter` 整个删掉了，
 * 只剩这个类型还在给 [AlmRepository.loadHistoryItems] 当返回元素，于是提到顶层。
 *
 * 变成顶层类型后调用方少一层前缀：`HistoryAdapter.HistoryItem.AlarmItem` →
 * [AlarmItem]。字段与结构一字未改。
 */
sealed interface HistoryItem {
    data class AlarmItem(val alarm: AlarmData) : HistoryItem
    data class TodoItem(val todo: TodoData) : HistoryItem
}
