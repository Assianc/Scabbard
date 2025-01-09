package com.assiance.alm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class TodoAdapter(
    private var todos: List<TodoData>,
    private val onDeleteClick: (TodoData) -> Unit,
    private val onToggleClick: (TodoData, Boolean) -> Unit,
    private val onTodoClick: (TodoData) -> Unit
) : RecyclerView.Adapter<TodoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.todoTitleText)
        val descriptionText: TextView = view.findViewById(R.id.todoDescriptionText)
        val timeText: TextView = view.findViewById(R.id.todoTimeText)
        val completeCheckBox: CheckBox = view.findViewById(R.id.todoCompleteCheckBox)
        val deleteButton: ImageButton = view.findViewById(R.id.todoDeleteButton)
        val container: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val todo = todos[position]
        
        // 设置标题和描述内容的显示逻辑
        if (todo.title.isNotEmpty()) {
            // 有标题时，显示标题和描述
            holder.titleText.visibility = View.VISIBLE
            holder.titleText.text = todo.title
            holder.titleText.apply {
                paintFlags = if (todo.isCompleted) {
                    paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }
            }
            
            // 有标题时，描述文本使用中等样式
            holder.descriptionText.apply {
                textSize = 16f  // 增大字号
                setTextColor(context.getColor(android.R.color.black))  // 加深颜色
                setTypeface(null, android.graphics.Typeface.NORMAL)
                gravity = android.view.Gravity.TOP
            }
        } else {
            // 没有标题时，隐藏标题，让描述文本使用标题样式
            holder.titleText.visibility = View.GONE
            holder.descriptionText.apply {
                textSize = 24f  // 使用标题的文字大小
                setTextColor(context.getColor(android.R.color.black))
                setTypeface(null, android.graphics.Typeface.BOLD)  // 使用粗体
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
        }
        
        // 设置描述内容
        if (todo.description.isNotEmpty()) {
            holder.descriptionText.visibility = View.VISIBLE
            holder.descriptionText.text = todo.description
            holder.descriptionText.apply {
                paintFlags = if (todo.isCompleted) {
                    paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }
            }
        } else {
            holder.descriptionText.visibility = View.GONE
        }
        
        // 设置时间信息
        val timeText = buildTimeText(todo)
        if (timeText.isNotEmpty()) {
            holder.timeText.visibility = View.VISIBLE
            holder.timeText.text = timeText
        } else {
            holder.timeText.visibility = View.GONE
        }
        
        // 设置完成状态
        holder.completeCheckBox.setOnCheckedChangeListener(null)
        holder.completeCheckBox.isChecked = todo.isCompleted
        holder.completeCheckBox.setOnCheckedChangeListener { _, isChecked ->
            onToggleClick(todo, isChecked)
        }
        
        // 设置删除按钮
        holder.deleteButton.setOnClickListener {
            onDeleteClick(todo)
        }

        // 设置整体点击
        holder.container.setOnClickListener {
            onTodoClick(todo)
        }
    }

    private fun buildTimeText(todo: TodoData): String {
        val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        val sb = StringBuilder()

        todo.startTime?.let {
            sb.append("开始于：")
            sb.append(dateFormat.format(it))
        }

        if (todo.startTime != null && todo.dueTime != null) {
            sb.append("\n")

        }

        todo.dueTime?.let {
            sb.append("截止于：")
            sb.append(dateFormat.format(it))
        }

        return sb.toString()
    }

    override fun getItemCount() = todos.size

    fun updateTodos(newTodos: List<TodoData>) {
        todos = newTodos
        notifyDataSetChanged()
    }
} 