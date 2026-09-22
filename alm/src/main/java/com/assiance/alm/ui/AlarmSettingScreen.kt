package com.assiance.alm.ui

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.assiance.alm.AlarmData
import com.assiance.alm.AlmContract
import com.assiance.alm.AlmRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 闹钟设置页（原 `AlarmSettingActivity`）。
 *
 * 逻辑与原实现逐条对齐：
 * - 重复日期：默认 7 天全选；编辑模式（`alarmId != -1`）从 SharedPreferences 里读回；
 * - 时间：24 小时制；`alarmTime != -1` 时用传入的时间初始化，并且标题变成「编辑闹钟」；
 * - 铃声：`RingtoneManager.ACTION_RINGTONE_PICKER`（**外部系统 UI**，仍然用
 *   `startActivityForResult` 契约拉起，不是模块内跳转）；名字取不到时回落到「默认铃声」；
 * - 保存：算出时间 → 写 SharedPreferences（编辑时先删旧记录）→ 设置下一次精确闹钟 →
 *   发 `ALARM_STATUS_CHANGED` 广播 → 提示 → 返回上一页。提示文案、Toast 时长都不变。
 *
 * 界面用 M3 组件：原有 `TimePicker` 换成 M3 的 [TimePicker]（同样是 24 小时制表盘，
 * 可切换为键盘输入），Toolbar 换成 [TopAppBar]，CheckBox 保留（只是排布方式改成
 * 7 等分 + 下方文字，仍然是同样的 `周一`…`周日` 与默认全选）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlarmSettingScreen(
    navController: NavController,
    alarmId: Int,
    alarmTime: Long,
    ringtoneUri: String?,
) {
    val context = LocalContext.current
    val isEditing = alarmId != -1

    val initialTime = remember {
        if (alarmTime != -1L) {
            Calendar.getInstance().apply { timeInMillis = alarmTime }
        } else {
            Calendar.getInstance()
        }
    }
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.get(Calendar.HOUR_OF_DAY),
        initialMinute = initialTime.get(Calendar.MINUTE),
        is24Hour = true,
    )

    // 重复日期：默认每天；编辑模式读回
    var repeatDays by remember { mutableStateOf(BooleanArray(7) { true }) }
    LaunchedEffect(alarmId) {
        if (alarmId != -1) {
            AlmRepository.loadRepeatDays(context, alarmId)?.let { repeatDays = it }
        }
    }

    // 铃声
    var selectedRingtoneUri by remember { mutableStateOf(ringtoneUri) }
    var ringtoneName by remember { mutableStateOf("默认铃声") }

    fun updateRingtoneText() {
        val uri = selectedRingtoneUri
        if (uri == null) {
            ringtoneName = "默认铃声"
            return
        }
        try {
            val ringtone = RingtoneManager.getRingtone(context, Uri.parse(uri))
            ringtoneName = ringtone.getTitle(context)
        } catch (e: Exception) {
            e.printStackTrace()
            ringtoneName = "默认铃声"
            selectedRingtoneUri = null
        }
    }

    LaunchedEffect(Unit) { updateRingtoneText() }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            selectedRingtoneUri = uri?.toString()
            updateRingtoneText()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (alarmTime != -1L) "编辑闹钟" else "设置闹钟") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TimePicker(state = timePickerState)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                DAY_NAMES.forEachIndexed { index, dayName ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                repeatDays = repeatDays.copyOf().also { it[index] = !it[index] }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Checkbox(
                            checked = repeatDays[index],
                            onCheckedChange = null,
                        )
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "选择闹钟铃声")
                            putExtra(
                                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                selectedRingtoneUri?.let { Uri.parse(it) },
                            )
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        }
                        ringtonePickerLauncher.launch(intent)
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "选择铃声",
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = ringtoneName,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    // ---- 原 AlarmSettingActivity.setAlarm() ----
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)

                        // 如果时间已过，且没有设置重复，则设置为明天
                        if (timeInMillis <= System.currentTimeMillis() && !repeatDays.any { it }) {
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                    }

                    val newAlarmId = if (isEditing) alarmId else System.currentTimeMillis().toInt()

                    val alarm = AlarmData(
                        id = newAlarmId,
                        timeInMillis = calendar.timeInMillis,
                        ringtoneUri = selectedRingtoneUri,
                        repeatDays = repeatDays,
                    )

                    AlmRepository.upsertAlarm(context, alarm, isEditing = isEditing)

                    // 设置下一次闹钟（内部自己会 Toast 一次，与原实现一样会有两次提示）
                    AlmRepository.setNextAlarmFromSetting(context, alarm)

                    // 发送广播通知主界面更新
                    context.sendBroadcast(Intent(AlmContract.ALARM_STATUS_CHANGED_ACTION))

                    val message = if (alarm.repeatDays.any { it }) {
                        "已设置重复闹钟"
                    } else {
                        val dateFormat = SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault())
                        "已设置单次闹钟：${dateFormat.format(calendar.time)}"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("设置闹钟")
            }
        }
    }
}

private val DAY_NAMES = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
