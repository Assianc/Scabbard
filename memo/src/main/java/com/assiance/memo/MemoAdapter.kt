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

class MemoAdapter(
    private val memoList: MutableList<Memo>,
    private val context: Context,
    private val memoDAO: MemoDAO
) : RecyclerView.Adapter<MemoAdapter.MemoViewHolder>() {

    companion object {
        private const val MAX_CONTENT_LENGTH = 50
    }

    var isMultiSelectMode = false
        private set

    private val selectedItems = mutableListOf<Memo>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.memo_item, parent, false)
        return MemoViewHolder(view)
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onBindViewHolder(holder: MemoViewHolder, position: Int) {
        val memo = memoList[position]
        holder.titleTextView.text = memo.title
        holder.updateTimeTextView.text = "上次更新: ${memo.updateTime}"

        holder.imageTextIndicator.visibility = 
            if (memo.imagePaths.isNotEmpty()) View.VISIBLE else View.GONE

        holder.titleTextView.typeface = FontUtils.getFontByName(memo.titleFontName)
        holder.titleTextView.textSize = memo.titleFontSize
        holder.titleTextView.setTypeface(holder.titleTextView.typeface, memo.titleStyle)
        holder.titleTextView.paint.isUnderlineText = memo.titleUnderline

        val content = memo.content
        val displayContent = if (content.length > 100) {
            "${content.substring(0, 100)}..."
        } else {
            content
        }
        holder.contentTextView.text = displayContent

        holder.contentTextView.typeface = FontUtils.getFontByName(memo.fontName)
        holder.contentTextView.textSize = memo.contentFontSize
        holder.contentTextView.setTypeface(holder.contentTextView.typeface, memo.contentStyle)
        holder.contentTextView.paint.isUnderlineText = memo.contentUnderline

        holder.checkBox.visibility = if (isMultiSelectMode) View.VISIBLE else View.GONE
        holder.checkBox.isChecked = selectedItems.contains(memo)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, MemoDetailActivity::class.java).apply {
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
            context.startActivity(intent)
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
                if (context is MainActivityMemo) {
                    context.toggleDeleteButton(true)
                }
                notifyDataSetChanged()
            }
            true
        }

        holder.checkBox.setOnClickListener {
            if (holder.checkBox.isChecked) {
                selectedItems.add(memo)
            } else {
                selectedItems.remove(memo)
            }
        }
    }

    override fun getItemCount(): Int = memoList.size

    @SuppressLint("NotifyDataSetChanged")
    fun deleteSelectedMemos() {
        for (memo in selectedItems) {
            memoDAO.deleteMemo(memo.id)
        }
        memoList.removeAll(selectedItems)
        selectedItems.clear()
        isMultiSelectMode = false
        notifyDataSetChanged()

        if (context is MainActivityMemo) {
            context.toggleDeleteButton(false)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun exitMultiSelectMode() {
        isMultiSelectMode = false
        selectedItems.clear()
        notifyDataSetChanged()
        if (context is MainActivityMemo) {
            context.toggleDeleteButton(false)
        }
    }

    class MemoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.memo_title)
        val contentTextView: TextView = itemView.findViewById(R.id.memo_content)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkbox)
        val updateTimeTextView: TextView = itemView.findViewById(R.id.memo_update_time)
        val imageTextIndicator: TextView = itemView.findViewById(R.id.image_text_indicator)
    }
}
