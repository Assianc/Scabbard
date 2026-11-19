package com.assiance.convert.ui.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.assiance.convert.logic.Hsv
import com.assiance.convert.logic.Rgb
import com.assiance.convert.logic.fromHsv
import com.assiance.convert.logic.parseHex
import com.assiance.convert.logic.toHex
import com.assiance.convert.logic.toHslString
import com.assiance.convert.logic.toHsv
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

    // 取色器用 HSV 作为交互状态：色相滑块只改 h，饱和度/明度面板只改 s/v。
    // 用 RGB 当取色器状态的话，拖到纯黑后色相信息就丢了，滑块会跳回红色。
    val initialHsv = remember { toHsv(DEFAULT_RGB) }
    var hue by rememberSaveable { mutableStateOf(initialHsv.h) }
    var saturation by rememberSaveable { mutableStateOf(initialHsv.s) }
    var value by rememberSaveable { mutableStateOf(initialHsv.v) }

    val rgb = Rgb(red, green, blue)

    fun copy(label: String, value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
    }

    /** 把一个确定的 RGB 同步到全部状态（含取色器 HSV）。 */
    fun syncFromRgb(target: Rgb) {
        red = target.r
        green = target.g
        blue = target.b
        redText = target.r.toString()
        greenText = target.g.toString()
        blueText = target.b.toString()
        hexText = toHex(target)
        val hsv = toHsv(target)
        hue = hsv.h
        saturation = hsv.s
        value = hsv.v
        error = null
    }

    /** 拖动取色器：从 HSV 反推 RGB 与各文本表示。 */
    fun applyHsv(newHue: Float, newSaturation: Float, newValue: Float) {
        hue = newHue
        saturation = newSaturation
        value = newValue
        val target = fromHsv(Hsv(newHue, newSaturation, newValue))
        red = target.r
        green = target.g
        blue = target.b
        redText = target.r.toString()
        greenText = target.g.toString()
        blueText = target.b.toString()
        hexText = toHex(target)
        error = null
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
            val hsv = toHsv(parsed)
            hue = hsv.h
            saturation = hsv.s
            value = hsv.v
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
            val target = Rgb(r, g, b)
            hexText = toHex(target)
            val hsv = toHsv(target)
            hue = hsv.h
            saturation = hsv.s
            value = hsv.v
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

            SectionLabel("常用颜色")
            PresetColorRow(
                selected = rgb,
                onSelect = ::syncFromRgb,
            )

            SectionLabel("取色器")
            HueSlider(
                hue = hue,
                onChange = { newHue -> applyHsv(newHue, saturation, value) },
            )
            SvPanel(
                hue = hue,
                saturation = saturation,
                value = value,
                onChange = { newSaturation, newValue ->
                    applyHsv(hue, newSaturation, newValue)
                },
            )

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

// ---------------------------------------------------------------------------- 取色器

/** 页面初始颜色：#3F51B5。 */
private val DEFAULT_RGB = Rgb(63, 81, 181)

/** 常用色板：黑白灰 + Material 基础色，选中项以主题主色描边。 */
private val PRESET_COLORS = listOf(
    Rgb(0, 0, 0), Rgb(96, 96, 96), Rgb(158, 158, 158), Rgb(255, 255, 255),
    Rgb(244, 67, 54), Rgb(255, 152, 0), Rgb(255, 193, 7), Rgb(139, 195, 74),
    Rgb(76, 175, 80), Rgb(0, 150, 136), Rgb(0, 188, 212), Rgb(33, 150, 243),
    Rgb(63, 81, 181), Rgb(103, 58, 183), Rgb(156, 39, 176), Rgb(233, 30, 99),
)

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
private fun PresetColorRow(
    selected: Rgb,
    onSelect: (Rgb) -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PRESET_COLORS.forEach { preset ->
            val isSelected = preset == selected
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(shape)
                    .background(Color(preset.r, preset.g, preset.b))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = shape,
                    )
                    .clickable { onSelect(preset) },
            )
        }
    }
}

/** 色相滑块：一条红→黄→绿→青→蓝→品红的渐变条，点按或拖动取色相。 */
@Composable
private fun HueSlider(
    hue: Float,
    onChange: (Float) -> Unit,
) {
    val hueColors = listOf(
        Color.hsv(0f, 1f, 1f),
        Color.hsv(60f, 1f, 1f),
        Color.hsv(120f, 1f, 1f),
        Color.hsv(180f, 1f, 1f),
        Color.hsv(240f, 1f, 1f),
        Color.hsv(300f, 1f, 1f),
        Color.hsv(360f, 1f, 1f),
    )
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onChange((offset.x / size.width).coerceIn(0f, 1f) * 360f)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onChange((change.position.x / size.width).coerceIn(0f, 1f) * 360f)
                }
            },
    ) {
        drawRect(brush = Brush.horizontalGradient(hueColors))
        val x = (hue / 360f).coerceIn(0f, 1f) * size.width
        val center = Offset(x, size.height / 2f)
        drawCircle(color = Color.White, radius = 8.dp.toPx(), center = center)
        drawCircle(
            color = Color.Black.copy(alpha = 0.3f),
            radius = 9.dp.toPx(),
            center = center,
            style = Stroke(width = 1.5.dp.toPx()),
        )
    }
}

/**
 * 饱和度 / 明度面板：横向白→当前色相，纵向透明→黑。
 *
 * 点和拖都直接映射成 (saturation, value)，并画一个圆圈指示当前位置。
 */
@Composable
private fun SvPanel(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (saturation: Float, value: Float) -> Unit,
) {
    val hueColor = Color.hsv(hue, 1f, 1f)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onChange(
                        (offset.x / size.width).coerceIn(0f, 1f),
                        (1f - offset.y / size.height).coerceIn(0f, 1f),
                    )
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onChange(
                        (change.position.x / size.width).coerceIn(0f, 1f),
                        (1f - change.position.y / size.height).coerceIn(0f, 1f),
                    )
                }
            },
    ) {
        drawRect(brush = Brush.horizontalGradient(listOf(Color.White, hueColor)))
        drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val center = Offset(
            x = saturation.coerceIn(0f, 1f) * size.width,
            y = (1f - value.coerceIn(0f, 1f)) * size.height,
        )
        drawCircle(
            color = Color.White,
            radius = 8.dp.toPx(),
            center = center,
            style = Stroke(width = 2.dp.toPx()),
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.3f),
            radius = 10.dp.toPx(),
            center = center,
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}
