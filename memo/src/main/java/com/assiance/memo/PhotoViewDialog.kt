package com.assiance.memo

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import kotlin.math.max
import kotlin.math.min

class PhotoViewDialog(context: Context, private val imagePath: String) : Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
    private lateinit var photoView: ImageView
    private lateinit var container: ConstraintLayout
    private var scaleGestureDetector: ScaleGestureDetector
    private var scaleFactor = 1.0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var translateX = 0f
    private var translateY = 0f
    private var mode = Mode.NONE

    private enum class Mode {
        NONE, DRAG, ZOOM
    }

    init {
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_photo_view)

        // 设置窗口全屏和背景半透明
        window?.apply {
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            statusBarColor = Color.BLACK
            navigationBarColor = Color.BLACK
        }

        photoView = findViewById(R.id.photo_view)
        container = findViewById(R.id.container)

        // 加载图片
        Glide.with(context)
            .load(imagePath)
            .into(photoView)

        // 设置触摸事件监听
        container.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            handleTouch(event)
            true
        }

        // 点击空白区域关闭对话框
        container.setOnClickListener {
            dismiss()
        }
    }

    private fun handleTouch(event: MotionEvent) {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                mode = Mode.DRAG
                lastTouchX = event.x
                lastTouchY = event.y
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                mode = Mode.ZOOM
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == Mode.DRAG && scaleFactor > 1.0f) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    
                    // 计算新的位置
                    translateX += dx
                    translateY += dy
                    
                    // 限制平移范围
                    val maxTranslateX = photoView.width * (scaleFactor - 1) / 2
                    val maxTranslateY = photoView.height * (scaleFactor - 1) / 2
                    translateX = translateX.coerceIn(-maxTranslateX, maxTranslateX)
                    translateY = translateY.coerceIn(-maxTranslateY, maxTranslateY)
                    
                    // 应用变换
                    photoView.translationX = translateX
                    photoView.translationY = translateY
                    
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = Mode.NONE
                // 如果缩放比例接近1，则重置所有变换
                if (scaleFactor < 1.1f) {
                    resetTransformation()
                }
            }
        }
    }

    private fun resetTransformation() {
        scaleFactor = 1.0f
        translateX = 0f
        translateY = 0f
        photoView.scaleX = 1.0f
        photoView.scaleY = 1.0f
        photoView.translationX = 0f
        photoView.translationY = 0f
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = max(1.0f, min(scaleFactor, 3.0f)) // 限制缩放范围在1-3倍之间
            
            photoView.scaleX = scaleFactor
            photoView.scaleY = scaleFactor
            
            return true
        }
    }
} 