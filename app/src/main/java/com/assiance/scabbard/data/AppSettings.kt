package com.assiance.scabbard.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.assiance.scabbard.ui.theme.AuroraPreset
import com.assiance.scabbard.ui.theme.ThemePalette
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

    // ---- 主题色板 / 背景 / 玻璃（移植自 LoveScore，键名是本项目自己的命名空间，
    //      与那边的 settings key 无关，不需要对齐）----
    const val KEY_THEME_PALETTE = "theme_palette"
    const val KEY_AURORA_PRESET = "aurora_preset"
    const val KEY_AURORA_VEIL = "aurora_veil"
    const val KEY_GLASS_BLUR = "glass_blur"

    const val DEFAULT_NAV_ALPHA = 230
    const val DEFAULT_ICON_ALIAS = "MainActivity.Default"

    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null

    private val _gradientStyle = MutableStateFlow(GradientAnimManager.GradientStyle.BLUE_PURPLE)
    val gradientStyle: StateFlow<GradientAnimManager.GradientStyle> = _gradientStyle.asStateFlow()

    private val _navAlpha = MutableStateFlow(DEFAULT_NAV_ALPHA)
    val navAlpha: StateFlow<Int> = _navAlpha.asStateFlow()

    /**
     * 主题色板。默认 [ThemePalette.DYNAMIC]，也就是本项目的原有行为（跟随壁纸取色）。
     *
     * 老用户升上来时 prefs 里没有这个键，读出来就是默认值 → 观感与升级前完全一致，
     * 不会因为多了一套色板就把人家的配色改掉。
     */
    private val _themePalette = MutableStateFlow(ThemePalette.DYNAMIC)
    val themePalette: StateFlow<ThemePalette> = _themePalette.asStateFlow()

    /** 双色极光背景的配色预设。默认跟随主题色板。 */
    private val _auroraPreset = MutableStateFlow(AuroraPreset.FOLLOW_THEME)
    val auroraPreset: StateFlow<AuroraPreset> = _auroraPreset.asStateFlow()

    /**
     * 炫彩流光背景是否再压一层主题色遮罩。
     *
     * 默认关 —— 关着才是原版浓度。开着会在炫彩之上补一层 surface 薄纱，
     * 给「觉得炫彩太跳、正文压不住」的用户留一条退路。
     */
    private val _auroraVeilEnabled = MutableStateFlow(false)
    val auroraVeilEnabled: StateFlow<Boolean> = _auroraVeilEnabled.asStateFlow()

    /**
     * 液态玻璃是否启用背景模糊。
     *
     * 默认开 —— 这是原行为。关掉后玻璃退化成「不模糊的玻璃质感」，
     * 具体表现见 [com.assiance.scabbard.ui.screen.HomeEntryCard]。
     */
    private val _glassBlurEnabled = MutableStateFlow(true)
    val glassBlurEnabled: StateFlow<Boolean> = _glassBlurEnabled.asStateFlow()

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
        _themePalette.value = readEnum(KEY_THEME_PALETTE, ThemePalette.entries, ThemePalette.DYNAMIC)
        _auroraPreset.value =
            readEnum(KEY_AURORA_PRESET, AuroraPreset.entries, AuroraPreset.FOLLOW_THEME)
        _auroraVeilEnabled.value = prefs?.getBoolean(KEY_AURORA_VEIL, false) ?: false
        _glassBlurEnabled.value = prefs?.getBoolean(KEY_GLASS_BLUR, true) ?: true
    }

    /**
     * 按枚举名读一项设置，读不出来就回落到 [fallback]。
     *
     * 之所以逐项比对而不是直接 `valueOf`：枚举名是**存在磁盘上的**，将来增删或改名某个
     * 枚举项时，老用户 prefs 里那条旧名字就成了非法值 —— 直接 `valueOf` 会抛异常，
     * 而 [refresh] 是在 `Application.onCreate` 里调的，等于开屏就崩。读不出来退默认值，
     * 用户最多丢一次主题选择。
     */
    private fun <T : Enum<T>> readEnum(key: String, entries: List<T>, fallback: T): T {
        val name = prefs?.getString(key, null) ?: return fallback
        return entries.firstOrNull { it.name == name } ?: fallback
    }

    // ---- 主题色板 ----

    fun setThemePalette(palette: ThemePalette) {
        prefs?.edit { putString(KEY_THEME_PALETTE, palette.name) }
        _themePalette.value = palette
    }

    // ---- 双色极光配色预设 ----

    fun setAuroraPreset(preset: AuroraPreset) {
        prefs?.edit { putString(KEY_AURORA_PRESET, preset.name) }
        _auroraPreset.value = preset
    }

    // ---- 炫彩遮罩 ----

    fun setAuroraVeilEnabled(enabled: Boolean) {
        prefs?.edit { putBoolean(KEY_AURORA_VEIL, enabled) }
        _auroraVeilEnabled.value = enabled
    }

    // ---- 液态玻璃模糊 ----

    fun setGlassBlurEnabled(enabled: Boolean) {
        prefs?.edit { putBoolean(KEY_GLASS_BLUR, enabled) }
        _glassBlurEnabled.value = enabled
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
