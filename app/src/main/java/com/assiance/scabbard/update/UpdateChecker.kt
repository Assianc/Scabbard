package com.assiance.scabbard.update

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.view.ContextThemeWrapper
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class UpdateChecker {
    companion object {
        private const val GITHUB_API_URL = "https://api.github.com/repos/Assianc/Scabbard/releases/latest"
        const val LANZOU_DOWNLOAD_URL = "https://assiance.lanzoub.com/b00y9rfbud"
        const val LANZOU_PASSWORD = "7rwd"
        const val LANZOU_VERSION = "4.2.3"
    }

    data class UpdateInfo(
        val latestVersion: String,
        val updateUrl: String,
        val updateDescription: String
    )

    fun getCurrentVersion(context: Context): String? {
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

            val currentVersion = getCurrentVersion(context)

            // 先尝试从 GitHub 获取更新
            val githubUpdate = checkGithubUpdate()
            if (githubUpdate != null) {
                // 如果成功获取到 GitHub 更新信息
                currentVersion?.let {
                    if (shouldUpdate(githubUpdate.latestVersion, it)) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "发现新版本", Toast.LENGTH_SHORT).show()
                        }
                        return@withContext githubUpdate
                    } else {
                        // GitHub 检查成功但是已是最新版本
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "当前已是最新版本", Toast.LENGTH_SHORT).show()
                        }
                        return@withContext null
                    }
                }
            }

            // GitHub 检查失败，尝试蓝奏云更新
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "正在从备用源检查更新...", Toast.LENGTH_SHORT).show()
            }

            // 检查蓝奏云版本
            currentVersion?.let {
                if (shouldUpdate(LANZOU_VERSION, it)) {
                    val lanzouUpdate = checkLanzouUpdate()
                    if (lanzouUpdate != null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "发现新版本", Toast.LENGTH_SHORT).show()
                        }
                        return@withContext lanzouUpdate
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "当前已是最新版本", Toast.LENGTH_SHORT).show()
                    }
                    return@withContext null
                }
            }

            // 如果所有更新源都检查失败
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "检查更新失败", Toast.LENGTH_SHORT).show()
            }
            return@withContext null
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "检查更新失败", Toast.LENGTH_SHORT).show()
            }
            return@withContext null
        }
    }

    private suspend fun checkGithubUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
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
            
            return@withContext UpdateInfo(
                latestVersion = tagName,
                updateUrl = apkUrl,
                updateDescription = body
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    private suspend fun checkLanzouUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            return@withContext UpdateInfo(
                latestVersion = LANZOU_VERSION,
                updateUrl = LANZOU_DOWNLOAD_URL,
                updateDescription = """
                    下载说明：
                    由于网络原因难以从github获取更新
                    1. 推荐使用蓝奏云获取更新。访问作者github主页，在Scabbard中获取最新版本以及更新说明
                    2. 点击更新后继续尝试获取最新版.
                    3. 蓝奏云提取码：$LANZOU_PASSWORD
                    
                    注意：请在电脑模式下预览，否则可能无法正常下载。
                """.trimIndent()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    fun shouldUpdate(latestVersion: String, currentVersion: String): Boolean {
        return try {
            compareVersions(latestVersion, currentVersion) > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false  // 如果比较出错，默认不更新
        }
    }

    private fun compareVersions(version1: String, version2: String): Int {
        try {
            val v1Parts = version1.split(".").map { it.toInt() }
            val v2Parts = version2.split(".").map { it.toInt() }
            
            // 补齐版本号长度，较短的版本号后面补0
            val maxLength = maxOf(v1Parts.size, v2Parts.size)
            val v1Complete = v1Parts + List(maxLength - v1Parts.size) { 0 }
            val v2Complete = v2Parts + List(maxLength - v2Parts.size) { 0 }
            
            // 逐位比较版本号
            for (i in 0 until maxLength) {
                val compare = v1Complete[i].compareTo(v2Complete[i])
                if (compare != 0) {
                    return compare
                }
            }
            return 0
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果解析失败，返回0表示版本相同
            return 0
        }
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

        val dialogContext = context as? Activity
            ?: ContextThemeWrapper(context, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)

        return try {
            val dialog = AlertDialog.Builder(dialogContext)
                .setTitle("发现新版本 ${updateInfo.latestVersion}")
                .setMessage(updateInfo.updateDescription)
                .setPositiveButton("确定") { dialog, _ ->
                    dialog.dismiss()
                    onConfirm()
                }
                .setNegativeButton("取消") { dialog, _ ->
                    dialog.dismiss()
                    onCancel()
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
} 