package com.assiance.alm.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.NumberPicker
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.assiance.alm.AlmRepository
import com.assiance.alm.TodoData
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 待办设置页（原 `TodoSettingActivity`）。
 *
 * 逻辑与原实现逐条对齐：
 * - 内容必填，空则报错「请输入待办内容」，不保存；
 * - 开始/截止时间用**系统** `DatePickerDialog` + `TimePickerDialog`（与原实现同一套控件），
 *   选择结果仍然走「开始必须早于截止、截止必须晚于开始」的校验与 Toast 文案；
 * - 提醒档位（不提醒 / 立即提醒 / 提前 15、30、60、120 分钟 / 自定义）与按钮文案映射不变；
 *   自定义弹窗仍然是 `NumberPicker(1..999)` + `Spinner(分钟/小时/天)`；
 * - 保存：先取消旧提醒 → 读回原完成状态/创建时间 → 写库 → 未完成才设置提醒 →
 *   提示「待办已添加 / 待办已更新」→ 返回上一页。
 *
 * 与原实现唯一的模型差异：复选框的选中状态直接由「时间是否已设置」推导
 * （原实现是「勾选后弹框，取消再回滚勾选」）。净效果一致：弹框取消 = 未勾选 = 时间为空。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TodoSettingScreen(
    navController: NavController,
    todoId: Int,
    initialTitle: String,
    initialDescription: String,
    initialStartTime: Long,
    initialDueTime: Long,
    initialStartRingtoneUri: String?,
    initialDueRingtoneUri: String?,
) {
    val context = LocalContext.current
    val isEditing = todoId != -1

    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var description by rememberSaveable { mutableStateOf(initialDescription) }
    var descriptionError by remember { mutableStateOf<String?>(null) }

    // 下面这些都是「用户填了但还没保存」的输入，旋转 / 进程重建后要原样留着。
    // 日期用的是 DatePickerDialog + TimePickerDialog（系统弹窗本身不跨重建），值只能自己存。
    var startTime by rememberSaveable { mutableStateOf(initialStartTime.takeIf { it != -1L }) }
    var dueTime by rememberSaveable { mutableStateOf(initialDueTime.takeIf { it != -1L }) }
    var startAdvanceMinutes by rememberSaveable { mutableIntStateOf(0) }
    var dueAdvanceMinutes by rememberSaveable { mutableIntStateOf(0) }
    var startRingtoneUri by rememberSaveable { mutableStateOf(initialStartRingtoneUri) }
    var dueRingtoneUri by rememberSaveable { mutableStateOf(initialDueRingtoneUri) }

    val dateTimeFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    fun clearStart() {
        startTime = null
        startAdvanceMinutes = 0
    }

    fun clearDue() {
        dueTime = null
        dueAdvanceMinutes = 0
    }

    // 铃声名字（与原实现一样：取不到就用「默认铃声」，保留原 URI）
    fun resolveRingtoneName(uri: String?): String {
        if (uri == null) return "默认铃声"
        return try {
            RingtoneManager.getRingtone(context, Uri.parse(uri)).getTitle(context)
        } catch (e: Exception) {
            "默认铃声"
        }
    }

    val startRingtoneName = remember(startRingtoneUri) { resolveRingtoneName(startRingtoneUri) }
    val dueRingtoneName = remember(dueRingtoneUri) { resolveRingtoneName(dueRingtoneUri) }

    // 提醒选择弹窗 / 自定义弹窗
    var reminderDialogTarget by remember { mutableStateOf<ReminderTarget?>(null) }
    var customReminderTarget by remember { mutableStateOf<ReminderTarget?>(null) }

    // 铃声选择（仍然是外部系统 UI）。
    // 原实现用两个 requestCode（1001 / 1002）区分开始与截止；新的 ActivityResult API 不带
    // requestCode，而且铃声选择器不会回传自定义 extra，所以这里用两个独立 launcher 对应原来的两个 requestCode。
    fun ringtonePickerIntent(existing: String?): Intent =
        Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "选择提醒铃声")
            putExtra(
                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                existing?.let { Uri.parse(it) },
            )
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        }

    fun pickedRingtoneUri(result: androidx.activity.result.ActivityResult): Uri? =
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        } else {
            null
        }

    val startRingtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            startRingtoneUri = pickedRingtoneUri(result)?.toString()
        }
    }

    val dueRingtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            dueRingtoneUri = pickedRingtoneUri(result)?.toString()
        }
    }

    // ---------------------------------------------------------------- 提醒选择弹窗
    reminderDialogTarget?.let { target ->
        val currentMinutes =
            if (target == ReminderTarget.START) startAdvanceMinutes else dueAdvanceMinutes
        AlertDialog(
            onDismissRequest = { reminderDialogTarget = null },
            title = {
                Text(if (target == ReminderTarget.START) "设置开始时间提醒" else "设置截止时间提醒")
            },
            text = {
                Column {
                    REMINDER_OPTIONS.forEachIndexed { index, label ->
                        val selected = selectionIndex(currentMinutes) == index
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(selected = selected, onClick = {
                                    if (index == REMINDER_OPTIONS.lastIndex) {
                                        reminderDialogTarget = null
                                        customReminderTarget = target
                                    } else {
                                        val minutes = optionMinutes(index)
                                        if (target == ReminderTarget.START) {
                                            startAdvanceMinutes = minutes
                                        } else {
                                            dueAdvanceMinutes = minutes
                                        }
                                        reminderDialogTarget = null
                                    }
                                })
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = selected, onClick = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { reminderDialogTarget = null }) { Text("取消") }
            },
        )
    }

    // ---------------------------------------------------------------- 自定义提醒弹窗
    customReminderTarget?.let { target ->
        val currentMinutes =
            if (target == ReminderTarget.START) startAdvanceMinutes else dueAdvanceMinutes
        val initialNumber = when {
            currentMinutes >= 1440 -> currentMinutes / 1440
            currentMinutes >= 60 -> currentMinutes / 60
            else -> currentMinutes.coerceAtLeast(1)
        }
        val initialUnitIndex = when {
            currentMinutes >= 1440 -> 2
            currentMinutes >= 60 -> 1
            else -> 0
        }

        var numberValue by remember(target) { mutableIntStateOf(initialNumber) }
        var unitIndex by remember(target) { mutableIntStateOf(initialUnitIndex) }

        AlertDialog(
            onDismissRequest = { customReminderTarget = null },
            title = {
                Text(
                    if (target == ReminderTarget.START) "自定义开始时间提醒" else "自定义截止时间提醒"
                )
            },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AndroidView(
                        factory = { ctx ->
                            NumberPicker(ctx).apply {
                                minValue = 1
                                maxValue = 999
                                value = initialNumber
                                setOnValueChangedListener { _, _, newValue ->
                                    numberValue = newValue
                                }
                            }
                        },
                        update = { picker ->
                            if (picker.value != numberValue) picker.value = numberValue
                        },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AndroidView(
                        factory = { ctx ->
                            Spinner(ctx).apply {
                                adapter = ArrayAdapter(
                                    ctx,
                                    android.R.layout.simple_spinner_item,
                                    REMINDER_UNITS,
                                ).apply {
                                    setDropDownViewResource(
                                        android.R.layout.simple_spinner_dropdown_item
                                    )
                                }
                                setSelection(initialUnitIndex)
                                onItemSelectedListener = object :
                                    AdapterView.OnItemSelectedListener {
                                    override fun onItemSelected(
                                        parent: AdapterView<*>?,
                                        view: View?,
                                        position: Int,
                                        id: Long,
                                    ) {
                                        unitIndex = position
                                    }

                                    override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                                }
                            }
                        },
                        update = { spinner ->
                            if (spinner.selectedItemPosition != unitIndex) {
                                spinner.setSelection(unitIndex)
                            }
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val multiplier = when (unitIndex) {
                        0 -> 1
                        1 -> 60
                        2 -> 60 * 24
                        else -> 1
                    }
                    val minutes = numberValue * multiplier
                    if (target == ReminderTarget.START) {
                        startAdvanceMinutes = minutes
                    } else {
                        dueAdvanceMinutes = minutes
                    }
                    customReminderTarget = null
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { customReminderTarget = null }) { Text("取消") }
            },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "编辑待办" else "添加待办") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
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
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("事件标题（选填）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    if (descriptionError != null) descriptionError = null
                },
                label = { Text("内容（必填）") },
                isError = descriptionError != null,
                supportingText = descriptionError?.let { message -> { Text(message) } },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ---------------- 开始时间
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = startTime != null,
                    onCheckedChange = { checked ->
                        if (checked) {
                            showDateTimePicker(
                                context = context,
                                initial = null,
                                onPicked = { selected ->
                                    if (dueTime != null && selected >= dueTime!!) {
                                        Toast.makeText(
                                            context,
                                            "开始时间必须早于截止时间",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        clearStart()
                                    } else {
                                        startTime = selected
                                    }
                                },
                                onCancelled = { if (startTime == null) clearStart() },
                            )
                        } else {
                            clearStart()
                        }
                    },
                )
                Text(text = "事件开始时间")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = startTime?.let { dateTimeFormatter.format(it) } ?: "未设置",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = startTime != null) {
                            showDateTimePicker(
                                context = context,
                                initial = startTime,
                                onPicked = { selected ->
                                    if (dueTime != null && selected >= dueTime!!) {
                                        Toast.makeText(
                                            context,
                                            "开始时间必须早于截止时间",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        clearStart()
                                    } else {
                                        startTime = selected
                                    }
                                },
                                onCancelled = { if (startTime == null) clearStart() },
                            )
                        }
                        .padding(4.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ---------------- 开始提醒 / 开始铃声
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { reminderDialogTarget = ReminderTarget.START },
                    enabled = startTime != null,
                ) {
                    Text(reminderLabel(startAdvanceMinutes))
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = startRingtoneName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        startRingtoneLauncher.launch(ringtonePickerIntent(startRingtoneUri))
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = "选择开始提醒铃声",
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---------------- 截止时间
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = dueTime != null,
                    onCheckedChange = { checked ->
                        if (checked) {
                            showDateTimePicker(
                                context = context,
                                initial = null,
                                onPicked = { selected ->
                                    if (startTime != null && selected <= startTime!!) {
                                        Toast.makeText(
                                            context,
                                            "截止时间必须晚于开始时间",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        clearDue()
                                    } else {
                                        dueTime = selected
                                    }
                                },
                                onCancelled = { if (dueTime == null) clearDue() },
                            )
                        } else {
                            clearDue()
                        }
                    },
                )
                Text(text = "事件截止时间")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dueTime?.let { dateTimeFormatter.format(it) } ?: "未设置",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = dueTime != null) {
                            showDateTimePicker(
                                context = context,
                                initial = dueTime,
                                onPicked = { selected ->
                                    if (startTime != null && selected <= startTime!!) {
                                        Toast.makeText(
                                            context,
                                            "截止时间必须晚于开始时间",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        clearDue()
                                    } else {
                                        dueTime = selected
                                    }
                                },
                                onCancelled = { if (dueTime == null) clearDue() },
                            )
                        }
                        .padding(4.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ---------------- 截止提醒 / 截止铃声
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { reminderDialogTarget = ReminderTarget.DUE },
                    enabled = dueTime != null,
                ) {
                    Text(reminderLabel(dueAdvanceMinutes))
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = dueRingtoneName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        dueRingtoneLauncher.launch(ringtonePickerIntent(dueRingtoneUri))
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = "选择截止提醒铃声",
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    // ---- 原 TodoSettingActivity.saveTodo() ----
                    val trimmedTitle = title.trim()
                    val trimmedDescription = description.trim()

                    if (trimmedDescription.isEmpty()) {
                        descriptionError = "请输入待办内容"
                        return@Button
                    }

                    // 取消旧的提醒（如果存在）
                    if (todoId != -1) {
                        AlmRepository.cancelTodoReminder(context, todoId)
                    }

                    // 读回原有待办的完成状态与时间
                    val snapshot = AlmRepository.readTodoSnapshot(context, todoId)

                    val newTodoId =
                        if (isEditing) todoId else System.currentTimeMillis().toInt()

                    val todo = TodoData(
                        id = newTodoId,
                        title = trimmedTitle,
                        description = trimmedDescription,
                        startTime = startTime,
                        dueTime = dueTime,
                        isCompleted = snapshot.isCompleted,
                        startRingtoneUri = startRingtoneUri,
                        dueRingtoneUri = dueRingtoneUri,
                        completedTime = snapshot.completedTime,
                        createdTime = if (!isEditing) {
                            System.currentTimeMillis()
                        } else {
                            snapshot.createdTime
                        },
                    )

                    AlmRepository.upsertTodo(context, todo, isEditing = isEditing)

                    // 设置提醒（已完成的待办内部会直接返回）
                    AlmRepository.setTodoReminder(
                        context = context,
                        todo = todo,
                        startAdvanceMinutes = startAdvanceMinutes,
                        dueAdvanceMinutes = dueAdvanceMinutes,
                    )

                    val message = if (!isEditing) "待办已添加" else "待办已更新"
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("保存")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private enum class ReminderTarget { START, DUE }


private val REMINDER_OPTIONS = arrayOf(
    "不提醒",
    "立即提醒",
    "提前15分钟",
    "提前30分钟",
    "提前1小时",
    "提前2小时",
    "自定义...",
)

private val REMINDER_UNITS = arrayOf("分钟", "小时", "天")

private fun optionMinutes(index: Int): Int = when (index) {
    0 -> -1
    1 -> 0
    2 -> 15
    3 -> 30
    4 -> 60
    5 -> 120
    else -> 0
}

/** 原 `showReminderDialog()` 里 `currentSelection` 的反向映射。 */
private fun selectionIndex(minutes: Int): Int = when (minutes) {
    -1 -> 0
    0 -> 1
    15 -> 2
    30 -> 3
    60 -> 4
    120 -> 5
    else -> if (minutes > 0) 6 else 0
}

/** 原 `updateReminderButtonText()` 的文案映射。 */
private fun reminderLabel(minutes: Int): String = when (minutes) {
    -1 -> "不提醒"
    0 -> "立即提醒"
    15 -> "提前15分钟提醒"
    30 -> "提前30分钟提醒"
    60 -> "提前1小时提醒"
    120 -> "提前2小时提醒"
    else -> when {
        minutes >= 1440 -> "提前${minutes / 1440}天提醒"
        minutes >= 60 -> "提前${minutes / 60}小时提醒"
        else -> "提前${minutes}分钟提醒"
    }
}

/**
 * 原 `showDateTimePicker()`：先弹系统日期选择，选完再弹系统时间选择。
 * 两个弹窗的取消都会走 [onCancelled]（原实现里就是「取消则回滚勾选」）。
 */
private fun showDateTimePicker(
    context: Context,
    initial: Long?,
    onPicked: (Long) -> Unit,
    onCancelled: () -> Unit,
) {
    val currentDateTime = Calendar.getInstance().apply {
        if (initial != null) timeInMillis = initial
    }

    val dateDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                    }
                    onPicked(calendar.timeInMillis)
                },
                currentDateTime.get(Calendar.HOUR_OF_DAY),
                currentDateTime.get(Calendar.MINUTE),
                true,
            ).apply {
                setOnCancelListener { onCancelled() }
            }.show()
        },
        currentDateTime.get(Calendar.YEAR),
        currentDateTime.get(Calendar.MONTH),
        currentDateTime.get(Calendar.DAY_OF_MONTH),
    )
    dateDialog.setOnCancelListener { onCancelled() }
    dateDialog.show()
}
