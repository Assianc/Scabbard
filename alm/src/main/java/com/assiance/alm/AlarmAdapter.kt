package com.assiance.alm

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.android.material.materialswitch.MaterialSwitch

class AlarmAdapter(
    private var alarms: List<AlarmData>,
    private val onDeleteClick: (AlarmData) -> Unit,
    private val onToggleClick: (AlarmData, Boolean) -> Unit,
    private val onAlarmClick: (AlarmData) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeText: TextView = view.findViewById(R.id.alarmTimeText)
        val repeatText: TextView = view.findViewById(R.id.alarmRepeatText)
        val enableSwitch: MaterialSwitch = view.findViewById(R.id.alarmEnableSwitch)
        val cardView: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val alarm = alarms[position]
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        holder.timeText.text = timeFormat.format(alarm.timeInMillis)

        // 添加重复日期显示
        val repeatText = buildRepeatText(alarm.repeatDays)
        holder.repeatText.text = repeatText

        holder.enableSwitch.setOnCheckedChangeListener(null)
        // 设置默认文本
        holder.enableSwitch.textOn = holder.enableSwitch.textOn ?: "开"
        holder.enableSwitch.textOff = holder.enableSwitch.textOff ?: "关"
        holder.enableSwitch.text = holder.enableSwitch.text ?: ""

        holder.enableSwitch.isChecked = alarm.isEnabled
        holder.enableSwitch.setOnCheckedChangeListener { _, isChecked ->
            onToggleClick(alarm, isChecked)
        }

        holder.cardView.setOnLongClickListener {
            onDeleteClick(alarm)
            true
        }

        holder.cardView.setOnClickListener {
            onAlarmClick(alarm)
        }
    }

    override fun getItemCount() = alarms.size

    fun updateAlarms(newAlarms: List<AlarmData>) {
        alarms = newAlarms
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
                notifyItemChanged(viewHolder.bindingAdapterPosition)
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
                        val position = viewHolder.bindingAdapterPosition
                        val alarm = alarms[position]
                        onDeleteClick(alarm)
                    }
                } else {
                    swipeBack = false
                }

                // 不调用父类的 onChildDraw，完全自己控制绘制
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.alpha = 1.0f
                swipeBack = false
            }
        })

        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun buildRepeatText(repeatDays: BooleanArray): String {
        val days = arrayOf("一", "二", "三", "四", "五", "六", "日")
        val selectedDays = repeatDays.mapIndexed { index, selected ->
            if (selected) days[index] else null
        }.filterNotNull()

        return when {
            selectedDays.isEmpty() -> "不重复"
            selectedDays.size == 7 -> "每天"
            selectedDays.size == 5 && !repeatDays[5] && !repeatDays[6] -> "工作日"
            selectedDays.size == 2 && repeatDays[5] && repeatDays[6] -> "周末"
            else -> "每周${selectedDays.joinToString("、")}"
        }
    }
} 