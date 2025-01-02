package com.example.scabbard.update

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL

class UpdateChecker {
    companion object {
        private const val GITHUB_API_URL = "https://api.github.com/repos/Assianc/Scabbard/releases/latest"
    }

    data class UpdateInfo(
        val latestVersion: String,
        val updateUrl: String,
        val updateDescription: String,
        val forceUpdate: Boolean
    )

    fun getCurrentVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            "0.0.0"
        }
    }

    suspend fun checkForUpdates(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "正在检查更新...", Toast.LENGTH_SHORT).show()
            }
            
            val connection = URL(GITHUB_API_URL).openConnection()
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            
            val response = connection.getInputStream().bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            
            val tagName = json.getString("tag_name").removePrefix("v")
            val body = json.getString("body")
            
            var apkUrl = ""
            val assets = json.getJSONArray("assets")
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk")) {
                    apkUrl = asset.getString("browser_download_url")
                    break
                }
            }

            val forceUpdate = body.contains("[强制更新]")
            
            withContext(Dispatchers.Main) {
                val message = if (shouldUpdate(tagName, getCurrentVersion(context))) {
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
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "检查更新失败", Toast.LENGTH_SHORT).show()
            }
            return@withContext null
        }
    }

    fun shouldUpdate(latestVersion: String, currentVersion: String): Boolean {
        return compareVersions(latestVersion, currentVersion) > 0
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
    ): AlertDialog? {
        if (context is Activity && context.isFinishing) {
            onCancel()
            return null
        }

        val dialogContext = if (context is Activity) {
            context
        } else {
            ContextThemeWrapper(context, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
        }

        return try {
            val dialog = AlertDialog.Builder(dialogContext)
                .setTitle("发现新版本")
                .setMessage("是否更新到最新版本？\n\n${updateInfo.updateDescription}")
                .setCancelable(!updateInfo.forceUpdate)
                .setPositiveButton("更新") { dialog, _ ->
                    dialog.dismiss()
                    onConfirm()
                }
                .apply {
                    if (!updateInfo.forceUpdate) {
                        setNegativeButton("取消") { dialog, _ ->
                            dialog.dismiss()
                            onCancel()
                        }
                    }
                }
                .create()

            if (context is Activity && !context.isFinishing) {
                dialog.show()
            }
            dialog
        } catch (e: Exception) {
            e.printStackTrace()
            onCancel()
            null
        }
    }

    private fun startDownload(context: Context, downloadUrl: String, version: String) {
        try {
            val fileName = "Scabbard-${version}.apk"
            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle("下载更新")
                .setDescription("正在下载 Scabbard ${version}")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(context, "开始下载更新", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "下载失败，请稍后重试", Toast.LENGTH_SHORT).show()
        }
    }
} 