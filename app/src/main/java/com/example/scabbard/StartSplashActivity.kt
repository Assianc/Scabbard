package com.example.scabbard

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.view.animation.ScaleAnimation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gyf.immersionbar.BarHide
import com.gyf.immersionbar.ImmersionBar
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.scabbard.update.UpdateChecker

class StartSplashActivity : StartActivity(), Animation.AnimationListener {

    companion object {
        private const val ANIM_TIME = 1700L // 将ANIM_TIME的类型更改为 Long 以避免转换
    }

    private lateinit var mImageView: View
    private lateinit var mIconView: View
    private lateinit var mNameView: View
    private lateinit var mDebugView: View
    private val handler = Handler(Looper.getMainLooper())

    private var hasPermissionGranted = false
    private var isAnimationEnded = false
    private val updateChecker = UpdateChecker()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 初始化视图
        mImageView = findViewById(R.id.iv_splash_bg)
        mIconView = findViewById(R.id.iv_splash_icon)
        mNameView = findViewById(R.id.iv_splash_name)
        mDebugView = findViewById(R.id.tv_splash_debug)

        initView()
        initData()
    }

    private fun initView() {
        // 初始化背景淡入动画
        val alphaAnimation = AlphaAnimation(0.4f, 1.0f).apply {
            duration = ANIM_TIME * 2
            setAnimationListener(this@StartSplashActivity)
        }
        mImageView.startAnimation(alphaAnimation)

        // 图标缩放动画
        val scaleAnimation = ScaleAnimation(
            0f, 1.2f, 0f, 1.2f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = ANIM_TIME
            fillAfter = true // 保持动画结束时的状态
        }
        mIconView.startAnimation(scaleAnimation)

        // 名称旋转动画
        val rotateAnimation = RotateAnimation(
            180f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = ANIM_TIME
        }
        mNameView.startAnimation(rotateAnimation)

        // 使用 ImmersionBar 设置状态栏和导航栏
        ImmersionBar.with(this)
            .fullScreen(true)                       // 全屏显示
            .hideBar(BarHide.FLAG_HIDE_STATUS_BAR)   // 隐藏状态栏
            .transparentNavigationBar()              // 透明导航栏
            .init()
    }

    // 初始化数据
    private fun initData() {
        checkForUpdates()
    }

    private fun checkForUpdates() {
        CoroutineScope(Dispatchers.Main).launch {
            val updateInfo = updateChecker.checkForUpdates(this@StartSplashActivity)
            
            updateInfo?.let { info ->
                // 获取当前版本号
                val currentVersion = updateChecker.getCurrentVersion(this@StartSplashActivity)
                
                if (updateChecker.shouldUpdate(info.latestVersion, currentVersion)) {
                    updateChecker.showUpdateDialog(
                        context = this@StartSplashActivity,
                        updateInfo = info,
                        onConfirm = {
                            try {
                                startDownload(info.updateUrl, info.latestVersion)
                                if (info.forceUpdate) {
                                    finish()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        onCancel = {
                            continueAppLaunch()
                        }
                    )
                } else {
                    continueAppLaunch()
                }
            } ?: run {
                Toast.makeText(this@StartSplashActivity, "检查更新失败", Toast.LENGTH_SHORT).show()
                continueAppLaunch()
            }
        }
    }

    private fun startDownload(downloadUrl: String, version: String) {
        try {
            val fileName = "Scabbard-${version}.apk"
            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle("下载更新")
                .setDescription("正在下载 Scabbard ${version}")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(this, "开始下载更新", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "下载失败，请稍后重试", Toast.LENGTH_SHORT).show()
        }
    }

    private fun continueAppLaunch() {
        // 原有的启动逻辑
        requestPermission()
    }

    private fun requestPermission() {
        val requiredPermissions = getRequiredPermissions()

        XXPermissions.with(this)
            .permission(*requiredPermissions)  // 使用 getRequiredPermissions 返回的权限列表
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: List<String>, all: Boolean) {
                    hasPermissionGranted = true
                    if (isAnimationEnded) {
                        startStartActivity()
                    }
                }

                override fun onDenied(permissions: List<String>, quick: Boolean) {
                    if (quick) {
                        Toast.makeText(
                            this@StartSplashActivity,
                            R.string.common_permission_fail,
                            Toast.LENGTH_SHORT
                        ).show()
                        XXPermissions.startPermissionActivity(this@StartSplashActivity, permissions)
                    } else {
                        Toast.makeText(
                            this@StartSplashActivity,
                            R.string.common_permission_hint,
                            Toast.LENGTH_SHORT
                        ).show()
                        handler.postDelayed({ requestPermission() }, 1000)
                    }
                }
            })
    }

    private fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10及以上
            arrayOf(
                Permission.READ_MEDIA_IMAGES,
                Permission.READ_MEDIA_VIDEO,
                Permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(
                Permission.READ_EXTERNAL_STORAGE,
                Permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }


    override fun onAnimationEnd(animation: Animation) {
        isAnimationEnded = true
        requestPermission()
        if (hasPermissionGranted) {
            startStartActivity()
        }
    }

    private fun startStartActivity() {
        startActivity(Intent(this@StartSplashActivity, StartActivity::class.java))
        finish()
    }

    override fun onAnimationStart(animation: Animation) {}

    override fun onAnimationRepeat(animation: Animation) {}
}
