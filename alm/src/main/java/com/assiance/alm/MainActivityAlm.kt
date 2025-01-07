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
import android.widget.Button
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import java.util.Calendar

class MainActivityAlm : AppCompatActivity() {
    private lateinit var timePicker: TimePicker
    private lateinit var setAlarmButton: Button
    private lateinit var cancelAlarmButton: Button
    private lateinit var alarmStatusText: TextView
    private lateinit var alarmManager: AlarmManager

    companion object {
        private const val ALARM_REQUEST_CODE = 100
        private const val CHANNEL_ID = "AlarmChannel"
        private const val NOTIFICATION_ID = 1
        const val ALARM_ACTION = "com.assiance.alm.ALARM_TRIGGER"
    }

    private val alarmReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ALARM_ACTION) {
                showNotification()
                updateAlarmStatus("闹钟响了！")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_alm)

        // 初始化视图
        timePicker = findViewById(R.id.timePicker)
        setAlarmButton = findViewById(R.id.setAlarmButton)
        cancelAlarmButton = findViewById(R.id.cancelAlarmButton)
        alarmStatusText = findViewById(R.id.alarmStatusText)

        // 初始化闹钟管理器
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 创建通知渠道
        createNotificationChannel()

        // 注册广播接收器，添加 RECEIVER_NOT_EXPORTED 标志
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                alarmReceiver,
                IntentFilter(ALARM_ACTION),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(alarmReceiver, IntentFilter(ALARM_ACTION))
        }

        // 设置按钮点击事件
        setAlarmButton.setOnClickListener {
            setAlarm()
        }

        cancelAlarmButton.setOnClickListener {
            cancelAlarm()
        }

        // 恢复已设置的闹钟状态
        restoreAlarmStatus()
    }

    private fun setAlarm() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, timePicker.hour)
            set(Calendar.MINUTE, timePicker.minute)
            set(Calendar.SECOND, 0)
            
            // 如果设置的时间早于当前时间，设置为明天
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(ALARM_ACTION).apply {
            `package` = packageName  // 添加包名以确保广播只发送给本应用
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 设置精确闹钟
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent),
            pendingIntent
        )

        // 保存闹钟状态
        getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE).edit()
            .putLong("alarm_time", calendar.timeInMillis)
            .apply()

        val timeString = String.format("%02d:%02d", timePicker.hour, timePicker.minute)
        updateAlarmStatus("闹钟已设置：$timeString")
        Toast.makeText(this, "闹钟已设置", Toast.LENGTH_SHORT).show()
    }

    private fun cancelAlarm() {
        val intent = Intent(ALARM_ACTION).apply {
            `package` = packageName
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
        
        // 清除保存的闹钟状态
        getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE).edit()
            .remove("alarm_time")
            .apply()

        updateAlarmStatus("未设置闹钟")
        Toast.makeText(this, "闹钟已取消", Toast.LENGTH_SHORT).show()
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

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(alarmReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}