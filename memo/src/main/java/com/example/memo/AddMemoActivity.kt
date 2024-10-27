package com.example.memo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.memo.databinding.ActivityAddMemoBinding // 引入 ViewBinding 生成的绑定类

class AddMemoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddMemoBinding // 定义 ViewBinding 变量
    private lateinit var memoDAO: MemoDAO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化 binding 对象并设置内容视图
        binding = ActivityAddMemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        memoDAO = MemoDAO(this)

        // 使用 binding 来访问视图元素
        binding.buttonSave.setOnClickListener {
            val title = binding.editTextTitle.text.toString()
            val content = binding.editTextContent.text.toString()
            memoDAO.insertMemo(title, content)
            finish() // 保存完成后关闭当前活动
        }
    }
}
