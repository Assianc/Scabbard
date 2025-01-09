package com.assiance.alm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat

class TodoReminderReceiver : BroadcastReceiver() {
    companion object {
        private var mediaPlayer: MediaPlayer? = null
        private var vibrator: Vibrator? = null
        private var handler: Handler? = null
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            MainActivityAlm.TODO_REMINDER_ACTION -> {
                val ringtoneUri = intent.getStringExtra("ringtone_uri")
                showNotification(context!!, intent)
                playRingtone(context!!, ringtoneUri)
                
                // 启动悬浮窗服务
                val serviceIntent = Intent(context, TodoFloatingService::class.java).apply {
                    putExtra("todo_title", intent.getStringExtra("todo_title"))
                    putExtra("todo_description", intent.getStringExtra("todo_description"))
                    putExtra("is_advance", intent.getBooleanExtra("is_advance", false))
                    putExtra("is_due_reminder", intent.getBooleanExtra("is_due_reminder", true))
                }
                context.startService(serviceIntent)
            }
            MainActivityAlm.TODO_REMINDER_STOP_ACTION -> {
                Log.d("TodoReminder", "收到停止提醒广播")
                // 立即停止铃声和振动
                stopRingtone()
                // 取消通知
                val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(MainActivityAlm.TODO_NOTIFICATION_ID)
                notificationManager?.cancel(MainActivityAlm.TODO_NOTIFICATION_ID + 1)
                notificationManager?.cancel(MainActivityAlm.TODO_NOTIFICATION_ID + 2)
            }
        }
    }

    private fun showNotification(context: Context, intent: Intent) {
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
                !intent.getBooleanExtra("is_due_reminder", true) -> "待办开始提醒"
                intent.getBooleanExtra("is_advance", true) -> "待办提醒"
                else -> "待办到期提醒"
            })
            .setContentText(when {
                !intent.getBooleanExtra("is_due_reminder", true) -> "${intent.getStringExtra("todo_title")} 开始时间到了"
                intent.getBooleanExtra("is_advance", true) -> "${intent.getStringExtra("todo_title")} 即将到期"
                else -> "${intent.getStringExtra("todo_title")} 已到期"
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
                !intent.getBooleanExtra("is_due_reminder", true) -> MainActivityAlm.TODO_NOTIFICATION_ID + 2
                intent.getBooleanExtra("is_advance", true) -> MainActivityAlm.TODO_NOTIFICATION_ID + 1
                else -> MainActivityAlm.TODO_NOTIFICATION_ID
            }
            notificationManager.notify(notificationId, notification)
            Log.d("TodoReminder", "通知已发送")
        } catch (e: Exception) {
            Log.e("TodoReminder", "发送通知失败", e)
        }
    }

    private fun playRingtone(context: Context, ringtoneUri: String?) {
        stopRingtone()
        
        try {
            val soundUri = if (ringtoneUri != null) {
                Uri.parse(ringtoneUri)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, soundUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = false
                setOnPreparedListener { mp -> mp.start() }
                prepareAsync()
            }

            // 设置振动
            vibrator = (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrate(1000)
                }
            }

            // 延长播放时间到30秒
            handler = Handler(Looper.getMainLooper())
            handler?.postDelayed({
                stopRingtone()
            }, 30000)

        } catch (e: Exception) {
            e.printStackTrace()
            stopRingtone()
        }
    }

    private fun stopRingtone() {
        Log.d("TodoReminder", "停止铃声和振动")
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
        }

        vibrator?.cancel()
        vibrator = null

        handler?.removeCallbacksAndMessages(null)
        handler = null
    }
} 