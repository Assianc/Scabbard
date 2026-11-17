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
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.assiance.alm.overlay.AlarmFloatingContent
import com.assiance.alm.overlay.OverlayViewTreeOwner
import com.assiance.alm.overlay.createOverlayComposeView
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 闹钟悬浮窗 Service。
 *
 * 行为（窗口参数、出现/消失动画、停止广播、START_STICKY）全部保持原样；
 * 唯一的变化是把 XML 布局换成 Compose（悬浮窗没有宿主 Activity，所以 ComposeView
 * 自带 ViewTree owner 与一个只服务于 overlay 的极小主题）。
 */
class AlarmFloatingService : Service() {
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private val viewTreeOwner = OverlayViewTreeOwner()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        viewTreeOwner.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (floatingView == null) {
            showFloatingWindow()
        }
        return START_STICKY
    }

    private fun showFloatingWindow() {
        val contextThemeWrapper = ContextThemeWrapper(
            this,
            R.style.Theme_Alm
        )

        // 设置当前时间
        val timeText = SimpleDateFormat("HH:mm", Locale.getDefault())
            .format(System.currentTimeMillis())

        val view = createOverlayComposeView(
            context = contextThemeWrapper,
            owner = viewTreeOwner,
        ) {
            AlarmFloatingContent(
                timeText = timeText,
                onStop = {
                    // 添加消失动画
                    animateClose {
                        // 发送停止闹钟的广播
                        val stopIntent = Intent(AlmContract.ALARM_STOP_ACTION).apply {
                            `package` = packageName
                            flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
                        }
                        sendBroadcast(stopIntent)

                        // 关闭悬浮窗
                        stopSelf()
                    }
                },
            )
        }

        // 与 animateShow() 的动画起点保持一致，避免出现前先闪一下
        view.alpha = 0f
        view.translationY = -200f
        floatingView = view

        // 设置窗口参数
        val params = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_APPLICATION
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP
        }

        // 添加悬浮窗到窗口管理器
        windowManager?.addView(view, params)

        // 视图已上屏，把 owner 推到 RESUMED（Compose 的动效/协程依赖它）
        viewTreeOwner.onStart()
        viewTreeOwner.onResume()

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
        viewTreeOwner.onDestroy()
    }
}
