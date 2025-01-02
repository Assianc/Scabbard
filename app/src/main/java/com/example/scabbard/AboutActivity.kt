package com.example.scabbard

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.scabbard.utils.IconManager

class AboutActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
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

        val appIcon = findViewById<ImageView>(R.id.app_icon)
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