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

class MainActivityMemo : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var memoAdapter: MemoAdapter
    private lateinit var memoDAO: MemoDAO
    private lateinit var memoList: MutableList<Memo>
    private lateinit var deleteButton: FloatingActionButton
    private var dX = 0f
    private var dY = 0f
    private var lastAction = 0

    companion object {
        private const val ANIMATION_DURATION = 150L
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_memo)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        memoDAO = MemoDAO(this)
        
        // 使用协程加载数据
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val memos = memoDAO.getAllMemos()
                withContext(Dispatchers.Main) {
                    memoList = memos.toMutableList()
                    memoAdapter = MemoAdapter(memoList, this@MainActivityMemo, memoDAO)
                    recyclerView.adapter = memoAdapter
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    memoList = mutableListOf()
                    memoAdapter = MemoAdapter(memoList, this@MainActivityMemo, memoDAO)
                    recyclerView.adapter = memoAdapter
                    // 可以在这里显示错误提示
                    Toast.makeText(this@MainActivityMemo, "加载数据失败", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val fab = findViewById<FloatingActionButton>(R.id.fab).apply {
            isClickable = true
            isFocusable = true
            
            // 简化为纯点击事件
            setOnClickListener {
                startActivity(Intent(this@MainActivityMemo, AddMemoActivity::class.java))
            }

            // 禁用长按和触摸事件
            setOnLongClickListener { true }
            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        performClick()
                        true
                    }
                    else -> false
                }
            }
        }

        // 将 FAB 固定在右下角
        (fab.layoutParams as? CoordinatorLayout.LayoutParams)?.apply {
            gravity = Gravity.BOTTOM or Gravity.END
            marginEnd = resources.getDimensionPixelSize(R.dimen.fab_margin)
            bottomMargin = resources.getDimensionPixelSize(R.dimen.fab_margin)
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

    // 控制删除按钮的显示/隐藏
    fun toggleDeleteButton(show: Boolean) {
        deleteButton.visibility = if (show) View.VISIBLE else View.GONE
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()

        // 在后台线程刷新数据
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val newMemos = memoDAO.getAllMemos()
                withContext(Dispatchers.Main) {
                    memoList.clear()
                    memoList.addAll(newMemos)
                    memoAdapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivityMemo, "刷新数据失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}

