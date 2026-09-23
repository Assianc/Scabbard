package com.assiance.alm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import android.app.Notification

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private var instance: AlarmReceiver? = null
        private var mediaPlayer: MediaPlayer? = null
        private var vibrator: Vibrator? = null
        private var alarmHandler: Handler? = null
        
        fun getInstance(): AlarmReceiver {
            if (instance == null) {
                instance = AlarmReceiver()
            }
            return instance!!
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        when (intent.action) {
            AlmContract.ALARM_STOP_ACTION -> {
                stopAlarm()
                // 停止闹钟时撤销「闹钟时间到了」通知。
                // 原实现里这条通知从未弹出过（原因见下面 ALARM_ACTION 分支的注释），因此没有
                // 配套的撤销代码；接通通知后若不在停止路径撤销，用户停止闹钟后会留下一条僵尸
                // 通知。TodoReminderReceiver 的停止分支已有同样的做法。
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .cancel(AlmContract.NOTIFICATION_ID)
            }
            AlmContract.ALARM_ACTION -> {
                val ringtoneUri = intent.getStringExtra("ringtone_uri")
                val isRepeating = intent.getBooleanExtra("is_repeating", true)
                
                playAlarm(context, ringtoneUri)
                context.startService(Intent(context, AlarmFloatingService::class.java))

                // 「闹钟时间到了！」通知。内容是 `AlmNotifications` 里原样搬运的
                // `MainActivityAlm.showNotification()`，一个字都没改；渠道 "AlarmChannel"
                // 由 `AlmRepository.createAlarmNotificationChannel()` 创建（首页组合时调用，
                // 能设闹钟就必然已经建过）。
                //
                // 原来这条通知挂在一个**从未 registerReceiver** 的内部 receiver 上，所以
                // 从来没弹出过 —— 这里把它接到真实的闹钟触发点，是本次接通通知的**唯一新增**。
                // 点击通知回到 alm 首页（路由 AlmRoutes.HOME，冷启动直接作为起始页）。
                AlmNotifications.showAlarmTriggered(context)
                
                // 如果是单次（非重复）闹钟，响铃后自动将状态更新为 false
                if (!isRepeating) {
                    // 从 Intent 传入 alarm_id
                    val alarmId = intent.getIntExtra("alarm_id", -1)
                    if (alarmId != -1) {
                        val prefs = context.getSharedPreferences(AlmContract.ALARM_PREFS, Context.MODE_PRIVATE)
                        val alarmsJson = prefs.getString(AlmContract.ALARM_LIST_KEY, "[]")
                        try {
                            val jsonArray = org.json.JSONArray(alarmsJson)
                            for (i in 0 until jsonArray.length()) {
                                val obj = jsonArray.getJSONObject(i)
                                if (obj.getInt("id") == alarmId) {
                                    // 将单次闹钟标记为禁用
                                    obj.put("isEnabled", false)
                                    break
                                }
                            }
                            prefs.edit()
                                .putString(AlmContract.ALARM_LIST_KEY, jsonArray.toString())
                                .apply()
                            // 发送广播通知界面更新状态
                            context.sendBroadcast(Intent(AlmContract.ALARM_STATUS_CHANGED_ACTION))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private fun playAlarm(context: Context, ringtoneUri: String?) {
        // 确保先停止之前的闹钟
        stopAlarm()
        
        try {
            // 获取铃声URI
            val soundUri = if (ringtoneUri != null) {
                Uri.parse(ringtoneUri)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }

            // 创建并配置 MediaPlayer
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, soundUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                setOnPreparedListener { mp ->
                    mp.start()
                }
                prepareAsync()
            }

            // 设置振动
            vibrator = (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrate(VibrationEffect.createWaveform(
                        longArrayOf(0, 1000, 1000),
                        0
                    ))
                } else {
                    @Suppress("DEPRECATION")
                    vibrate(longArrayOf(0, 1000, 1000), 0)
                }
            }

            // 设置超时自动停止（30分钟）
            alarmHandler = Handler(Looper.getMainLooper())
            alarmHandler?.postDelayed({
                stopAlarm()
            }, 30 * 60 * 1000L)

        } catch (e: Exception) {
            e.printStackTrace()
            // 如果播放失败，确保清理资源
            stopAlarm()
        }
    }

    private fun stopAlarm() {
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

        alarmHandler?.removeCallbacksAndMessages(null)
        alarmHandler = null
    }
} 