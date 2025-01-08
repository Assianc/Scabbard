package com.assiance.alm

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
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
    private lateinit var startDateCheckBox: CheckBox
    private lateinit var startDateText: TextView
    private lateinit var dueDateCheckBox: CheckBox
    private lateinit var dueDateText: TextView
    private var todoId: Int = -1
    private var startTime: Long? = null
    private var dueTime: Long? = null
    private lateinit var alarmManager: AlarmManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todo_setting)

        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 设置工具栏
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "添加待办"

        // 初始化视图
        titleInput = findViewById(R.id.todoTitleInput)
        descriptionInput = findViewById(R.id.todoDescriptionInput)
        startDateCheckBox = findViewById(R.id.startDateCheckBox)
        startDateText = findViewById(R.id.startDateText)
        dueDateCheckBox = findViewById(R.id.dueDateCheckBox)
        dueDateText = findViewById(R.id.dueDateText)

        // 设置开始时间选择
        startDateCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showDateTimePicker(true)
            } else {
                startTime = null
                startDateText.text = "未设置"
            }
        }

        // 设置截止时间选择
        dueDateCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showDateTimePicker(false)
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
            
            intent.getLongExtra("todo_start_time", -1L).takeIf { it != -1L }?.let {
                startTime = it
                startDateCheckBox.isChecked = true
                updateDateText(true, it)
            }
            
            intent.getLongExtra("todo_due_time", -1L).takeIf { it != -1L }?.let {
                dueTime = it
                dueDateCheckBox.isChecked = true
                updateDateText(false, it)
            }
        }

        // 保存按钮
        findViewById<Button>(R.id.saveTodoButton).setOnClickListener {
            saveTodo()
        }
    }

    private fun showDateTimePicker(isStartTime: Boolean) {
        val currentDateTime = Calendar.getInstance()
        val currentTime = if (isStartTime) startTime else dueTime
        currentTime?.let {
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
                
                val selectedTime = calendar.timeInMillis
                
                // 检查时间是否合理
                if (isStartTime) {
                    if (dueTime != null && selectedTime >= dueTime!!) {
                        Toast.makeText(this, "开始时间必须早于截止时间", Toast.LENGTH_SHORT).show()
                        startDateCheckBox.isChecked = false
                        return@TimePickerDialog
                    }
                    startTime = selectedTime
                } else {
                    if (startTime != null && selectedTime <= startTime!!) {
                        Toast.makeText(this, "截止时间必须晚于开始时间", Toast.LENGTH_SHORT).show()
                        dueDateCheckBox.isChecked = false
                        return@TimePickerDialog
                    }
                    dueTime = selectedTime
                }
                
                updateDateText(isStartTime, selectedTime)
            }, currentDateTime.get(Calendar.HOUR_OF_DAY), currentDateTime.get(Calendar.MINUTE), true).show()
        }, currentDateTime.get(Calendar.YEAR), currentDateTime.get(Calendar.MONTH), 
           currentDateTime.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun updateDateText(isStartTime: Boolean, timeInMillis: Long) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        if (isStartTime) {
            startDateText.text = dateFormat.format(timeInMillis)
        } else {
            dueDateText.text = dateFormat.format(timeInMillis)
        }
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
            startTime = if (startDateCheckBox.isChecked) startTime else null,
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
            put("startTime", todo.startTime ?: 0L)
            put("dueTime", todo.dueTime ?: 0L)
            put("isCompleted", false)
        })

        prefs.edit()
            .putString(MainActivityAlm.TODO_LIST_KEY, jsonArray.toString())
            .apply()

        // 设置提醒
        if (dueDateCheckBox.isChecked && dueTime != null) {
            setTodoReminder(todo)
        } else {
            cancelTodoReminder(todo.id)
        }

        Toast.makeText(this, if (todoId != -1) "待办已更新" else "待办已添加", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun setTodoReminder(todo: TodoData) {
        val intent = Intent(MainActivityAlm.TODO_REMINDER_ACTION).apply {
            `package` = packageName
            putExtra("todo_title", todo.title)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            todo.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        todo.dueTime?.let { dueTime ->
            // 设置在截止时间前15分钟提醒
            val reminderTime = dueTime - (15 * 60 * 1000)
            if (reminderTime > System.currentTimeMillis()) {
                try {
                    // 使用 setExactAndAllowWhileIdle 确保准确提醒
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminderTime,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            reminderTime,
                            pendingIntent
                        )
                    }
                    
                    // 打印日志以便调试
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val reminderTimeStr = sdf.format(reminderTime)
                    Log.d("TodoReminder", "设置提醒：${todo.title} - $reminderTimeStr")
                } catch (e: Exception) {
                    Log.e("TodoReminder", "设置提醒失败", e)
                    Toast.makeText(this, "设置提醒失败：${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.w("TodoReminder", "提醒时间已过：${todo.title}")
                Toast.makeText(this, "提醒时间已过", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cancelTodoReminder(todoId: Int) {
        val intent = Intent(MainActivityAlm.TODO_REMINDER_ACTION).apply {
            `package` = packageName
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            todoId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 