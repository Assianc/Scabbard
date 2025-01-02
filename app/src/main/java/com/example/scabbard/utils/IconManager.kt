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
        val pm = context.packageManager
        for ((alias, resourceId) in iconMap) {
            val componentName = ComponentName(context, "${context.packageName}.$alias")
            val enabled = pm.getComponentEnabledSetting(componentName) == 
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            if (enabled) {
                return resourceId
            }
        }
        return R.mipmap.jianqiao5 // 默认图标
    }
} 