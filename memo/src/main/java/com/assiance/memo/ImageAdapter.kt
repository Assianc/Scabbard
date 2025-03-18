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
import android.media.MediaPlayer
import android.widget.ImageButton
import android.widget.SeekBar
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.animation.Animator

class ImageAdapter(
    private val context: Context,
    private val imagePaths: List<String>,
    private var isEditMode: Boolean,
    private var onLongClick: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // 定义不同的 ViewType
    companion object {
        private const val VIEW_TYPE_IMAGE = 0
        private const val VIEW_TYPE_AUDIO = 1
    }

    // 图片的 ViewHolder
    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.image_view)
    }

    // 音频的 ViewHolder
    class AudioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val playPauseButton: ImageButton = view.findViewById(R.id.playPauseButton)
        val seekBar: SeekBar = view.findViewById(R.id.seekBar)
        val currentTimeText: TextView = view.findViewById(R.id.currentTimeText)
        val totalTimeText: TextView = view.findViewById(R.id.totalTimeText)
        val waveformView: AudioWaveView = view.findViewById(R.id.waveformView)
    }

    private val requestOptions = RequestOptions()
        .override(Target.SIZE_ORIGINAL)
        .fitCenter()
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .placeholder(R.drawable.ic_image_placeholder)
        .error(R.drawable.ic_image_error)

    override fun getItemViewType(position: Int): Int {
        return if (imagePaths[position].startsWith("audio:")) {
            VIEW_TYPE_AUDIO
        } else {
            VIEW_TYPE_IMAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_AUDIO -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.audio_player_item, parent, false)
                AudioViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.image_item, parent, false)
                ImageViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val path = imagePaths[position]
        
        when (holder) {
            is AudioViewHolder -> {
                setupAudioPlayer(holder, path.substring(6))
                
                if (isEditMode) {
                    holder.itemView.setOnLongClickListener {
                        onLongClick(position)
                        true
                    }
                }
            }
            is ImageViewHolder -> {
                // 处理图片项（原有的图片加载逻辑）
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
        }
    }

    private var currentMediaPlayer: MediaPlayer? = null
    private var currentPlayingPosition: Int = -1
    private var updateSeekBarHandler: Handler = Handler(Looper.getMainLooper())
    private var updateSeekBarRunnable: Runnable? = null

    private fun setupAudioPlayer(holder: AudioViewHolder, audioPath: String) {
        try {
            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(audioPath)
            mediaPlayer.prepare()
            
            val duration = mediaPlayer.duration
            holder.seekBar.max = duration
            holder.totalTimeText.text = formatTime(duration)
            mediaPlayer.release()
            
            holder.playPauseButton.setOnClickListener {
                if (currentPlayingPosition == holder.adapterPosition && currentMediaPlayer?.isPlaying == true) {
                    pausePlayback(holder)
                } else {
                    startPlayback(holder, audioPath)
                }
            }
            
            holder.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        currentMediaPlayer?.seekTo(progress)
                        holder.currentTimeText.text = formatTime(progress)
                    }
                }
                
                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    updateSeekBarRunnable?.let { updateSeekBarHandler.removeCallbacks(it) }
                }
                
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    if (currentMediaPlayer?.isPlaying == true) {
                        startSeekBarUpdate(holder)
                    }
                }
            })
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "音频加载失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startPlayback(holder: AudioViewHolder, audioPath: String) {
        // 停止当前播放
        stopCurrentPlayback()
        
        try {
            currentMediaPlayer = MediaPlayer().apply {
                setDataSource(audioPath)
                prepare()
                start()
                
                setOnCompletionListener {
                    stopCurrentPlayback()
                }
            }
            
            currentPlayingPosition = holder.adapterPosition
            holder.playPauseButton.setImageResource(R.drawable.ic_pause)
            holder.waveformView.setPlaying(true)
            startSeekBarUpdate(holder)
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "音频播放失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pausePlayback(holder: AudioViewHolder) {
        currentMediaPlayer?.pause()
        holder.playPauseButton.setImageResource(R.drawable.ic_play)
        holder.waveformView.setPlaying(false)
        updateSeekBarRunnable?.let { updateSeekBarHandler.removeCallbacks(it) }
    }

    private fun stopCurrentPlayback() {
        currentMediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        currentMediaPlayer = null
        
        if (currentPlayingPosition != -1) {
            notifyItemChanged(currentPlayingPosition)
        }
        currentPlayingPosition = -1
        
        updateSeekBarRunnable?.let { updateSeekBarHandler.removeCallbacks(it) }
    }

    private fun startSeekBarUpdate(holder: AudioViewHolder) {
        updateSeekBarRunnable?.let { updateSeekBarHandler.removeCallbacks(it) }
        
        updateSeekBarRunnable = object : Runnable {
            override fun run() {
                currentMediaPlayer?.let { player ->
                    holder.seekBar.progress = player.currentPosition
                    holder.currentTimeText.text = formatTime(player.currentPosition)
                    holder.waveformView.updateWaveform()
                    updateSeekBarHandler.postDelayed(this, 100)
                }
            }
        }.also {
            updateSeekBarHandler.post(it)
        }
    }

    private fun formatTime(milliseconds: Int): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        stopCurrentPlayback()
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

        val dialog = PhotoViewDialog(context, imagePath)
        dialog.show()
    }

    override fun getItemCount() = imagePaths.size

    fun updateEditMode(editMode: Boolean, newOnLongClick: (Int) -> Unit) {
        isEditMode = editMode
        onLongClick = newOnLongClick
        notifyDataSetChanged()
    }
}