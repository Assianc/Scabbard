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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.assiance.convert.logic.StorageBase
import com.assiance.convert.logic.StorageUnit
import com.assiance.convert.logic.convertStorage
import com.assiance.convert.logic.formatStorageNumber
import com.assiance.convert.ui.GLASS_TINT_ALPHA
import com.assiance.convert.ui.glassGrainFor
import com.assiance.convert.ui.rememberConvertGlassState
import io.github.nadeemiqbal.liquidglass.liquidGlass

/**
 * 存储单位换算：输入数值 + 单位，同时给出全部单位的结果，支持 1024 / 1000 两种口径。
 *
 * 结果行点击即复制（带单位后缀），避免用户再手动拼单位。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun StorageUnitScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("1") }
    var fromUnit by rememberSaveable { mutableStateOf(StorageUnit.GB) }
    var base by rememberSaveable { mutableStateOf(StorageBase.BINARY) }

    val value = remember(input) { input.trim().toBigDecimalOrNull() }
    val results = remember(value, fromUnit, base) {
        value?.let { convertStorage(it, fromUnit, base.value) }
    }

    fun copy(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("存储单位", text))
        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("存储单位") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
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
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("数值") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel("当前单位")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StorageUnit.entries.forEach { unit ->
                    FilterChip(
                        selected = unit == fromUnit,
                        onClick = { fromUnit = unit },
                        label = { Text(unit.label) },
                    )
                }
            }

            SectionLabel("换算口径")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StorageBase.entries.forEach { option ->
                    FilterChip(
                        selected = option == base,
                        onClick = { base = option },
                        label = { Text(option.label) },
                    )
                }
            }
            Text(
                text = if (base == StorageBase.BINARY) {
                    "1 KB = 1024 B（KiB 系，沿用 KB 叫法）"
                } else {
                    "1 KB = 1000 B（十进口径）"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (results == null) {
                Text(
                    text = "请输入有效的数字",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                GlassCard {
                    StorageUnit.entries.forEachIndexed { index, unit ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            )
                        }
                        val text = "${formatStorageNumber(results.getValue(unit))} ${unit.label}"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { copy(text) }
                                .padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = unit.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = formatStorageNumber(results.getValue(unit)),
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "复制 ${unit.label}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
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

/** 本页的玻璃卡容器：材质参数与模块内其他玻璃面一致。 */
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
                tint = MaterialTheme.colorScheme.surfaceContainer
                    .copy(alpha = GLASS_TINT_ALPHA),
                grain = glassGrainFor(glassState.quality),
            )
            .clip(shape)
            .padding(horizontal = 20.dp, vertical = 4.dp),
        content = content,
    )
}
