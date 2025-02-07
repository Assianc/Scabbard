package com.assiance.alm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import android.provider.Settings
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Log

class MainActivityAlm : AppCompatActivity() {
    private lateinit var alarmManager: AlarmManager
    private lateinit var dateText: TextView
    private lateinit var timeText: TextView
    private var timeUpdateHandler: Handler = Handler(Looper.getMainLooper())
    private var timeUpdateRunnable: Runnable? = null
    
    // 闹钟列表相关
    private lateinit var alarmListView: RecyclerView
    private lateinit var alarmAdapter: AlarmAdapter
    private val alarmList = mutableListOf<AlarmData>()
    
    // 待办列表相关
    private lateinit var todoListView: RecyclerView
    private lateinit var todoAdapter: TodoAdapter
    private val todoList = mutableListOf<TodoData>()
    
    // 视图切换相关
    private lateinit var viewFlipper: ViewFlipper
    private lateinit var alarmTabButton: MaterialButton
    private lateinit var todoTabButton: MaterialButton
    private lateinit var fabAdd: FloatingActionButton
    private var currentTab = 0 // 0: 闹钟, 1: 待办

    // 添加指示器视图属性
    private lateinit var tabIndicator: View
    
    // 添加指示器动画属性
    private var isFirstLayout = true

