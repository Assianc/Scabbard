package com.example.scabbard.update

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

class UpdateChecker {
    companion object {
        private const val GITHUB_API_URL = "https://api.github.com/repos/Assianc/Scabbard/releases/latest"
        private const val CURRENT_VERSION = "3.2.1" // 当前应用版本号
    }

    data class UpdateInfo(
        val latestVersion: String,
        val updateUrl: String,
        val updateDescription: String,
        val forceUpdate: Boolean
    )

    suspend fun checkForUpdates(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            // 在主线程显示检查更新的Toast
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "正在检查更新...", Toast.LENGTH_SHORT).show()
            }
            
            val connection = URL(GITHUB_API_URL).openConnection()
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            
            val response = connection.getInputStream().bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            
            // 从GitHub release获取信息
            val tagName = json.getString("tag_name").removePrefix("v") // 移除版本号前的'v'前缀
            val body = json.getString("body")
            
            // 获取apk下载链接
            val assets = json.getJSONArray("assets")
            var apkUrl = ""
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk")) {
                    apkUrl = asset.getString("browser_download_url")
                    break
                }
            }

            // 检查更新说明中是否包含强制更新标记
            val forceUpdate = body.contains("[强制更新]")
            
            // 在主线程显示检查结果的Toast
            withContext(Dispatchers.Main) {
                val message = if (shouldUpdate(tagName)) {
                    "发现新版本：$tagName"
                } else {
                    "当前已是最新版本"
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
            
            return@withContext UpdateInfo(
                latestVersion = tagName,
                updateUrl = apkUrl,
                updateDescription = body,
                forceUpdate = forceUpdate
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // 在主线程显示错误提示
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "检查更新失败", Toast.LENGTH_SHORT).show()
            }
            return@withContext null
        }
    }

    fun shouldUpdate(latestVersion: String): Boolean {
        return compareVersions(latestVersion, CURRENT_VERSION) > 0
    }

    private fun compareVersions(version1: String, version2: String): Int {
        val v1Parts = version1.split(".")
        val v2Parts = version2.split(".")
        
        for (i in 0 until minOf(v1Parts.size, v2Parts.size)) {
            val v1 = v1Parts[i].toIntOrNull() ?: 0
            val v2 = v2Parts[i].toIntOrNull() ?: 0
            if (v1 != v2) {
                return v1.compareTo(v2)
            }
        }
        return v1Parts.size.compareTo(v2Parts.size)
    }

    fun showUpdateDialog(
        context: Context,
        updateInfo: UpdateInfo,
        onConfirm: () -> Unit,
        onCancel: () -> Unit
    ) {
        val builder = AlertDialog.Builder(context)
        
        if (shouldUpdate(updateInfo.latestVersion)) {
            builder.setTitle("发现新版本")
                .setMessage("""
                    当前版本：$CURRENT_VERSION
                    最新版本：${updateInfo.latestVersion}
                    
                    更新内容：
                    ${updateInfo.updateDescription}
                """.trimIndent())
                .setPositiveButton("立即更新") { _, _ -> onConfirm() }

            if (!updateInfo.forceUpdate) {
                builder.setNegativeButton("稍后再说") { _, _ -> onCancel() }
            }
            
            val dialog = builder.create()
            dialog.setCancelable(!updateInfo.forceUpdate)
            dialog.show()
        } else {
            builder.setTitle("检查更新")
                .setMessage("当前已是最新版本")
                .setPositiveButton("确定") { _, _ -> onCancel() }
                .create()
                .show()
        }
    }
} 