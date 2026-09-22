package com.assiance.scabbard.ui.screen

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.assiance.scabbard.R
import com.assiance.scabbard.data.AppSettings
import com.assiance.scabbard.ui.util.findActivity
import com.assiance.scabbard.update.UpdateChecker
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import kotlinx.coroutines.launch

/**
 * 开场动画页，也是 NavHost 的 startDestination。对应改造前的 `StartSplashActivity`。
 *
 * 行为照搬：三层动画（背景 0.4→1.0 共 4200ms、图标 0→2.2 倍共 2100ms、
 * 应用名 180°→360° 共 2100ms）、隐藏状态栏、媒体权限申请、更新检查。
 *
 * 背景图 `preview1` 和底部那句"愿你我成为生活的高手"都保留了 —— 用户说的"去掉背景图"
 * 是针对首页那张 `background4`，不适用于这张。
 *
 * 注意一处原样保留的历史行为：媒体权限被拒绝时**不会进入首页**，而是走"跳权限设置页"或
 * "1 秒后重试"。这是改造前就有的设计，我没有擅自改掉。
 */
@SuppressLint("CustomSplashScreen")
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context.findActivity()
    val updateChecker = remember { UpdateChecker() }

    val splashIconRes by AppSettings.splashIconRes.collectAsStateWithLifecycle()

    val backgroundAlpha = remember { Animatable(0.4f) }
    val iconScale = remember { Animatable(0f) }
    val nameRotation = remember { Animatable(180f) }

    var animationDone by remember { mutableStateOf(false) }
    var permissionSettled by remember { mutableStateOf(false) }
    var pendingUpdate by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }

    // 全屏：隐藏状态栏与导航栏（原 ImmersionBar fullScreen + FLAG_HIDE_STATUS_BAR）
    DisposableEffect(Unit) {
        val window = activity?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, view) }
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    // 三层动画。背景动画时长是 ANIM_TIME * 2，它结束时才算"动画结束"——与原来把
    // AnimationListener 挂在 alphaAnimation 上一致。
    LaunchedEffect(Unit) {
        launch { iconScale.animateTo(2.2f, tween(ANIM_TIME_MILLIS)) }
        launch { nameRotation.animateTo(360f, tween(ANIM_TIME_MILLIS)) }
        backgroundAlpha.animateTo(1f, tween(ANIM_TIME_MILLIS * 2, easing = LinearEasing))
        animationDone = true
    }

    // 更新检查（原 checkForUpdates → 对话框 → 确定/取消 都走 continueAppLaunch）
    LaunchedEffect(Unit) {
        val info = updateChecker.checkForUpdates(context) ?: return@LaunchedEffect
        val current = updateChecker.getCurrentVersion(context) ?: return@LaunchedEffect
        if (updateChecker.shouldUpdate(info.latestVersion, current)) {
            pendingUpdate = info
        }
    }

    // 动画结束后申请媒体权限（原 onAnimationEnd → requestPermission）
    LaunchedEffect(animationDone) {
        if (!animationDone || activity == null) return@LaunchedEffect
        requestMediaPermissions(activity) { permissionSettled = true }
    }

    LaunchedEffect(animationDone, permissionSettled) {
        if (animationDone && permissionSettled) onFinished()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.preview1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(backgroundAlpha.value),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 75.dp, bottom = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (splashIconRes != 0) {
                Image(
                    painter = painterResource(splashIconRes),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                        .size(width = 105.dp, height = 100.dp)
                        .scale(iconScale.value),
                )
            }
            Spacer(modifier = Modifier.height(50.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .rotate(nameRotation.value),
            )
        }

        Text(
            text = "愿你我成为生活的高手",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 30.dp),
        )
    }

    pendingUpdate?.let { info ->
        AlertDialog(
            onDismissRequest = { pendingUpdate = null },
            title = { Text("发现新版本 ${info.latestVersion}") },
            text = { Text(info.updateDescription) },
            confirmButton = {
                TextButton(onClick = {
                    pendingUpdate = null
                    try {
                        if (info.updateUrl.contains("lanzoub.com")) {
                            // 蓝奏云链接用浏览器打开
                            context.startActivity(Intent(Intent.ACTION_VIEW, info.updateUrl.toUri()))
                        } else {
                            startApkDownload(context, info.updateUrl, info.latestVersion)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "下载失败，请稍后重试", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { pendingUpdate = null }) { Text("取消") }
            },
        )
    }
}

/**
 * 媒体读权限。Android 10 及以上用 READ_MEDIA_*，更低版本用读写外部存储 —— 与原来
 * `getRequiredPermissions()` 的判断一致。
 *
 * 拒绝时的处理也照搬原逻辑：永久拒绝（never）跳权限设置页，临时拒绝 1 秒后重试。
 */
private fun requestMediaPermissions(
    activity: Activity,
    onGranted: () -> Unit,
) {
    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(
            Permission.READ_MEDIA_IMAGES,
            Permission.READ_MEDIA_VIDEO,
            Permission.READ_MEDIA_AUDIO,
        )
    } else {
        arrayOf(
            Permission.READ_EXTERNAL_STORAGE,
            Permission.WRITE_EXTERNAL_STORAGE,
        )
    }

    try {
        XXPermissions.with(activity)
            .permission(*requiredPermissions)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    onGranted()
                }

                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    if (never) {
                        Toast.makeText(
                            activity,
                            R.string.common_permission_fail,
                            Toast.LENGTH_SHORT,
                        ).show()
                        XXPermissions.startPermissionActivity(activity, permissions)
                    } else {
                        Toast.makeText(
                            activity,
                            R.string.common_permission_hint,
                            Toast.LENGTH_SHORT,
                        ).show()
                        Handler(Looper.getMainLooper()).postDelayed({
                            requestMediaPermissions(activity, onGranted)
                        }, 1000)
                    }
                }
            })
    } catch (e: Exception) {
        e.printStackTrace()
        onGranted()
    }
}

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

private const val ANIM_TIME_MILLIS = 2100
