package com.assiance.alm

import android.content.Context
import android.os.Bundle
import android.widget.CalendarView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageButton
import android.view.LayoutInflater
import android.app.AlertDialog
import android.widget.NumberPicker
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : AppCompatActivity() {
    private lateinit var calendarView: CalendarView
    private lateinit var historyList: RecyclerView
    private lateinit var dateTitle: TextView
    private lateinit var historyAdapter: HistoryAdapter
    private val historyItems = mutableListOf<HistoryAdapter.HistoryItem>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        // 设置工具栏
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "历史记录"

        calendarView = findViewById(R.id.calendarView)
        historyList = findViewById(R.id.historyList)
        dateTitle = findViewById(R.id.dateTitle)

        // 设置日历选择监听
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }
            updateHistoryForDate(calendar.timeInMillis)
        }

        // 初始化RecyclerView
        historyList.layoutManager = LinearLayoutManager(this)
        
        // 初始化适配器
        historyAdapter = HistoryAdapter()
        historyList.adapter = historyAdapter

        // 加载所有闹钟和待办数据
        loadData()

        // 设置跳转按钮点击事件
        findViewById<ImageButton>(R.id.jumpButton).setOnClickListener {
            showDateJumpDialog()
        }
    }

    private fun loadData() {
        // 加载闹钟数据
        val prefs = getSharedPreferences(MainActivityAlm.ALARM_PREFS, Context.MODE_PRIVATE)
        val alarmsJson = prefs.getString(MainActivityAlm.ALARM_LIST_KEY, "[]")
        val alarms = try {
            val jsonArray = org.json.JSONArray(alarmsJson)
            (0 until jsonArray.length()).map { i ->
                val obj = jsonArray.getJSONObject(i)
                val repeatDaysArray = obj.optJSONArray("repeatDays")
                val repeatDays = BooleanArray(7) { true }
                if (repeatDaysArray != null) {
                    for (j in 0 until repeatDaysArray.length()) {
                        repeatDays[j] = repeatDaysArray.getBoolean(j)
                    }
                }
                AlarmData(
                    id = obj.getInt("id"),
                    timeInMillis = obj.getLong("timeInMillis"),
                    isEnabled = obj.getBoolean("isEnabled"),
                    ringtoneUri = obj.optString("ringtoneUri", null),
                    repeatDays = repeatDays
                )
            }
        } catch (e: Exception) {
            emptyList()
        }

        // 生成未来7天的闹钟时间
        val startDate = Calendar.getInstance()
        val endDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 7)  // 只看未来7天
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }
        
        val alarmTimes = mutableListOf<HistoryAdapter.HistoryItem>()
        
        // 遍历每个启用的闹钟
        alarms.filter { it.isEnabled }.forEach { alarm ->
            val alarmCalendar = Calendar.getInstance().apply {
                timeInMillis = alarm.timeInMillis
            }
            
            // 如果是重复闹钟
            if (alarm.repeatDays.any { it }) {
                val currentCal = Calendar.getInstance()
                while (currentCal.before(endDate)) {
                    val dayOfWeek = (currentCal.get(Calendar.DAY_OF_WEEK) + 5) % 7
                    if (alarm.repeatDays[dayOfWeek]) {
                        // 创建该日期的闹钟时间
                        val alarmTime = Calendar.getInstance().apply {
                            set(Calendar.YEAR, currentCal.get(Calendar.YEAR))
                            set(Calendar.MONTH, currentCal.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, currentCal.get(Calendar.DAY_OF_MONTH))
                            set(Calendar.HOUR_OF_DAY, alarmCalendar.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, alarmCalendar.get(Calendar.MINUTE))
                            set(Calendar.SECOND, 0)
                        }
                        
                        // 只添加未来的时间
                        if (alarmTime.timeInMillis > System.currentTimeMillis()) {
                            alarmTimes.add(HistoryAdapter.HistoryItem.AlarmItem(
                                AlarmData(
                                    id = alarm.id,
                                    timeInMillis = alarmTime.timeInMillis,
                                    isEnabled = true,
                                    ringtoneUri = alarm.ringtoneUri,
                                    repeatDays = alarm.repeatDays
                                )
                            ))
                        }
                    }
                    currentCal.add(Calendar.DAY_OF_YEAR, 1)
                }
            } else if (alarm.timeInMillis > System.currentTimeMillis() && 
                      alarm.timeInMillis < endDate.timeInMillis) {
                // 非重复闹钟，只添加未来7天内的时间
                alarmTimes.add(HistoryAdapter.HistoryItem.AlarmItem(alarm))
            }
        }

        // 加载待办数据
        val todosPrefs = getSharedPreferences(MainActivityAlm.TODO_PREFS, MODE_PRIVATE)
        val todosJson = todosPrefs.getString(MainActivityAlm.TODO_LIST_KEY, "[]")
        val todos = mutableListOf<TodoData>()
        
        try {
            val jsonArray = org.json.JSONArray(todosJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val startTime = obj.optLong("startTime")
                val dueTime = obj.optLong("dueTime")
                val isCompleted = obj.getBoolean("isCompleted")
                
                // 只加载未完成的待办
                if (!isCompleted) {
                    // 对于无时间限制的待办，只加载当天的
                    if ((startTime == 0L && dueTime == 0L && 
                         isSameDay(System.currentTimeMillis(), System.currentTimeMillis())) ||
                        // 对于有时间限制的待办，检查是否在未来7天内
                        (startTime > 0 && startTime < endDate.timeInMillis) ||
                        (dueTime > 0 && dueTime > System.currentTimeMillis())) {
                        
                        todos.add(TodoData(
                            id = obj.getInt("id"),
                            title = obj.getString("title"),
                            description = obj.getString("description"),
                            startTime = if (startTime > 0) startTime else null,
                            dueTime = if (dueTime > 0) dueTime else null,
                            isCompleted = isCompleted
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 合并所有数据并按时间排序
        historyItems.clear()
        historyItems.addAll(alarmTimes)
        historyItems.addAll(todos.map { HistoryAdapter.HistoryItem.TodoItem(it) })
        
        // 按时间排序
        historyItems.sortBy { item ->
            when (item) {
                is HistoryAdapter.HistoryItem.AlarmItem -> item.alarm.timeInMillis
                is HistoryAdapter.HistoryItem.TodoItem -> {
                    val todo = item.todo
                    when {
                        todo.startTime != null -> todo.startTime
                        todo.dueTime != null -> todo.dueTime
                        else -> System.currentTimeMillis() // 无时间限制的待办按当前时间排序
                    }
                }
            }
        }

        // 更新适配器
        historyAdapter.updateItems(historyItems)
        updateHistoryForDate(calendarView.date)
    }

    private fun updateHistoryForDate(date: Long) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val nextDay = Calendar.getInstance().apply {
            timeInMillis = calendar.timeInMillis
            add(Calendar.DAY_OF_YEAR, 1)
        }

        val currentTime = System.currentTimeMillis()

        // 获取一周后的时间戳
        val oneWeekLater = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 7)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis

        // 过滤出选定日期的项目
        val filteredItems = historyItems.filter { item ->
            when (item) {
                is HistoryAdapter.HistoryItem.AlarmItem -> {
                    val itemCal = Calendar.getInstance().apply {
                        timeInMillis = item.alarm.timeInMillis
                    }
                    itemCal.timeInMillis >= calendar.timeInMillis && 
                    itemCal.timeInMillis < nextDay.timeInMillis
                }
                is HistoryAdapter.HistoryItem.TodoItem -> {
                    val todo = item.todo
                    val startTime = todo.startTime
                    val dueTime = todo.dueTime
                    val isCompleted = todo.isCompleted
                    
                    when {
                        isCompleted -> false
                        // 有开始时间或截止时间的待办
                        startTime != null || dueTime != null -> {
                            when {
                                // 只有开始时间
                                startTime != null && dueTime == null -> {
                                    isSameDay(startTime, date)
                                }
                                // 只有截止时间
                                startTime == null && dueTime != null -> {
                                    // 从当前时间到截止时间之间都显示
                                    date >= currentTime && date <= dueTime
                                }
                                // 同时有开始时间和截止时间
                                startTime != null && dueTime != null -> {
                                    date >= startTime && date <= dueTime
                                }
                                else -> false
                            }
                        }
                        // 无时间限制的待办只在当天且未来一周内显示
                        else -> {
                            isSameDay(date, System.currentTimeMillis()) && 
                            date <= oneWeekLater
                        }
                    }
                }
            }
        }

        historyAdapter.updateItems(filteredItems)
        
        // 更新日期标题
        val dateFormat = SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINESE)
        dateTitle.text = dateFormat.format(date)
    }

    private fun getTodosForDate(timestamp: Long): List<TodoData> {
        val prefs = getSharedPreferences(MainActivityAlm.TODO_PREFS, MODE_PRIVATE)
        val todosJson = prefs.getString(MainActivityAlm.TODO_LIST_KEY, "[]")
        val todos = mutableListOf<TodoData>()
        
        try {
            val jsonArray = org.json.JSONArray(todosJson)
            // 获取一周后的时间戳
            val oneWeekLater = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 7)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.timeInMillis

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val startTime = obj.optLong("startTime")
                val dueTime = obj.optLong("dueTime")
                
                // 修改筛选逻辑：
                // 1. 如果有开始时间或截止时间，检查是否是同一天
                // 2. 如果都没有时间，只在当天显示，且仅显示未来一周内的
                if ((startTime > 0 && isSameDay(startTime, timestamp)) ||
                    (dueTime > 0 && isSameDay(dueTime, timestamp)) ||
                    (startTime == 0L && dueTime == 0L && 
                     isSameDay(timestamp, System.currentTimeMillis()) && 
                     timestamp <= oneWeekLater)) {
                        
                    todos.add(TodoData(
                        id = obj.getInt("id"),
                        title = obj.getString("title"),
                        description = obj.getString("description"),
                        startTime = if (startTime > 0) startTime else null,
                        dueTime = if (dueTime > 0) dueTime else null,
                        isCompleted = obj.getBoolean("isCompleted")
                    ))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return todos
    }

    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
               cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    private fun showDateJumpDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_date_jump, null)
        val yearPicker = dialogView.findViewById<NumberPicker>(R.id.yearPicker)
        val monthPicker = dialogView.findViewById<NumberPicker>(R.id.monthPicker)

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        yearPicker.apply {
            minValue = currentYear - 10
            maxValue = currentYear + 10
            value = currentYear
        }

        monthPicker.apply {
            minValue = 1
            maxValue = 12
            displayedValues = arrayOf(
                "1月", "2月", "3月", "4月", "5月", "6月",
                "7月", "8月", "9月", "10月", "11月", "12月"
            )
        }

        AlertDialog.Builder(this)
            .setTitle("跳转到指定日期")
            .setView(dialogView)
            .setPositiveButton("确定") { _, _ ->
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, yearPicker.value)
                    set(Calendar.MONTH, monthPicker.value - 1)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                calendarView.date = calendar.timeInMillis
                updateHistoryForDate(calendar.timeInMillis)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 