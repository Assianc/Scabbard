package com.assiance.memo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MixedContentAdapter(
    private val context: Context,
    private val contents: List<MemoContent>,
    private val isEditMode: Boolean,
    private var currentFontName: String,
    private var currentStyle: Int,
    private var currentFontSize: Float,
    private val onImageClick: (String) -> Unit,
    private val onImageLongClick: (Int) -> Unit,
    private val onTextChange: (Int, String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_TEXT = 0
        private const val TYPE_IMAGE = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (contents[position]) {
            is MemoContent.TextContent -> TYPE_TEXT
            is MemoContent.ImageContent -> TYPE_IMAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_TEXT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_text_content, parent, false)
                TextViewHolder(view)
            }
            TYPE_IMAGE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_image_content, parent, false)
                ImageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val content = contents[position]) {
            is MemoContent.TextContent -> {
                val textHolder = holder as TextViewHolder
                textHolder.editText.apply {
                    setText(content.text)
                    isEnabled = isEditMode
                    textSize = currentFontSize
                    
                    // 使用 FontUtils
                    typeface = FontUtils.getFontByName(currentFontName)
                    
                    // 应用样式
                    setTypeface(typeface, currentStyle)
                    
                    setOnFocusChangeListener { _, hasFocus ->
                        if (!hasFocus) {
                            onTextChange(position, text.toString())
                        }
                    }
                }
            }
            is MemoContent.ImageContent -> {
                val imageHolder = holder as ImageViewHolder
                Glide.with(context)
                    .load(content.imagePath)
                    .override(1024, 1024)
                    .into(imageHolder.imageView)
                
                imageHolder.imageView.setOnClickListener {
                    onImageClick(content.imagePath)
                }
                
                if (isEditMode) {
                    imageHolder.imageView.setOnLongClickListener {
                        onImageLongClick(position)
                        true
                    }
                }
            }
        }
    }

    override fun getItemCount() = contents.size

    class TextViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val editText: EditText = view.findViewById(R.id.content_edit_text)
    }

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.content_image_view)
    }

    fun updateFont(fontName: String) {
        currentFontName = fontName
        notifyDataSetChanged()
    }

    fun updateStyle(style: Int) {
        currentStyle = style
        notifyDataSetChanged()
    }

    fun updateFontSize(size: Float) {
        currentFontSize = size
        notifyDataSetChanged()
    }

    fun updateUnderline(underline: Boolean) {
        // 在 ViewHolder 中处理下划线
        notifyDataSetChanged()
    }
} 