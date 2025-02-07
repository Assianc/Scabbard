package com.assiance.memo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.util.Random
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator

class AudioWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.BLUE
        strokeWidth = 5f
        isAntiAlias = true
    }

    private val random = Random()
    private var amplitudes = FloatArray(30) { 0f }
    private var animator: ValueAnimator? = null
    private var isPlaying = false

    init {
        // 初始化波形数据
        updateAmplitudes()
    }

    private fun updateAmplitudes() {
        for (i in amplitudes.indices) {
            amplitudes[i] = random.nextFloat() * 0.8f + 0.2f // 生成0.2到1.0之间的随机数
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val width = width.toFloat()
        val height = height.toFloat()
        val barWidth = width / (amplitudes.size * 2f)
        
        for (i in amplitudes.indices) {
            val amplitude = amplitudes[i] * height / 2
            val startX = width / 2 + (i * barWidth * 2)
            val startY = height / 2 + amplitude
            val stopY = height / 2 - amplitude
            
            // 绘制右侧
            canvas.drawLine(startX, height / 2, startX, stopY, paint)
            
            // 绘制左侧对称部分
            val leftX = width / 2 - (i * barWidth * 2)
            canvas.drawLine(leftX, height / 2, leftX, stopY, paint)
        }
    }

    fun startAnimation() {
        stopAnimation()
        isPlaying = true
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 200
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
            
            addUpdateListener {
                if (isPlaying) {
                    updateAmplitudes()
                    invalidate()
                }
            }
            start()
        }
    }

    fun stopAnimation() {
        isPlaying = false
        animator?.cancel()
        animator = null
        // 重置波形
        amplitudes.fill(0.2f)
        invalidate()
    }

    override fun onDetachedFromWindow() {
        stopAnimation()
        super.onDetachedFromWindow()
    }
} 