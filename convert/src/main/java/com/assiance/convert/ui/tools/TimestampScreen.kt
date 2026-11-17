package com.assiance.convert.ui.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.assiance.convert.logic.TimestampUnit
import com.assiance.convert.logic.formatTimestampBoth
import com.assiance.convert.logic.localDateTimeMillis
import com.assiance.convert.logic.parseTimestampMillis
import com.assiance.convert.ui.GLASS_TINT_ALPHA
import com.assiance.convert.ui.glassGrainFor
import com.assiance.convert.ui.rememberConvertGlassState
import io.github.nadeemiqbal.liquidglass.liquidGlass
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * 时间戳 ↔ 日期时间，双向。
 *
 * 上半部分是「时间戳 → 日期」：输入数字后自动识别秒/毫秒（也可手动切换），
 * 同时给出本机时区与 UTC 两种表示。
 * 下半部分是「日期 → 时间戳」：选日期与时刻，输出秒/毫秒。
 *
 * 每一行结果都可一键复制。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimestampScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val now = remember { Calendar.getInstance() }

    var input by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf(TimestampUnit.AUTO) }

    var year by rememberSaveable { mutableStateOf(now.get(Calendar.YEAR)) }
    var month by rememberSaveable { mutableStateOf(now.get(Calendar.MONTH) + 1) }
    var day by rememberSaveable { mutableStateOf(now.get(Calendar.DAY_OF_MONTH)) }
    var hour by rememberSaveable { mutableStateOf(now.get(Calendar.HOUR_OF_DAY)) }
    var minute by rememberSaveable { mutableStateOf(now.get(Calendar.MINUTE)) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("timestamp", text))
        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
    }

    val parsedMillis = parseTimestampMillis(input, unit)
    val generatedMillis = localDateTimeMillis(year, month, day, hour, minute)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("时间戳转换") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionLabel("时间戳 → 日期")
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("时间戳") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            ChipRow(
                options = TimestampUnit.entries,
                selected = unit,
                label = { it.label },
                onSelect = { unit = it },
            )

            GlassCard {
                when {
                    input.isBlank() -> Text(
                        text = "输入时间戳，自动识别秒 / 毫秒",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    parsedMillis == null -> Text(
                        text = "请输入有效的时间戳数字",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )

                    else -> {
                        val (local, utc) = formatTimestampBoth(parsedMillis)
                        ResultRow(label = "本地时间", value = local, onCopy = { copyToClipboard(local) })
                        ResultRow(label = "UTC 时间", value = utc, onCopy = { copyToClipboard(utc) })
                    }
                }
            }

            SectionLabel("日期 → 时间戳")
            SettingRow(
                label = "日期",
                value = String.format(Locale.ROOT, "%04d-%02d-%02d", year, month, day),
                onClick = { showDatePicker = true },
            )
            SettingRow(
                label = "时刻",
                value = String.format(Locale.ROOT, "%02d:%02d", hour, minute),
                onClick = { showTimePicker = true },
            )

            GlassCard {
                val secondsText = (generatedMillis / 1000L).toString()
                val millisText = generatedMillis.toString()
                ResultRow(label = "秒", value = secondsText, onCopy = { copyToClipboard(secondsText) })
                ResultRow(label = "毫秒", value = millisText, onCopy = { copyToClipboard(millisText) })
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = utcMillisOf(year, month, day),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                            .apply { timeInMillis = millis }
                        year = cal.get(Calendar.YEAR)
                        month = cal.get(Calendar.MONTH) + 1
                        day = cal.get(Calendar.DAY_OF_MONTH)
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = state)
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialHour = hour,
            initialMinute = minute,
            onDismiss = { showTimePicker = false },
            onConfirm = { h, m ->
                hour = h
                minute = m
                showTimePicker = false
            },
        )
    }
}

// ---------------------------------------------------------------------------- 组件

@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = CardDefaults.shape
    val glassState = rememberConvertGlassState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .liquidGlass(
                state = glassState,
                shape = shape,
                tint = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = GLASS_TINT_ALPHA),
                grain = glassGrainFor(glassState.quality),
            )
            .clip(shape)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        content = content,
    )
}

@Composable
private fun ResultRow(label: String, value: String, onCopy: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = onCopy) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "复制$label",
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

@Composable
private fun SettingRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true,
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "选择时刻",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(16.dp))
                TimePicker(state = state)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("确定") }
                }
            }
        }
    }
}

/** DatePicker 的 selectedDateMillis 是 UTC 午夜，用本地时区构造会偏一天。 */
private fun utcMillisOf(year: Int, month: Int, day: Int): Long =
    Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(year, month - 1, day)
    }.timeInMillis
