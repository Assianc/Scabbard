package com.assiance.alm

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
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
        val statusText: TextView = view.findViewById(R.id.todoStatusText)
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
                // 设置文字颜色
                setTextColor(if (todo.isCompleted) Color.GRAY else Color.BLACK)
            }
            
            holder.descriptionText.apply {
                textSize = 16f
                setTextColor(if (todo.isCompleted) Color.GRAY else Color.BLACK)
                setTypeface(null, android.graphics.Typeface.NORMAL)
                gravity = android.view.Gravity.TOP
            }
        } else {
            holder.titleText.visibility = View.GONE
            holder.descriptionText.apply {
                textSize = 24f
                setTextColor(if (todo.isCompleted) Color.GRAY else Color.BLACK)
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
        
        // 设置时间文本颜色
        val timeText = buildTimeText(todo)
        if (timeText.isNotEmpty()) {
            holder.timeText.visibility = View.VISIBLE
            holder.timeText.text = timeText
            holder.timeText.setTextColor(if (todo.isCompleted) Color.GRAY else Color.BLACK)
        } else {
            holder.timeText.visibility = View.GONE
        }
        
        // 设置状态文本
        holder.statusText.visibility = if (todo.isCompleted) View.VISIBLE else View.GONE
        holder.statusText.text = "已完成"
        holder.statusText.setTextColor(Color.GRAY)
        
        // 修改复选框行为
        holder.completeCheckBox.setOnCheckedChangeListener(null)
        holder.completeCheckBox.isChecked = todo.isCompleted
        
        if (todo.isCompleted) {
            // 如果已完成，点击复选框不响应，需要长按才能恢复
            holder.completeCheckBox.setOnCheckedChangeListener { _, _ ->
                holder.completeCheckBox.isChecked = true  // 保持选中状态
            }
            
            holder.cardView.setOnLongClickListener {
                showRestoreDialog(holder.itemView.context, todo)
                true
            }
        } else {
            // 未完成状态下正常响应点击
            holder.completeCheckBox.setOnCheckedChangeListener { _, isChecked ->
                onToggleClick(todo, isChecked)
            }
            
            holder.cardView.setOnLongClickListener {
                onDeleteClick(todo)
                true
            }
        }

        holder.cardView.setOnClickListener {
            onTodoClick(todo)
        }
    }

    private fun showRestoreDialog(context: Context, todo: TodoData) {
        AlertDialog.Builder(context)
            .setTitle("恢复待办")
            .setMessage("是否将此待办恢复为未完成状态？")
            .setPositiveButton("恢复") { _, _ ->
                onToggleClick(todo, false)
            }
            .setNegativeButton("取消", null)
            .show()
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

    fun attachToRecyclerView(recyclerView: RecyclerView) {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT
        ) {
            private var swipeBack = false
            
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // 不在这里处理删除，而是在 onChildDraw 中处理
                // 重置 item 位置
                notifyItemChanged(viewHolder.adapterPosition)
            }

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val todo = todos[viewHolder.adapterPosition]
                // 已完成的待办不能侧滑删除
                return if (todo.isCompleted) 0 else super.getSwipeDirs(recyclerView, viewHolder)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val deleteIcon = ContextCompat.getDrawable(itemView.context, R.drawable.ic_delete)
                val background = ColorDrawable(Color.RED)
                
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    // 限制最大滑动距离为宽度的三分之一
                    val maxSwipeDistance = itemView.width / 3f
                    val limitedDX = dX.coerceIn(-maxSwipeDistance, 0f)
                    
                    val alpha = 1.0f - Math.abs(limitedDX) / maxSwipeDistance
                    itemView.alpha = alpha
                    itemView.translationX = limitedDX

                    // 设置背景
                    background.setBounds(
                        itemView.right + limitedDX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    background.draw(c)

                    // 绘制删除图标
                    deleteIcon?.let {
                        val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                        val iconTop = itemView.top + iconMargin
                        val iconBottom = iconTop + it.intrinsicHeight
                        val iconLeft = itemView.right - iconMargin - it.intrinsicWidth
                        val iconRight = itemView.right - iconMargin
                        it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        it.draw(c)
                    }

                    // 当滑动超过阈值时触发删除
                    if (!swipeBack && Math.abs(limitedDX) >= maxSwipeDistance * 0.75f) {
                        swipeBack = true
                        val position = viewHolder.adapterPosition
                        val todo = todos[position]
                        onDeleteClick(todo)
                    }
                } else {
                    swipeBack = false
                }

                // 不调用父类的 onChildDraw，完全自己控制绘制
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.alpha = 1.0f
                swipeBack = false
            }
        })

        itemTouchHelper.attachToRecyclerView(recyclerView)
    }
} 