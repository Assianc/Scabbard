package com.assiance.scabbard

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Intent
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
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.assiance.scabbard.update.UpdateChecker
import com.assiance.scabbard.utils.IconManager
import com.gyf.immersionbar.BarHide
import com.gyf.immersionbar.ImmersionBar
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

@SuppressLint("CustomSplashScreen")
class StartSplashActivity : AppCompatActivity(), Animation.AnimationListener {

    companion object {
        private const val ANIM_TIME = 2100L
    }

    private lateinit var mImageView: View
    private lateinit var mIconView: ImageView
    private lateinit var mNameView: View
    private lateinit var mDebugView: View
    private val handler = Handler(Looper.getMainLooper())

    private var hasPermissionGranted = false
    private var isAnimationEnded = false
    private val updateChecker = UpdateChecker()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 使用 lifecycleScope 进行初始化
        lifecycleScope.launch(Dispatchers.Default) {
            // 在后台线程初始化视图引用
            val imageView = findViewById<View>(R.id.iv_splash_bg)
            val iconView = findViewById<ImageView>(R.id.iv_splash_icon)
            val nameView = findViewById<View>(R.id.iv_splash_name)
            val debugView = findViewById<View>(R.id.tv_splash_debug)

            withContext(Dispatchers.Main) {
                // 在主线程中设置视图引用
                mImageView = imageView
                mIconView = iconView
                mNameView = nameView
                mDebugView = debugView

                // 初始化视图和数据
                initView()
                initData()

                mIconView.setImageResource(IconManager.getSplashIconResourceId(this@StartSplashActivity))
            }
        }
    }

    private fun initView() {
        // 预先创建动画对象
        lifecycleScope.launch(Dispatchers.Default) {
            // 在后台线程准备所有动画
            val animations = prepareAnimations()

            withContext(Dispatchers.Main) {
                // 在主线程中应用动画
                applyAnimations(animations)

                // 设置状态栏
                setupStatusBar()
            }
        }
    }

    private fun prepareAnimations(): Triple<AlphaAnimation, ScaleAnimation, RotateAnimation> {
        val alphaAnimation = AlphaAnimation(0.4f, 1.0f).apply {
            duration = ANIM_TIME * 2
            setAnimationListener(this@StartSplashActivity)
        }

        val scaleAnimation = ScaleAnimation(
            0f, 2.2f, 0f, 2.2f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = ANIM_TIME
            fillAfter = true
        }

        val rotateAnimation = RotateAnimation(
            180f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = ANIM_TIME
        }

        return Triple(alphaAnimation, scaleAnimation, rotateAnimation)
    }

    private fun applyAnimations(animations: Triple<AlphaAnimation, ScaleAnimation, RotateAnimation>) {
        mImageView.startAnimation(animations.first)
        mIconView.startAnimation(animations.second)
        mNameView.startAnimation(animations.third)
    }

    private fun setupStatusBar() {
        ImmersionBar.with(this)
            .fullScreen(true)
            .hideBar(BarHide.FLAG_HIDE_STATUS_BAR)
            .transparentNavigationBar()
            .init()
    }

    // 初始化数据
    private fun initData() {
        checkForUpdates()
    }

    private fun checkForUpdates() {
        if (isFinishing) {
            continueAppLaunch()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val updateInfo = updateChecker.checkForUpdates(this@StartSplashActivity)

                if (isFinishing) {
                    continueAppLaunch()
                    return@launch
                }

                updateInfo?.let { info ->
                    val currentVersion = updateChecker.getCurrentVersion(this@StartSplashActivity)

                    currentVersion?.let {
                        if (updateChecker.shouldUpdate(info.latestVersion, it)) {
                            if (!isFinishing) {
                                val dialog = updateChecker.showUpdateDialog(
                                    context = this@StartSplashActivity,
                                    updateInfo = info,
                                    onConfirm = {
                                        try {
                                            if (info.updateUrl.contains("lanzoub.com")) {
                                                // 蓝奏云链接使用浏览器打开
                                                val intent = Intent(
                                                    Intent.ACTION_VIEW,
                                                    info.updateUrl.toUri()
                                                )
                                                startActivity(intent)
                                            } else {
                                                // GitHub 更新直接下载
                                                startDownload(info.updateUrl, info.latestVersion)
                                            }
                                            // 继续启动应用
                                            continueAppLaunch()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            Toast.makeText(
                                                this@StartSplashActivity,
                                                "下载失败，请稍后重试",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            continueAppLaunch()
                                        }
                                    },
                                    onCancel = {
                                        continueAppLaunch()
                                    }
                                )

                                // 在 Activity 销毁时关闭对话框
                                lifecycle.addObserver(object : DefaultLifecycleObserver {
                                    override fun onDestroy(owner: LifecycleOwner) {
                                        dialog?.dismiss()
                                    }
                                })
                            } else {
                                continueAppLaunch()
                            }
                        } else {
                            continueAppLaunch()
                        }
                    }
                } ?: run {
                    continueAppLaunch()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                continueAppLaunch()
            }
        }
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

    private fun continueAppLaunch() {
        if (!isFinishing) {
            requestPermission()
        } else {
            startStartActivity()
        }
    }

    private fun requestPermission() {
        if (isFinishing) {
            startStartActivity()
            return
        }

        val requiredPermissions = getRequiredPermissions()

        try {
            XXPermissions.with(this)
                .permission(*requiredPermissions)
                .request(object : OnPermissionCallback {
                    override fun onGranted(permissions: List<String>, all: Boolean) {
                        if (!isFinishing) {
                            hasPermissionGranted = true
                            if (isAnimationEnded) {
                                startStartActivity()
                            }
                        }
                    }

                    override fun onDenied(permissions: List<String>, quick: Boolean) {
                        if (isFinishing) return

                        if (quick) {
                            Toast.makeText(
                                this@StartSplashActivity,
                                R.string.common_permission_fail,
                                Toast.LENGTH_SHORT
                            ).show()
                            XXPermissions.startPermissionActivity(
                                this@StartSplashActivity,
                                permissions
                            )
                        } else {
                            Toast.makeText(
                                this@StartSplashActivity,
                                R.string.common_permission_hint,
                                Toast.LENGTH_SHORT
                            ).show()
                            handler.postDelayed({
                                if (!isFinishing) {
                                    requestPermission()
                                }
                            }, 1000)
                        }
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
            startStartActivity()
        }
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

    override fun onAnimationStart(animation: Animation) {
        // 动画开始时的处理
    }

    override fun onAnimationEnd(animation: Animation) {
        isAnimationEnded = true
        requestPermission()
        if (hasPermissionGranted) {
            startStartActivity()
        }
    }

    override fun onAnimationRepeat(animation: Animation) {
        // 动画重复时的处理
    }

    private fun startStartActivity() {
        try {
            if (!isFinishing) {
                startActivity(Intent(this@StartSplashActivity, StartActivity::class.java))
                finish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
