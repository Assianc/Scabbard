package com.assiance.alm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val items = mutableListOf<HistoryItem>()
    
    companion object {
        private const val TYPE_ALARM = 0
        private const val TYPE_TODO = 1
    }
    
    sealed class HistoryItem {
        data class AlarmItem(val alarm: AlarmData) : HistoryItem()
        data class TodoItem(val todo: TodoData) : HistoryItem()
    }

    class AlarmViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeText: TextView = view.findViewById(R.id.timeText)
        val statusText: TextView = view.findViewById(R.id.statusText)
    }

    class TodoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.titleText)
        val timeText: TextView = view.findViewById(R.id.timeText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_ALARM -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_history_alarm, parent, false)
                AlarmViewHolder(view)
            }
            TYPE_TODO -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_history_todo, parent, false)
                TodoViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is AlarmViewHolder -> {
                if (item is HistoryItem.AlarmItem) {
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    holder.timeText.text = timeFormat.format(item.alarm.timeInMillis)
                    holder.statusText.text = if (item.alarm.isEnabled) "已启用" else "已禁用"
                }
            }
            is TodoViewHolder -> {
                if (item is HistoryItem.TodoItem) {
                    holder.titleText.text = item.todo.title
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val timeText = when {
                        item.todo.startTime != null -> "开始：${timeFormat.format(item.todo.startTime)}"
                        item.todo.dueTime != null -> "截止：${timeFormat.format(item.todo.dueTime)}"
                        else -> "无时间限制"
                    }
                    holder.timeText.text = timeText
                }
            }
        }
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is HistoryItem.AlarmItem -> TYPE_ALARM
            is HistoryItem.TodoItem -> TYPE_TODO
        }
    }

    fun updateItems(newItems: List<HistoryItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
} 