package com.assiance.alm

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.assiance.alm.overlay.OverlayViewTreeOwner
import com.assiance.alm.overlay.TodoFloatingContent
import com.assiance.alm.overlay.createOverlayComposeView

/**
 * 待办悬浮窗 Service。
 *
 * 行为（窗口参数、出现/消失动画、标题与正文的拼装规则、停止广播、START_STICKY）
 * 全部保持原样；唯一的变化是把 XML 布局换成 Compose。
 */
class TodoFloatingService : Service() {
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private val viewTreeOwner = OverlayViewTreeOwner()
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        viewTreeOwner.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (floatingView == null) {
            showFloatingWindow(intent)
        }
        return START_STICKY
    }

    private fun showFloatingWindow(intent: Intent?) {
        val contextThemeWrapper = ContextThemeWrapper(
            this,
            com.google.android.material.R.style.Theme_MaterialComponents_Light
        )

        // 设置标题和内容
        val title = intent?.getStringExtra("todo_title") ?: "待办提醒"
        val description = intent?.getStringExtra("todo_description") ?: ""
        val isAdvance = intent?.getBooleanExtra("is_advance", false) ?: false
        val isDueReminder = intent?.getBooleanExtra("is_due_reminder", true) ?: true

        val titleText = when {
            !isDueReminder && isAdvance -> "即将开始"
            !isDueReminder && !isAdvance -> "事件已开始"
            isDueReminder && isAdvance -> "即将截至"
            else -> "事件已截至"
        }

        val descriptionText = if (title.isNotEmpty()) {
            buildString {
                append(title)
                if (description.isNotEmpty()) {
                    append("\n")
                    append(description)
                }
                append("\n")
                // 添加时间状态说明
                append(
                    when {
                        !isDueReminder && isAdvance -> "还有5分钟开始"
                        !isDueReminder && !isAdvance -> "现在开始"
                        isDueReminder && isAdvance -> "还有30分钟截止"
                        else -> "已经截止"
                    }
                )
            }
        } else {
            description
        }

        val view = createOverlayComposeView(
            context = contextThemeWrapper,
            owner = viewTreeOwner,
        ) {
            TodoFloatingContent(
                title = titleText,
                description = descriptionText,
                onStop = {
                    // 立即发送停止提醒的广播
                    val stopIntent = Intent(AlmContract.TODO_REMINDER_STOP_ACTION).apply {
                        `package` = packageName
                        flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
                    }
                    sendBroadcast(stopIntent)

                    // 原实现是按钮先播 100ms + 100ms 的缩放反馈再开始关闭动画，
                    // 这里保持同样的节奏（按钮反馈在 Compose 里，Service 只等它播完）。
                    handler.postDelayed(
                        { animateClose { stopSelf() } },
                        BUTTON_FEEDBACK_DURATION_MS
                    )
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
                WindowManager.LayoutParams.TYPE_PHONE
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP
        }

        windowManager?.addView(view, params)

        // 视图已上屏，把 owner 推到 RESUMED（Compose 的动效/协程依赖它）
        viewTreeOwner.onStart()
        viewTreeOwner.onResume()

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
        handler.removeCallbacksAndMessages(null)
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
            floatingView = null
        }
        viewTreeOwner.onDestroy()
    }

    private companion object {
        /** 与原实现里按钮「按下 100ms + 弹回 100ms」的反馈时长一致。 */
        const val BUTTON_FEEDBACK_DURATION_MS = 200L
    }
}
