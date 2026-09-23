package com.assiance.scabbard.ui.screen

import android.Manifest
import android.app.AlarmManager
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.assiance.memo.ui.GLASS_TINT_ALPHA
import com.assiance.memo.ui.LocalLiquidGlassState
import com.assiance.memo.ui.glassGrainFor
import com.assiance.memo.ui.rememberSharedGlassState
import com.assiance.scabbard.R
import com.assiance.scabbard.data.AppSettings
import com.assiance.scabbard.ui.component.GradientTitle
import com.assiance.scabbard.ui.util.findActivity
import com.assiance.scabbard.update.UpdateChecker
import io.github.nadeemiqbal.liquidglass.liquidGlass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 首页：M3 卡片式入口 + 左侧抽屉。
 *
 * 改造前这里是 `StartActivity`（DrawerLayout + NavigationView + 3 个描边 Button + 一张
 * 707dp 写死高度的背景图）。现在换成 ModalNavigationDrawer + 3 张 M3 卡片，去掉了背景图，
 * 底色用 M3 surface。
 *
 * 抽屉里仍是原来那三项（关于 / 外观设置 / 检查更新），顺序不变；抽屉只在首页出现，
 * 和改造前一样（原来也只有 StartActivity 有抽屉）。
 *
 * 抽屉头部那层"浅紫→白"的纵向渐变背景保留了，但两头改成从主题里取
 * （primaryContainer → surface），这样深色模式下不会硬顶着白色。
 * 原来不透明度是写死的白色叠加，现在走 nav_alpha 设置。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenAllocate: () -> Unit,
    onOpenMemo: () -> Unit,
    onOpenAlm: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val gradientStyle by AppSettings.gradientStyle.collectAsStateWithLifecycle()
    val navAlpha by AppSettings.navAlpha.collectAsStateWithLifecycle()
    val appIconRes by AppSettings.appIconRes.collectAsStateWithLifecycle()

    val updateChecker = remember { UpdateChecker() }

    // 通知权限 / 精确闹钟权限（原 StartActivity.checkAndRequestPermissions）
    var showNotificationRationale by remember { mutableStateOf(false) }
    var showExactAlarmDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val message = if (granted) "通知权限已授予" else "未授予通知权限，闹钟将无法显示通知"
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) {
                val activity = context.findActivity()
                if (activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true) {
                    showNotificationRationale = true
                } else {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                showExactAlarmDialog = true
            }
        }
    }

    // 检查更新（原抽屉里 nav_update 那条 + showUpdateSourceDialog 的两级对话框）
    var updateSourceDialog by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    var confirmDialog by remember { mutableStateOf<Pair<UpdateChecker.UpdateInfo, Boolean>?>(null) }
    val alphaFraction = navAlpha / 255f

    ModalNavigationDrawer(
        drawerState = drawerState,
        // 原 DrawerLayout.setScrimColor(Color.argb(33, 0, 0, 0))
        scrimColor = Color.Black.copy(alpha = 33f / 255f),
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = alphaFraction),
            ) {
                // 原 nav_header.xml：72dp 图标 + 24sp 粗体渐变标题，底部对齐，16dp 内边距
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = alphaFraction),
                                    MaterialTheme.colorScheme.surface.copy(alpha = alphaFraction),
                                )
                            )
                        )
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    if (appIconRes != 0) {
                        // 这里必须是 Image 而不是 Icon：Icon 会按 LocalContentColor 把整张
                        // painter 染成单色，彩色应用图标就糊成一个纯色块（抽屉里原先的毛病）。
                        // Image 不做染色，与 AboutScreen 顶部应用图标一致。
                        Image(
                            painter = painterResource(appIconRes),
                            contentDescription = stringResource(R.string.app_name),
                            modifier = Modifier.size(72.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    GradientTitle(
                        text = stringResource(R.string.app_name),
                        style = gradientStyle,
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()

                // 顺序与原 drawer_menu.xml 一致：关于 / 外观设置 / 检查更新
                NavigationDrawerItem(
                    label = { Text("关于") },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenAbout()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                NavigationDrawerItem(
                    label = { Text("外观设置") },
                    icon = { Icon(Icons.Default.Palette, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenAppearance()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                NavigationDrawerItem(
                    label = { Text("检查更新") },
                    icon = { Icon(Icons.Default.SystemUpdate, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        scope.launch {
                            val info = withContext(Dispatchers.Main) {
                                updateChecker.checkForUpdates(context)
                            }
                            if (info != null) {
                                updateSourceDialog = info
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "菜单")
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HomeEntryCard(
                    icon = Icons.AutoMirrored.Filled.CallSplit,
                    title = "分 道",
                    onClick = onOpenAllocate,
                )
                HomeEntryCard(
                    icon = Icons.Default.Cloud,
                    title = "云端事",
                    onClick = onOpenMemo,
                )
                HomeEntryCard(
                    icon = Icons.Default.NotificationsActive,
                    title = "冬去鸟鸣时",
                    onClick = onOpenAlm,
                )
            }
        }
    }

    if (showNotificationRationale) {
        AlertDialog(
            onDismissRequest = { showNotificationRationale = false },
            title = { Text("需要通知权限") },
            text = { Text("为了在闹钟响起时显示通知，需要授予通知权限。") },
            confirmButton = {
                TextButton(onClick = {
                    showNotificationRationale = false
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text("授权") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNotificationRationale = false
                    Toast.makeText(context, "未授予通知权限，闹钟将无法显示通知", Toast.LENGTH_LONG).show()
                }) { Text("取消") }
            },
        )
    }

    if (showExactAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showExactAlarmDialog = false },
            title = { Text("需要权限") },
            text = { Text("为了确保闹钟准时响起，需要授予精确闹钟权限。") },
            confirmButton = {
                TextButton(onClick = {
                    showExactAlarmDialog = false
                    try {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "打开设置失败，请手动授予权限", Toast.LENGTH_LONG).show()
                    }
                }) { Text("去设置") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExactAlarmDialog = false
                    Toast.makeText(context, "未授予权限，闹钟可能不会准时响起", Toast.LENGTH_LONG).show()
                }) { Text("取消") }
            },
        )
    }

    updateSourceDialog?.let { info ->
        AlertDialog(
            onDismissRequest = { updateSourceDialog = null },
            title = { Text("选择更新源") },
            text = {
                Column {
                    TextButton(onClick = {
                        updateSourceDialog = null
                        confirmDialog = info to false
                    }) { Text("GitHub 更新源") }
                    TextButton(onClick = {
                        updateSourceDialog = null
                        confirmDialog = UpdateChecker.UpdateInfo(
                            latestVersion = UpdateChecker.LANZOU_VERSION,
                            updateUrl = UpdateChecker.LANZOU_DOWNLOAD_URL,
                            updateDescription = """
                                下载说明：
                                1. 点击更新后将跳转到蓝奏云获取最新版
                                2. 输入提取码：${UpdateChecker.LANZOU_PASSWORD}

                                注意：请在电脑模式下预览，否则可能无法正常下载。
                            """.trimIndent(),
                        ) to true
                    }) { Text("蓝奏云更新源") }
                }
            },
            confirmButton = {},
        )
    }

    confirmDialog?.let { (info, isLanzou) ->
        AlertDialog(
            onDismissRequest = { confirmDialog = null },
            title = { Text("发现新版本 ${info.latestVersion}") },
            text = { Text(info.updateDescription) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDialog = null
                    try {
                        if (isLanzou) {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, info.updateUrl.toUri())
                            )
                        } else {
                            startApkDownload(context, info.updateUrl, info.latestVersion)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "操作失败，请稍后重试", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDialog = null }) { Text("取消") }
            },
        )
    }
}

/**
 * 首页入口卡：材质由 Material3 [Card] 换成液态玻璃。
 *
 * 布局、内边距、圆角、点击范围都保持原样，只换材质 —— 圆角仍取 [CardDefaults.shape]，
 * 也就是被替换掉的那张 Card 的默认圆角，不趁换材质顺手改形状。
 *
 * 档位（含降级与「液态玻璃模糊」开关）**不在这里决定**，由 App 根的
 * [LocalLiquidGlassState] 统一给：那边一处按 `AppSettings.glassBlurEnabled` 把 quality
 * 切成 Fallback，本组件只负责消费。放在根上是因为背板和玻璃面必须共用同一个实例
 * （原因见 [LocalLiquidGlassState]）。
 *
 * 只切 quality、不动 tint：那条开关的字面语义就是"模糊"这一个维度，卡片的底色
 * （[GLASS_TINT_ALPHA]）不归它管。玻璃面的颗粒（[GLASS_GRAIN]）则**跟着档位走** ——
 * Fallback 时一并收掉，见 [glassGrainFor]。
 * （移植来源 LoveScore 在关掉模糊时还会把 tint 一起清成全透明、改画一圈极淡描边，
 * 那是它的观感选择，本项目不跟。）
 *
 * tint / 颗粒这两个观感参数**不在本文件定义**，和另外两处玻璃面（备忘录记录卡、关于页
 * 作者行）共用 [com.assiance.memo.ui.LiquidGlassContract] 里那一份 —— 调参只改一个地方。
 */
@Composable
private fun HomeEntryCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    val shape = CardDefaults.shape
    // 玻璃状态由 App 根的 [LocalLiquidGlassState] 提供，**必须与背板共用同一个实例**：
    // 模糊挂在该 state 的 `backdrop` 这个 GraphicsLayer 上，而那个 layer 只由背景层的
    // `Modifier.liquidGlassSource(state)` 录制 —— 卡片自己 new 一个 state 就采不到任何像素，
    // 不报错、不崩，只是不模糊。（本项目此前正是各自持一份，所以玻璃从来没模糊过。）
    //
    // 取不到时（预览、单测里脱离 App 根复用本组件）的兜底收在 [rememberSharedGlassState] 里：
    // 退回一份自持的 Fallback state，此时没有背板，渲染成纯 tint + 边缘高光。
    val glassState = rememberSharedGlassState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // 先把命中区裁进圆角内，否则圆角外的"空矩形"会吞掉抽屉 / 列表的滑动手势。
            .clip(shape)
            .liquidGlass(
                state = glassState,
                shape = shape,
                tint = MaterialTheme.colorScheme.surfaceContainer
                    .copy(alpha = GLASS_TINT_ALPHA),
                grain = glassGrainFor(glassState.quality),
            )
            // liquidGlass 只裁渲染，触摸命中仍由整条 modifier 链决定，所以这里再裁一次。
            .clip(shape)
            // clickable 放在 padding 之前，保持"整张卡含内边距都可点"（与原 Card 一致）；
            // role = Button 是补回 Card(onClick) 自带的语义，避免无障碍回退。
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** 走系统 DownloadManager 下 APK，与改造前 startDownload 的行为一致。 */
private fun startApkDownload(context: Context, downloadUrl: String, version: String) {
    try {
        val fileName = "Scabbard-$version.apk"
        val request = DownloadManager.Request(downloadUrl.toUri())
            .setTitle("下载更新")
            .setDescription("正在下载 Scabbard $version")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        Toast.makeText(context, "开始下载更新", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "下载失败，请稍后重试", Toast.LENGTH_SHORT).show()
    }
}