    // 添加权限请求启动器
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (!Settings.canDrawOverlays(this)) {
            // 如果用户拒绝了权限，显示提示
            Toast.makeText(this, 
                "需要悬浮窗权限来显示闹钟提醒，请在设置中开启", 
                Toast.LENGTH_LONG
            ).show()
        }
    }

    companion object {
        internal const val ALARM_REQUEST_CODE = 100
        internal const val CHANNEL_ID = "AlarmChannel"
        internal const val NOTIFICATION_ID = 1
        const val ALARM_ACTION = "com.assiance.alm.ALARM_TRIGGER"
        const val ALARM_STATUS_CHANGED_ACTION = "com.assiance.alm.ALARM_STATUS_CHANGED"
        internal const val ALARM_PREFS = "alarm_prefs"
        internal const val ALARM_LIST_KEY = "alarm_list"
        internal const val TODO_PREFS = "todo_prefs"
        internal const val TODO_LIST_KEY = "todo_list"
        const val TODO_REMINDER_ACTION = "com.assiance.alm.TODO_REMINDER"
        const val TODO_CHANNEL_ID = "TodoChannel"
        const val TODO_NOTIFICATION_ID = 2
        const val ALARM_STOP_ACTION = "com.assiance.alm.ALARM_STOP"
        const val TODO_REMINDER_STOP_ACTION = "com.assiance.alm.TODO_REMINDER_STOP"
    }

    private val alarmReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ALARM_ACTION) {
                showNotification()
                updateAlarmStatus("闹钟响了！")
            }
        }
    }

    private val alarmStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ALARM_STATUS_CHANGED_ACTION) {
                restoreAlarmStatus()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_alm)

        // 初始化时间显示相关的视图
        dateText = findViewById(R.id.dateText)
        timeText = findViewById(R.id.timeText)
        
        // 启动时间更新
        startTimeUpdate()

        // 初始化闹钟管理器
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 创建通知渠道
        createNotificationChannel()

        // 修改广播注册方式
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                AlarmReceiver.getInstance(),
                IntentFilter().apply {
                    addAction(ALARM_ACTION)
                    addAction(ALARM_STOP_ACTION)
                    addAction(ALARM_STATUS_CHANGED_ACTION)
                },
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(
                AlarmReceiver.getInstance(),
                IntentFilter().apply {
                    addAction(ALARM_ACTION)
                    addAction(ALARM_STOP_ACTION)
                    addAction(ALARM_STATUS_CHANGED_ACTION)
                }
            )
        }

        // 初始化视图切换器和标签按钮
        viewFlipper = findViewById(R.id.viewFlipper)
        alarmTabButton = findViewById(R.id.alarmTabButton)
        todoTabButton = findViewById(R.id.todoTabButton)
        fabAdd = findViewById(R.id.fabAdd)

        // 初始化指示器
        tabIndicator = findViewById(R.id.tabIndicator)
        
        // 设置指示器初始状态
        tabIndicator.alpha = 0f
        tabIndicator.scaleX = 0.8f
        tabIndicator.scaleY = 0.8f

        // 在视图完成布局后设置指示器
        alarmTabButton.post {
            if (isFirstLayout) {
                // 设置指示器的初始大小和位置
                val layoutParams = tabIndicator.layoutParams
                layoutParams.width = alarmTabButton.width
                layoutParams.height = alarmTabButton.height
                tabIndicator.layoutParams = layoutParams
                
                // 设置初始位置
                tabIndicator.translationX = alarmTabButton.x
                
                // 添加组合动画效果
                tabIndicator.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(500)
                    .setStartDelay(300) // 稍微延迟一下，让用户能看到动画
                    .setInterpolator(DecelerateInterpolator())
                    .withStartAction {
                        // 动画开始时设置按钮状态
                        alarmTabButton.isChecked = true
                        todoTabButton.isChecked = false
                    }
                    .withEndAction {
                        isFirstLayout = false
                    }
                    .start()

                // 同时添加按钮的动画效果
                alarmTabButton.alpha = 0f
                todoTabButton.alpha = 0f
                
                alarmTabButton.animate()
                    .alpha(1f)
                    .setDuration(400)
                    .setStartDelay(200)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
                
                todoTabButton.animate()
                    .alpha(1f)
                    .setDuration(400)
                    .setStartDelay(200)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
        }

        // 初始化闹钟列表
        alarmListView = findViewById(R.id.alarmListView)
        alarmListView.layoutManager = LinearLayoutManager(this)
        alarmAdapter = AlarmAdapter(
            alarmList,
            onDeleteClick = { alarm -> deleteAlarm(alarm) },
            onToggleClick = { alarm, isEnabled -> toggleAlarm(alarm, isEnabled) },
            onAlarmClick = { alarm -> editAlarm(alarm) }
        )
        alarmListView.adapter = alarmAdapter

        // 初始化待办列表
        todoListView = findViewById(R.id.todoListView)
        todoListView.layoutManager = LinearLayoutManager(this)
        todoAdapter = TodoAdapter(
            todoList,
            onDeleteClick = { todo -> deleteTodo(todo) },
            onToggleClick = { todo, isCompleted -> toggleTodo(todo, isCompleted) },
            onTodoClick = { todo -> editTodo(todo) }
        )
        todoListView.adapter = todoAdapter

        // 为闹钟列表启用侧滑
        alarmAdapter.attachToRecyclerView(alarmListView)
        
        // 为待办列表启用侧滑
        todoAdapter.attachToRecyclerView(todoListView)

        // 设置标签按钮点击事件
        alarmTabButton.setOnClickListener {
            if (currentTab != 0) {
                viewFlipper.setInAnimation(this, R.anim.slide_in_left)
                viewFlipper.setOutAnimation(this, R.anim.slide_out_right)
                viewFlipper.displayedChild = 0
                updateFabIcon(true)
                
                // 添加指示器动画
                animateIndicator(todoTabButton, alarmTabButton)
                
                // 添加按钮动画
                animateTabButton(alarmTabButton, true)
                animateTabButton(todoTabButton, false)
                
                alarmTabButton.isChecked = true
                todoTabButton.isChecked = false
                currentTab = 0
                
                fabAdd.animate()
                    .rotation(360f)
                    .setDuration(300)
                    .withEndAction {
                        fabAdd.rotation = 0f
                    }
                    .start()
            }
        }

        todoTabButton.setOnClickListener {
            if (currentTab != 1) {
                viewFlipper.setInAnimation(this, R.anim.slide_in_right)
                viewFlipper.setOutAnimation(this, R.anim.slide_out_left)
                viewFlipper.displayedChild = 1
                updateFabIcon(false)
                
                // 添加指示器动画
                animateIndicator(alarmTabButton, todoTabButton)
                
                // 添加按钮动画
                animateTabButton(todoTabButton, true)
                animateTabButton(alarmTabButton, false)
                
                alarmTabButton.isChecked = false
                todoTabButton.isChecked = true
                currentTab = 1
                
                fabAdd.animate()
                    .rotation(-360f)
                    .setDuration(300)
                    .withEndAction {
                        fabAdd.rotation = 0f
                    }
                    .start()
            }
        }

        // 设置浮动按钮点击事件
        fabAdd.setOnClickListener {
            if (viewFlipper.displayedChild == 0) {
                startActivity(Intent(this, AlarmSettingActivity::class.java))
            } else {
                startActivity(Intent(this, TodoSettingActivity::class.java))
            }
        }

        // 初始状态
        alarmTabButton.isChecked = true
        todoTabButton.isChecked = false
        updateFabIcon(true)

        // 加载数据
        loadAlarms()
        loadTodos()

        // 检查悬浮窗权限
        checkOverlayPermission()

        // 在 onCreate 方法中修改广播注册
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                TodoReminderReceiver(),
                IntentFilter().apply {
                    addAction(TODO_REMINDER_ACTION)
                    addAction(TODO_REMINDER_STOP_ACTION)
                },
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(
                TodoReminderReceiver(),
                IntentFilter().apply {
                    addAction(TODO_REMINDER_ACTION)
                    addAction(TODO_REMINDER_STOP_ACTION)
                }
            )
        }

        // 在 onCreate 方法中修改
        timeText.setOnClickListener {
            try {
                val intent = Intent(this, CalendarActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "打开日历失败: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "闹钟通知"
            val descriptionText = "冬去鸟鸣时闹钟通知"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification() {
        val intent = Intent(this, MainActivityAlm::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("冬去鸟鸣时")
            .setContentText("闹钟时间到了！")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun updateAlarmStatus(status: String) {
        // 不再需要更新状态文本
    }

    private fun restoreAlarmStatus() {
        // 不再需要恢复状态文本
        loadAlarms()
    }

    private fun startTimeUpdate() {
        timeUpdateRunnable = object : Runnable {
            override fun run() {
                updateCurrentTime()
                timeUpdateHandler.postDelayed(this, 1000) // 每秒更新一次
            }
        }
        timeUpdateRunnable?.let { timeUpdateHandler.post(it) }
    }

    private fun updateCurrentTime() {
        val calendar = Calendar.getInstance()
        
        // 更新日期显示，添加星期
        val dateFormat = SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINESE)
        dateText.text = dateFormat.format(calendar.time)
        
        // 更新时间显示，使用24小时制
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.CHINESE)
        timeText.text = timeFormat.format(calendar.time)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 停止时间更新
        timeUpdateRunnable?.let { timeUpdateHandler.removeCallbacks(it) }
        timeUpdateRunnable = null
        
        try {
            unregisterReceiver(alarmReceiver)
            unregisterReceiver(alarmStatusReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadAlarms() {
        val prefs = getSharedPreferences(ALARM_PREFS, Context.MODE_PRIVATE)
        val alarmsJson = prefs.getString(ALARM_LIST_KEY, "[]")
        try {
            val jsonArray = org.json.JSONArray(alarmsJson)
            alarmList.clear()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                // 获取重复日期数组
                val repeatDaysArray = obj.optJSONArray("repeatDays")
                val repeatDays = BooleanArray(7) { true } // 默认每天重复
                if (repeatDaysArray != null) {
                    for (j in 0 until repeatDaysArray.length()) {
                        repeatDays[j] = repeatDaysArray.getBoolean(j)
                    }
                }
                
                alarmList.add(AlarmData(
                    id = obj.getInt("id"),
                    timeInMillis = obj.getLong("timeInMillis"),
                    isEnabled = obj.getBoolean("isEnabled"),
                    ringtoneUri = obj.optString("ringtoneUri", null),
                    repeatDays = repeatDays
                ))
            }
            // 按时间排序
            alarmList.sortBy { it.timeInMillis }
            alarmAdapter.updateAlarms(alarmList.toList())
            updateAlarmStatus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveAlarms() {
        val jsonArray = org.json.JSONArray()
        alarmList.forEach { alarm ->
            val obj = org.json.JSONObject().apply {
                put("id", alarm.id)
                put("timeInMillis", alarm.timeInMillis)
                put("isEnabled", alarm.isEnabled)
                put("ringtoneUri", alarm.ringtoneUri)
            }
            jsonArray.put(obj)
        }
        
        getSharedPreferences(ALARM_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(ALARM_LIST_KEY, jsonArray.toString())
            .apply()
    }

    private fun deleteAlarm(alarm: AlarmData) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("删除闹钟")
            .setMessage("确定要删除这个闹钟吗？")
            .setPositiveButton("删除") { _, _ ->
                // 取消闹钟
                cancelAlarm(alarm)
                // 从列表中移除
                alarmList.remove(alarm)
                alarmAdapter.updateAlarms(alarmList.toList())
                saveAlarms()
                Toast.makeText(this, "闹钟已删除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun toggleAlarm(alarm: AlarmData, isEnabled: Boolean) {
        val index = alarmList.indexOf(alarm)
        if (index != -1) {
            val updatedAlarm = alarm.copy(isEnabled = isEnabled)
            alarmList[index] = updatedAlarm
            
            if (isEnabled) {
                setAlarm(updatedAlarm)
            } else {
                cancelAlarm(updatedAlarm)
            }
            
            // 使用 post 延迟更新，避免在布局计算过程中更新
            alarmListView.post {
                alarmAdapter.updateAlarms(alarmList.toList())
                saveAlarms()
                updateAlarmStatus()
            }
        }
    }

    private fun updateAlarmStatus() {
        val enabledAlarms = alarmList.count { it.isEnabled }
        if (enabledAlarms > 0) {
            val timeStrings = alarmList
                .filter { it.isEnabled }
                .joinToString(", ") { 
                    SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(it.timeInMillis) 
                }
            updateAlarmStatus("已设置 $enabledAlarms 个闹钟：$timeStrings")
        } else {
            updateAlarmStatus("未设置闹钟")
        }
    }

    private fun setAlarm(alarm: AlarmData) {
        val intent = Intent(ALARM_ACTION).apply {
            `package` = packageName
            putExtra("ringtone_uri", alarm.ringtoneUri)
            putExtra("alarm_id", alarm.id)
            putExtra("is_repeating", alarm.repeatDays.any { it })
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = alarm.timeInMillis
            // 如果时间已过，设置为下一个有效日期
            if (timeInMillis <= System.currentTimeMillis()) {
                // 检查是否有重复日期
                val hasRepeatDays = alarm.repeatDays.any { it }
                if (hasRepeatDays) {
                    // 找到下一个重复的日期
                    val today = (get(Calendar.DAY_OF_WEEK) + 5) % 7
                    var daysToAdd = 1
                    for (i in 1..7) {
                        val checkDay = (today + i) % 7
                        if (alarm.repeatDays[checkDay]) {
                            daysToAdd = i
                            break
                        }
                    }
                    add(Calendar.DAY_OF_YEAR, daysToAdd)
                } else {
                    // 非重复闹钟，只往后延一天
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        }

        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent),
                pendingIntent
            )
            Log.d("Alarm", "设置闹钟：${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(calendar.time)}")
        } catch (e: Exception) {
            Log.e("Alarm", "设置闹钟失败", e)
            Toast.makeText(this, "设置闹钟失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelAlarm(alarm: AlarmData) {
        val intent = Intent(ALARM_ACTION).apply {
            `package` = packageName
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun editAlarm(alarm: AlarmData) {
        val intent = Intent(this, AlarmSettingActivity::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("alarm_time", alarm.timeInMillis)
            putExtra("ringtone_uri", alarm.ringtoneUri)
        }
        startActivity(intent)
    }

    private fun updateFabIcon(isAlarmTab: Boolean) {
        fabAdd.animate()
            .scaleX(0f)
            .scaleY(0f)
            .setDuration(150)
            .withEndAction {
                fabAdd.setImageResource(
                    if (isAlarmTab) R.drawable.ic_alarm_add else R.drawable.ic_add_task
                )
                fabAdd.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .start()
            }
            .start()
    }

    private fun loadTodos() {
        val prefs = getSharedPreferences(TODO_PREFS, Context.MODE_PRIVATE)
        val todosJson = prefs.getString(TODO_LIST_KEY, "[]")
        try {
            val jsonArray = org.json.JSONArray(todosJson)
            todoList.clear()
            val currentTime = System.currentTimeMillis()
            var hasChanges = false

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val dueTime = obj.optLong("dueTime").takeIf { it != 0L }
                val isCompleted = obj.getBoolean("isCompleted")
                val completedTime = obj.optLong("completedTime").takeIf { it > 0L }
                
                // 如果待办已过期且未完成，自动标记为完成
                val shouldComplete = dueTime != null && 
                    dueTime < currentTime && 
                    !isCompleted

                if (shouldComplete) {
                    obj.put("isCompleted", true)
                    obj.put("completedTime", dueTime) // 设置完成时间为截止时间
                    hasChanges = true
                }

                todoList.add(TodoData(
                    id = obj.getInt("id"),
                    title = obj.getString("title"),
                    description = obj.optString("description", ""),
                    startTime = obj.optLong("startTime").takeIf { it != 0L },
                    dueTime = dueTime,
                    isCompleted = shouldComplete || isCompleted,
                    startRingtoneUri = obj.optString("startRingtoneUri", null),
                    dueRingtoneUri = obj.optString("dueRingtoneUri", null),
                    completedTime = if (shouldComplete) dueTime else completedTime // 设置完成时间
                ))
            }

            // 如果有待办被自动完成，保存更新
            if (hasChanges) {
                prefs.edit()
                    .putString(TODO_LIST_KEY, jsonArray.toString())
                    .apply()
            }

            // 按完成状态、开始时间和截止时间排序
            todoList.sortWith(
                compareBy<TodoData> { it.isCompleted }
                    .thenBy { it.startTime ?: Long.MAX_VALUE }
                    .thenBy { it.dueTime ?: Long.MAX_VALUE }
            )
            todoAdapter.updateTodos(todoList.toList())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveTodos() {
        val jsonArray = org.json.JSONArray()
        todoList.forEach { todo ->
            val obj = org.json.JSONObject().apply {
                put("id", todo.id)
                put("title", todo.title)
                put("description", todo.description)
                put("startTime", todo.startTime ?: 0L)
                put("dueTime", todo.dueTime ?: 0L)
                put("isCompleted", todo.isCompleted)
                put("startRingtoneUri", todo.startRingtoneUri)
                put("dueRingtoneUri", todo.dueRingtoneUri)
                put("completedTime", todo.completedTime ?: 0L)  // 添加完成时间的保存
            }
            jsonArray.put(obj)
        }
        
        getSharedPreferences(TODO_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(TODO_LIST_KEY, jsonArray.toString())
            .apply()
    }

    private fun deleteTodo(todo: TodoData) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("删除待办")
            .setMessage("确定要删除这个待办事项吗？")
            .setPositiveButton("删除") { _, _ ->
                // 取消提醒
                cancelTodoReminder(todo.id)
                // 从列表中移除
                todoList.remove(todo)
                todoAdapter.updateTodos(todoList.toList())
                saveTodos()
                Toast.makeText(this, "待办已删除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun toggleTodo(todo: TodoData, isCompleted: Boolean) {
        val index = todoList.indexOf(todo)
        if (index != -1) {
            val completedTime = if (isCompleted) {
                System.currentTimeMillis() // 用户手动完成时使用当前时间
            } else {
                null // 取消完成时清除完成时间
            }
            
            val updatedTodo = todo.copy(
                isCompleted = isCompleted,
                completedTime = completedTime
            )
            todoList[index] = updatedTodo

            // 如果待办被标记为完成，取消所有提醒
            if (isCompleted) {
                cancelTodoReminder(todo.id)
            }

            // 重新排序并更新
            todoList.sortWith(compareBy<TodoData> { it.isCompleted }
                .thenBy { it.dueTime ?: Long.MAX_VALUE })
            todoAdapter.updateTodos(todoList.toList())
            
            // 保存更新后的待办列表
            val jsonArray = org.json.JSONArray()
            todoList.forEach { item ->
                jsonArray.put(org.json.JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("description", item.description)
                    put("startTime", item.startTime ?: 0L)
                    put("dueTime", item.dueTime ?: 0L)
                    put("isCompleted", item.isCompleted)
                    put("startRingtoneUri", item.startRingtoneUri)
                    put("dueRingtoneUri", item.dueRingtoneUri)
                    put("completedTime", item.completedTime ?: 0L)  // 保存完成时间
                })
            }
            
            getSharedPreferences(TODO_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(TODO_LIST_KEY, jsonArray.toString())
                .apply()
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
        val intent = Intent(TODO_REMINDER_ACTION).apply {
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

    private fun editTodo(todo: TodoData) {
        val intent = Intent(this, TodoSettingActivity::class.java).apply {
            putExtra("todo_id", todo.id)
            putExtra("todo_title", todo.title)
            putExtra("todo_description", todo.description)
            putExtra("todo_start_time", todo.startTime)
            putExtra("todo_due_time", todo.dueTime)
            // 添加铃声 URI
            putExtra("todo_start_ringtone_uri", todo.startRingtoneUri)
            putExtra("todo_due_ringtone_uri", todo.dueRingtoneUri)
            // 根据提醒时间计算提前分钟数
            val advanceMinutes = todo.dueTime?.let { dueTime ->
                val prefs = getSharedPreferences(TODO_PREFS, Context.MODE_PRIVATE)
                val advanceTime = prefs.getLong("advance_time_${todo.id}", 0L)
                if (advanceTime > 0) {
                    ((dueTime - advanceTime) / (60 * 1000)).toInt()
                } else {
                    0
                }
            } ?: 0
            putExtra("todo_advance_minutes", advanceMinutes)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        loadAlarms()
        loadTodos()
    }

    private fun animateTabButton(button: MaterialButton, isSelected: Boolean) {
        // 缩放动画
        button.animate()
            .scaleX(if (isSelected) 1.05f else 1.0f)
            .scaleY(if (isSelected) 1.05f else 1.0f)
            .setDuration(200)
            .start()

        // 加载按压动画
        if (isSelected) {
            button.startAnimation(AnimationUtils.loadAnimation(this, R.anim.button_press))
        }

        // 文字颜色渐变
        val startColor = if (isSelected) 
            getColor(R.color.tab_unselected) else getColor(R.color.tab_selected)
        val endColor = if (isSelected) 
            getColor(R.color.tab_selected) else getColor(R.color.tab_unselected)

        ValueAnimator.ofArgb(startColor, endColor).apply {
            duration = 200
            addUpdateListener { animator ->
                button.setTextColor(animator.animatedValue as Int)
            }
            start()
        }

        // 背景颜色渐变
        val backgroundTint = if (isSelected) 
            android.content.res.ColorStateList.valueOf(getColor(R.color.tab_background_selected))
        else 
            android.content.res.ColorStateList.valueOf(getColor(R.color.tab_background_unselected))
        
        button.backgroundTintList = backgroundTint
    }

    // 添加指示器动画方法
    private fun animateIndicator(fromButton: MaterialButton, toButton: MaterialButton) {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.addUpdateListener { animation ->
            val fraction = animation.animatedFraction
            val fromX = fromButton.x
            val toX = toButton.x
            val currentX = fromX + (toX - fromX) * fraction
            tabIndicator.translationX = currentX
        }
        animator.duration = 300
        animator.interpolator = FastOutSlowInInterpolator()
        
        // 添加缩放动画效果
        tabIndicator.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(150)
            .withEndAction {
                tabIndicator.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .start()
            }
            .start()
        
        animator.start()
    }

    // 添加更新指示器位置的方法
    private fun updateIndicator(button: MaterialButton, animate: Boolean = true) {
        val layoutParams = tabIndicator.layoutParams
        layoutParams.width = button.width
        layoutParams.height = button.height
        tabIndicator.layoutParams = layoutParams

        if (animate) {
            tabIndicator.animate()
                .translationX(button.x)
                .setDuration(200)
                .setInterpolator(DecelerateInterpolator())
                .start()
        } else {
            tabIndicator.translationX = button.x
        }
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            // 创建提示对话框
            AlertDialog.Builder(this)
                .setTitle("需要悬浮窗权限")
                .setMessage("为了在闹钟响起时显示提醒窗口，需要您授予悬浮窗权限。")
                .setPositiveButton("去设置") { _, _ ->
                    // 跳转到悬浮窗权限设置页面
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    overlayPermissionLauncher.launch(intent)
                }
                .setNegativeButton("取消") { _, _ ->
                    Toast.makeText(this, 
                        "未授予悬浮窗权限，闹钟提醒可能无法正常显示", 
                        Toast.LENGTH_LONG
                    ).show()
                }
                .show()
        }
    }
}