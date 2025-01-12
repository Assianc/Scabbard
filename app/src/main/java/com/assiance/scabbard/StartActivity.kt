package com.assiance.scabbard

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import com.assiance.alm.MainActivityAlm
import com.assiance.memo.MainActivityMemo
import com.assiance.scabbard.update.UpdateChecker
import com.assiance.scabbard.utils.IconManager
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class StartActivity : AppCompatActivity() {

    private lateinit var startButton: Button
    private lateinit var memoButton: Button
    private lateinit var drawerLayout: DrawerLayout
    private val updateChecker = UpdateChecker()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_allocator)

        // 初始化抽屉布局
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        
        // 设置菜单按钮点击事件
        val menuButton = findViewById<Button>(R.id.toolbar)
        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // 设置导航菜单的头部图标
        val headerView = navView.getHeaderView(0)
        val headerIcon = headerView.findViewById<ImageView>(R.id.nav_header_icon)
        headerIcon.setImageResource(IconManager.getCurrentIconResourceId(this))

        // 设置导航菜单项的点击事件
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_appearance -> {
                    startActivity(Intent(this, AppearanceActivity::class.java))
                }
                R.id.nav_about -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                }
                R.id.nav_update -> {
                    checkForUpdates()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // 更新当前图标
        findViewById<ImageView>(R.id.app_icon)?.setImageResource(
            IconManager.getCurrentIconResourceId(this)
        )

        startButton = findViewById(R.id.start_button)
        memoButton = findViewById(R.id.memo_button)

        startButton.setOnClickListener {
            val intent = Intent(this@StartActivity, MainActivity::class.java)
            startActivity(intent)
        }

        memoButton.setOnClickListener {
            val intent = Intent(this@StartActivity, MainActivityMemo::class.java)
            startActivity(intent)
        }

        val almButton = findViewById<Button>(R.id.alm_button)
        almButton.setOnClickListener {
            val intent = Intent(this@StartActivity, MainActivityAlm::class.java)
            startActivity(intent)
        }

        // 在应用启动时检查并请求权限
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        // 检查通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!checkNotificationPermission()) {
                requestNotificationPermission()
            }
        }

        // 检查精确闹钟权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!checkAlarmPermission()) {
                showAlarmPermissionDialog()
            }
        }
    }

    private fun checkNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
                AlertDialog.Builder(this)
                    .setTitle("需要通知权限")
                    .setMessage("为了在闹钟响起时显示通知，需要授予通知权限。")
                    .setPositiveButton("授权") { _, _ ->
                        requestPermissions(
                            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                            100
                        )
                    }
                    .setNegativeButton("取消") { dialog, _ ->
                        dialog.dismiss()
                        Toast.makeText(this, "未授予通知权限，闹钟将无法显示通知", Toast.LENGTH_LONG).show()
                    }
                    .show()
            } else {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }

    private fun checkAlarmPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            return alarmManager.canScheduleExactAlarms()
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun showAlarmPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("需要权限")
            .setMessage("为了确保闹钟准时响起，需要授予精确闹钟权限。")
            .setPositiveButton("去设置") { _, _ ->
                try {
                    val intent = Intent().apply {
                        action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "打开设置失败，请手动授予权限", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "未授予权限，闹钟可能不会准时响起", Toast.LENGTH_LONG).show()
            }
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            100 -> {
                if (grantResults.isNotEmpty() && 
                    grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "通知权限已授予", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "未授予通知权限，闹钟将无法显示通知", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun checkForUpdates() {
        CoroutineScope(Dispatchers.Main).launch {
            val updateInfo = updateChecker.checkForUpdates(this@StartActivity)

            updateInfo?.let { info ->
                val currentVersion = updateChecker.getCurrentVersion(this@StartActivity)

                currentVersion?.let {
                    if (updateChecker.shouldUpdate(info.latestVersion, it)) {
                        // 先显示更新源选择对话框
                        showUpdateSourceDialog(info)
                    }
                }
            }
        }
    }

    private fun showUpdateSourceDialog(updateInfo: UpdateChecker.UpdateInfo) {
        val sources = arrayOf("GitHub 更新源", "蓝奏云更新源")
        
        AlertDialog.Builder(this)
            .setTitle("选择更新源")
            .setItems(sources) { dialog, which ->
                when (which) {
                    0 -> {
                        // GitHub 更新源
                        showUpdateDialog(updateInfo, false)
                    }
                    1 -> {
                        // 蓝奏云更新源
                        val lanzouInfo = UpdateChecker.UpdateInfo(
                            latestVersion = UpdateChecker.LANZOU_VERSION,
                            updateUrl = UpdateChecker.LANZOU_DOWNLOAD_URL,
                            updateDescription = """
                                下载说明：
                                1. 点击更新后将跳转到蓝奏云获取最新版
                                2. 输入提取码：${UpdateChecker.LANZOU_PASSWORD}
                                
                                注意：请在电脑模式下预览，否则可能无法正常下载。
                            """.trimIndent()
                        )
                        showUpdateDialog(lanzouInfo, true)
                    }
                }
            }
            .show()
    }

    private fun showUpdateDialog(updateInfo: UpdateChecker.UpdateInfo, isLanzou: Boolean) {
        updateChecker.showUpdateDialog(
            context = this@StartActivity,
            updateInfo = updateInfo,
            onConfirm = {
                try {
                    if (isLanzou) {
                        // 蓝奏云链接使用浏览器打开
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.updateUrl))
                        startActivity(intent)
                    } else {
                        // GitHub 更新直接下载
                        startDownload(updateInfo.updateUrl, updateInfo.latestVersion)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "操作失败，请稍后重试", Toast.LENGTH_SHORT).show()
                }
            },
            onCancel = {}
        )
    }

    private fun startDownload(downloadUrl: String, version: String) {
        try {
            val fileName = "Scabbard-${version}.apk"
            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle("下载更新")
                .setDescription("正在下载 Scabbard $version")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(this, "开始下载更新", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "下载失败，请稍后重试", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}

