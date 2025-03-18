package com.assiance.memo

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.util.concurrent.TimeUnit

class AudioPlayerAdapter(private val audioFiles: List<File>) : 
    RecyclerView.Adapter<AudioPlayerAdapter.AudioViewHolder>() {
    
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var currentPlayingPosition = -1
    
    inner class AudioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val playPauseButton: ImageButton = itemView.findViewById(R.id.playPauseButton)
        val seekBar: SeekBar = itemView.findViewById(R.id.seekBar)
        val currentTimeText: TextView = itemView.findViewById(R.id.currentTimeText)
        val totalTimeText: TextView = itemView.findViewById(R.id.totalTimeText)
        val waveformView: AudioWaveView = itemView.findViewById(R.id.waveformView)
        
        private val updateSeekBar = object : Runnable {
            override fun run() {
                if (adapterPosition == currentPlayingPosition && mediaPlayer != null && mediaPlayer!!.isPlaying) {
                    val currentPosition = mediaPlayer!!.currentPosition
                    seekBar.progress = currentPosition
                    currentTimeText.text = formatTime(currentPosition.toLong())
                    waveformView.updateWaveform(mediaPlayer)
                    handler.postDelayed(this, 50) // 更快的更新频率
                }
            }
        }
        
        fun bind(audioFile: File) {
            try {
                // 设置初始状态
                playPauseButton.setImageResource(R.drawable.ic_play)
                
                // 设置音频文件路径到波形视图
                waveformView.setAudioFilePath(audioFile.absolutePath)
                
                // 设置总时长（需要临时创建MediaPlayer来获取）
                val tempPlayer = MediaPlayer().apply {
                    setDataSource(audioFile.absolutePath)
                    prepare()
                }
                val duration = tempPlayer.duration
                seekBar.max = duration
                totalTimeText.text = formatTime(duration.toLong())
                tempPlayer.release()
                
                // 播放/暂停按钮点击事件
                playPauseButton.setOnClickListener {
                    if (adapterPosition == currentPlayingPosition && mediaPlayer != null && mediaPlayer!!.isPlaying) {
                        // 暂停播放
                        mediaPlayer?.pause()
                        playPauseButton.setImageResource(R.drawable.ic_play)
                        waveformView.setPlaying(false)
                        handler.removeCallbacks(updateSeekBar)
                    } else if (adapterPosition == currentPlayingPosition && mediaPlayer != null) {
                        // 继续播放
                        mediaPlayer?.start()
                        playPauseButton.setImageResource(R.drawable.ic_pause)
                        waveformView.setPlaying(true)
                        handler.post(updateSeekBar)
                    } else {
                        // 开始新的播放
                        stopCurrentPlayback()
                        currentPlayingPosition = adapterPosition
                        startPlayback(audioFile)
                        playPauseButton.setImageResource(R.drawable.ic_pause)
                        waveformView.setPlaying(true)
                        handler.post(updateSeekBar)
                    }
                }
                
                // 设置SeekBar监听
                seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (fromUser && mediaPlayer != null) {
                            mediaPlayer?.seekTo(progress)
                            currentTimeText.text = formatTime(progress.toLong())
                            waveformView.updateWaveform(mediaPlayer)
                        }
                    }
                    
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            } catch (e: Exception) {
                e.printStackTrace()
                // 处理错误情况
                currentTimeText.text = "00:00"
                totalTimeText.text = "00:00"
                playPauseButton.isEnabled = false
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.audio_player_item, parent, false)
        return AudioViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        holder.bind(audioFiles[position])
        
        // 如果是当前播放的项目，更新UI状态
        if (position == currentPlayingPosition && mediaPlayer != null) {
            if (mediaPlayer!!.isPlaying) {
                holder.playPauseButton.setImageResource(R.drawable.ic_pause)
                holder.waveformView.setPlaying(true)
            } else {
                holder.playPauseButton.setImageResource(R.drawable.ic_play)
                holder.waveformView.setPlaying(false)
            }
            holder.seekBar.progress = mediaPlayer!!.currentPosition
            holder.currentTimeText.text = formatTime(mediaPlayer!!.currentPosition.toLong())
        } else {
            holder.playPauseButton.setImageResource(R.drawable.ic_play)
            holder.waveformView.setPlaying(false)
            holder.seekBar.progress = 0
            holder.currentTimeText.text = "00:00"
        }
    }
    
    override fun getItemCount() = audioFiles.size
    
    private fun startPlayback(audioFile: File) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(audioFile.absolutePath)
                prepare()
                start()
                
                // 设置波形可视化
                val viewHolder = getViewHolderForPosition(currentPlayingPosition)
                viewHolder?.waveformView?.setupWithMediaPlayer(this)
                
                setOnCompletionListener {
                    notifyItemChanged(currentPlayingPosition)
                    currentPlayingPosition = -1
                    mediaPlayer?.release()
                    mediaPlayer = null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 处理播放错误
            currentPlayingPosition = -1
        }
    }
    
    private fun getViewHolderForPosition(position: Int): AudioViewHolder? {
        return if (position != -1) {
            val recyclerView = getRecyclerView()
            recyclerView?.findViewHolderForAdapterPosition(position) as? AudioViewHolder
        } else null
    }
    
    private fun getRecyclerView(): RecyclerView? {
        return try {
            val field = RecyclerView.Adapter::class.java.getDeclaredField("mRecyclerView")
            field.isAccessible = true
            field.get(this) as? RecyclerView
        } catch (e: Exception) {
            null
        }
    }
    
    private fun stopCurrentPlayback() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer!!.isPlaying) {
                    mediaPlayer?.stop()
                }
                mediaPlayer?.release()
                mediaPlayer = null
                
                // 更新之前播放项的UI
                if (currentPlayingPosition != -1) {
                    notifyItemChanged(currentPlayingPosition)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun release() {
        stopCurrentPlayback()
        handler.removeCallbacksAndMessages(null)
    }
    
    private fun formatTime(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - 
                TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%02d:%02d", minutes, seconds)
    }
} 