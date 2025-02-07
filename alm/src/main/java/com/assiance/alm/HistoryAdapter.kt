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
    
    // 添加长按删除回调
    var onDeleteClick: ((HistoryItem, Int) -> Unit)? = null

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

                // 根据待办事项的开始时间和截止时间分别显示
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val hasStart = todo.startTime != null && todo.startTime > 0
                val hasDue = todo.dueTime != null && todo.dueTime > 0

                when {
                    hasStart && hasDue -> {
                        // 同时存在：显示开始时间（左侧）和截止时间（右侧）
                        holder.timeText.visibility = View.VISIBLE
                        holder.dueTimeText.visibility = View.VISIBLE
                        holder.timeText.text = "开始时间：${timeFormat.format(Date(todo.startTime!!))}"
                        holder.dueTimeText.text = "截止：${timeFormat.format(Date(todo.dueTime!!))}"
                    }
                    hasStart -> {
                        // 只有开始时间
                        holder.timeText.visibility = View.VISIBLE
                        holder.dueTimeText.visibility = View.GONE
                        holder.timeText.text = "开始时间：${timeFormat.format(Date(todo.startTime!!))}"
                    }
                    hasDue -> {
                        // 只有截止时间
                        holder.timeText.visibility = View.VISIBLE
                        holder.dueTimeText.visibility = View.GONE
                        holder.timeText.text = "截止时间：${timeFormat.format(Date(todo.dueTime!!))}"
                    }
                    else -> {
                        // 都没有：隐藏时间控件
                        holder.timeText.visibility = View.GONE
                        holder.dueTimeText.visibility = View.GONE
                    }
                }
            }
        }
        // 新增：添加长按监听，触发删除回调
        holder.itemView.setOnLongClickListener {
            onDeleteClick?.invoke(items[position], holder.adapterPosition)
            true
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

    fun removeItemAt(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }
} 