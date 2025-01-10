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
                // 只调用一次启动悬浮窗服务
                val serviceIntent = Intent(context, TodoFloatingService::class.java).apply {
                    putExtra("todo_title", intent.getStringExtra("todo_title"))
                    putExtra("todo_description", intent.getStringExtra("todo_description"))
                    putExtra("is_advance", intent.getBooleanExtra("is_advance", false))
                    putExtra("is_due_reminder", intent.getBooleanExtra("is_due_reminder", true))
                    putExtra("target_time", intent.getLongExtra("target_time", 0))
                }
                context?.startService(serviceIntent)

                // 播放铃声
                playRingtone(context!!, ringtoneUri)
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