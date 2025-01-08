package com.assiance.alm

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TodoSettingActivity : AppCompatActivity() {
    private lateinit var titleInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var dueDateCheckBox: CheckBox
    private lateinit var dueDateText: TextView
    private var todoId: Int = -1
    private var dueTime: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todo_setting)

        // 设置工具栏
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "添加待办"

        // 初始化视图
        titleInput = findViewById(R.id.todoTitleInput)
        descriptionInput = findViewById(R.id.todoDescriptionInput)
        dueDateCheckBox = findViewById(R.id.dueDateCheckBox)
        dueDateText = findViewById(R.id.dueDateText)

        // 设置截止时间选择
        dueDateCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showDateTimePicker()
            } else {
                dueTime = null
                dueDateText.text = "未设置"
            }
        }

        // 获取传入的待办信息
        todoId = intent.getIntExtra("todo_id", -1)
        if (todoId != -1) {
            supportActionBar?.title = "编辑待办"
            titleInput.setText(intent.getStringExtra("todo_title"))
            descriptionInput.setText(intent.getStringExtra("todo_description"))
            intent.getLongExtra("todo_due_time", -1L).takeIf { it != -1L }?.let {
                dueTime = it
                dueDateCheckBox.isChecked = true
                updateDueDateText(it)
            }
        }

        // 保存按钮
        findViewById<Button>(R.id.saveTodoButton).setOnClickListener {
            saveTodo()
        }
    }

    private fun showDateTimePicker() {
        val currentDateTime = Calendar.getInstance()
        dueTime?.let {
            currentDateTime.timeInMillis = it
        }

        DatePickerDialog(this, { _, year, month, day ->
            TimePickerDialog(this, { _, hour, minute ->
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }
                dueTime = calendar.timeInMillis
                updateDueDateText(calendar.timeInMillis)
            }, currentDateTime.get(Calendar.HOUR_OF_DAY), currentDateTime.get(Calendar.MINUTE), true).show()
        }, currentDateTime.get(Calendar.YEAR), currentDateTime.get(Calendar.MONTH), 
           currentDateTime.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun updateDueDateText(timeInMillis: Long) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        dueDateText.text = dateFormat.format(timeInMillis)
    }

    private fun saveTodo() {
        val title = titleInput.text?.toString()?.trim() ?: ""
        if (title.isEmpty()) {
            titleInput.error = "请输入标题"
            return
        }

        val todo = TodoData(
            id = if (todoId != -1) todoId else System.currentTimeMillis().toInt(),
            title = title,
            description = descriptionInput.text?.toString()?.trim() ?: "",
            dueTime = if (dueDateCheckBox.isChecked) dueTime else null
        )

        // 保存到 SharedPreferences
        val prefs = getSharedPreferences(MainActivityAlm.TODO_PREFS, Context.MODE_PRIVATE)
        val todosJson = prefs.getString(MainActivityAlm.TODO_LIST_KEY, "[]")
        val jsonArray = org.json.JSONArray(todosJson)

        // 如果是编辑，先删除旧的
        if (todoId != -1) {
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.getInt("id") == todoId) {
                    jsonArray.remove(i)
                    break
                }
            }
        }

        // 添加新的待办数据
        jsonArray.put(org.json.JSONObject().apply {
            put("id", todo.id)
            put("title", todo.title)
            put("description", todo.description)
            put("dueTime", todo.dueTime ?: 0L)
            put("isCompleted", false)
        })

        prefs.edit()
            .putString(MainActivityAlm.TODO_LIST_KEY, jsonArray.toString())
            .apply()

        Toast.makeText(this, if (todoId != -1) "待办已更新" else "待办已添加", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 