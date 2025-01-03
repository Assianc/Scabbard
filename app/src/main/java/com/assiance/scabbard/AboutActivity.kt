package com.assiance.scabbard

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.assiance.scabbard.utils.IconManager

class AboutActivity : AppCompatActivity() {
    private lateinit var appIcon: ImageView
    private val iconChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.assiance.scabbard.ACTION_ICON_CHANGED") {
                appIcon.setImageResource(IconManager.getCurrentIconResourceId(this@AboutActivity))
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
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            versionInfo.text = "Version $versionName"
        } catch (e: Exception) {
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
        
        // 创建渐变色画笔
        val paint = scabbardText.paint
        val width = paint.measureText(scabbardText.text.toString())
        val textShader = LinearGradient(
            0f, 0f, width, scabbardText.textSize,
            intArrayOf(
                Color.parseColor("#2196F3"),  // 起始颜色
                Color.parseColor("#9C27B0")   // 结束颜色
            ),
            null,
            Shader.TileMode.CLAMP
        )
        
        // 应用渐变效果
        scabbardText.paint.shader = textShader

        // 修改广播注册代码，添加 exported 标志
        val filter = IntentFilter("com.assiance.scabbard.ACTION_ICON_CHANGED")
        registerReceiver(iconChangeReceiver, filter, RECEIVER_NOT_EXPORTED)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
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
        onBackPressed()
        return true
    }
} 