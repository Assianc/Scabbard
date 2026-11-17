package com.assiance.convert.ui.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.assiance.convert.logic.UnitCategory
import com.assiance.convert.logic.convertValue
import com.assiance.convert.logic.formatNumber
import com.assiance.convert.ui.GLASS_TINT_ALPHA
import com.assiance.convert.ui.glassGrainFor
import com.assiance.convert.ui.rememberConvertGlassState
import io.github.nadeemiqbal.liquidglass.liquidGlass

/**
 * 生活向单位换算：温度 / 长度 / 重量。
 *
 * 输入一个数值并选定单位，下方实时列出同类所有单位的换算结果；每一行都可点击复制。
 * 换算本身是纯函数（见 `logic/UnitConvertTools.kt`），本文件只管状态与展示。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UnitConvertScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var category by rememberSaveable { mutableStateOf(UnitCategory.TEMPERATURE) }
    var inputUnitIndex by rememberSaveable { mutableStateOf(0) }
    var inputText by rememberSaveable { mutableStateOf("1") }

    val units = category.units
    val inputUnit = units.getOrElse(inputUnitIndex) { units.first() }
    val inputValue = inputText.toDoubleOrNull()

    fun copy(label: String, value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("单位换算") },
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
            SectionLabel("类别")
            CategoryChips(
                selected = category,
                onSelect = {
                    category = it
                    inputUnitIndex = 0
                },
            )

            SectionLabel("数值")
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("输入数值") },
                suffix = { Text(inputUnit.symbol) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = inputText.isNotEmpty() && inputValue == null,
            )

            SectionLabel("单位")
            UnitChips(
                units = units,
                selectedIndex = inputUnitIndex,
                onSelect = { inputUnitIndex = it },
            )

            if (inputValue == null) {
                Text(
                    text = "请输入有效数字",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                ResultCard(
                    units = units,
                    convert = { unit -> convertValue(inputValue, inputUnit, unit) },
                    onCopy = ::copy,
                )
            }
        }
    }
}

@Composable
private fun CategoryChips(
    selected: UnitCategory,
    onSelect: (UnitCategory) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        UnitCategory.entries.forEach { category ->
            FilterChip(
                selected = category == selected,
                onClick = { onSelect(category) },
                label = { Text(category.label) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UnitChips(
    units: List<com.assiance.convert.logic.UnitDef>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        units.forEachIndexed { index, unit ->
            FilterChip(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                label = { Text(unit.label) },
            )
        }
    }
}

/**
 * 换算结果卡：每行「单位名 —— 数值」，整行可点复制。
 *
 * 输入单位也列出来（值就是输入值本身），方便用户对照与一起复制。
 */
@Composable
private fun ResultCard(
    units: List<com.assiance.convert.logic.UnitDef>,
    convert: (com.assiance.convert.logic.UnitDef) -> Double,
    onCopy: (String, String) -> Unit,
) {
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
            .padding(vertical = 8.dp),
    ) {
        units.forEach { unit ->
            val text = "${formatNumber(convert(unit))} ${unit.symbol}"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCopy(unit.label, text) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = unit.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(
                    onClick = { onCopy(unit.label, text) },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "复制${unit.label}",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
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
