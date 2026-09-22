package com.assiance.scabbard.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** 从 Compose 的 LocalContext 里挖出宿主 Activity，权限申请之类的 API 需要它。 */
fun Context.findActivity(): Activity? {
    var context: Context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
