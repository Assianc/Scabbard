package com.assiance.memo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import java.util.Random
import kotlin.math.min

class AudioWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        isAntiAlias = true
    }
    
    private val barWidth = 4f
    private val barSpace = 2f
    private var amplitudes = floatArrayOf()
    private val random = Random()
    private var isPlaying = false
    private val rect = RectF()
    
    // 渐变颜色数组
    private val startColor = Color.parseColor("#4CAF50")  // 绿色
    private val midColor = Color.parseColor("#2196F3")    // 蓝色
    private val endColor = Color.parseColor("#9C27B0")    // 紫色

    init {
        // 初始化一些随机振幅值
        generateRandomAmplitudes(50)
    }

    fun setAmplitudes(newAmplitudes: FloatArray) {
        amplitudes = newAmplitudes
        invalidate()
    }
    
    fun setPlaying(playing: Boolean) {
        isPlaying = playing
        if (playing) {
            // 播放时实时更新波形
            postInvalidateOnAnimation()
        }
    }

    private fun generateRandomAmplitudes(count: Int) {
        amplitudes = FloatArray(count) { random.nextFloat() }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (amplitudes.isEmpty()) return
        
        val centerY = height / 2f
        val maxBarHeight = height * 0.8f
        
        var startX = paddingLeft.toFloat()
        
        for (i in amplitudes.indices) {
            // 如果正在播放，为每个条形生成新的随机高度
            val amplitude = if (isPlaying) {
                0.2f + 0.8f * random.nextFloat() // 确保最小高度
            } else {
                amplitudes[i]
            }
            
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
        
        // 如果正在播放，继续请求重绘以实现动画效果
        if (isPlaying) {
            postInvalidateDelayed(50) // 更快的刷新率，更流畅的动画
        }
    }
    
    // 更新波形数据的方法
    fun updateWaveform() {
        if (isPlaying) {
            generateRandomAmplitudes((width / (barWidth + barSpace)).toInt())
            invalidate()
        }
    }
} 