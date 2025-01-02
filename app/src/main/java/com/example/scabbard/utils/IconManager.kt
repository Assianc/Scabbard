package com.example.scabbard.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.example.scabbard.R

object IconManager {
    private val iconMap = mapOf(
        "MainActivity.IconAlternative1" to R.mipmap.jianqiao1,
        "MainActivity.IconAlternative2" to R.mipmap.jianqiao2,
        "MainActivity.IconAlternative3" to R.mipmap.jianqiao3,
        "MainActivity.IconAlternative4" to R.mipmap.jianqiao4,
        "MainActivity.IconAlternative5" to R.mipmap.jianqiao5,
        "MainActivity.IconAlternative6" to R.mipmap.jianqiao6,
        "MainActivity.IconAlternative7" to R.mipmap.jianqiao7,
        "MainActivity.Default" to R.mipmap.jianqiao5
    )

    fun getCurrentIconResourceId(context: Context): Int {
        // 从 SharedPreferences 读取当前设置的图标
        val savedIcon = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getString("current_icon", null)
            
        // 如果有保存的图标设置，返回对应的资源ID
        if (!savedIcon.isNullOrEmpty()) {
            return iconMap[savedIcon] ?: R.mipmap.jianqiao5
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
                        .edit()
                        .putString("current_icon", alias)
                        .apply()
                    return resourceId
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // 如果都没找到，使用默认图标并保存设置
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("current_icon", "MainActivity.Default")
            .apply()
        return R.mipmap.jianqiao5
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

            // 保存当前图标设置
            context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                .edit()
                .putString("current_icon", activityName)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
} 