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
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

        // 注册广播接收器
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                alarmReceiver,
                IntentFilter(ALARM_ACTION),
                Context.RECEIVER_NOT_EXPORTED
            )
            registerReceiver(
                alarmStatusReceiver,
                IntentFilter(ALARM_STATUS_CHANGED_ACTION),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(alarmReceiver, IntentFilter(ALARM_ACTION))
            registerReceiver(alarmStatusReceiver, IntentFilter(ALARM_STATUS_CHANGED_ACTION))
        }

        // 初始化视图切换器和标签按钮
        viewFlipper = findViewById(R.id.viewFlipper)
        alarmTabButton = findViewById(R.id.alarmTabButton)
        todoTabButton = findViewById(R.id.todoTabButton)
        fabAdd = findViewById(R.id.fabAdd)

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

        // 设置标签按钮点击事件
        alarmTabButton.setOnClickListener {
            viewFlipper.displayedChild = 0
            updateFabIcon(true)
            alarmTabButton.isChecked = true
            todoTabButton.isChecked = false
        }

        todoTabButton.setOnClickListener {
            viewFlipper.displayedChild = 1
            updateFabIcon(false)
            alarmTabButton.isChecked = false
            todoTabButton.isChecked = true
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
                alarmList.add(AlarmData(
                    id = obj.getInt("id"),
                    timeInMillis = obj.getLong("timeInMillis"),
                    isEnabled = obj.getBoolean("isEnabled")
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
            }
            jsonArray.put(obj)
        }
        
        getSharedPreferences(ALARM_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(ALARM_LIST_KEY, jsonArray.toString())
            .apply()
    }

    private fun deleteAlarm(alarm: AlarmData) {
        // 取消闹钟
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

        // 从列表中移除并更新
        alarmList.remove(alarm)
        alarmListView.post {
            alarmAdapter.updateAlarms(alarmList.toList())
            saveAlarms()
            updateAlarmStatus()
        }
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
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = alarm.timeInMillis
            // 如果时间已过，设置为下一天
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent),
            pendingIntent
        )
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
        }
        startActivity(intent)
    }

    private fun updateFabIcon(isAlarmTab: Boolean) {
        fabAdd.setImageResource(
            if (isAlarmTab) R.drawable.ic_alarm_add else R.drawable.ic_add_task
        )
    }

    private fun loadTodos() {
        val prefs = getSharedPreferences(TODO_PREFS, Context.MODE_PRIVATE)
        val todosJson = prefs.getString(TODO_LIST_KEY, "[]")
        try {
            val jsonArray = org.json.JSONArray(todosJson)
            todoList.clear()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                todoList.add(TodoData(
                    id = obj.getInt("id"),
                    title = obj.getString("title"),
                    description = obj.optString("description", ""),
                    dueTime = obj.optLong("dueTime").takeIf { it != 0L },
                    isCompleted = obj.getBoolean("isCompleted")
                ))
            }
            // 按完成状态和截止时间排序
            todoList.sortWith(compareBy<TodoData> { it.isCompleted }
                .thenBy { it.dueTime ?: Long.MAX_VALUE })
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
                put("dueTime", todo.dueTime ?: 0L)
                put("isCompleted", todo.isCompleted)
            }
            jsonArray.put(obj)
        }
        
        getSharedPreferences(TODO_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(TODO_LIST_KEY, jsonArray.toString())
            .apply()
    }

    private fun deleteTodo(todo: TodoData) {
        todoList.remove(todo)
        todoAdapter.updateTodos(todoList.toList())
        saveTodos()
    }

    private fun toggleTodo(todo: TodoData, isCompleted: Boolean) {
        val index = todoList.indexOf(todo)
        if (index != -1) {
            val updatedTodo = todo.copy(isCompleted = isCompleted)
            todoList[index] = updatedTodo
            // 重新排序并更新
            todoList.sortWith(compareBy<TodoData> { it.isCompleted }
                .thenBy { it.dueTime ?: Long.MAX_VALUE })
            todoAdapter.updateTodos(todoList.toList())
            saveTodos()
        }
    }

    private fun editTodo(todo: TodoData) {
        val intent = Intent(this, TodoSettingActivity::class.java).apply {
            putExtra("todo_id", todo.id)
            putExtra("todo_title", todo.title)
            putExtra("todo_description", todo.description)
            putExtra("todo_due_time", todo.dueTime)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        loadAlarms()
        loadTodos()
    }
}