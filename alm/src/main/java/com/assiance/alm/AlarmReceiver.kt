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
            MainActivityAlm.ALARM_STOP_ACTION -> {
                stopAlarm()
            }
            MainActivityAlm.ALARM_ACTION -> {
                val ringtoneUri = intent.getStringExtra("ringtone_uri")
                val isRepeating = intent.getBooleanExtra("is_repeating", true)
                
                playAlarm(context, ringtoneUri)
                context.startService(Intent(context, AlarmFloatingService::class.java))
                
                // 如果是非重复闹钟，响铃后自动禁用
                if (!isRepeating) {
                    // 获取闹钟ID并更新状态
                    val alarmId = intent.getIntExtra("alarm_id", -1)
                    if (alarmId != -1) {
                        val prefs = context.getSharedPreferences(
                            MainActivityAlm.ALARM_PREFS, 
                            Context.MODE_PRIVATE
                        )
                        val alarmsJson = prefs.getString(MainActivityAlm.ALARM_LIST_KEY, "[]")
                        val jsonArray = org.json.JSONArray(alarmsJson)
                        
                        // 查找并更新闹钟状态
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            if (obj.getInt("id") == alarmId) {
                                obj.put("isEnabled", false)
                                break
                            }
                        }
                        
                        // 保存更新后的闹钟列表
                        prefs.edit()
                            .putString(MainActivityAlm.ALARM_LIST_KEY, jsonArray.toString())
                            .apply()
                        
                        // 发送广播通知主界面更新
                        context.sendBroadcast(
                            Intent(MainActivityAlm.ALARM_STATUS_CHANGED_ACTION)
                        )
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