package com.assiance.memo

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import androidx.coordinatorlayout.widget.CoordinatorLayout
import android.view.Gravity
import androidx.recyclerview.widget.DiffUtil

class MainActivityMemo : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var memoAdapter: MemoAdapter
    private lateinit var memoDAO: MemoDAO
    private lateinit var memoList: MutableList<Memo>
    private lateinit var deleteButton: FloatingActionButton
    private var dX = 0f
    private var dY = 0f
    private var lastAction = 0
    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var touchStartTime = 0L
    private val CLICK_DURATION_THRESHOLD = 50L // 点击时间阈值，单位毫秒
    private val MOVE_THRESHOLD = 10f // 移动距离阈值，单位像素
    private var isFirstLoad = true

    companion object {
        private const val ANIMATION_DURATION = 150L
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_memo)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        memoList = mutableListOf()
        memoDAO = MemoDAO(this)
        memoAdapter = MemoAdapter(memoList, this, memoDAO)
        recyclerView.adapter = memoAdapter

        // 首次加载数据
        loadMemos()

        val fab = findViewById<FloatingActionButton>(R.id.fab).apply {
            isClickable = true
            isFocusable = true
            
            setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        // 记录初始位置
                        dX = view.x - event.rawX
                        dY = view.y - event.rawY
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        // 计算移动距离
                        val moveX = Math.abs(event.rawX - initialTouchX)
                        val moveY = Math.abs(event.rawY - initialTouchY)
                        
                        // 如果移动距离超过阈值，则更新位置
                        if (moveX > MOVE_THRESHOLD || moveY > MOVE_THRESHOLD) {
                            lastAction = MotionEvent.ACTION_MOVE
                            view.x = (event.rawX + dX).coerceIn(
                                0f, 
                                (view.parent as View).width - view.width.toFloat()
                            )
                            view.y = (event.rawY + dY).coerceIn(
                                0f, 
                                (view.parent as View).height - view.height.toFloat()
                            )
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        view.parent?.requestDisallowInterceptTouchEvent(false)
                        
                        // 如果没有明显的移动，则触发点击
                        if (lastAction != MotionEvent.ACTION_MOVE) {
                            view.performClick()
                        } else {
                            // 自动停靠到最近的边缘
                            val parent = view.parent as View
                            val parentWidth = parent.width
                            val centerX = view.x + view.width / 2
                            
                            // 计算目标X坐标（左边或右边）
                            val targetX = if (centerX < parentWidth / 2) {
                                0f // 靠左
                            } else {
                                parentWidth - view.width.toFloat() // 靠右
                            }
                            
                            // 使用动画平滑移动到目标位置
                            view.animate()
                                .x(targetX)
                                .setDuration(200)
                                .start()
                        }
                        
                        lastAction = 0
                        true
                    }
                    else -> false
                }
            }

            setOnClickListener {
                startActivity(Intent(this@MainActivityMemo, AddMemoActivity::class.java))
            }
        }

        deleteButton = findViewById(R.id.delete_button)
        deleteButton.visibility = View.GONE // 初始状态隐藏删除按钮

        // 设置点击事件，删除选中的备忘录
        deleteButton.setOnClickListener {
            memoAdapter.deleteSelectedMemos()
            toggleDeleteButton(false) // 删除完成后隐藏按钮
        }

        // 使用 OnBackPressedDispatcher 代替 onBackPressed
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (memoAdapter.isMultiSelectMode) {
                    memoAdapter.exitMultiSelectMode()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun loadMemos() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val newMemos = memoDAO.getAllMemos()
                withContext(Dispatchers.Main) {
                    updateMemoList(newMemos)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (e !is IllegalStateException && memoList.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivityMemo,
                            "数据加载异常，请重试",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun updateMemoList(newMemos: List<Memo>) {
        if (isFirstLoad) {
            memoList.clear()
            memoList.addAll(newMemos)
            memoAdapter.notifyDataSetChanged()
            isFirstLoad = false
            return
        }

        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = memoList.size
            override fun getNewListSize() = newMemos.size

            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                return memoList[oldPos].id == newMemos[newPos].id
            }

            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                return memoList[oldPos] == newMemos[newPos]
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)
        memoList.clear()
        memoList.addAll(newMemos)
        diffResult.dispatchUpdatesTo(memoAdapter)
    }

    // 控制删除按钮的显示/隐藏
    fun toggleDeleteButton(show: Boolean) {
        deleteButton.visibility = if (show) View.VISIBLE else View.GONE
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        if (!isFirstLoad) {
            loadMemos()
        }
    }

}

