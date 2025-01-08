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
        holder.titleText.text = todo.title
        
        val timeText = buildTimeText(todo)
        if (timeText.isNotEmpty()) {
            holder.timeText.text = timeText
            holder.timeText.visibility = View.VISIBLE
        } else {
            holder.timeText.visibility = View.GONE
        }
        
        holder.titleText.apply {
            paintFlags = if (todo.isCompleted) {
                paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }
        
        holder.completeCheckBox.setOnCheckedChangeListener(null)
        holder.completeCheckBox.isChecked = todo.isCompleted
        holder.completeCheckBox.setOnCheckedChangeListener { _, isChecked ->
            onToggleClick(todo, isChecked)
        }
        
        holder.deleteButton.setOnClickListener {
            onDeleteClick(todo)
        }

        holder.container.setOnClickListener {
            onTodoClick(todo)
        }
    }

    private fun buildTimeText(todo: TodoData): String {
        val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        val sb = StringBuilder()

        todo.startTime?.let {
            sb.append("开始：")
            sb.append(dateFormat.format(it))
        }

        if (todo.startTime != null && todo.dueTime != null) {
            sb.append(" | ")
        }

        todo.dueTime?.let {
            sb.append("截止：")
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