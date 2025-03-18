package com.assiance.memo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.media.MediaPlayer
import android.media.audiofx.Visualizer
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import android.util.Log

class AudioWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        isAntiAlias = true
    }
    
    private val barWidth = 3f
    private val barSpace = 1f
    private var waveformData = FloatArray(0)
    private var isPlaying = false
    private val rect = RectF()
    private var visualizer: Visualizer? = null
    private var audioFilePath: String? = null
    private var currentPosition = 0f
    
    // 渐变颜色数组
    private val startColor = Color.parseColor("#4CAF50")  // 绿色
    private val midColor = Color.parseColor("#2196F3")    // 蓝色
    private val endColor = Color.parseColor("#9C27B0")    // 紫色
    
    // 用于平滑动画的数据
    private var targetData = FloatArray(0)
    private var currentData = FloatArray(0)
    private val smoothingFactor = 0.3f  // 平滑因子，值越小动画越平滑

    fun setPlaying(playing: Boolean) {
        isPlaying = playing
        if (playing) {
            // 播放时实时更新波形
            postInvalidateOnAnimation()
        } else {
            // 停止时释放可视化器
            releaseVisualizer()
        }
    }
    
    fun setupWithMediaPlayer(mediaPlayer: MediaPlayer?) {
        releaseVisualizer()
        
        mediaPlayer?.let { player ->
            try {
                visualizer = Visualizer(player.audioSessionId).apply {
                    captureSize = Visualizer.getCaptureSizeRange()[1] // 使用最大采集大小
                    setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer,
                            waveform: ByteArray,
                            samplingRate: Int
                        ) {
                            // 更新波形数据
                            processWaveformData(waveform)
                            postInvalidateOnAnimation()
                        }

                        override fun onFftDataCapture(
                            visualizer: Visualizer,
                            fft: ByteArray,
                            samplingRate: Int
                        ) {
                            // 不使用FFT数据
                        }
                    }, Visualizer.getMaxCaptureRate() / 2, true, false)
                    enabled = true
                }
                
                // 保存当前播放位置的百分比
                currentPosition = player.currentPosition.toFloat() / player.duration
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun setAudioFilePath(path: String?) {
        audioFilePath = path
        if (path != null) {
            loadWaveformFromFile(path)
        } else {
            waveformData = FloatArray(0)
            invalidate()
        }
    }
    
    private fun loadWaveformFromFile(path: String) {
        try {
            val file = File(path)
            if (!file.exists()) {
                Log.e("AudioWaveView", "音频文件不存在: $path")
                return
            }
            
            // 尝试预先分析音频文件生成波形数据
            Thread {
                try {
                    val fis = FileInputStream(file)
                    val buffer = ByteArray(1024)
                    val samples = mutableListOf<Float>()
                    var bytesRead: Int
                    
                    // 跳过WAV文件头（如果是WAV格式）
                    if (path.endsWith(".wav", ignoreCase = true)) {
                        fis.skip(44) // 标准WAV头大小
                    }
                    
                    // 读取音频数据
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        for (i in 0 until bytesRead step 2) {
                            if (i + 1 < bytesRead) {
                                // 将两个字节转换为一个16位短整型
                                val sample = ByteBuffer.wrap(buffer, i, 2)
                                    .order(ByteOrder.LITTLE_ENDIAN)
                                    .short.toInt()
                                // 归一化到-1到1
                                samples.add(sample / 32768f)
                            }
                        }
                    }
                    fis.close()
                    
                    // 压缩样本以适应视图宽度
                    val maxSamples = 200 // 最大样本数
                    val compressedSamples = if (samples.size > maxSamples) {
                        val step = samples.size / maxSamples
                        val result = FloatArray(maxSamples)
                        for (i in 0 until maxSamples) {
                            val start = i * step
                            val end = min((i + 1) * step, samples.size)
                            var sum = 0f
                            for (j in start until end) {
                                sum += abs(samples[j])
                            }
                            result[i] = if (end > start) sum / (end - start) else 0f
                        }
                        result
                    } else {
                        samples.map { abs(it) }.toFloatArray()
                    }
                    
                    // 更新UI线程中的波形数据
                    post {
                        waveformData = compressedSamples
                        invalidate()
                    }
                    
                } catch (e: Exception) {
                    Log.e("AudioWaveView", "加载波形数据失败", e)
                }
            }.start()
            
        } catch (e: Exception) {
            Log.e("AudioWaveView", "处理音频文件失败", e)
        }
    }
    
    private fun processWaveformData(waveform: ByteArray) {
        // 将字节数组转换为浮点数组
        val floatData = FloatArray(waveform.size)
        for (i in waveform.indices) {
            // 将-128到127的值映射到0到1
            floatData[i] = abs(waveform[i].toInt()) / 128f
        }
        
        // 更新目标数据
        targetData = floatData
        
        // 初始化当前数据（如果需要）
        if (currentData.size != targetData.size) {
            currentData = targetData.clone()
        }
    }
    
    private fun releaseVisualizer() {
        visualizer?.let {
            if (it.enabled) {
                it.enabled = false
            }
            it.release()
        }
        visualizer = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // 平滑过渡到目标数据
        if (targetData.isNotEmpty() && currentData.size == targetData.size) {
            for (i in currentData.indices) {
                currentData[i] += (targetData[i] - currentData[i]) * smoothingFactor
            }
            
            // 使用平滑后的数据绘制波形
            drawWaveform(canvas, currentData)
        } else if (waveformData.isNotEmpty()) {
            // 使用预加载的波形数据
            drawWaveform(canvas, waveformData)
        } else {
            // 如果没有波形数据，绘制静态波形
            drawStaticWaveform(canvas)
        }
        
        // 如果正在播放，继续请求重绘以实现动画效果
        if (isPlaying) {
            postInvalidateDelayed(16) // 约60fps的刷新率
        }
    }
    
    private fun drawWaveform(canvas: Canvas, data: FloatArray) {
        val centerY = height / 2f
        val maxBarHeight = height * 0.8f
        
        var startX = paddingLeft.toFloat()
        val barCount = min(
            ((width - paddingLeft - paddingRight) / (barWidth + barSpace)).toInt(),
            data.size
        )
        
        // 计算步长，以便在可用空间内均匀分布条形
        val step = max(1, data.size / barCount)
        
        for (i in 0 until barCount) {
            val dataIndex = (i * step).coerceAtMost(data.size - 1)
            // 确保振幅在0到1之间
            val amplitude = data[dataIndex].coerceIn(0.05f, 1f)
            
            val barHeight = maxBarHeight * amplitude
            
            // 为每个条创建垂直渐变
            val shader = LinearGradient(
                startX, 
                centerY - barHeight / 2,
                startX, 
                centerY + barHeight / 2,
                intArrayOf(startColor, midColor, endColor),
                null,
                Shader.TileMode.CLAMP
            )
            paint.shader = shader
            
            rect.set(
                startX,
                centerY - barHeight / 2,
                startX + barWidth,
                centerY + barHeight / 2
            )
            
            // 绘制圆角矩形
            canvas.drawRoundRect(rect, 2f, 2f, paint)
            
            startX += barWidth + barSpace
            
            // 如果超出视图宽度，停止绘制
            if (startX > width - paddingRight) break
        }
        
        // 绘制播放位置指示器
        if (isPlaying && visualizer != null) {
            val indicatorX = paddingLeft + (width - paddingLeft - paddingRight) * currentPosition
            paint.shader = null
            paint.color = Color.WHITE
            paint.strokeWidth = 2f
            canvas.drawLine(indicatorX, 0f, indicatorX, height.toFloat(), paint)
        }
    }
    
    private fun drawStaticWaveform(canvas: Canvas) {
        val centerY = height / 2f
        val maxBarHeight = height * 0.5f  // 静态波形高度较低
        
        var startX = paddingLeft.toFloat()
        val barCount = ((width - paddingLeft - paddingRight) / (barWidth + barSpace)).toInt()
        
        // 创建静态波形模式
        for (i in 0 until barCount) {
            // 生成静态波形的高度模式
            val amplitude = when {
                i % 8 < 4 -> 0.3f + (i % 4) * 0.1f
                else -> 0.7f - (i % 4) * 0.1f
            }
            
            val barHeight = maxBarHeight * amplitude
            
            paint.shader = LinearGradient(
                startX, 
                centerY - barHeight / 2,
                startX, 
                centerY + barHeight / 2,
                intArrayOf(startColor, midColor, endColor),
                null,
                Shader.TileMode.CLAMP
            )
            
            rect.set(
                startX,
                centerY - barHeight / 2,
                startX + barWidth,
                centerY + barHeight / 2
            )
            
            canvas.drawRoundRect(rect, 2f, 2f, paint)
            
            startX += barWidth + barSpace
            
            if (startX > width - paddingRight) break
        }
    }
    
    // 更新波形数据和播放位置的方法
    fun updateWaveform(mediaPlayer: MediaPlayer? = null) {
        mediaPlayer?.let {
            if (it.isPlaying) {
                currentPosition = it.currentPosition.toFloat() / it.duration
            }
        }
        
        if (isPlaying) {
            invalidate()
        }
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        releaseVisualizer()
    }
} 