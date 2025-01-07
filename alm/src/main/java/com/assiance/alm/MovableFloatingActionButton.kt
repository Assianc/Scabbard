package com.assiance.alm

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MovableFloatingActionButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FloatingActionButton(context, attrs, defStyleAttr), View.OnTouchListener {

    private var downRawX: Float = 0f
    private var downRawY: Float = 0f
    private var dX: Float = 0f
    private var dY: Float = 0f
    private var isDragging: Boolean = false
    private val touchSlop = 20f // 触摸滑动的最小距离

    init {
        setOnTouchListener(this)
    }

    override fun onTouch(view: View?, event: MotionEvent): Boolean {
        val action = event.action
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downRawY = event.rawY
                dX = view!!.x - downRawX
                dY = view.y - downRawY
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val moveX = event.rawX - downRawX
                val moveY = event.rawY - downRawY
                
                // 判断是否开始拖动
                if (!isDragging && (Math.abs(moveX) > touchSlop || Math.abs(moveY) > touchSlop)) {
                    isDragging = true
                }

                if (isDragging) {
                    val viewParent = view!!.parent as ViewGroup
                    val newX = event.rawX + dX
                    val newY = event.rawY + dY

                    // 确保按钮不会移出屏幕
                    if (newX > 0 && newX < viewParent.width - view.width &&
                        newY > 0 && newY < viewParent.height - view.height) {
                        view.animate()
                            .x(newX)
                            .y(newY)
                            .setDuration(0)
                            .start()
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val upRawX = event.rawX
                val upRawY = event.rawY

                if (isDragging) {
                    // 自动停靠到最近的边缘
                    val viewParent = view!!.parent as ViewGroup
                    val centerX = view.x + view.width / 2
                    
                    // 计算目标X坐标（左边或右边）
                    val targetX = if (centerX < viewParent.width / 2) {
                        0f // 靠左
                    } else {
                        viewParent.width - view.width.toFloat() // 靠右
                    }

                    // 带动画效果的停靠
                    view.animate()
                        .x(targetX)
                        .setDuration(200)
                        .setInterpolator(DecelerateInterpolator())
                        .start()

                    isDragging = false
                } else {
                    // 如果没有拖动，则视为点击
                    val isClick = Math.abs(upRawX - downRawX) < touchSlop && 
                                Math.abs(upRawY - downRawY) < touchSlop
                    if (isClick) {
                        performClick()
                    }
                }
                return true
            }
        }
        return false
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
} 