package com.assiance.alm

import android.os.Bundle
import android.widget.CalendarView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : AppCompatActivity() {
    private lateinit var calendarView: CalendarView
    private lateinit var historyList: RecyclerView
    private lateinit var dateTitle: TextView
    private lateinit var historyAdapter: HistoryAdapter
    
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

        // 显示当前日期的历史记录
        updateHistoryForDate(System.currentTimeMillis())
    }

    private fun updateHistoryForDate(timestamp: Long) {
        val dateFormat = SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINESE)
        dateTitle.text = dateFormat.format(Date(timestamp))

        // 获取选定日期的闹钟和待办事项
        val alarms = getAlarmsForDate(timestamp)
        val todos = getTodosForDate(timestamp)
        
        // 更新列表显示
        historyAdapter.updateItems(alarms, todos)
    }

    private fun getAlarmsForDate(timestamp: Long): List<AlarmData> {
        // 从SharedPreferences获取闹钟数据并筛选指定日期的闹钟
        val prefs = getSharedPreferences(MainActivityAlm.ALARM_PREFS, MODE_PRIVATE)
        val alarmsJson = prefs.getString(MainActivityAlm.ALARM_LIST_KEY, "[]")
        val alarms = mutableListOf<AlarmData>()
        
        try {
            val jsonArray = org.json.JSONArray(alarmsJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val alarmTime = obj.getLong("timeInMillis")
                if (isSameDay(alarmTime, timestamp)) {
                    alarms.add(AlarmData(
                        id = obj.getInt("id"),
                        timeInMillis = alarmTime,
                        isEnabled = obj.getBoolean("isEnabled"),
                        ringtoneUri = obj.optString("ringtoneUri", null)
                    ))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return alarms
    }

    private fun getTodosForDate(timestamp: Long): List<TodoData> {
        // 从SharedPreferences获取待办数据并筛选指定日期的待办
        val prefs = getSharedPreferences(MainActivityAlm.TODO_PREFS, MODE_PRIVATE)
        val todosJson = prefs.getString(MainActivityAlm.TODO_LIST_KEY, "[]")
        val todos = mutableListOf<TodoData>()
        
        try {
            val jsonArray = org.json.JSONArray(todosJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val startTime = obj.optLong("startTime")
                val dueTime = obj.optLong("dueTime")
                if (startTime > 0 && isSameDay(startTime, timestamp) ||
                    dueTime > 0 && isSameDay(dueTime, timestamp)) {
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
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 