package com.assiance.scabbard

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import com.assiance.memo.MainActivityMemo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.assiance.scabbard.update.UpdateChecker
import android.widget.ImageView
import com.assiance.scabbard.utils.IconManager
import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.widget.Toast
import android.app.AlertDialog

open class StartActivity : AppCompatActivity() {

    private lateinit var startButton: Button
    private lateinit var memoButton: Button
    private lateinit var moreButton: Button
    private val updateChecker = UpdateChecker()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_allocator)

        // 更新当前图标
        findViewById<ImageView>(R.id.app_icon)?.setImageResource(
            IconManager.getCurrentIconResourceId(this)
        )

        startButton = findViewById(R.id.start_button)
        memoButton = findViewById(R.id.memo_button)
        moreButton = findViewById(R.id.toolbar)

        // 设置更多按钮点击事件
        moreButton.setOnClickListener { view ->
            showPopupMenu(view)
        }

        startButton.setOnClickListener {
            val intent = Intent(this@StartActivity, MainActivity::class.java)
            startActivity(intent)
        }

        memoButton.setOnClickListener {
            val intent = Intent(this@StartActivity, MainActivityMemo::class.java)
            startActivity(intent)
        }
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.menu_more, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_about -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                    true
                }
                R.id.menu_appearance -> {
                    startActivity(Intent(this, AppearanceActivity::class.java))
                    true
                }
                R.id.menu_update -> {
                    checkForUpdates()
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
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
                            """.trimIndent(),
                            forceUpdate = false
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

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(this, "开始下载更新", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "下载失败，请稍后重试", Toast.LENGTH_SHORT).show()
        }
    }
}

