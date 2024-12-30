package com.example.memo

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MemoDetailActivity : AppCompatActivity() {

    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var updateTimeTextView: TextView
    private lateinit var editButton: ImageButton
    private lateinit var saveButton: Button
    private lateinit var memoDAO: MemoDAO
    private var memoId: Int = -1
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memo_detail)

        // 初始化视图
        titleEditText = findViewById(R.id.memo_detail_title)
        contentEditText = findViewById(R.id.memo_detail_content)
        updateTimeTextView = findViewById(R.id.memo_detail_update_time)
        editButton = findViewById(R.id.edit_button)
        saveButton = findViewById(R.id.save_button)
        memoDAO = MemoDAO(this)

        // 获取传递过来的数据
        memoId = intent.getIntExtra("memo_id", -1)
        val title = intent.getStringExtra("memo_title") ?: ""
        val content = intent.getStringExtra("memo_content") ?: ""
        val updateTime = intent.getStringExtra("memo_update_time") ?: ""

        // 设置数据
        titleEditText.setText(title)
        contentEditText.setText(content)
        updateTimeTextView.text = updateTime

        // 设置编辑按钮点击事件
        editButton.setOnClickListener {
            toggleEditMode(true)
        }

        // 设置保存按钮点击事件
        saveButton.setOnClickListener {
            saveMemo()
            toggleEditMode(false)
        }
    }

    private fun toggleEditMode(edit: Boolean) {
        isEditMode = edit
        titleEditText.isEnabled = edit
        contentEditText.isEnabled = edit
        
        // 根据编辑状态设置不同的文字颜色
        val textColor = if (edit) {
            getColor(R.color.edit_mode_text)  // 编辑模式的颜色
        } else {
            getColor(R.color.view_mode_text)  // 查看模式的颜色
        }
        
        titleEditText.setTextColor(textColor)
        contentEditText.setTextColor(textColor)
        
        editButton.visibility = if (edit) View.GONE else View.VISIBLE
        saveButton.visibility = if (edit) View.VISIBLE else View.GONE
    }

    private fun saveMemo() {
        if (memoId != -1) {
            val newTitle = titleEditText.text.toString()
            val newContent = contentEditText.text.toString()
            memoDAO.updateMemo(memoId, newTitle, newContent)
            // 更新显示的更新时间
            updateTimeTextView.text = "刚刚更新"
        }
    }
}
