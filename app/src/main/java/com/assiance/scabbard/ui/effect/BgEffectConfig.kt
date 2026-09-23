package com.assiance.scabbard.ui.effect

/**
 * 炫彩背景的预设参数表。
 *
 * 每个预设含 4 个控制点的坐标与半径（`points`，每组 3 个 float：x / y / 半径）、
 * 3 组循环色（各 4 个 RGBA）、以及色彩插值周期 / 光照明暗 / 饱和 / 点位漂移等偏移量。
 *
 * **只保留了一组预设**：移植来源 LoveScore 存了 OS2/OS3 × PHONE/PAD × 明/暗 共 8 组，
 * 外加一个 `isOs3Effect` 开关。本项目没有平板专属布局，也没有 OS2 风格的诉求 ——
 * 那些分支全是没有人会走到的死代码，一并裁掉，只留 OS3 手机版明暗两份。
 * 将来真要做大屏适配，回 LoveScore 的 `BgEffectConfig.kt` 取 PAD 那两组即可。
 */
internal class BgEffectConfig(
    val points: FloatArray,
    val colors1: FloatArray,
    val colors2: FloatArray,
    val colors3: FloatArray,
    val colorInterpPeriod: Float,
    val lightOffset: Float,
    val saturateOffset: Float,
    val pointOffset: Float,
) {
    companion object {
        /**
         * 4 个控制点：前两台在右上与右下、后两台在左下与左上，半径都是 1.0 ——
         * 也就是每颗色团都大到铺满整屏，靠 mix 的软边叠出层次，而不是画成四颗可见的球。
         * 明暗预设共用同一份点位，差别只在颜色与偏移量。
         */
        private val OS3_PHONE_POINTS = floatArrayOf(
            0.8f, 0.2f, 1.0f,
            0.8f, 0.9f, 1.0f,
            0.2f, 0.9f, 1.0f,
            0.2f, 0.2f, 1.0f,
        )

        /**
         * 浅色预设：alpha 全是 1.0（色团不透明，靠位置错开叠出三组色）。
         * 色彩循环快（[colorInterpPeriod] = 5s），配 `lightOffset` 正 0.1 —— 整体往亮走，
         * 所以浅色下是"流光"而不是"暗涌"。
         */
        internal val OS3_PHONE_LIGHT = BgEffectConfig(
            points = OS3_PHONE_POINTS,
            colors1 = floatArrayOf(
                1.0f, 0.9f, 0.94f, 1.0f, 1.0f, 0.84f, 0.89f, 1.0f,
                0.97f, 0.73f, 0.82f, 1.0f, 0.64f, 0.65f, 0.98f, 1.0f,
            ),
            colors2 = floatArrayOf(
                0.58f, 0.74f, 1.0f, 1.0f, 1.0f, 0.9f, 0.93f, 1.0f,
                0.74f, 0.76f, 1.0f, 1.0f, 0.97f, 0.77f, 0.84f, 1.0f,
            ),
            colors3 = floatArrayOf(
                0.98f, 0.86f, 0.9f, 1.0f, 0.6f, 0.73f, 0.98f, 1.0f,
                0.92f, 0.93f, 1.0f, 1.0f, 0.56f, 0.69f, 1.0f, 1.0f,
            ),
            colorInterpPeriod = 5.0f,
            lightOffset = 0.1f,
            saturateOffset = 0.2f,
            pointOffset = 0.2f,
        )

        /**
         * 深色预设：alpha 压到 0.4~0.6。这不是"颜色淡"，而是刻意让 shader 输出的
         * 预乘 alpha 与底下那层 surface 实色**混出层次** —— 所以背景层必须先铺一层实色，
         * 顺序反了就等于把炫彩整体乘掉一大半（见 [com.assiance.scabbard.ui.component.AppBackground]）。
         *
         * 色彩循环比浅色慢（8s），`lightOffset` 为 0，`pointOffset` 反而更大（0.4）——
         * 深色下靠"点位漂移"而不是"明暗变化"制造流动感。
         */
        internal val OS3_PHONE_DARK = BgEffectConfig(
            points = OS3_PHONE_POINTS,
            colors1 = floatArrayOf(
                0.2f, 0.06f, 0.88f, 0.4f, 0.3f, 0.14f, 0.55f, 0.5f,
                0.0f, 0.64f, 0.96f, 0.5f, 0.11f, 0.16f, 0.83f, 0.4f,
            ),
            colors2 = floatArrayOf(
                0.07f, 0.15f, 0.79f, 0.5f, 0.62f, 0.21f, 0.67f, 0.5f,
                0.06f, 0.25f, 0.84f, 0.5f, 0.0f, 0.2f, 0.78f, 0.5f,
            ),
            colors3 = floatArrayOf(
                0.58f, 0.3f, 0.74f, 0.4f, 0.27f, 0.18f, 0.6f, 0.5f,
                0.66f, 0.26f, 0.62f, 0.5f, 0.12f, 0.16f, 0.7f, 0.6f,
            ),
            colorInterpPeriod = 8.0f,
            lightOffset = 0.0f,
            saturateOffset = 0.17f,
            pointOffset = 0.4f,
        )

        /** 按深浅模式取预设。 */
        fun of(isDark: Boolean): BgEffectConfig = if (isDark) OS3_PHONE_DARK else OS3_PHONE_LIGHT
    }
}
