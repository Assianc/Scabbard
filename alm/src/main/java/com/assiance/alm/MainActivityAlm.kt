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
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivityAlm : AppCompatActivity() {
    private lateinit var timePicker: TimePicker
    private lateinit var setAlarmButton: Button
    private lateinit var cancelAlarmButton: Button
    private lateinit var alarmStatusText: TextView
    private lateinit var alarmManager: AlarmManager
    private lateinit var dateText: TextView
    private lateinit var timeText: TextView
    private var timeUpdateHandler: Handler = Handler(Looper.getMainLooper())
    private var timeUpdateRunnable: Runnable? = null

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

        // 初始化时间显示相关的视图
        dateText = findViewById(R.id.dateText)
        timeText = findViewById(R.id.timeText)
        
        // 启动时间更新
        startTimeUpdate()

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
            if (checkAllPermissions()) {
                setAlarm()
            }
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

    private fun checkAllPermissions(): Boolean {
        // 检查通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!checkNotificationPermission()) {
                requestNotificationPermission()
                return false
            }
        }

        // 检查精确闹钟权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!checkAlarmPermission()) {
                showAlarmPermissionDialog()
                return false
            }
        }

        return true
    }

    private fun checkNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
                // 显示解释对话框
                AlertDialog.Builder(this)
                    .setTitle("需要通知权限")
                    .setMessage("为了在闹钟响起时显示通知，需要授予通知权限。")
                    .setPositiveButton("授权") { _, _ ->
                        requestPermissions(
                            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                            100
                        )
                    }
                    .setNegativeButton("取消") { dialog, _ ->
                        dialog.dismiss()
                        Toast.makeText(this, "未授予通知权限，闹钟将无法显示通知", Toast.LENGTH_LONG).show()
                    }
                    .show()
            } else {
                // 直接请求权限
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }

    private fun checkAlarmPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            return alarmManager.canScheduleExactAlarms()
        }
        return true
    }

    // 处理权限请求结果
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            100 -> {
                if (grantResults.isNotEmpty() && 
                    grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    // 用户授予了通知权限
                    if (checkAlarmPermission()) {
                        setAlarm()
                    }
                } else {
                    Toast.makeText(this, "未授予通知权限，闹钟将无法显示通知", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showAlarmPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("需要权限")
            .setMessage("为了确保闹钟准时响起，需要授予精确闹钟权限。")
            .setPositiveButton("去设置") { _, _ ->
                try {
                    // 跳转到精确闹钟权限设置页面
                    val intent = Intent().apply {
                        action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "打开设置失败，请手动授予权限", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "未授予权限，闹钟可能不会准时响起", Toast.LENGTH_LONG).show()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        // 检查所有权限状态
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !checkNotificationPermission()) {
            Toast.makeText(this, "请授予通知权限以确保闹钟通知正常显示", Toast.LENGTH_LONG).show()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !checkAlarmPermission()) {
            Toast.makeText(this, "请授予精确闹钟权限以确保闹钟准时响起", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 停止时间更新
        timeUpdateRunnable?.let { timeUpdateHandler.removeCallbacks(it) }
        timeUpdateRunnable = null
        
        try {
            unregisterReceiver(alarmReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
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
        
        // 更新日期显示
        val dateFormat = SimpleDateFormat("yyyy年MM月dd日 E", Locale.CHINESE)
        dateText.text = dateFormat.format(calendar.time)
        
        // 更新时间显示
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.CHINESE)
        timeText.text = timeFormat.format(calendar.time)
    }
}