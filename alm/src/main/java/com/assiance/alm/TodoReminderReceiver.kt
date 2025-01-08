package com.assiance.alm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

class TodoReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("TodoReminder", "收到提醒广播：${intent?.action}")
        
        if (context != null && intent?.action == MainActivityAlm.TODO_REMINDER_ACTION) {
            val todoTitle = intent.getStringExtra("todo_title") ?: "待办事项"
            val isAdvance = intent.getBooleanExtra("is_advance", true)
            val isDueReminder = intent.getBooleanExtra("is_due_reminder", true)
            Log.d("TodoReminder", "显示提醒通知：$todoTitle")
            showNotification(context, todoTitle, isAdvance, isDueReminder)
        }
    }

    private fun showNotification(context: Context, todoTitle: String, isAdvance: Boolean, isDueReminder: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // 创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MainActivityAlm.TODO_CHANNEL_ID,
                "待办提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "待办事项截止时间提醒"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 创建打开应用的 Intent
        val intent = Intent(context, MainActivityAlm::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // 添加额外标志以打开待办列表
            putExtra("open_todo_list", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // 创建通知
        val notification = NotificationCompat.Builder(context, MainActivityAlm.TODO_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_add_task)
            .setContentTitle(when {
                !isDueReminder -> "待办开始提醒"
                isAdvance -> "待办提醒"
                else -> "待办到期提醒"
            })
            .setContentText(when {
                !isDueReminder -> "$todoTitle 开始时间到了"
                isAdvance -> "$todoTitle 即将到期"
                else -> "$todoTitle 已到期"
            })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            // 使用不同的通知ID
            val notificationId = when {
                !isDueReminder -> MainActivityAlm.TODO_NOTIFICATION_ID + 2
                isAdvance -> MainActivityAlm.TODO_NOTIFICATION_ID + 1
                else -> MainActivityAlm.TODO_NOTIFICATION_ID
            }
            notificationManager.notify(notificationId, notification)
            Log.d("TodoReminder", "通知已发送")
        } catch (e: Exception) {
            Log.e("TodoReminder", "发送通知失败", e)
        }
    }
} 