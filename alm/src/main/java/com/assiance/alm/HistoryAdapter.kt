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
        val descriptionText: TextView = view.findViewById(R.id.descriptionText)
        val timeText: TextView = view.findViewById(R.id.timeText)
        val statusText: TextView = view.findViewById(R.id.statusText)
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
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        when (val item = items[position]) {
            is HistoryItem.AlarmItem -> {
                holder as AlarmViewHolder
                val alarm = item.alarm
                holder.timeText.text = timeFormat.format(alarm.timeInMillis)
                holder.statusText.text = if (alarm.isEnabled) "已启用" else "已禁用"
            }
            is HistoryItem.TodoItem -> {
                holder as TodoViewHolder
                val todo = item.todo
                holder.titleText.text = todo.title
                holder.descriptionText.text = todo.description
                
                val timeText = StringBuilder()
                todo.startTime?.let { 
                    timeText.append("开始: ${timeFormat.format(it)}")
                }
                todo.dueTime?.let {
                    if (timeText.isNotEmpty()) timeText.append(" | ")
                    timeText.append("截止: ${timeFormat.format(it)}")
                }
                holder.timeText.text = timeText
                
                holder.statusText.text = if (todo.isCompleted) "已完成" else "未完成"
            }
        }
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) = when (items[position]) {
        is HistoryItem.AlarmItem -> TYPE_ALARM
        is HistoryItem.TodoItem -> TYPE_TODO
    }

    fun updateItems(alarms: List<AlarmData>, todos: List<TodoData>) {
        items.clear()
        
        // 添加闹钟项
        items.addAll(alarms.map { HistoryItem.AlarmItem(it) })
        
        // 添加待办项
        items.addAll(todos.map { HistoryItem.TodoItem(it) })
        
        // 按时间排序
        items.sortBy {
            when (it) {
                is HistoryItem.AlarmItem -> it.alarm.timeInMillis
                is HistoryItem.TodoItem -> it.todo.startTime ?: it.todo.dueTime ?: Long.MAX_VALUE
            }
        }
        
        notifyDataSetChanged()
    }
} 