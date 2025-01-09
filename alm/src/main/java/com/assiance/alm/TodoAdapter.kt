package com.assiance.alm

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
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
        val cardView: View = view
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
            holder.titleText.visibility = View.VISIBLE
            holder.titleText.text = todo.title
            holder.titleText.apply {
                paintFlags = if (todo.isCompleted) {
                    paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }
            }
            
            holder.descriptionText.apply {
                textSize = 16f
                setTextColor(context.getColor(android.R.color.black))
                setTypeface(null, android.graphics.Typeface.NORMAL)
                gravity = android.view.Gravity.TOP
            }
        } else {
            holder.titleText.visibility = View.GONE
            holder.descriptionText.apply {
                textSize = 24f
                setTextColor(context.getColor(android.R.color.black))
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
        }
        
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
        
        val timeText = buildTimeText(todo)
        if (timeText.isNotEmpty()) {
            holder.timeText.visibility = View.VISIBLE
            holder.timeText.text = timeText
        } else {
            holder.timeText.visibility = View.GONE
        }
        
        holder.completeCheckBox.setOnCheckedChangeListener(null)
        holder.completeCheckBox.isChecked = todo.isCompleted
        holder.completeCheckBox.setOnCheckedChangeListener { _, isChecked ->
            onToggleClick(todo, isChecked)
        }
        
        holder.cardView.setOnLongClickListener {
            onDeleteClick(todo)
            true
        }

        holder.cardView.setOnClickListener {
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