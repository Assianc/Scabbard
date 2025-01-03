package com.assiance.memo

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import java.io.File

class ImageAdapter(
    private val context: Context,
    private val imagePaths: List<String>,
    private val isEditMode: Boolean,
    private val onLongClick: (Int) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.image_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.image_item, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val path = imagePaths[position]
        
        // 创建请求选项
        val requestOptions = RequestOptions()
            .override(800, 800)
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_error)

        // 使用 Glide 加载缩略图
        Glide.with(context)
            .load(File(path))
            .apply(requestOptions)
            .thumbnail(0.5f)  // 添加缩略图支持
            .into(holder.imageView)

        // 设置长按事件（编辑模式下删除图片）
        if (isEditMode) {
            holder.imageView.setOnLongClickListener {
                onLongClick(position)
                true
            }
        }

        // 点击查看完整图片
        holder.imageView.setOnClickListener {
            showFullImage(path)
        }
    }

    override fun getItemCount() = imagePaths.size

    private fun showFullImage(imagePath: String) {
        // 创建对话框显示完整图片
        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        
        // 加载原始大小的图片
        Glide.with(context)
            .load(File(imagePath))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .error(R.drawable.ic_image_error)
            .into(imageView)

        // 点击图片关闭对话框
        imageView.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(imageView)
        dialog.show()
    }
} 