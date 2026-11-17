package com.assiance.convert.ui.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.unit.dp
import com.assiance.convert.logic.daysBetween
import com.assiance.convert.ui.GLASS_TINT_ALPHA
import com.assiance.convert.ui.glassGrainFor
import com.assiance.convert.ui.rememberConvertGlassState
import io.github.nadeemiqbal.liquidglass.liquidGlass
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/**
 * 日期差计算：选起止日期，输出相差的日历日数。
 *
 * 按日历日计算、忽略时分秒 —— 用「公历日 → 连续天数」的纯算术换算，
 * 不受夏令时 23/25 小时的影响（见 [daysBetween]）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateDiffScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val today = remember { Calendar.getInstance() }
    val defaultEnd = remember {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 7) }
    }

    var startYear by rememberSaveable { mutableStateOf(today.get(Calendar.YEAR)) }
    var startMonth by rememberSaveable { mutableStateOf(today.get(Calendar.MONTH) + 1) }
    var startDay by rememberSaveable { mutableStateOf(today.get(Calendar.DAY_OF_MONTH)) }

    var endYear by rememberSaveable { mutableStateOf(defaultEnd.get(Calendar.YEAR)) }
    var endMonth by rememberSaveable { mutableStateOf(defaultEnd.get(Calendar.MONTH) + 1) }
    var endDay by rememberSaveable { mutableStateOf(defaultEnd.get(Calendar.DAY_OF_MONTH)) }

    var pickerTarget by remember { mutableStateOf<DateTarget?>(null) }

    val diff = daysBetween(startYear, startMonth, startDay, endYear, endMonth, endDay)
    val description = diffDescription(diff)

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("date_diff", text))
        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("日期差计算") },
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
            SettingRow(
                label = "起始日期",
                value = formatDate(startYear, startMonth, startDay),
                onClick = { pickerTarget = DateTarget.START },
            )
            SettingRow(
                label = "结束日期",
                value = formatDate(endYear, endMonth, endDay),
                onClick = { pickerTarget = DateTarget.END },
            )

            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${abs(diff)} 天",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { copyToClipboard(description) }) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "复制结果",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Text(
                text = "按日历日计算，忽略时分秒",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    pickerTarget?.let { target ->
        val initial = if (target == DateTarget.START) {
            Triple(startYear, startMonth, startDay)
        } else {
            Triple(endYear, endMonth, endDay)
        }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = utcMillisOf(initial.first, initial.second, initial.third),
        )
        DatePickerDialog(
            onDismissRequest = { pickerTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                            .apply { timeInMillis = millis }
                        val y = cal.get(Calendar.YEAR)
                        val m = cal.get(Calendar.MONTH) + 1
                        val d = cal.get(Calendar.DAY_OF_MONTH)
                        when (target) {
                            DateTarget.START -> {
                                startYear = y
                                startMonth = m
                                startDay = d
                            }

                            DateTarget.END -> {
                                endYear = y
                                endMonth = m
                                endDay = d
                            }
                        }
                    }
                    pickerTarget = null
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { pickerTarget = null }) { Text("取消") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

private enum class DateTarget { START, END }

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

/** 结束日期晚于起始日期为正；同一天为 0。 */
private fun diffDescription(diff: Long): String = when {
    diff > 0 -> "起始日期比结束日期早 $diff 天"
    diff < 0 -> "起始日期比结束日期晚 ${-diff} 天"
    else -> "两个日期是同一天"
}

private fun formatDate(year: Int, month: Int, day: Int): String =
    String.format(Locale.ROOT, "%04d-%02d-%02d", year, month, day)

private fun utcMillisOf(year: Int, month: Int, day: Int): Long =
    Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(year, month - 1, day)
    }.timeInMillis
