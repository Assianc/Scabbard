package com.assiance.alm

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class AlarmAdapter(
    private var alarms: List<AlarmData>,
    private val onDeleteClick: (AlarmData) -> Unit,
    private val onToggleClick: (AlarmData, Boolean) -> Unit,
    private val onAlarmClick: (AlarmData) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeText: TextView = view.findViewById(R.id.alarmTimeText)
        val repeatText: TextView = view.findViewById(R.id.alarmRepeatText)
        val enableSwitch: Switch = view.findViewById(R.id.alarmEnableSwitch)
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
        
        holder.enableSwitch.setOnCheckedChangeListener(null)
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
} 