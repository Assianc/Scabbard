package com.assiance.alm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TimePicker
import android.widget.Toast
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class AlarmSettingActivity : AppCompatActivity() {
    private lateinit var timePicker: TimePicker
    private lateinit var alarmManager: AlarmManager
    private var alarmId: Int = -1  // -1 表示新建闹钟
    private var selectedRingtoneUri: String? = null
    private lateinit var ringtoneButton: ImageButton
    private lateinit var ringtoneName: TextView

    companion object {
        private const val RINGTONE_PICKER_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_setting)

        // 设置工具栏
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "设置闹钟"

        // 初始化所有视图
        timePicker = findViewById(R.id.timePicker)
        ringtoneButton = findViewById(R.id.ringtoneButton)
        ringtoneName = findViewById(R.id.ringtoneName)
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 设置24小时制
        timePicker.setIs24HourView(true)

        findViewById<android.widget.Button>(R.id.setAlarmButton).setOnClickListener {
            setAlarm()
        }

        ringtoneButton.setOnClickListener {
            openRingtonePicker()
        }

        // 获取传入的闹钟信息
        alarmId = intent.getIntExtra("alarm_id", -1)
        val alarmTime = intent.getLongExtra("alarm_time", -1L)
        selectedRingtoneUri = intent.getStringExtra("ringtone_uri")
        
        if (alarmTime != -1L) {
            // 设置时间选择器的初始值
            val calendar = Calendar.getInstance().apply {
                timeInMillis = alarmTime
            }
            timePicker.hour = calendar.get(Calendar.HOUR_OF_DAY)
            timePicker.minute = calendar.get(Calendar.MINUTE)
            
            // 更新标题
            supportActionBar?.title = "编辑闹钟"
        }

        // 更新铃声名称显示
        updateRingtoneText()
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

        // 使用传入的ID或生成新ID
        val newAlarmId = if (alarmId != -1) alarmId else System.currentTimeMillis().toInt()

        val alarm = AlarmData(
            id = newAlarmId,
            timeInMillis = calendar.timeInMillis,
            ringtoneUri = selectedRingtoneUri
        )

        // 更新闹钟列表
        val prefs = getSharedPreferences(MainActivityAlm.ALARM_PREFS, Context.MODE_PRIVATE)
        val alarmsJson = prefs.getString(MainActivityAlm.ALARM_LIST_KEY, "[]")
        val jsonArray = org.json.JSONArray(alarmsJson)
        
        // 如果是编辑现有闹钟，先删除旧的
        if (alarmId != -1) {
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.getInt("id") == alarmId) {
                    jsonArray.remove(i)
                    break
                }
            }
        }

        // 添加新的闹钟数据
        jsonArray.put(org.json.JSONObject().apply {
            put("id", alarm.id)
            put("timeInMillis", alarm.timeInMillis)
            put("isEnabled", alarm.isEnabled)
            put("ringtoneUri", alarm.ringtoneUri)
        })
        
        prefs.edit()
            .putString(MainActivityAlm.ALARM_LIST_KEY, jsonArray.toString())
            .apply()

        // 设置闹钟
        val intent = Intent(MainActivityAlm.ALARM_ACTION).apply {
            `package` = packageName
            putExtra("ringtone_uri", selectedRingtoneUri)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent),
            pendingIntent
        )

        // 立即发送广播通知主界面更新
        sendBroadcast(Intent(MainActivityAlm.ALARM_STATUS_CHANGED_ACTION))
        Toast.makeText(this, if (alarmId != -1) "闹钟已更新" else "闹钟已设置", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun openRingtonePicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "选择闹钟铃声")
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, 
                selectedRingtoneUri?.let { Uri.parse(it) })
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        }
        startActivityForResult(intent, RINGTONE_PICKER_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RINGTONE_PICKER_REQUEST && resultCode == RESULT_OK) {
            val uri = data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            selectedRingtoneUri = uri?.toString()
            updateRingtoneText()
        }
    }

    private fun updateRingtoneText() {
        if (selectedRingtoneUri == null) {
            ringtoneName.text = "默认铃声"
            return
        }
        
        try {
            val ringtone = RingtoneManager.getRingtone(this, Uri.parse(selectedRingtoneUri))
            ringtoneName.text = ringtone.getTitle(this)
        } catch (e: Exception) {
            ringtoneName.text = "默认铃声"
            selectedRingtoneUri = null
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 