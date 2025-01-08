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
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

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

        // 初始化指示器
        tabIndicator = findViewById(R.id.tabIndicator)
        
        // 在视图完成布局后设置初始指示器位置
        alarmTabButton.post {
            updateIndicator(alarmTabButton, true)
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
                
                // 如果待办已过期且未完成，自动标记为完成
                val shouldComplete = dueTime != null && 
                    dueTime < currentTime && 
                    !isCompleted

                if (shouldComplete) {
                    obj.put("isCompleted", true)
                    hasChanges = true
                }

                todoList.add(TodoData(
                    id = obj.getInt("id"),
                    title = obj.getString("title"),
                    description = obj.optString("description", ""),
                    startTime = obj.optLong("startTime").takeIf { it != 0L },
                    dueTime = dueTime,
                    isCompleted = shouldComplete || isCompleted
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
            putExtra("todo_start_time", todo.startTime)
            putExtra("todo_due_time", todo.dueTime)
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

    private fun cancelTodoReminder(todoId: Int) {
        val intent = Intent(TODO_REMINDER_ACTION).apply {
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
            val fromLeft = fromButton.left.toFloat()
            val toLeft = toButton.left.toFloat()
            val fromRight = fromButton.right.toFloat()
            val toRight = toButton.right.toFloat()
            
            val currentLeft = fromLeft + (toLeft - fromLeft) * fraction
            val currentRight = fromRight + (toRight - fromRight) * fraction
            
            tabIndicator.left = currentLeft.toInt()
            tabIndicator.right = currentRight.toInt()
        }
        animator.duration = 300
        animator.interpolator = FastOutSlowInInterpolator()
        animator.start()
    }

    // 添加更新指示器位置的方法
    private fun updateIndicator(button: MaterialButton, animate: Boolean = true) {
        if (animate) {
            tabIndicator.animate()
                .x(button.left.toFloat())
                .setDuration(200)
                .start()
        } else {
            tabIndicator.left = button.left
            tabIndicator.right = button.right
        }
        
        // 设置指示器高度为按钮高度
        val layoutParams = tabIndicator.layoutParams
        layoutParams.height = button.height
        tabIndicator.layoutParams = layoutParams
    }
}