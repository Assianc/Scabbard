package com.assiance.scabbard

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.assiance.scabbard.utils.IconManager
import android.animation.ValueAnimator
import android.graphics.Matrix
import android.os.Build
import android.view.animation.LinearInterpolator
import androidx.annotation.RequiresApi
import com.assiance.scabbard.utils.GradientAnimManager

class AboutActivity : AppCompatActivity() {
    private lateinit var appIcon: ImageView
    private var gradientAnimator: ValueAnimator? = null
    private var gradientMatrix: Matrix = Matrix()
    private var translateX: Float = 0f
    private val iconChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.assiance.scabbard.ACTION_ICON_CHANGED") {
                appIcon.setImageResource(IconManager.getCurrentIconResourceId(this@AboutActivity))
            }
        }
    }
    private val gradientChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.assiance.scabbard.ACTION_GRADIENT_CHANGED") {
                val scabbardText = findViewById<TextView>(R.id.scabbard_title)
                val paint = scabbardText.paint
                val width = paint.measureText(scabbardText.text.toString())

                val currentStyle = GradientAnimManager.getCurrentStyle(this@AboutActivity)
                val textShader = GradientAnimManager.createGradient(
                    width,
                    scabbardText.textSize,
                    currentStyle
                )

                paint.shader = textShader
                scabbardText.invalidate()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        // 设置工具栏
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "关于"

        // 设置版本信息
        val versionInfo = findViewById<TextView>(R.id.version_info)
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            versionInfo.text = "Version $versionName($versionCode)"
        } catch (_: Exception) {
            versionInfo.text = "Version 未知"
        }

        // 设置开发者点击事件
        findViewById<LinearLayout>(R.id.dev_1).setOnClickListener {
            openGitHub("cuxt")
        }

        findViewById<LinearLayout>(R.id.dev_2).setOnClickListener {
            openGitHub("Assianc")
        }

        appIcon = findViewById<ImageView>(R.id.app_icon)
        appIcon.setImageResource(IconManager.getCurrentIconResourceId(this))

        // 找到 Scabbard 文字的 TextView
        val scabbardText = findViewById<TextView>(R.id.scabbard_title)

        // 创建动态渐变效果
        val paint = scabbardText.paint
        val width = paint.measureText(scabbardText.text.toString())

        // 使用 GradientAnimManager 创建渐变
        val currentStyle = GradientAnimManager.getCurrentStyle(this@AboutActivity)
        val textShader = GradientAnimManager.createGradient(
            width,
            scabbardText.textSize,
            currentStyle
        )

        // 设置着色器
        paint.shader = textShader

        // 创建动画
        gradientAnimator = ValueAnimator.ofFloat(0f, width)  // 只需要移动一个宽度的距离
        gradientAnimator?.apply {
            duration = 2100 // 稍微加快动画速度
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART  // 使用 RESTART 模式
            interpolator = LinearInterpolator()

            addUpdateListener { animator ->
                translateX = animator.animatedValue as Float
                gradientMatrix.setTranslate(-translateX, 0f)
                textShader.setLocalMatrix(gradientMatrix)
                scabbardText.invalidate()
            }

            start()
        }

        // 修改广播注册代码，添加 exported 标志
        val filter = IntentFilter("com.assiance.scabbard.ACTION_ICON_CHANGED")
        registerReceiver(iconChangeReceiver, filter, RECEIVER_NOT_EXPORTED)

        val gradientFilter = IntentFilter("com.assiance.scabbard.ACTION_GRADIENT_CHANGED")
        registerReceiver(gradientChangeReceiver, gradientFilter, RECEIVER_NOT_EXPORTED)
    }

    override fun onDestroy() {
        // 停止动画
        gradientAnimator?.cancel()
        gradientAnimator = null

        super.onDestroy()
        try {
            unregisterReceiver(gradientChangeReceiver)
            unregisterReceiver(iconChangeReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openGitHub(username: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/$username"))
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
} 