package com.assiance.scabbard

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.assiance.scabbard.utils.IconManager

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

    private var dialog: AlertDialog? = null

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
        val options = arrayOf(
            "仅更换应用图标", 
            "仅更换开场动画图标",
            "更换所有图标"
        )
        
        AlertDialog.Builder(this)
            .setTitle("图标更换选项")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> changeOnlyAppIcon(activityName)
                    1 -> changeSplashIcon(activityName)
                    2 -> changeAllIcons(activityName)
                }
            }
            .show()
    }

    private fun changeOnlyAppIcon(activityName: String) {
        AlertDialog.Builder(this)
            .setTitle("更换应用图标")
            .setMessage("确定要更换应用图标吗？更换后需要重启应用才能生效。")
            .setPositiveButton("确定") { dialog, _ ->
                dialog.dismiss()
                // 只执行图标切换
                IconManager.setCurrentIcon(this, activityName)
                restartApp()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun changeAllIcons(activityName: String) {
        AlertDialog.Builder(this)
            .setTitle("更换所有图标")
            .setMessage("确定要更换所有图标（包括应用图标、启动图标和关于界面图标）吗？更换后需要重启应用才能生效。")
            .setPositiveButton("确定") { dialog, _ ->
                dialog.dismiss()
                // 执行所有图标切换
                IconManager.setAllIcons(this, activityName)
                restartApp()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun changeSplashIcon(activityName: String) {
        AlertDialog.Builder(this)
            .setTitle("更换开场动画图标")
            .setMessage("确定要更换开场动画图标吗？更换后需要重启应用才能生效。")
            .setPositiveButton("确定") { dialog, _ ->
                dialog.dismiss()
                // 只执行开场动画图标的切换
                IconManager.setSplashIconOnly(this, activityName)
                restartApp()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun restartApp() {
        try {
            dialog?.dismiss()
            
            // 使用 PackageManager 重启应用
            val packageManager = packageManager
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            
            finishAffinity() // 结束所有 Activity
            
            if (intent != null) {
                startActivity(intent)
            }
            
            // 使用 Handler 延迟结束进程，确保新的 Intent 能够启动
            android.os.Handler(mainLooper).postDelayed({
                android.os.Process.killProcess(android.os.Process.myPid())
            }, 100)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "重启失败，请手动重启应用", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dialog?.dismiss()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 