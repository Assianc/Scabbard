package com.assiance.alm

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import android.widget.Button
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.Animator
import android.animation.AnimatorListenerAdapter

class TodoFloatingService : Service() {
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (floatingView == null) {
            showFloatingWindow(intent)
        }
        return START_STICKY
    }

    private fun showFloatingWindow(intent: Intent?) {
        val contextThemeWrapper = android.view.ContextThemeWrapper(
            this, 
            com.google.android.material.R.style.Theme_MaterialComponents_Light
        )
        val inflater = LayoutInflater.from(contextThemeWrapper)
        floatingView = inflater.inflate(R.layout.layout_todo_floating, null)

        // 设置标题和内容
        val titleText = floatingView?.findViewById<TextView>(R.id.todoTitleText)
        val descriptionText = floatingView?.findViewById<TextView>(R.id.todoDescriptionText)
        
        val title = intent?.getStringExtra("todo_title") ?: "待办提醒"
        val description = intent?.getStringExtra("todo_description") ?: ""
        val isAdvance = intent?.getBooleanExtra("is_advance", false) ?: false
        val isDueReminder = intent?.getBooleanExtra("is_due_reminder", true) ?: true

        titleText?.text = when {
            !isDueReminder -> "开始时间到了"
            isAdvance -> "即将开始"
            else -> "到期提醒"
        }
        
        descriptionText?.text = if (title.isNotEmpty()) {
            "$title\n$description"
        } else {
            description
        }

        // 设置关闭按钮点击事件
        floatingView?.findViewById<Button>(R.id.stopTodoButton)?.setOnClickListener {
            // 禁用按钮，防止重复点击
            it.isEnabled = false
            
            // 立即发送停止提醒的广播
            val stopIntent = Intent(MainActivityAlm.TODO_REMINDER_STOP_ACTION).apply {
                `package` = packageName
                flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
            }
            sendBroadcast(stopIntent)
            
            // 添加按钮点击反馈
            it.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .withEndAction {
                            // 开始关闭动画
                            animateClose {
                                stopSelf()
                            }
                        }
                        .start()
                }
                .start()
        }

        // 设置窗口参数
        val params = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP
        }

        windowManager?.addView(floatingView, params)
        animateShow()
    }

    private fun animateShow() {
        floatingView?.let { view ->
            val translateY = ObjectAnimator.ofFloat(view, "translationY", -200f, 0f)
            val alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
            
            AnimatorSet().apply {
                playTogether(translateY, alpha)
                duration = 500
                interpolator = OvershootInterpolator(1.2f)
                start()
            }
        }
    }

    private fun animateClose(onEnd: () -> Unit) {
        floatingView?.let { view ->
            val translateY = ObjectAnimator.ofFloat(view, "translationY", 0f, -200f)
            val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
            
            AnimatorSet().apply {
                playTogether(translateY, alpha)
                duration = 200  // 减少动画时间
                interpolator = DecelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        onEnd()
                    }
                })
                start()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
            floatingView = null
        }
    }
} 