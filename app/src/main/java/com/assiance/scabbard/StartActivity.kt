package com.assiance.scabbard

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.assiance.alm.MainActivityAlm
import com.assiance.memo.MainActivityMemo
import com.assiance.scabbard.update.UpdateChecker
import com.assiance.scabbard.utils.GradientAnimManager
import com.assiance.scabbard.utils.IconManager
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import androidx.core.graphics.drawable.toDrawable

open class StartActivity : AppCompatActivity() {

    private lateinit var startButton: Button
    private lateinit var memoButton: Button
    private lateinit var drawerLayout: DrawerLayout
    private val updateChecker = UpdateChecker()
    private var gradientAnimator: ValueAnimator? = null
    private var gradientMatrix: Matrix = Matrix()
    private var translateX: Float = 0f

    // 添加 gradientReceiver 作为类属性
    private val gradientReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.assiance.scabbard.ACTION_GRADIENT_CHANGED") {
                val navView = findViewById<NavigationView>(R.id.nav_view)
                val headerView = navView.getHeaderView(0)
                val navTitle = headerView.findViewById<TextView>(R.id.nav_header_title)
                
                // 停止并清除旧的动画
                gradientAnimator?.cancel()
                gradientAnimator = null
                
                // 清除旧的渐变效果
                navTitle.paint.shader = null
                
                // 重新设置渐变效果
                setupGradientEffect(navTitle)
                
