package com.assiance.memo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MemoHistoryAdapter(
    private val historyList: List<MemoHistory>,
    private val onRestoreClick: (MemoHistory) -> Unit
) : RecyclerView.Adapter<MemoHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeText: TextView = view.findViewById(R.id.history_time)
        val titleText: TextView = view.findViewById(R.id.history_title)
        val contentText: TextView = view.findViewById(R.id.history_content)
        val restoreButton: Button = view.findViewById(R.id.restore_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.history_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val history = historyList[position]
        holder.timeText.text = history.modifyTime
        holder.titleText.text = history.oldTitle
        holder.contentText.text = history.oldContent
        holder.restoreButton.setOnClickListener {
            onRestoreClick(history)
        }
    }

    override fun getItemCount() = historyList.size
} 