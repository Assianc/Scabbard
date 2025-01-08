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
        val dueTimeText: TextView = view.findViewById(R.id.todoDueTimeText)
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
        
        todo.dueTime?.let {
            val timeFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            holder.dueTimeText.text = timeFormat.format(it)
            holder.dueTimeText.visibility = View.VISIBLE
        } ?: run {
            holder.dueTimeText.visibility = View.GONE
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

    override fun getItemCount() = todos.size

    fun updateTodos(newTodos: List<TodoData>) {
        todos = newTodos
        notifyDataSetChanged()
    }
} 