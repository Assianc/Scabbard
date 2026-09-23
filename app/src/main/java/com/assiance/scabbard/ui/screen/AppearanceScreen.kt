package com.assiance.scabbard.ui.screen

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.assiance.scabbard.data.AppSettings
import com.assiance.scabbard.ui.theme.AuroraPreset
import com.assiance.scabbard.ui.theme.ThemePalette
import com.assiance.scabbard.ui.theme.fixedColorSchemeFor
import com.assiance.scabbard.ui.util.findActivity
import com.assiance.scabbard.utils.GradientAnimManager

/**
 * 外观设置页。对应改造前的 `AppearanceActivity`。
 *
 * 三块能力全部保留：14 个应用图标切换、标题渐变样式（5 种）、抽屉不透明度（0~255）。
 *
 * 关键改动在**不透明度那条链路**：原来拖动滑杆时，它会从 `ScabbardApplication.allActivities`
 * 里翻出首页的 StartActivity 实例，直接改对方的 nav_view 背景和 header 渐变 —— 跨 Activity
 * 摸别人的视图树，既泄漏 Activity，单 Activity 之后也根本不成立（首页和这里不再是两个
 * Activity）。现在改成：拖动只改本页的 pending 值，点"确认更改"才写进 [AppSettings]，
 * 首页抽屉订阅同一个 StateFlow 自动跟随。持久化时机与原实现一致（只在点确认时落盘）。
 *
 * 渐变样式改完不再发广播 —— 首页和关于页都直接订阅 [AppSettings.gradientStyle]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    val currentIconAlias by AppSettings.currentIconAlias.collectAsStateWithLifecycle()
    val currentGradientStyle by AppSettings.gradientStyle.collectAsStateWithLifecycle()
    val savedNavAlpha by AppSettings.navAlpha.collectAsStateWithLifecycle()
    val currentPalette by AppSettings.themePalette.collectAsStateWithLifecycle()
    val currentPreset by AppSettings.auroraPreset.collectAsStateWithLifecycle()
    val auroraVeil by AppSettings.auroraVeilEnabled.collectAsStateWithLifecycle()
    val glassBlur by AppSettings.glassBlurEnabled.collectAsStateWithLifecycle()

    var pendingAlias by remember { mutableStateOf<String?>(null) }
    var pendingChange by remember { mutableStateOf<IconChange?>(null) }
    var showGradientDialog by remember { mutableStateOf(false) }
    var pendingAlpha by remember { mutableFloatStateOf(savedNavAlpha.toFloat()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("外观设置") },
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
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "个性化图标",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp),
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(AppSettings.iconAliases) { alias ->
                    val selected = alias == currentIconAlias
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.2f else 1f,
                        label = "iconScale",
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { pendingAlias = alias }
                            .padding(8.dp),
                    ) {
                        Image(
                            painter = androidx.compose.ui.res.painterResource(
                                AppSettings.iconResOf(alias)
                            ),
                            contentDescription = alias,
                            modifier = Modifier
                                .size(64.dp)
                                .scale(scale),
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            SectionTitle("主题色板")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(ThemePalette.entries) { palette ->
                    PaletteSwatch(
                        palette = palette,
                        selected = palette == currentPalette,
                        onClick = { AppSettings.setThemePalette(palette) },
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // 极光配色与炫彩遮罩互斥显示：前者只对双色光斑背景有意义，
            // 后者只对炫彩 shader 背景有意义 —— 选中的色板决定了背景走哪条路
            // （见 AppBackground）。同时列出来会让用户对着一个不起作用的开关发呆。
            if (currentPalette == ThemePalette.AURORA) {
                SwitchRow(
                    title = "炫彩遮罩",
                    subtitle = "在炫彩流光之上再压一层主题色薄纱，收住浓度让正文更好读。" +
                        "关闭时是原版浓度。",
                    checked = auroraVeil,
                    onCheckedChange = { AppSettings.setAuroraVeilEnabled(it) },
                )
                HorizontalDivider()
            } else {
                SectionTitle("双色极光配色")
                AuroraPreset.entries.forEach { preset ->
                    OptionRow(
                        title = preset.title,
                        subtitle = preset.subtitle,
                        selected = preset == currentPreset,
                        onClick = { AppSettings.setAuroraPreset(preset) },
                    )
                }
                HorizontalDivider()
            }

            SwitchRow(
                title = "液态玻璃模糊",
                subtitle = "关闭后首页入口卡不再对背板做高斯模糊，只保留玻璃的着色与边缘高光。",
                checked = glassBlur,
                onCheckedChange = { AppSettings.setGlassBlurEnabled(it) },
            )

            HorizontalDivider()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showGradientDialog = true }
                    .padding(16.dp),
            ) {
                Text(
                    text = "标题渐变样式",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "点击切换标题文字的渐变效果（当前：${currentGradientStyle.title}）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            HorizontalDivider()

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "不透明度: ${(pendingAlpha / 255f * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                )
                Slider(
                    value = pendingAlpha,
                    onValueChange = { pendingAlpha = it },
                    valueRange = 0f..255f,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Button(
                    onClick = {
                        AppSettings.setNavAlpha(pendingAlpha.toInt())
                        Toast.makeText(context, "透明度设置已保存", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text("确认更改") }
            }
        }
    }

    // 第一级：选更换范围（原 changeAppIcon 的 setItems）
    pendingAlias?.let { alias ->
        AlertDialog(
            onDismissRequest = { pendingAlias = null },
            title = { Text("图标更换选项") },
            text = {
                Column {
                    TextButton(onClick = {
                        pendingAlias = null
                        pendingChange = IconChange(alias, IconChangeMode.APP_ONLY)
                    }) { Text("仅更换应用图标") }
                    TextButton(onClick = {
                        pendingAlias = null
                        pendingChange = IconChange(alias, IconChangeMode.SPLASH_ONLY)
                    }) { Text("仅更换开场动画图标") }
                    TextButton(onClick = {
                        pendingAlias = null
                        pendingChange = IconChange(alias, IconChangeMode.ALL)
                    }) { Text("更换所有图标") }
                }
            },
            confirmButton = {},
        )
    }

    // 第二级：确认后落盘并重启（原 changeOnlyAppIcon / changeSplashIcon / changeAllIcons）
    pendingChange?.let { change ->
        val (title, message) = when (change.mode) {
            IconChangeMode.APP_ONLY ->
                "更换应用图标" to "确定要更换应用图标吗？更换后需要重启应用才能生效。"

            IconChangeMode.SPLASH_ONLY ->
                "更换开场动画图标" to "确定要更换开场动画图标吗？更换后需要重启应用才能生效。"

            IconChangeMode.ALL ->
                "更换所有图标" to "确定要更换所有图标（包括应用图标、启动图标和关于界面图标）吗？更换后需要重启应用才能生效。"
        }
        AlertDialog(
            onDismissRequest = { pendingChange = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    pendingChange = null
                    when (change.mode) {
                        IconChangeMode.APP_ONLY -> AppSettings.setAppIconOnly(change.alias)
                        IconChangeMode.SPLASH_ONLY -> AppSettings.setSplashIconOnly(change.alias)
                        IconChangeMode.ALL -> AppSettings.setAllIcons(change.alias)
                    }
                    restartApp(context)
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { pendingChange = null }) { Text("取消") }
            },
        )
    }

    if (showGradientDialog) {
        val styles = GradientAnimManager.GradientStyle.entries
        AlertDialog(
            onDismissRequest = { showGradientDialog = false },
            title = { Text("选择标题渐变样式") },
            text = {
                Column {
                    styles.forEach { style ->
                        TextButton(onClick = {
                            showGradientDialog = false
                            AppSettings.setGradientStyle(style)
                            Toast.makeText(
                                context,
                                "渐变样式已更改为：${style.title}",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }) { Text(style.title) }
                    }
                }
            },
            confirmButton = {},
        )
    }
}

/**
 * 小节标题。与页内既有那两块（「个性化图标」「标题渐变样式」）的排版保持一致：
 * 16dp 内边距 + titleMedium 加粗。
 */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(16.dp),
    )
}

