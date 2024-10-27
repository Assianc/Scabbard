package com.example.memo

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MemoDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memo_detail)

        val titleTextView: TextView = findViewById(R.id.memo_detail_title)
        val contentTextView: TextView = findViewById(R.id.memo_detail_content)
        val updateTimeTextView: TextView = findViewById(R.id.memo_detail_update_time)

        // 获取传递过来的数据并显示
        val title = intent.getStringExtra("memo_title")
        val content = intent.getStringExtra("memo_content")
        val updateTime = intent.getStringExtra("memo_update_time")

        titleTextView.text = title
        contentTextView.text = content
        updateTimeTextView.text = updateTime
    }
}
