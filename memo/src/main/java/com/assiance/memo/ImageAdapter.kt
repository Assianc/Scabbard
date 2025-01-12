package com.assiance.memo

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import java.io.File
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build

class ImageAdapter(
    private val context: Context,
    private val imagePaths: List<String>,
    private val isEditMode: Boolean,
    private val onLongClick: (Int) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.image_view)
    }

    private val requestOptions = RequestOptions()
        .override(Target.SIZE_ORIGINAL)
        .fitCenter()
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .placeholder(R.drawable.ic_image_placeholder)
        .error(R.drawable.ic_image_error)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.image_item, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val path = imagePaths[position]
        Log.d("ImageAdapter", "正在加载图片路径: $path")

        loadImage(holder.imageView, path)

        if (isEditMode) {
            holder.imageView.setOnLongClickListener {
                onLongClick(position)
                true
            }
        }

        holder.imageView.setOnClickListener {
            showFullImage(path)
        }
    }

    private fun loadImage(imageView: ImageView, path: String) {
        Log.d("ImageAdapter", "开始加载图片: $path")

        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        if (!hasPermission) {
            Log.e("ImageAdapter", "没有存储权限，无法加载图片: $path")
            Glide.with(context)
                .load(R.drawable.ic_image_error)
                .into(imageView)
            return
        }

        val file = File(path)
        if (!file.exists()) {
            Log.e("ImageAdapter", "图片文件不存在: $path")
            Glide.with(context)
                .load(R.drawable.ic_image_error)
                .into(imageView)
            return
        }

        Log.d("ImageAdapter", "文件大小: ${file.length()} bytes")

        // 缩略图请求
        val thumbnailRequest: RequestBuilder<Drawable> = Glide.with(context)
            .load(R.drawable.ic_image_placeholder)

        // 主图像请求
        Glide.with(context)
            .load(file)
            .apply(requestOptions)
            .thumbnail(thumbnailRequest)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(imageView)
    }

    private fun showFullImage(imagePath: String) {
        val file = File(imagePath)
        if (!file.exists()) {
            Toast.makeText(context, "图片文件不存在", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        Glide.with(context)
            .load(file)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .error(R.drawable.ic_image_error)
            .into(imageView)

        imageView.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(imageView)
        dialog.show()
    }

    override fun getItemCount() = imagePaths.size
}