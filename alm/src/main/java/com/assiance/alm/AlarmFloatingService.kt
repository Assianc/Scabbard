package com.assiance.alm

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
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
import java.text.SimpleDateFormat
import java.util.Locale

class AlarmFloatingService : Service() {
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (floatingView == null) {
            showFloatingWindow()
        }
        return START_STICKY
    }

    private fun showFloatingWindow() {
        // 创建悬浮窗布局，使用带主题的 ContextThemeWrapper
        val contextThemeWrapper = android.view.ContextThemeWrapper(
            this, 
            com.google.android.material.R.style.Theme_MaterialComponents_Light
        )
        val inflater = LayoutInflater.from(contextThemeWrapper)
        floatingView = inflater.inflate(R.layout.layout_alarm_floating, null)

        // 设置当前时间
        val timeText = floatingView?.findViewById<TextView>(R.id.alarmTimeText)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        timeText?.text = timeFormat.format(System.currentTimeMillis())

        // 设置停止按钮点击事件
        floatingView?.findViewById<Button>(R.id.stopAlarmButton)?.setOnClickListener {
            // 添加消失动画
            animateClose {
                // 发送停止闹钟的广播
                val stopIntent = Intent(MainActivityAlm.ALARM_STOP_ACTION).apply {
                    `package` = packageName
                    flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
                }
                sendBroadcast(stopIntent)
                
                // 关闭悬浮窗
                stopSelf()
            }
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

        // 添加悬浮窗到窗口管理器
        windowManager?.addView(floatingView, params)

        // 添加出现动画
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
                duration = 300
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