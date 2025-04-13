package com.assiance.scabbard.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.assiance.scabbard.R
import androidx.core.content.edit

object IconManager {
    private val iconMap = mapOf(
        "MainActivity.IconAlternative1" to R.mipmap.scabbard11,
        "MainActivity.IconAlternative2" to R.mipmap.scabbard12,
        "MainActivity.IconAlternative3" to R.mipmap.scabbard13,
        "MainActivity.IconAlternative4" to R.mipmap.scabbard14,
        "MainActivity.IconAlternative5" to R.mipmap.scabbard15,
        "MainActivity.IconAlternative6" to R.mipmap.scabbard16,
        "MainActivity.IconAlternative7" to R.mipmap.scabbard17,
        "MainActivity.IconAlternative8" to R.mipmap.scabbard21,
        "MainActivity.IconAlternative9" to R.mipmap.scabbard22,
        "MainActivity.IconAlternative10" to R.mipmap.scabbard23,
        "MainActivity.IconAlternative11" to R.mipmap.scabbard24,
        "MainActivity.IconAlternative12" to R.mipmap.scabbard25,
        "MainActivity.IconAlternative13" to R.mipmap.scabbard26,
        "MainActivity.IconAlternative14" to R.mipmap.scabbard27,
        "MainActivity.Default" to R.mipmap.scabbard14
    )

    fun getCurrentIconResourceId(context: Context): Int {
        // 先检查是否有其他图标设置（关于界面和启动动画）
        val otherIcon = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getString("other_icon", null)

        if (!otherIcon.isNullOrEmpty()) {
            return iconMap[otherIcon] ?: R.mipmap.scabbard14
        }

        // 如果没有其他图标设置，则返回应用图标
        val savedIcon = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getString("current_icon", null)

        if (!savedIcon.isNullOrEmpty()) {
            return iconMap[savedIcon] ?: R.mipmap.scabbard14
        }

        // 如果没有保存的设置，检查当前启用的组件
        val pm = context.packageManager
        for ((alias, resourceId) in iconMap) {
            val componentName = ComponentName(context, "${context.packageName}.$alias")
            try {
                val state = pm.getComponentEnabledSetting(componentName)
                if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    // 找到启用的图标后，保存到 SharedPreferences
                    context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                        .edit {
                            putString("current_icon", alias)
                        }
                    return resourceId
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 如果都没找到，使用默认图标并保存设置
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit {
                putString("current_icon", "MainActivity.Default")
            }
        return R.mipmap.scabbard14
    }

    fun setCurrentIcon(context: Context, activityName: String) {
        try {
            val pm = context.packageManager

            // 禁用所有图标别名
            iconMap.keys.forEach { alias ->
                val component = ComponentName(context, "${context.packageName}.$alias")
                try {
                    pm.setComponentEnabledSetting(
                        component,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 启用选中的图标
            val selectedComponent = ComponentName(context, "${context.packageName}.$activityName")
            pm.setComponentEnabledSetting(
                selectedComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            // 保存当前图标设置，同时清除其他图标设置
            context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                .edit {
                    putString("current_icon", activityName)
                        .remove("other_icon")  // 清除其他图标设置
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getSplashIconResourceId(context: Context): Int {
        return context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getInt("splash_icon", R.mipmap.scabbard24)
    }

    fun setSplashIconResourceId(context: Context, resourceId: Int) {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit {
                putInt("splash_icon", resourceId)
            }
    }

    fun setAllIcons(context: Context, activityName: String) {
        // 设置应用图标
        setCurrentIcon(context, activityName)

        // 设置启动图标
        setSplashIconResourceId(context, iconMap[activityName] ?: R.mipmap.scabbard24)

        // 清除其他图标设置
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit {
                remove("other_icon")
            }

        // 发送广播通知其他界面更新
        val intent = Intent("com.assiance.scabbard.ACTION_ICON_CHANGED")
        context.sendBroadcast(intent)
    }

    fun setSplashIconOnly(context: Context, activityName: String) {
        // 只设置启动图标
        setSplashIconResourceId(context, iconMap[activityName] ?: R.mipmap.scabbard24)
    }
} 