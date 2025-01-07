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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivityAlm : AppCompatActivity() {
    private lateinit var alarmStatusText: TextView
    private lateinit var alarmManager: AlarmManager
    private lateinit var dateText: TextView
    private lateinit var timeText: TextView
    private var timeUpdateHandler: Handler = Handler(Looper.getMainLooper())
    private var timeUpdateRunnable: Runnable? = null

    companion object {
        internal const val ALARM_REQUEST_CODE = 100
        internal const val CHANNEL_ID = "AlarmChannel"
        internal const val NOTIFICATION_ID = 1
        const val ALARM_ACTION = "com.assiance.alm.ALARM_TRIGGER"
        const val ALARM_STATUS_CHANGED_ACTION = "com.assiance.alm.ALARM_STATUS_CHANGED"
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
        alarmStatusText = findViewById(R.id.alarmStatusText)
        
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

        // 设置浮动按钮点击事件
        findViewById<FloatingActionButton>(R.id.fabSetAlarm).setOnClickListener {
            startActivity(Intent(this, AlarmSettingActivity::class.java))
        }

        // 恢复已设置的闹钟状态
        restoreAlarmStatus()
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
        alarmStatusText.text = status
    }

    private fun restoreAlarmStatus() {
        val savedAlarmTime = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
            .getLong("alarm_time", -1)
        
        if (savedAlarmTime != -1L) {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = savedAlarmTime
            }
            val timeString = String.format(
                "%02d:%02d",
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE)
            )
            updateAlarmStatus("闹钟已设置：$timeString")
        } else {
            updateAlarmStatus("未设置闹钟")
        }
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
}