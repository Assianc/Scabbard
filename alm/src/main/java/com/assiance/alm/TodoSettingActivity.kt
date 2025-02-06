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
import android.view.Menu
import android.view.MenuItem
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
import android.widget.NumberPicker
import android.widget.Spinner
import android.app.AlertDialog
import android.widget.ArrayAdapter
import android.media.RingtoneManager
import android.net.Uri
import android.widget.ImageButton

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
    private var advanceMinutes: Int = 0  // 提前提醒的分钟数，0表示仅到期提醒
    private var startAdvanceMinutes: Int = 0  // 开始时间的提前提醒分钟数
    private var dueAdvanceMinutes: Int = 0    // 截止时间的提前提醒分钟数
    private lateinit var startReminderButton: Button
    private lateinit var dueReminderButton: Button
    private var startRingtoneUri: String? = null
    private var dueRingtoneUri: String? = null
    private lateinit var startRingtoneButton: ImageButton
    private lateinit var dueRingtoneButton: ImageButton
    private lateinit var startRingtoneName: TextView
    private lateinit var dueRingtoneName: TextView
    private val RINGTONE_PICKER_START_REQUEST = 1001
    private val RINGTONE_PICKER_DUE_REQUEST = 1002

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
        
        // 初始化提醒按钮 - 移到这里
        startReminderButton = findViewById(R.id.startReminderButton)
        dueReminderButton = findViewById(R.id.dueReminderButton)

        // 设置按钮点击事件
        startReminderButton.setOnClickListener {
            showReminderDialog(true)
        }

        dueReminderButton.setOnClickListener {
            showReminderDialog(false)
        }

        // 设置开始时间选择
        startDateCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && startTime == null) {
                showDateTimePicker(true)
            } else if (!isChecked) {
                startTime = null
                startDateText.text = "未设置"
                startReminderButton.isEnabled = false
                startAdvanceMinutes = 0
                updateReminderButtonText()
            }
        }

        // 设置截止时间选择
        dueDateCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && dueTime == null) {
                showDateTimePicker(false)
            } else if (!isChecked) {
                dueTime = null
                dueDateText.text = "未设置"
                dueReminderButton.isEnabled = false
                dueAdvanceMinutes = 0
                updateReminderButtonText()
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
                startReminderButton.isEnabled = true
            }
            
            intent.getLongExtra("todo_due_time", -1L).takeIf { it != -1L }?.let {
                dueTime = it
                dueDateCheckBox.isChecked = true
                updateDateText(false, it)
                dueReminderButton.isEnabled = true
            }
        }

        // 获取传入的提醒设置
        advanceMinutes = intent.getIntExtra("todo_advance_minutes", 0)

        // 更新按钮状态
        updateReminderButtonText()

        // 保存按钮
        findViewById<Button>(R.id.saveTodoButton).setOnClickListener {
            saveTodo()
        }

        // 添加时间文本的点击事件
        startDateText.setOnClickListener {
            if (startDateCheckBox.isChecked) {
                showDateTimePicker(true)
            }
        }

        dueDateText.setOnClickListener {
            if (dueDateCheckBox.isChecked) {
                showDateTimePicker(false)
            }
        }

        // 初始化铃声相关视图
        startRingtoneButton = findViewById(R.id.startRingtoneButton)
        dueRingtoneButton = findViewById(R.id.dueRingtoneButton)
        startRingtoneName = findViewById(R.id.startRingtoneName)
        dueRingtoneName = findViewById(R.id.dueRingtoneName)

        // 设置铃声按钮点击事件
        startRingtoneButton.setOnClickListener {
            // 添加点击动画效果
            it.animate()
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                    openRingtonePicker(RINGTONE_PICKER_START_REQUEST)
                }
                .start()
        }

        dueRingtoneButton.setOnClickListener {
            // 添加点击动画效果
            it.animate()
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                    openRingtonePicker(RINGTONE_PICKER_DUE_REQUEST)
                }
                .start()
        }

        // 获取传入的铃声设置
        startRingtoneUri = intent.getStringExtra("todo_start_ringtone_uri")
        dueRingtoneUri = intent.getStringExtra("todo_due_ringtone_uri")
        
        // 更新铃声按钮文本
        updateRingtoneButtonText()
    }

    private fun showDateTimePicker(isStartTime: Boolean) {
        val currentDateTime = Calendar.getInstance()
        val currentTime = if (isStartTime) startTime else dueTime
        currentTime?.let {
            currentDateTime.timeInMillis = it
        }

        // 创建日期选择对话框
        val dateDialog = DatePickerDialog(this, { _, year, month, day ->
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
                    // 启用开始时间提醒按钮
                    startReminderButton.isEnabled = true
                } else {
                    if (startTime != null && selectedTime <= startTime!!) {
                        Toast.makeText(this, "截止时间必须晚于开始时间", Toast.LENGTH_SHORT).show()
                        dueDateCheckBox.isChecked = false
                        return@TimePickerDialog
                    }
                    dueTime = selectedTime
                    // 启用截止时间提醒按钮
                    dueReminderButton.isEnabled = true
                }
                
                updateDateText(isStartTime, selectedTime)
            }, currentDateTime.get(Calendar.HOUR_OF_DAY), currentDateTime.get(Calendar.MINUTE), true).apply {
                // 为时间选择对话框添加取消按钮的监听
                setOnCancelListener {
                    if (isStartTime) {
                        if (startTime == null) {
                            startDateCheckBox.isChecked = false
                        }
                    } else {
                        if (dueTime == null) {
                            dueDateCheckBox.isChecked = false
                        }
                    }
                }
            }.show()
        }, currentDateTime.get(Calendar.YEAR), currentDateTime.get(Calendar.MONTH), 
           currentDateTime.get(Calendar.DAY_OF_MONTH))

        // 为日期选择对话框添加取消按钮的监听
        dateDialog.setOnCancelListener {
            if (isStartTime) {
                if (startTime == null) {
                    startDateCheckBox.isChecked = false
                }
            } else {
                if (dueTime == null) {
                    dueDateCheckBox.isChecked = false
                }
            }
        }

        dateDialog.show()
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
        val description = descriptionInput.text?.toString()?.trim() ?: ""
        
        if (description.isEmpty()) {
            descriptionInput.error = "请输入待办内容"
            return
        }

        // 取消旧的提醒（如果存在）
        if (todoId != -1) {
            cancelTodoReminder(todoId)
        }

        // 获取原有待办的完成状态和时间
        var originalIsCompleted = false
        var originalStartTime: Long? = null
        var originalDueTime: Long? = null
        var originalCompletedTime: Long? = null
        var originalCreatedTime: Long = System.currentTimeMillis()  // 默认为当前时间
        
        // 生成新的ID（如果是新建待办）
        val newTodoId = if (todoId != -1) todoId else System.currentTimeMillis().toInt()

        if (todoId != -1) {
            val prefs = getSharedPreferences(MainActivityAlm.TODO_PREFS, Context.MODE_PRIVATE)
            val todosJson = prefs.getString(MainActivityAlm.TODO_LIST_KEY, "[]")
            val jsonArray = org.json.JSONArray(todosJson)
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.getInt("id") == todoId) {
                    originalIsCompleted = obj.getBoolean("isCompleted")
                    originalStartTime = obj.getLong("startTime").takeIf { it != 0L }
                    originalDueTime = obj.getLong("dueTime").takeIf { it != 0L }
                    originalCompletedTime = obj.optLong("completedTime").takeIf { it > 0L }
                    originalCreatedTime = obj.optLong("createdTime", obj.getInt("id").toLong())
                    break
                }
            }
        }

        // 创建新的待办对象
        val todo = TodoData(
            id = newTodoId,
            title = title,
            description = description,
            startTime = startTime,
            dueTime = dueTime,
            isCompleted = originalIsCompleted,
            startRingtoneUri = startRingtoneUri,
            dueRingtoneUri = dueRingtoneUri,
            completedTime = originalCompletedTime,
            createdTime = if (todoId == -1) System.currentTimeMillis() else originalCreatedTime
        )

        // 保存到 SharedPreferences
        val prefs = getSharedPreferences(MainActivityAlm.TODO_PREFS, Context.MODE_PRIVATE)
        val todosJson = prefs.getString(MainActivityAlm.TODO_LIST_KEY, "[]")
        val jsonArray = org.json.JSONArray(todosJson)
        
        // 如果是编辑现有待办，先删除旧的
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
            put("isCompleted", todo.isCompleted)
            put("startRingtoneUri", todo.startRingtoneUri)
            put("dueRingtoneUri", todo.dueRingtoneUri)
            put("completedTime", todo.completedTime ?: 0L)
            put("createdTime", todo.createdTime)
        })
        
        prefs.edit()
            .putString(MainActivityAlm.TODO_LIST_KEY, jsonArray.toString())
            .apply()

        // 设置提醒
        if (!todo.isCompleted) {
            setTodoReminder(todo)
        }

        // 显示提示信息
        val message = if (todoId == -1) "待办已添加" else "待办已更新"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun setTodoReminder(todo: TodoData) {
        // 如果待办已完成，不设置任何提醒
        if (todo.isCompleted) {
            return
        }

        Log.d("TodoReminder", "开始设置提醒：${todo.title}")
        
        // 处理开始时间提醒
        todo.startTime?.let { startTime ->
            if (startTime > System.currentTimeMillis() && startAdvanceMinutes != -1) {
                // 设置开始时间的提前提醒
                if (startAdvanceMinutes > 0) {
                    val advanceTime = startTime - (startAdvanceMinutes * 60 * 1000)
                    if (advanceTime > System.currentTimeMillis()) {
                        setReminder(todo, advanceTime, true, false)
                    }
                }
                // 设置开始时间提醒
                setReminder(todo, startTime, false, false)
            }
        }

        // 处理截止时间提醒
        todo.dueTime?.let { dueTime ->
            if (dueTime > System.currentTimeMillis() && dueAdvanceMinutes != -1) {
                // 设置截止时间的提前提醒
                if (dueAdvanceMinutes > 0) {
                    val advanceTime = dueTime - (dueAdvanceMinutes * 60 * 1000)
                    if (advanceTime > System.currentTimeMillis()) {
                        setReminder(todo, advanceTime, true, true)
                    }
                }
                // 设置截止时间提醒
                setReminder(todo, dueTime, false, true)
            }
        }
    }

    private fun setReminder(todo: TodoData, reminderTime: Long, isAdvance: Boolean, isDueReminder: Boolean) {
        val intent = Intent(MainActivityAlm.TODO_REMINDER_ACTION).apply {
            `package` = packageName
            putExtra("todo_title", todo.title)
            putExtra("todo_description", todo.description)
            putExtra("is_advance", isAdvance)
            putExtra("is_due_reminder", isDueReminder)
            putExtra("ringtone_uri", if (isDueReminder) todo.dueRingtoneUri else todo.startRingtoneUri)
        }
        
        // 修改请求码的生成方式，确保每个提醒都有唯一的请求码
        val requestCode = when {
            !isDueReminder -> {
                if (isAdvance) todo.id + 2000000 else todo.id + 3000000  // 区分开始时间的提前提醒和准时提醒
            }
            isAdvance -> todo.id + 1000000  // 截止时间的提前提醒
            else -> todo.id                  // 截止时间的准时提醒
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAlarmClock(  // 使用 setAlarmClock 替代 setExactAndAllowWhileIdle
                    AlarmManager.AlarmClockInfo(reminderTime, pendingIntent),
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            }
            
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val reminderTimeStr = sdf.format(reminderTime)
            val reminderType = when {
                !isDueReminder -> if (isAdvance) "开始时间提前" else "开始时间"
                isAdvance -> "截止时间提前"
                else -> "截止时间"
            }
            Log.d("TodoReminder", "成功设置${reminderType}提醒：${todo.title} - $reminderTimeStr")
            Toast.makeText(this, "已设置${reminderType}提醒", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("TodoReminder", "设置提醒失败", e)
            Toast.makeText(this, "设置提醒失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelTodoReminder(todoId: Int) {
        // 取消所有类型的提醒
        cancelSingleReminder(todoId)           // 取消截止时间提醒
        cancelSingleReminder(todoId + 1000000) // 取消截止时间提前提醒
        cancelSingleReminder(todoId + 2000000) // 取消开始时间提前提醒
        cancelSingleReminder(todoId + 3000000) // 取消开始时间准时提醒
    }

    private fun cancelSingleReminder(requestCode: Int) {
        val intent = Intent(MainActivityAlm.TODO_REMINDER_ACTION).apply {
            `package` = packageName
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun showReminderDialog(isStartTime: Boolean) {
        val items = arrayOf(
            "不提醒",
            "立即提醒",
            "提前15分钟",
            "提前30分钟",
            "提前1小时",
            "提前2小时",
            "自定义..."
        )

        val currentMinutes = if (isStartTime) startAdvanceMinutes else dueAdvanceMinutes
        val currentSelection = when (currentMinutes) {
            -1 -> 0
            0 -> 1
            15 -> 2
            30 -> 3
            60 -> 4
            120 -> 5
            else -> if (currentMinutes > 0) 6 else 0
        }

        AlertDialog.Builder(this)
            .setTitle(if (isStartTime) "设置开始时间提醒" else "设置截止时间提醒")
            .setSingleChoiceItems(items, currentSelection) { dialog, which ->
                when (which) {
                    0 -> {
                        if (isStartTime) startAdvanceMinutes = -1 else dueAdvanceMinutes = -1
                        updateReminderButtonText()
                        dialog.dismiss()
                    }
                    1 -> {
                        if (isStartTime) startAdvanceMinutes = 0 else dueAdvanceMinutes = 0
                        updateReminderButtonText()
                        dialog.dismiss()
                    }
                    2 -> {
                        if (isStartTime) startAdvanceMinutes = 15 else dueAdvanceMinutes = 15
                        updateReminderButtonText()
                        dialog.dismiss()
                    }
                    3 -> {
                        if (isStartTime) startAdvanceMinutes = 30 else dueAdvanceMinutes = 30
                        updateReminderButtonText()
                        dialog.dismiss()
                    }
                    4 -> {
                        if (isStartTime) startAdvanceMinutes = 60 else dueAdvanceMinutes = 60
                        updateReminderButtonText()
                        dialog.dismiss()
                    }
                    5 -> {
                        if (isStartTime) startAdvanceMinutes = 120 else dueAdvanceMinutes = 120
                        updateReminderButtonText()
                        dialog.dismiss()
                    }
                    6 -> {
                        dialog.dismiss()
                        showCustomReminderDialog(isStartTime)
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showCustomReminderDialog(isStartTime: Boolean) {
        val view = layoutInflater.inflate(R.layout.dialog_custom_reminder, null)
        val numberPicker = view.findViewById<NumberPicker>(R.id.minutesPicker)
        val unitSpinner = view.findViewById<Spinner>(R.id.unitSpinner)
        
        val currentMinutes = if (isStartTime) startAdvanceMinutes else dueAdvanceMinutes
        numberPicker.minValue = 1
        numberPicker.maxValue = 999
        numberPicker.value = when {
            currentMinutes >= 1440 -> currentMinutes / 1440
            currentMinutes >= 60 -> currentMinutes / 60
            else -> currentMinutes.coerceAtLeast(1)
        }

        val units = arrayOf("分钟", "小时", "天")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, units)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        unitSpinner.adapter = adapter
        
        unitSpinner.setSelection(when {
            currentMinutes >= 1440 -> 2
            currentMinutes >= 60 -> 1
            else -> 0
        })

        AlertDialog.Builder(this)
            .setTitle(if (isStartTime) "自定义开始时间提醒" else "自定义截止时间提醒")
            .setView(view)
            .setPositiveButton("确定") { _, _ ->
                val number = numberPicker.value
                val multiplier = when (unitSpinner.selectedItemPosition) {
                    0 -> 1           // 分钟
                    1 -> 60          // 小时
                    2 -> 60 * 24     // 天
                    else -> 1
                }
                val minutes = number * multiplier
                
                if (isStartTime) {
                    startAdvanceMinutes = minutes
                } else {
                    dueAdvanceMinutes = minutes
                }
                
                updateReminderButtonText()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateReminderButtonText() {
        startReminderButton.text = when (startAdvanceMinutes) {
            -1 -> "不提醒"
            0 -> "立即提醒"
            15 -> "提前15分钟提醒"
            30 -> "提前30分钟提醒"
            60 -> "提前1小时提醒"
            120 -> "提前2小时提醒"
            else -> when {
                startAdvanceMinutes >= 1440 -> "提前${startAdvanceMinutes / 1440}天提醒"
                startAdvanceMinutes >= 60 -> "提前${startAdvanceMinutes / 60}小时提醒"
                else -> "提前${startAdvanceMinutes}分钟提醒"
            }
        }

        dueReminderButton.text = when (dueAdvanceMinutes) {
            -1 -> "不提醒"
            0 -> "立即提醒"
            15 -> "提前15分钟提醒"
            30 -> "提前30分钟提醒"
            60 -> "提前1小时提醒"
            120 -> "提前2小时提醒"
            else -> when {
                dueAdvanceMinutes >= 1440 -> "提前${dueAdvanceMinutes / 1440}天提醒"
                dueAdvanceMinutes >= 60 -> "提前${dueAdvanceMinutes / 60}小时提醒"
                else -> "提前${dueAdvanceMinutes}分钟提醒"
            }
        }

        // 根据复选框状态更新按钮可用性
        startReminderButton.isEnabled = startDateCheckBox.isChecked
        dueReminderButton.isEnabled = dueDateCheckBox.isChecked
    }

    private fun openRingtonePicker(requestCode: Int) {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "选择提醒铃声")
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, 
                if (requestCode == RINGTONE_PICKER_START_REQUEST) 
                    startRingtoneUri?.let { Uri.parse(it) }
                else 
                    dueRingtoneUri?.let { Uri.parse(it) }
            )
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        }
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val uri = data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            when (requestCode) {
                RINGTONE_PICKER_START_REQUEST -> {
                    startRingtoneUri = uri?.toString()
                    updateRingtoneButtonText()
                }
                RINGTONE_PICKER_DUE_REQUEST -> {
                    dueRingtoneUri = uri?.toString()
                    updateRingtoneButtonText()
                }
            }
        }
    }

    private fun updateRingtoneButtonText() {
        fun getRingtoneName(uri: String?): String {
            if (uri == null) return "默认铃声"
            try {
                val ringtone = RingtoneManager.getRingtone(this, Uri.parse(uri))
                return ringtone.getTitle(this)
            } catch (e: Exception) {
                return "默认铃声"
            }
        }
        startRingtoneName.text = getRingtoneName(startRingtoneUri)
        dueRingtoneName.text = getRingtoneName(dueRingtoneUri)
    }
} 