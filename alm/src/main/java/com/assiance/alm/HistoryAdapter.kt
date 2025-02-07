package com.assiance.alm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

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
        val todoTitle: TextView = view.findViewById(R.id.todoTitle)
        val todoDescription: TextView = view.findViewById(R.id.todoDescription)
        val todoStatus: TextView = view.findViewById(R.id.todoStatus)
        val timeText: TextView = view.findViewById(R.id.timeText)
        val dueTimeText: TextView = view.findViewById(R.id.todoDueTime)
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
        when (val item = items[position]) {
            is HistoryItem.AlarmItem -> {
                holder as AlarmViewHolder
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                holder.timeText.text = timeFormat.format(item.alarm.timeInMillis)
                holder.statusText.text = if (item.alarm.isEnabled) "已启用" else "已禁用"
            }
            is HistoryItem.TodoItem -> {
                holder as TodoViewHolder
                val todo = item.todo
                holder.todoTitle.text = todo.title
                holder.todoDescription.text = todo.description
                
                // 显示完成状态
                if (todo.isCompleted) {
                    holder.todoStatus.visibility = View.VISIBLE
                    holder.todoStatus.text = "已完成"
                } else {
                    holder.todoStatus.visibility = View.GONE
                }

                // 显示时间
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val timeText = when {
                    todo.startTime != null -> "开始时间：${timeFormat.format(todo.startTime)}"
                    todo.dueTime != null -> "截止时间：${timeFormat.format(todo.dueTime)}"
                    else -> ""
                }
                holder.timeText.text = timeText
                holder.timeText.visibility = if (timeText.isEmpty()) View.GONE else View.VISIBLE

                // 新增：显示截止时间（dueTime）逻辑
                if (todo.dueTime != null && todo.dueTime > 0) {
                    // 如果存在截止时间，则显示并格式化展示
                    holder.dueTimeText.visibility = View.VISIBLE
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    holder.dueTimeText.text = "截止: " + sdf.format(Date(todo.dueTime))
                } else {
                    // 没有截止时间，则隐藏该控件
                    holder.dueTimeText.visibility = View.GONE
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