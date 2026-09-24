package com.assiance.scabbard.ui.theme

/**
 * 主题色板。移植自 LoveScore 的 `data/settings/SettingsRepository.kt`，多了一档 [DYNAMIC]。
 *
 * [DYNAMIC] 是本项目原有行为（Material You 动态取色，API 31+ 从壁纸取色），作为**默认**保留 ——
 * 移植过来的八套固定色板与它并列，用户不选就还是跟随壁纸。
 *
 * 八套固定色板的取值逐字来自 LoveScore 的 `ui/theme/Color.kt` / `Theme.kt`，只补了一处：
 * 每套的 `tertiaryContainer`。LoveScore 的 scheme 没设这个 token（8 套全部落回 M3 基线粉
 * `#FFD8E4`），而本项目的双色极光背景正是靠它画第二道光斑 —— 不补的话薄荷绿主题的背景里
 * 会飘出一团粉。补值的口径见 [fixedColorSchemeFor] 上方注释。
 *
 * [AURORA] 与前七套不同：它不只换配色，还会把全 App 背景从双色极光换成全屏炫彩 shader。
 * 详见 [com.assiance.scabbard.ui.component.AppBackground]。
 */
enum class ThemePalette(val title: String) {
    /** 跟随壁纸（Material You 动态取色）。默认。 */
    DYNAMIC("跟随壁纸"),

    /** 浪漫粉 */
    PINK("浪漫粉"),

    /** 天空蓝 */
    SKY("天空蓝"),

    /** 中性灰（无色彩倾向） */
    NEUTRAL("中性灰"),

    /** 薄荷绿 */
    MINT("薄荷绿"),

    /** 暮色紫 */
    PURPLE("暮色紫"),

    /** 日出橙 */
    ORANGE("日出橙"),

    /** 沙漠棕 */
    BROWN("沙漠棕"),

    /** 炫彩流光：粉紫配色 + 全屏 RuntimeShader 流动背景。 */
    AURORA("炫彩流光"),
}
