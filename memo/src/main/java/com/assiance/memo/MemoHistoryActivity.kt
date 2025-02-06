package com.assiance.memo

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MemoHistoryActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var memoDAO: MemoDAO
    private var memoId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memo_history)

        memoId = intent.getIntExtra("memo_id", -1)
        if (memoId == -1) {
            Toast.makeText(this, "无法加载历史记录", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        memoDAO = MemoDAO(this)
        recyclerView = findViewById(R.id.history_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val historyList = memoDAO.getMemoHistory(memoId)
        val adapter = MemoHistoryAdapter(historyList) { history ->
            showRestoreConfirmDialog(history)
        }
        recyclerView.adapter = adapter
    }

    private fun showRestoreConfirmDialog(history: MemoHistory) {
        AlertDialog.Builder(this)
            .setTitle("恢复确认")
            .setMessage("确定要恢复到这个版本吗？当前版本将被保存到历史记录中。")
            .setPositiveButton("确定") { _, _ ->
                restoreHistory(history)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun restoreHistory(history: MemoHistory) {
        memoDAO.updateMemo(
            id = memoId,
            title = history.oldTitle,
            content = history.oldContent,
            imagePaths = history.oldImagePaths
        )
        Toast.makeText(this, "已恢复到选中的版本", Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
        finish()
    }
} 