package com.assiance.alm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent?.action == MainActivityAlm.ALARM_ACTION) {
            showNotification(context)
            // 发送广播通知主界面更新状态
            context.sendBroadcast(Intent(MainActivityAlm.ALARM_STATUS_CHANGED_ACTION))
        }
    }

    private fun showNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // 创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MainActivityAlm.CHANNEL_ID,
                "闹钟通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "冬去鸟鸣时闹钟通知"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 创建打开应用的 Intent
        val intent = Intent(context, MainActivityAlm::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // 创建通知
        val notification = NotificationCompat.Builder(context, MainActivityAlm.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("冬去鸟鸣时")
            .setContentText("闹钟时间到了！")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(MainActivityAlm.NOTIFICATION_ID, notification)
    }
} 