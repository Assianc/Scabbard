package com.assiance.alm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TimePicker
import android.widget.Toast
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

class AlarmSettingActivity : AppCompatActivity() {
    private lateinit var timePicker: TimePicker
    private lateinit var alarmManager: AlarmManager
    private var alarmId: Int = -1  // -1 表示新建闹钟
    private var selectedRingtoneUri: String? = null
    private lateinit var ringtoneButton: ImageButton
    private lateinit var ringtoneName: TextView
    private lateinit var repeatDaysContainer: LinearLayout
    private val repeatDays = BooleanArray(7) { true } // 默认每天重复
    private val dayNames = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

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

        findViewById<Button>(R.id.setAlarmButton).setOnClickListener {
            setAlarm()
        }

        ringtoneButton.setOnClickListener {
            openRingtonePicker()
        }

        // 初始化重复日期选择（使用 repeatDays 数组的初始值，全选）
        repeatDaysContainer = findViewById(R.id.repeatDaysContainer)
        initRepeatDays()

        // 根据编辑状态加载当前闹钟的重复天数设置
        alarmId = intent.getIntExtra("alarm_id", -1)
        if (alarmId != -1) {
            // 编辑模式：从 SharedPreferences 中加载当前闹钟数据
            val prefs = getSharedPreferences(MainActivityAlm.ALARM_PREFS, Context.MODE_PRIVATE)
            val alarmsJson = prefs.getString(MainActivityAlm.ALARM_LIST_KEY, "[]")
            try {
                val jsonArray = org.json.JSONArray(alarmsJson)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    if (obj.getInt("id") == alarmId) {
                        val jsonRepeat = obj.optJSONArray("repeatDays")
                        if (jsonRepeat != null && jsonRepeat.length() == 7) {
                            for (j in 0 until 7) {
                                repeatDays[j] = jsonRepeat.getBoolean(j)
                            }
                            updateRepeatDaysUI() // 更新 UI
                        }
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // 若不在编辑模式则保持默认全选状态

        // 获取传入的闹钟信息（时间、铃声等）
        val alarmTime = intent.getLongExtra("alarm_time", -1L)
        selectedRingtoneUri = intent.getStringExtra("ringtone_uri")
        if (alarmTime != -1L) {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = alarmTime
            }
            timePicker.hour = calendar.get(Calendar.HOUR_OF_DAY)
            timePicker.minute = calendar.get(Calendar.MINUTE)
            supportActionBar?.title = "编辑闹钟"
        }
        updateRingtoneText()
    }

    private fun initRepeatDays() {
        repeatDaysContainer.removeAllViews() // 先清除所有已有的视图
        
        for (i in 0..6) {
            val checkBox = CheckBox(this).apply {
                text = dayNames[i]
                isChecked = repeatDays[i]
                setOnCheckedChangeListener { _, isChecked ->
                    repeatDays[i] = isChecked
                }
                // 设置 CheckBox 的布局参数，使其均匀分布
                val params = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f
                )
                params.setMargins(4, 0, 4, 0)
                layoutParams = params
            }
            repeatDaysContainer.addView(checkBox)
        }
    }

    private fun updateRepeatDaysUI() {
        for (i in 0..6) {
            val checkBox = repeatDaysContainer.getChildAt(i) as? CheckBox
            checkBox?.isChecked = repeatDays[i]
        }
    }

    private fun setAlarm() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, timePicker.hour)
            set(Calendar.MINUTE, timePicker.minute)
            set(Calendar.SECOND, 0)
            
            // 如果时间已过，且没有设置重复，则设置为明天
            if (timeInMillis <= System.currentTimeMillis() && !repeatDays.any { it }) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val newAlarmId = if (alarmId != -1) alarmId else System.currentTimeMillis().toInt()

        val alarm = AlarmData(
            id = newAlarmId,
            timeInMillis = calendar.timeInMillis,
            ringtoneUri = selectedRingtoneUri,
            repeatDays = repeatDays
        )

        // 保存到 SharedPreferences
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
            put("repeatDays", org.json.JSONArray(alarm.repeatDays.toList()))
        })
        
        prefs.edit()
            .putString(MainActivityAlm.ALARM_LIST_KEY, jsonArray.toString())
            .apply()

        // 设置下一次闹钟
        setNextAlarm(alarm)

        // 发送广播通知主界面更新
        sendBroadcast(Intent(MainActivityAlm.ALARM_STATUS_CHANGED_ACTION))
        
        // 显示设置成功提示
        val message = if (alarm.repeatDays.any { it }) {
            "已设置重复闹钟"
        } else {
            val dateFormat = SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault())
            "已设置单次闹钟：${dateFormat.format(calendar.time)}"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        
        finish()
    }

    private fun setNextAlarm(alarm: AlarmData) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = alarm.timeInMillis
        }
        
        // 检查是否有任何重复日期被选中
        val hasRepeatDays = alarm.repeatDays.any { it }
        
        if (!hasRepeatDays) {
            // 如果没有选择重复日期，只响铃一次
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        } else {
            // 找到下一个需要响铃的日期
            var daysToAdd = 0
            val today = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 转换为周一为0的索引
            
            for (i in 0..7) {
                val checkDay = (today + i) % 7
                if (alarm.repeatDays[checkDay]) {
                    daysToAdd = i
                    break
                }
            }
            calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
        }

        val intent = Intent(MainActivityAlm.ALARM_ACTION).apply {
            `package` = packageName
            putExtra("ringtone_uri", alarm.ringtoneUri)
            putExtra("is_repeating", hasRepeatDays) // 添加是否重复的标志
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

        // 显示提示信息
        val message = if (hasRepeatDays) {
            "已设置重复闹钟"
        } else {
            val dateFormat = SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault())
            "已设置单次闹钟：${dateFormat.format(calendar.time)}"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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