                // 强制重绘
                navTitle.invalidate()
            }
        }
    }

    // 在 class StartActivity 中添加广播接收器
    private val alphaChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.assiance.scabbard.ACTION_NAV_ALPHA_CHANGED") {
                val alpha = intent.getIntExtra("alpha", 230)
                updateNavigationViewAlpha(alpha)
            }
        }
    }

    // 添加新的方法来处理透明度更新
    private fun updateNavigationViewAlpha(alpha: Int) {
        try {
            val navView = findViewById<NavigationView>(R.id.nav_view) ?: return
            
            // 使用 post 方法确保在主线程上执行 UI 更新
            navView.post {
                // 更新 NavigationView 的背景色
                navView.setBackgroundColor(Color.argb(alpha, 255, 255, 255))
                
                // 更新 header 的渐变背景
                val headerView = navView.getHeaderView(0)
                val headerLayout = headerView.findViewById<LinearLayout>(R.id.nav_header_layout)
                
                // 创建新的渐变背景
                val gradientDrawable = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(
                        Color.argb(alpha, 230, 220, 255),  // 浅紫色
                        Color.argb(alpha, 255, 255, 255)   // 白色
                    )
                )
                
                // 应用新的背景
                headerLayout.background = gradientDrawable
                
                // 使用单次刷新，避免多次重绘
                drawerLayout.postInvalidate()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_allocator)

        // 初始化抽屉布局
        drawerLayout = findViewById(R.id.drawer_layout)
        // 设置 scrimColor 为轻微的遮罩效果
        drawerLayout.setScrimColor(Color.argb(33, 0, 0, 0))  // 设置为15%透明度的黑色
        
        val navView = findViewById<NavigationView>(R.id.nav_view)
        
        // 设置导航菜单背景
        setupNavigationViewBackground(navView)
        
        // 设置菜单按钮点击事件
        val menuButton = findViewById<Button>(R.id.toolbar)
        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // 设置导航菜单的头部图标和标题
        val headerView = navView.getHeaderView(0)
        val headerIcon = headerView.findViewById<ImageView>(R.id.nav_header_icon)
        headerIcon.setImageResource(IconManager.getCurrentIconResourceId(this))
        
        // 设置导航菜单标题的渐变效果
        val navTitle = headerView.findViewById<TextView>(R.id.nav_header_title)
        setupGradientEffect(navTitle)

        // 注册广播接收器的代码改为使用类属性
        val gradientFilter = IntentFilter("com.assiance.scabbard.ACTION_GRADIENT_CHANGED")
        registerReceiver(gradientReceiver, gradientFilter, RECEIVER_NOT_EXPORTED)

        // 注册透明度变化广播接收器
        val alphaFilter = IntentFilter("com.assiance.scabbard.ACTION_NAV_ALPHA_CHANGED")
        registerReceiver(alphaChangeReceiver, alphaFilter, RECEIVER_NOT_EXPORTED)

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

        // 设置侧滑手势
        setupSwipeGesture()
    }

    private fun setupGradientEffect(navTitle: TextView) {
        // 停止现有动画
        gradientAnimator?.cancel()
        
        // 主标题渐变设置
        val paint = navTitle.paint
        val width = paint.measureText(navTitle.text.toString())
        val currentStyle = GradientAnimManager.getCurrentStyle(this)
        val textShader = GradientAnimManager.createGradient(
            width, // 将宽度翻倍，使渐变效果更平滑
            navTitle.textSize,
            currentStyle
        )
        
        // 重置矩阵和位移
        gradientMatrix = Matrix()
        translateX = 0f
        
        paint.shader = textShader

        // 创建新的渐变动画
        gradientAnimator = ValueAnimator.ofFloat(0f, width).apply {
            duration = 2100  // 保持动画时长一致
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()

            addUpdateListener { animator ->
                translateX = animator.animatedValue as Float
                gradientMatrix.setTranslate(-translateX, 0f)
                textShader.setLocalMatrix(gradientMatrix)
                navTitle.invalidate()
            }
            start()
        }
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
                        val intent = Intent(Intent.ACTION_VIEW, updateInfo.updateUrl.toUri())
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
            val request = DownloadManager.Request(downloadUrl.toUri())
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

    override fun onDestroy() {

        gradientAnimator?.cancel()
        gradientAnimator = null

        super.onDestroy()
        try {
            unregisterReceiver(gradientReceiver)
            unregisterReceiver(alphaChangeReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        
        // 检查是否存在NavigationView
        val navView = findViewById<NavigationView>(R.id.nav_view) ?: return
        
        // 获取headerView
        val headerView = navView.getHeaderView(0)
        val navTitle = headerView.findViewById<TextView>(R.id.nav_header_title)
        
        // 停止并清除旧的动画
        gradientAnimator?.cancel()
        gradientAnimator = null
        
        // 清除旧的渐变效果
        navTitle.paint.shader = null
        
        // 重新设置渐变效果
        setupGradientEffect(navTitle)
        
        // 强制重绘
        navTitle.invalidate()
    }

    private fun setupNavigationViewBackground(navView: NavigationView) {
        // 获取当前设置的透明度
        val alpha = GradientAnimManager.getCurrentNavAlpha(this)
        
        // 设置NavigationView的背景为半透明白色
        navView.setBackgroundColor(Color.argb(alpha, 255, 255, 255))

        drawerLayout.setScrimColor(Color.argb(33, 0, 0, 0))  // 设置为13%透明度的黑色
        
        // 获取header并设置渐变背景
        val headerView = navView.getHeaderView(0)
        val headerLayout = headerView.findViewById<LinearLayout>(R.id.nav_header_layout)
        
        // 创建渐变背景，使用紫色系的渐变
        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.argb(alpha, 230, 220, 255),  // 浅紫色
                Color.argb(alpha, 255, 255, 255)   // 白色
            )
        )
        
        // 设置圆角
        gradientDrawable.cornerRadius = 0f
        
        // 应用背景
        headerLayout.background = gradientDrawable
        
        // 设置菜单项的背景色为透明
        navView.itemBackground = Color.TRANSPARENT.toDrawable()
        
        // 设置菜单项的文字颜色为黑色
        navView.itemTextColor = ColorStateList.valueOf(Color.BLACK)
        
        // 设置菜单项的图标颜色为黑色
        navView.itemIconTintList = ColorStateList.valueOf(Color.BLACK)
    }

    // 添加侧滑手势支持
    private fun setupSwipeGesture() {
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        
        // 创建触摸监听器
        val touchListener = object : View.OnTouchListener {
            private var startX = 0f
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100
            
            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.x
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val endX = event.x
                        val distanceX = endX - startX
                        
                        // 如果是从左向右滑动，且距离足够大
                        if (distanceX > SWIPE_THRESHOLD && Math.abs(distanceX) > SWIPE_VELOCITY_THRESHOLD) {
                            // 打开侧边栏
                            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                                drawerLayout.openDrawer(GravityCompat.START)
                                return true
                            }
                        }
                        return false
                    }
                }
                return false
            }
        }
        
        // 将触摸监听器应用到主内容视图，而不是整个布局
        val mainContent = findViewById<View>(R.id.main_content) // 确保你的布局中有这个ID
        mainContent?.setOnTouchListener(touchListener)
    }
}

