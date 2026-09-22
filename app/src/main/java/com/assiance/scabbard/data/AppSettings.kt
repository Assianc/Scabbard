package com.assiance.scabbard.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.assiance.scabbard.utils.GradientAnimManager
import com.assiance.scabbard.utils.IconManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全 App 设置项的唯一数据源，对外用 StateFlow 暴露，供 Compose 订阅。
 *
 * 改造前 AppearanceActivity 是这么改抽屉透明度的：从 ScabbardApplication.allActivities
 * 里翻出 StartActivity 实例，拿到它的 nav_view 直接改背景色。单 Activity 之后那条链路
 * 不成立，而且它本身就持有 Activity 引用（泄漏）。现在改成"设置改状态、Compose 观察状态"，
 * 两边互不认识。
 *
 * prefs 文件名和键名与改造前**完全一致**（app_settings / current_icon / other_icon /
 * splash_icon / gradient_style / nav_alpha），保证老用户已有设置不丢。
 */
object AppSettings {

    const val PREFS_NAME = "app_settings"
    const val KEY_CURRENT_ICON = "current_icon"
    const val KEY_OTHER_ICON = "other_icon"
    const val KEY_SPLASH_ICON = "splash_icon"
    const val KEY_GRADIENT_STYLE = "gradient_style"
    const val KEY_NAV_ALPHA = "nav_alpha"

    const val DEFAULT_NAV_ALPHA = 230
    const val DEFAULT_ICON_ALIAS = "MainActivity.Default"

    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null

    private val _gradientStyle = MutableStateFlow(GradientAnimManager.GradientStyle.BLUE_PURPLE)
    val gradientStyle: StateFlow<GradientAnimManager.GradientStyle> = _gradientStyle.asStateFlow()

    private val _navAlpha = MutableStateFlow(DEFAULT_NAV_ALPHA)
    val navAlpha: StateFlow<Int> = _navAlpha.asStateFlow()

    private val _currentIconAlias = MutableStateFlow(DEFAULT_ICON_ALIAS)
    val currentIconAlias: StateFlow<String> = _currentIconAlias.asStateFlow()

    /** 当前应用图标资源，抽屉头部和关于页用。 */
    private val _appIconRes = MutableStateFlow(0)
    val appIconRes: StateFlow<Int> = _appIconRes.asStateFlow()

    /** 开场动画图标资源。 */
    private val _splashIconRes = MutableStateFlow(0)
    val splashIconRes: StateFlow<Int> = _splashIconRes.asStateFlow()

    /** 在 Application.onCreate 里调一次。重复调用无副作用。 */
    fun init(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx
        if (prefs == null) {
            prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            refresh()
        }
    }

    /** 从 prefs 重新同步一次内存状态。外部直接写过 prefs 之后调它。 */
    fun refresh() {
        val ctx = appContext ?: return
        _gradientStyle.value = GradientAnimManager.getCurrentStyle(ctx)
        _navAlpha.value = GradientAnimManager.getCurrentNavAlpha(ctx)
        _currentIconAlias.value = prefs?.getString(KEY_CURRENT_ICON, null) ?: DEFAULT_ICON_ALIAS
        _appIconRes.value = IconManager.getCurrentIconResourceId(ctx)
        _splashIconRes.value = IconManager.getSplashIconResourceId(ctx)
    }

    // ---- 渐变标题样式 ----

    fun setGradientStyle(style: GradientAnimManager.GradientStyle) {
        val ctx = appContext ?: return
        GradientAnimManager.setCurrentStyle(ctx, style)
        _gradientStyle.value = style
    }

    // ---- 抽屉不透明度 ----

    fun setNavAlpha(alpha: Int) {
        val ctx = appContext ?: return
        val clamped = alpha.coerceIn(0, 255)
        GradientAnimManager.setCurrentNavAlpha(ctx, clamped)
        _navAlpha.value = clamped
    }

    // ---- 图标 ----

    /** 只换应用图标（activity-alias 组件开关）。 */
    fun setAppIconOnly(alias: String) {
        val ctx = appContext ?: return
        IconManager.setCurrentIcon(ctx, alias)
        _currentIconAlias.value = alias
        _appIconRes.value = IconManager.getCurrentIconResourceId(ctx)
    }

    /** 只换开场动画图标。 */
    fun setSplashIconOnly(alias: String) {
        val ctx = appContext ?: return
        IconManager.setSplashIconOnly(ctx, alias)
        _splashIconRes.value = IconManager.getSplashIconResourceId(ctx)
    }

    /** 应用图标 + 开场动画图标一起换。 */
    fun setAllIcons(alias: String) {
        val ctx = appContext ?: return
        IconManager.setAllIcons(ctx, alias)
        _currentIconAlias.value = alias
        _appIconRes.value = IconManager.getCurrentIconResourceId(ctx)
        _splashIconRes.value = IconManager.getSplashIconResourceId(ctx)
    }

    /** AppearanceScreen 的 14 个候选别名。与应用清单里的 activity-alias 一一对应，顺序不能动。 */
    val iconAliases: List<String> = List(14) { "MainActivity.IconAlternative${it + 1}" }

    /** 别名 → mipmap 资源，用于外观设置页的图标预览网格。 */
    fun iconResOf(alias: String): Int = IconManager.iconResOf(alias)
}
