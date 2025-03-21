package com.assiance.memo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MemoHistoryAdapter(
    private val historyList: MutableList<MemoHistory>,
    private val onRestoreClick: (MemoHistory) -> Unit,
    private val onDeleteClick: (MemoHistory, Int) -> Unit
) : RecyclerView.Adapter<MemoHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeText: TextView = view.findViewById(R.id.history_time)
        val titleText: TextView = view.findViewById(R.id.history_title)
        val contentText: TextView = view.findViewById(R.id.history_content)
        val restoreButton: Button = view.findViewById(R.id.restore_button)
        val mediaNoticeText: TextView = view.findViewById(R.id.media_notice)
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
        
        // 检查是否包含图片和音频
        val hasImages = history.oldImagePaths.any { !it.startsWith("audio:") }
        val hasAudio = history.oldImagePaths.any { it.startsWith("audio:") }
        
        // 如果有图片或音频，显示提示信息
        if (hasImages || hasAudio) {
            holder.mediaNoticeText.visibility = View.VISIBLE
            holder.mediaNoticeText.text = "此处图文和音频不支持历史记录查阅"
        } else {
            holder.mediaNoticeText.visibility = View.GONE
        }
        
        holder.restoreButton.setOnClickListener {
            onRestoreClick(history)
        }
        holder.itemView.setOnLongClickListener {
            onDeleteClick(history, holder.adapterPosition)
            true
        }
    }

    override fun getItemCount() = historyList.size
} 