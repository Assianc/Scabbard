package com.assiance.memo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.widget.Toast
import androidx.lifecycle.lifecycleScope

class MemoAdapter(
    private val memoList: MutableList<Memo>,
    private val activity: MainActivityMemo,
    private val memoDAO: MemoDAO
) : RecyclerView.Adapter<MemoAdapter.ViewHolder>() {

    companion object {
        private const val MAX_CONTENT_LENGTH = 50
    }

    private val selectedItems = HashSet<Int>()
    var isMultiSelectMode = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.memo_item, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val memo = memoList[position]
        holder.titleTextView.text = memo.title
        holder.updateTimeTextView.text = "上次更新: ${memo.updateTime}"

        holder.imageTextIndicator.visibility = 
            if (memo.imagePaths.isNotEmpty()) View.VISIBLE else View.GONE

        val content = memo.content
        val displayContent = if (content.length > MAX_CONTENT_LENGTH) {
            content.substring(0, MAX_CONTENT_LENGTH) + "..."
        } else {
            content
        }
        holder.contentTextView.text = displayContent
        holder.contentTextView.maxLines = 2

        try {
            when (memo.titleFontName) {
                "宋体" -> ResourcesCompat.getFont(activity, R.font.simsunch)
                "仿宋" -> ResourcesCompat.getFont(activity, R.font.fasimsunch)
                "黑体" -> ResourcesCompat.getFont(activity, R.font.simheich)
                else -> Typeface.DEFAULT
            }?.let { typeface ->
                holder.titleTextView.typeface = typeface
            }
        } catch (e: Exception) {
            e.printStackTrace()
            holder.titleTextView.typeface = Typeface.DEFAULT
        }

        try {
            when (memo.fontName) {
                "宋体" -> ResourcesCompat.getFont(activity, R.font.simsunch)
                "仿宋" -> ResourcesCompat.getFont(activity, R.font.fasimsunch)
                "黑体" -> ResourcesCompat.getFont(activity, R.font.simheich)
                else -> Typeface.DEFAULT
            }?.let { typeface ->
                holder.contentTextView.typeface = typeface
            }
        } catch (e: Exception) {
            e.printStackTrace()
            holder.contentTextView.typeface = Typeface.DEFAULT
        }

        holder.checkBox.visibility = if (isMultiSelectMode) View.VISIBLE else View.GONE
        holder.checkBox.isChecked = selectedItems.contains(position)

        holder.itemView.setOnClickListener {
            val intent = Intent(activity, MemoDetailActivity::class.java).apply {
                putExtra("memo_id", memo.id)
                putExtra("memo_title", memo.title)
                putExtra("memo_content", memo.content)
                putExtra("memo_update_time", "${memo.updateTime} 修改")
                putStringArrayListExtra("memo_image_paths", ArrayList(memo.imagePaths))
                putExtra("memo_font_name", memo.fontName)
                putExtra("memo_title_font_name", memo.titleFontName)
                putExtra("memo_title_style", memo.titleStyle)
                putExtra("memo_content_style", memo.contentStyle)
                putExtra("memo_title_underline", memo.titleUnderline)
                putExtra("memo_content_underline", memo.contentUnderline)
                putExtra("memo_title_font_size", memo.titleFontSize)
                putExtra("memo_content_font_size", memo.contentFontSize)
            }
            
            if (activity is MainActivityMemo) {
                val options = androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(
                    activity,
                    androidx.core.util.Pair(holder.titleTextView, "memo_title"),
                    androidx.core.util.Pair(holder.contentTextView, "memo_content"),
                    androidx.core.util.Pair(holder.updateTimeTextView, "memo_time")
                )
                activity.startActivity(intent, options.toBundle())
            }
        }

        holder.contentTextView.setOnClickListener(object : View.OnClickListener {
            private var isExpanded = false

            override fun onClick(v: View?) {
                isExpanded = !isExpanded
                holder.contentTextView.text = if (isExpanded) content else displayContent
            }
        })

        holder.itemView.setOnLongClickListener {
            if (!isMultiSelectMode) {
                isMultiSelectMode = true
                if (activity is MainActivityMemo) {
                    activity.toggleDeleteButton(true)
                }
                notifyDataSetChanged()
            }
            true
        }

        holder.checkBox.setOnClickListener {
            if (holder.checkBox.isChecked) {
                selectedItems.add(position)
            } else {
                selectedItems.remove(position)
            }
        }

        holder.titleTextView.setTypeface(holder.titleTextView.typeface, memo.titleStyle)
        holder.contentTextView.setTypeface(holder.contentTextView.typeface, memo.contentStyle)
        holder.titleTextView.paint.isUnderlineText = memo.titleUnderline
        holder.contentTextView.paint.isUnderlineText = memo.contentUnderline
    }

    override fun getItemCount(): Int = memoList.size

    @SuppressLint("NotifyDataSetChanged")
    fun deleteSelectedMemos() {
        activity.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 获取要删除的备忘录
                val memosToDelete = selectedItems.mapNotNull { index ->
                    if (index < memoList.size) memoList[index] else null
                }
                
                // 在数据库中删除
                memosToDelete.forEach { memo ->
                    memoDAO.deleteMemo(memo.id)
                }
                
                withContext(Dispatchers.Main) {
                    // 从列表中删除，从大到小删除以避免索引变化
                    selectedItems.sortedByDescending { it }.forEach { index ->
                        if (index < memoList.size) {
                            memoList.removeAt(index)
                            notifyItemRemoved(index)
                        }
                    }
                    
                    // 清理选择状态
                    selectedItems.clear()
                    isMultiSelectMode = false
                    activity.toggleDeleteButton(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "删除失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun exitMultiSelectMode() {
        isMultiSelectMode = false
        selectedItems.clear()
        notifyDataSetChanged()
        if (activity is MainActivityMemo) {
            activity.toggleDeleteButton(false)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.memo_title)
        val contentTextView: TextView = itemView.findViewById(R.id.memo_content)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkbox)
        val updateTimeTextView: TextView = itemView.findViewById(R.id.memo_update_time)
        val imageTextIndicator: TextView = itemView.findViewById(R.id.image_text_indicator)
    }
}
