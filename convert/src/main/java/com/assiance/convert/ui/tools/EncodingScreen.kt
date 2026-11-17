package com.assiance.convert.ui.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.assiance.convert.logic.ConvertOutcome
import com.assiance.convert.logic.base64Decode
import com.assiance.convert.logic.base64Encode
import com.assiance.convert.logic.compactJson
import com.assiance.convert.logic.prettyJson
import com.assiance.convert.logic.urlDecode
import com.assiance.convert.logic.urlEncode
import com.assiance.convert.ui.GLASS_TINT_ALPHA
import com.assiance.convert.ui.glassGrainFor
import com.assiance.convert.ui.rememberConvertGlassState
import io.github.nadeemiqbal.liquidglass.liquidGlass

/** 编码转换页的三个 tab，顺序与界面一致。 */
private const val TAB_URL = 0
private const val TAB_BASE64 = 1
private const val TAB_JSON = 2

/**
 * 编码转换页：URL / Base64 / JSON 三类工具共用一套「输入 → 操作 → 结果」结构。
 *
 * 操作是显式按钮而不是输入即转：解码方向对错误输入很敏感（Base64 乱码、URL 非法
 * 百分号），边打字边报错体验太吵；由用户点按钮触发，错误提示才有意义。
 * 切换 tab 时清空上一次结果，避免把 URL 的编码结果误当成 JSON 的。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EncodingScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(TAB_URL) }
    var input by rememberSaveable { mutableStateOf("") }
    var output by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    fun apply(outcome: ConvertOutcome) {
        output = outcome.text
        error = outcome.error
    }

    fun switchTab(index: Int) {
        if (tab == index) return
        tab = index
        output = ""
        error = null
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("编码转换") },
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
            // containerColor 必须显式透明：默认值是不透明的 surface，会在全 App 的
            // 渐变背景上留一条实色横带（与 alm 首页的 TabRow 同一个坑）。
            PrimaryTabRow(
                selectedTabIndex = tab,
                containerColor = Color.Transparent,
            ) {
                Tab(selected = tab == TAB_URL, onClick = { switchTab(TAB_URL) }, text = { Text("URL") })
                Tab(selected = tab == TAB_BASE64, onClick = { switchTab(TAB_BASE64) }, text = { Text("Base64") })
                Tab(selected = tab == TAB_JSON, onClick = { switchTab(TAB_JSON) }, text = { Text("JSON") })
            }

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(if (tab == TAB_JSON) "JSON 文本" else "文本") },
                placeholder = {
                    Text(
                        when (tab) {
                            TAB_BASE64 -> "输入原文，或粘贴 Base64 文本"
                            TAB_JSON -> "粘贴 JSON"
                            else -> "输入文本"
                        }
                    )
                },
                singleLine = tab != TAB_JSON,
                minLines = if (tab == TAB_JSON) 4 else 1,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (tab) {
                    TAB_BASE64 -> {
                        OutlinedButton(onClick = { apply(base64Encode(input)) }) { Text("编码") }
                        OutlinedButton(onClick = { apply(base64Decode(input)) }) { Text("解码") }
                    }

                    TAB_JSON -> {
                        OutlinedButton(onClick = { apply(prettyJson(input)) }) { Text("美化") }
                        OutlinedButton(onClick = { apply(compactJson(input)) }) { Text("压缩") }
                    }

                    else -> {
                        OutlinedButton(onClick = { apply(urlEncode(input)) }) { Text("编码") }
                        OutlinedButton(onClick = { apply(urlDecode(input)) }) { Text("解码") }
                    }
                }
            }

            ResultCard(
                output = output,
                error = error,
                onCopy = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("convert", output))
                    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                },
            )
        }
    }
}

/** 结果卡：有结果时等宽字体展示并可复制；出错时只显示错误；都没有时给占位提示。 */
@Composable
private fun ResultCard(
    output: String,
    error: String?,
    onCopy: () -> Unit,
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
                tint = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = GLASS_TINT_ALPHA),
                grain = glassGrainFor(glassState.quality),
            )
            .clip(shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "结果",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (output.isNotEmpty()) {
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "复制结果",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        when {
            error != null -> Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )

            output.isNotEmpty() -> Text(
                text = output,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
            )

            else -> Text(
                text = "转换结果会显示在这里",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
