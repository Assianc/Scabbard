package com.assiance.convert.ui.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.assiance.convert.logic.Rgb
import com.assiance.convert.logic.parseHex
import com.assiance.convert.logic.toHex
import com.assiance.convert.logic.toHslString
import com.assiance.convert.logic.toRgbString
import com.assiance.convert.ui.GLASS_TINT_ALPHA
import com.assiance.convert.ui.glassGrainFor
import com.assiance.convert.ui.rememberConvertGlassState
import io.github.nadeemiqbal.liquidglass.liquidGlass

/**
 * 颜色格式转换：HEX / RGB / HSL。
 *
 * 输入既可以直接填 HEX，也可以分别填 R/G/B；任一输入有效就同步到另一侧，并更新上方预览块。
 * 输入非法时保留上一次的有效颜色、显示错误提示，而不是把预览清空 —— 用户打字过程中
 * （比如把 #FF0000 改成 #FF000）不应该闪一下黑块。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ColorScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var red by rememberSaveable { mutableStateOf(63) }
    var green by rememberSaveable { mutableStateOf(81) }
    var blue by rememberSaveable { mutableStateOf(181) }
    var hexText by rememberSaveable { mutableStateOf("#3F51B5") }
    var redText by rememberSaveable { mutableStateOf("63") }
    var greenText by rememberSaveable { mutableStateOf("81") }
    var blueText by rememberSaveable { mutableStateOf("181") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    val rgb = Rgb(red, green, blue)

    fun copy(label: String, value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
    }

    fun applyHex(text: String) {
        hexText = text
        val parsed = parseHex(text)
        if (parsed == null) {
            error = "HEX 格式应为 #RRGGBB、#RGB 或 #AARRGGBB"
        } else {
            red = parsed.r
            green = parsed.g
            blue = parsed.b
            redText = parsed.r.toString()
            greenText = parsed.g.toString()
            blueText = parsed.b.toString()
            error = null
        }
    }

    fun applyRgbTexts(newRed: String, newGreen: String, newBlue: String) {
        redText = newRed
        greenText = newGreen
        blueText = newBlue
        val r = newRed.toIntOrNull()
        val g = newGreen.toIntOrNull()
        val b = newBlue.toIntOrNull()
        if (r != null && g != null && b != null && r in 0..255 && g in 0..255 && b in 0..255) {
            red = r
            green = g
            blue = b
            hexText = toHex(Rgb(r, g, b))
            error = null
        } else {
            error = "RGB 取值应为 0–255"
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("颜色格式") },
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
            ColorPreview(color = Color(red, green, blue))

            OutlinedTextField(
                value = hexText,
                onValueChange = ::applyHex,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("HEX") },
                isError = error != null,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RgbField(
                    label = "R",
                    value = redText,
                    onValueChange = { applyRgbTexts(it, greenText, blueText) },
                    modifier = Modifier.weight(1f),
                )
                RgbField(
                    label = "G",
                    value = greenText,
                    onValueChange = { applyRgbTexts(redText, it, blueText) },
                    modifier = Modifier.weight(1f),
                )
                RgbField(
                    label = "B",
                    value = blueText,
                    onValueChange = { applyRgbTexts(redText, greenText, it) },
                    modifier = Modifier.weight(1f),
                )
            }

            error?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            ResultCard(
                rgb = rgb,
                onCopy = ::copy,
            )
        }
    }
}

@Composable
private fun RgbField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

/** 颜色预览块：圆角与卡片一致，直接铺当前颜色。 */
@Composable
private fun ColorPreview(color: Color) {
    val shape = CardDefaults.shape
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(shape)
            .background(color),
    )
}

/** 三种格式的结果行，整行可点复制。 */
@Composable
private fun ResultCard(
    rgb: Rgb,
    onCopy: (String, String) -> Unit,
) {
    val shape = CardDefaults.shape
    val glassState = rememberConvertGlassState()
    val rows = listOf(
        "HEX" to toHex(rgb),
        "RGB" to toRgbString(rgb),
        "HSL" to toHslString(rgb),
    )

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
        rows.forEach { (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCopy(label, value) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(
                    onClick = { onCopy(label, value) },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "复制$label",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
