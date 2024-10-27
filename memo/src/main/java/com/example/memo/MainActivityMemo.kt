package com.example.memo

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

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
        private const val ANIMATION_DURATION = 300L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_memo)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        memoDAO = MemoDAO(this)
        memoList = memoDAO.getAllMemos().toMutableList()

        // 设置适配器
        memoAdapter = MemoAdapter(memoList, this, memoDAO) // 传递 memoDAO 实例
        recyclerView.adapter = memoAdapter

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        deleteButton = findViewById(R.id.delete_button)
        deleteButton.visibility = View.GONE // 初始状态隐藏删除按钮

        fab.setOnClickListener {
            // 跳转到添加备忘录界面
            val intent = Intent(this@MainActivityMemo, AddMemoActivity::class.java)
            startActivity(intent)
        }

        // 设置点击事件，删除选中的备忘录
        deleteButton.setOnClickListener {
            memoAdapter.deleteSelectedMemos()
            toggleDeleteButton(false) // 删除完成后隐藏按钮
        }

        // 设置 FloatingActionButton 可拖动
        fab.setOnTouchListener(object : View.OnTouchListener {
            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(view: View, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        dX = view.x - event.rawX
                        dY = view.y - event.rawY
                        lastAction = MotionEvent.ACTION_DOWN
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        view.x = event.rawX + dX
                        view.y = event.rawY + dY
                        lastAction = MotionEvent.ACTION_MOVE
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (lastAction == MotionEvent.ACTION_MOVE) {
                            // 松手后将按钮移动到最近的屏幕侧边
                            val finalX = if ((view.x + view.width / 2) >= (window.decorView.width / 2)) {
                                window.decorView.width - view.width.toFloat() // 右侧
                            } else {
                                0f // 左侧
                            }

                            view.animate()
                                .x(finalX)
                                .setDuration(ANIMATION_DURATION)
                                .start()
                        } else if (lastAction == MotionEvent.ACTION_DOWN) {
                            view.performClick()
                        }
                        return true
                    }
                    else -> return false
                }
            }
        })

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

    override fun onResume() {
        super.onResume()

        // 重新加载数据库中的备忘录数据，确保删除或添加操作后的数据是最新的
        memoList.clear()
        memoList.addAll(memoDAO.getAllMemos())  // 从数据库重新加载最新的备忘录列表
        memoAdapter.notifyDataSetChanged()      // 通知适配器更新显示
    }

}