/**
 * 一个色板的色卡：圆点 + 名字，选中时放大并描一圈主题色边。
 *
 * 色点取该色板浅色模式的 `primary`。[ThemePalette.DYNAMIC] 没有固定色值
 * （它跟随壁纸），取当前生效 scheme 的 `primary` —— 显示的正是"现在这套"，
 * 与它的标题「跟随壁纸」自洽。
 */
@Composable
private fun PaletteSwatch(
    palette: ThemePalette,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        label = "paletteScale",
    )
    val swatch = fixedColorSchemeFor(palette, dark = false)?.primary
        ?: MaterialTheme.colorScheme.primary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(swatch)
                .then(
                    if (selected) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    } else {
                        Modifier
                    }
                ),
        )
        Text(
            text = palette.title,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** 带说明文字的开关行。 */
@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** 单选项行：整行可点，选中时标题染色并打勾。 */
@Composable
private fun OptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "已选中",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private enum class IconChangeMode { APP_ONLY, SPLASH_ONLY, ALL }

private data class IconChange(val alias: String, val mode: IconChangeMode)

/**
 * 原样照搬原来的重启方式：拿 launcher intent 打上 CLEAR_TOP/NEW_TASK/CLEAR_TASK，
 * finishAffinity() 之后重启并杀掉本进程。图标 alias 的启用状态要重启才生效。
 */
private fun restartApp(context: Context) {
    try {
        val activity = context.findActivity() ?: return
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        activity.finishAffinity()

        if (intent != null) {
            context.startActivity(intent)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            android.os.Process.killProcess(android.os.Process.myPid())
        }, 100)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "重启失败，请手动重启应用", Toast.LENGTH_SHORT).show()
    }
}
