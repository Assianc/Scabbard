package com.assiance.alm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class AlarmSettingActivity : AppCompatActivity() {
    private lateinit var timePicker: TimePicker
    private lateinit var alarmManager: AlarmManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_setting)

        // 设置工具栏
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "设置闹钟"

        timePicker = findViewById(R.id.timePicker)
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 设置24小时制
        timePicker.setIs24HourView(true)

        findViewById<android.widget.Button>(R.id.setAlarmButton).setOnClickListener {
            setAlarm()
        }
    }

    private fun setAlarm() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, timePicker.hour)
            set(Calendar.MINUTE, timePicker.minute)
            set(Calendar.SECOND, 0)
            
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(MainActivityAlm.ALARM_ACTION).apply {
            `package` = packageName
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            MainActivityAlm.ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent),
            pendingIntent
        )

        // 保存闹钟状态
        getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE).edit()
            .putLong("alarm_time", calendar.timeInMillis)
            .apply()

        // 发送广播通知主界面更新状态
        sendBroadcast(Intent(MainActivityAlm.ALARM_STATUS_CHANGED_ACTION))

        Toast.makeText(this, "闹钟已设置", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 