package com.example.scabbard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.memo.MainActivityMemo

open class StartActivity : AppCompatActivity() {

    private lateinit var startButton: Button
    private lateinit var memoButton: Button // 云端按钮

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_allocator)

        startButton = findViewById(R.id.start_button)
        memoButton = findViewById(R.id.memo_button) // 初始化新增的按钮

        // 设置按钮点击事件，点击跳转到团队分配界面
        startButton.setOnClickListener {
            val intent = Intent(this@StartActivity, MainActivity::class.java)
            startActivity(intent)
//            finish() // 结束当前 Activity
        }

        // 设置备忘录按钮点击事件，跳转到备忘录界面
        memoButton.setOnClickListener {
            val intent = Intent(this@StartActivity, MainActivityMemo::class.java)
            startActivity(intent)
//            finish() // 结束当前 Activity
        }
    }
}
