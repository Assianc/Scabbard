package com.example.scabbard

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class AppearanceActivity : AppCompatActivity() {

    private val iconAliases = listOf(
        "MainActivity.IconAlternative1",
        "MainActivity.IconAlternative2",
        "MainActivity.IconAlternative3",
        "MainActivity.IconAlternative4",
        "MainActivity.IconAlternative5",
        "MainActivity.IconAlternative6",
        "MainActivity.IconAlternative7"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appearance)

        // 设置工具栏
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "外观设置"

        // 设置图标点击事件
        val iconIds = listOf(
            R.id.icon_1, R.id.icon_2, R.id.icon_3, 
            R.id.icon_4, R.id.icon_5, R.id.icon_6, 
            R.id.icon_7
        )

        iconIds.forEachIndexed { index, iconId ->
            findViewById<ImageView>(iconId)?.setOnClickListener {
                changeAppIcon(iconAliases[index])
            }
        }
    }

    private fun changeAppIcon(activityName: String) {
        try {
            val pm = packageManager
            
            // 禁用所有图标别名
            iconAliases.forEach { alias ->
                val component = ComponentName(this, "$packageName.$alias")
                pm.setComponentEnabledSetting(
                    component,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }

            // 启用选中的图标
            val selectedComponent = ComponentName(this, "$packageName.$activityName")
            pm.setComponentEnabledSetting(
                selectedComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            // 显示确认对话框
            AlertDialog.Builder(this)
                .setTitle("更换图标")
                .setMessage("图标更换成功，需要重启应用才能生效，是否立即重启？")
                .setPositiveButton("重启") { _, _ ->
                    restartApp()
                }
                .setNegativeButton("稍后") { dialog, _ ->
                    dialog.dismiss()
                    Toast.makeText(this, "请手动重启应用以完成图标更换", Toast.LENGTH_SHORT).show()
                }
                .show()

        } catch (e: Exception) {
            Toast.makeText(this, "更改图标失败: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun restartApp() {
        val intent = Intent(this, StartSplashActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
        Runtime.getRuntime().exit(0)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 