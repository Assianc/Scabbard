package com.assiance.scabbard.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/*
 * ============================ 八套固定色板的取色 ============================
 *
 * 色值逐字移植自 LoveScore 的 `ui/theme/Color.kt` 与 `Theme.kt`，命名保留其语义
 * （色名 + 用途），便于两边对照。改动前先回去核对 —— 本文件里能对上 LoveScore 的值
 * 占了绝大多数，只有三处是搬不过来的，各自在定义处注明：
 *
 *   1. 每套的 `tertiaryContainer` —— LoveScore 八套 scheme 都没设这个 token；
 *   2. `outline` / `outlineVariant` —— LoveScore 没设，见 [outlineFrom] 的规则；
 *   3. `surfaceContainer*` 阶梯 / `surfaceDim` / `surfaceBright` —— LoveScore 没设，
 *      见 [SurfaceLadder] 的规则。
 *
 * 深色模式的 `secondaryContainer` 同样没设，这里取该色板的深色 `surfaceVariant` ——
 * 留 M3 基线（`#4A4458` 带紫）会在薄荷绿主题的深色模式下露出紫块。
 */

// ---- 粉色 ----
private val PinkPrimary = Color(0xFFE91E63)
private val PinkPrimaryDark = Color(0xFFC2185B)
private val PinkPrimaryLight = Color(0xFFF8BBD9)
private val RoseSecondary = Color(0xFFC2185B)
private val RoseSecondaryLight = Color(0xFFE91E63)

// ---- 天蓝色 ----
private val SkyPrimary = Color(0xFF2196F3)
private val SkyPrimaryDark = Color(0xFF1565C0)
private val SkyPrimaryLight = Color(0xFFBBDEFB)
private val DeepSeaSecondary = Color(0xFF0277BD)
private val DeepSeaSecondaryLight = Color(0xFF03A9F4)

// ---- 中性灰 ----
private val NeutralPrimary = Color(0xFF607D8B)
private val NeutralPrimaryDark = Color(0xFF455A64)
private val NeutralPrimaryLight = Color(0xFFB0BEC5)
private val SlateSecondary = Color(0xFF546E7A)
private val SlateSecondaryLight = Color(0xFF78909C)
private val MistTertiary = Color(0xFFCFD8DC)

// ---- 薄荷绿 ----
private val MintPrimary = Color(0xFF2E7D32)
private val MintPrimaryDark = Color(0xFF1B5E20)
private val MintPrimaryLight = Color(0xFFA5D6A7)
private val ForestSecondary = Color(0xFF1B5E20)
private val ForestSecondaryLight = Color(0xFF4CAF50)

// ---- 暮色紫 ----
private val PurplePrimary = Color(0xFF673AB7)
private val PurplePrimaryDark = Color(0xFF4527A0)
private val PurplePrimaryLight = Color(0xFFD1C4E9)
private val VioletSecondary = Color(0xFF4527A0)
private val VioletSecondaryLight = Color(0xFF9575CD)

// ---- 日出橙 ----
private val OrangePrimary = Color(0xFFE65100)
private val OrangePrimaryDark = Color(0xFFBF360C)
private val OrangePrimaryLight = Color(0xFFFFCCBC)
private val AmberSecondary = Color(0xFFBF360C)
private val AmberSecondaryLight = Color(0xFFFF8A65)

// ---- 沙漠棕 ----
private val BrownPrimary = Color(0xFF795548)
private val BrownPrimaryDark = Color(0xFF4E342E)
private val BrownPrimaryLight = Color(0xFFD7CCC8)
private val CaramelSecondary = Color(0xFF4E342E)
private val CaramelSecondaryLight = Color(0xFFA1887F)

// ---- 炫彩流光 ----
private val AuroraPrimary = Color(0xFFD81B60)
private val AuroraPrimaryDark = Color(0xFFAD1457)
private val AuroraPrimaryLight = Color(0xFFF8BBD9)
private val IrisSecondary = Color(0xFF7E57C2)
private val IrisSecondaryLight = Color(0xFFB39DDB)

// ---- 浅色模式的通用底色 / 文字 ----
private val CreamBackground = Color(0xFFFFF8F9)
private val NeutralBackground = Color(0xFFFAFAFA)
private val SkyBackground = Color(0xFFF1F8FE)
private val AuroraBackground = Color(0xFFFFF7FC)
private val WhiteSurface = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1C1B1F)
private val TextSecondary = Color(0xFF49454F)
private val TextOnPrimary = Color(0xFFFFFFFF)

// ---- 深色模式的通用底色（八套共用，LoveScore 原样）----
//
// 名字带 Palette 前缀是为了避开 [Color.kt] 里那套兜底调色板的 `DarkBackground` / `DarkSurface`
// —— Kotlin 的顶层重声明检查跨文件、不看可见性，同名即报 Conflicting declarations。
// 两套值也确实不能合并：那边是 M3 基线（#141218），这边是 LoveScore 的（#1C1B1F / #2D2D2D）。
private val PaletteDarkBackground = Color(0xFF1C1B1F)
private val PaletteDarkSurface = Color(0xFF2D2D2D)
private val DarkPink = Color(0xFFEFB8C8)
private val DarkSky = Color(0xFF90CAF9)
private val DarkNeutral = Color(0xFFB0BEC5)
private val DarkMint = Color(0xFFA5D6A7)
private val DarkPurple = Color(0xFFB39DDB)
private val DarkOrange = Color(0xFFFFAB91)
private val DarkBrown = Color(0xFFBCAAA4)
private val DarkAurora = Color(0xFFF0B8D0)

/**
 * 深色模式下 `onSurfaceVariant` 的取值。
 *
 * LoveScore 的深色 scheme 没设它，会落回 M3 基线 `#CAC4D0`（带一点紫的浅灰）。
 * 这点紫在任何一套深色底上都看不出来；反过来，让八套各带一个色相会让**正文次要色**
 * 随主题跳变，那才是不该有的动静。所以把 M3 的取值显式写出来，数值不改。
 */
private val PaletteDarkOnSurfaceVariant = Color(0xFFCAC4D0)

/** 错误色：LoveScore 明暗共用同一个值。 */
private val ErrorRed = Color(0xFFB00020)

/**
 * 一套色板的紧凑描述 —— 只写「这个色板自己的颜色」，其余 token 由 [toColorScheme] 按
 * 固定规则铺开。
 *
 * 为什么不把 16 份 [ColorScheme] 逐个手写：M3 的 ColorScheme 有 30+ 个 token，
 * 全写就是近 500 行近乎重复的赋值，而且改一处规则要同步 16 遍。
 *
 * 注意成对的字段是真有差异、不是冗余：深色模式的容器色不是浅色那档压暗，而是一条
 * **饱和中间调**——它在深底上要当光源用（见 [com.assiance.scabbard.ui.component.AppBackground]
 * 的两颗光斑都取容器色），压成近黑色背景就死了。
 */
private class PaletteSpec(
    val primary: Color,
    /** 深色模式的主色：一条更亮、更浅的同色相，保证在深底上可读。 */
    val primaryDark: Color,
    val primaryContainer: Color,
    val primaryContainerDark: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val tertiary: Color,
    val tertiaryContainer: Color,
    /**
     * 第三强调容器，深色模式用**饱和中间调**。
     *
     * 这是本文件唯一自己配的一档：LoveScore 的八套 scheme 都没设 `tertiaryContainer`，
     * 会全部落回 M3 基线粉 `#FFD8E4`。而 [com.assiance.scabbard.ui.component.AppBackground]
     * 的双色极光背景正是拿它当第二道光斑 —— 不补的话，薄荷绿、天空蓝这些主题的背景里
     * 会凭空飘出一团粉，和控件配色直接打架。
     *
     * 补值的口径（已与用户确认）：不新造色相，按 M3 对 tertiary 的惯例 —— **同族但偏色相**，
     * 取该色板主色相偏移约 60° 上的一个色调（粉→薰衣草、天蓝→青绿、薄荷→黄绿…），明暗各一份。
     * 这么取是为了让每套色板的两道光斑都能看出两色，而不是叠成一道。
     */
    val tertiaryContainerDark: Color,
    /** [tertiaryContainer] 上的前景色（浅色模式用同色相的深色调）。 */
    val onTertiaryContainer: Color,
    val background: Color,
    val surface: Color,
    val surfaceContainer: Color,
    val surfaceContainerDark: Color,
    val surfaceVariant: Color,
    val surfaceVariantDark: Color,
    val onSurfaceVariant: Color,
)

/**
 * 把紧凑描述铺成一份完整的 [ColorScheme]。
 *
 * 派生规则明暗共用一条式子，靠 `surface` 的明暗自动改变方向：
 * - `outline` / `outlineVariant`：从 `onSurfaceVariant` 朝 `surface` 提亮两档，见 [outlineFrom]；
 * - `surfaceContainer*` 阶梯、`surfaceDim`、`surfaceBright`：以 `surface` 与
 *   `surfaceContainer` 为锚点铺开，见 [SurfaceLadder]。
 */
private fun PaletteSpec.toColorScheme(dark: Boolean): ColorScheme {
    // on* 只有两种取值，LoveScore 明暗各用一档：浅底写深字、深底写浅字。
    val onContainer = if (dark) TextOnPrimary else TextPrimary
    val sv = if (dark) surfaceVariantDark else surfaceVariant
    // 正文次要色：浅底用本套的深字，深底用共用的浅灰（见 [PaletteDarkOnSurfaceVariant]）。
    val osv = if (dark) PaletteDarkOnSurfaceVariant else onSurfaceVariant
    val sc = if (dark) surfaceContainerDark else surfaceContainer
    val baseSurface = if (dark) PaletteDarkSurface else surface
    val ladder = SurfaceLadder.of(surface = baseSurface, container = sc, dark = dark)

    return if (dark) {
        darkColorScheme(
            primary = primaryDark,
            onPrimary = TextPrimary,
            primaryContainer = primaryContainerDark,
            onPrimaryContainer = TextOnPrimary,
            // 深色的次色直接用浅色那档容器色 —— LoveScore 就是这么取的（`secondary = PinkPrimaryLight`）。
            secondary = primaryContainer,
            onSecondary = TextPrimary,
            // 深色的次色容器 LoveScore 没设，取该色板的深色 surfaceVariant，
            // 避免落回 M3 基线的紫 `#4A4458`。
            secondaryContainer = sv,
            onSecondaryContainer = TextOnPrimary,
            tertiary = tertiaryContainer,
            onTertiary = TextPrimary,
            tertiaryContainer = tertiaryContainerDark,
            onTertiaryContainer = TextOnPrimary,
            background = PaletteDarkBackground,
            onBackground = TextOnPrimary,
            surface = baseSurface,
            onSurface = TextOnPrimary,
            surfaceVariant = sv,
            onSurfaceVariant = osv,
            surfaceContainerLowest = ladder.lowest,
            surfaceContainerLow = ladder.low,
            surfaceContainer = ladder.container,
            surfaceContainerHigh = ladder.high,
            surfaceContainerHighest = ladder.highest,
            surfaceDim = ladder.dim,
            surfaceBright = ladder.bright,
            outline = outlineFrom(osv, baseSurface, OUTLINE_MIX),
            outlineVariant = outlineFrom(osv, baseSurface, OUTLINE_VARIANT_MIX),
            inverseSurface = Color(0xFFE6E0E9),
            inverseOnSurface = Color(0xFF322F35),
            // inversePrimary 用在 inverseSurface（Snackbar 之类）之上，取值应是「另一个模式下
            // 的主色」—— 深色 scheme 里就是浅色模式那个深色主色。
            inversePrimary = primary,
            error = ErrorRed,
            onError = TextPrimary,
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = TextOnPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = TextPrimary,
            secondary = secondary,
            onSecondary = TextOnPrimary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = TextPrimary,
            tertiary = tertiary,
            onTertiary = TextOnPrimary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = TextPrimary,
            surface = baseSurface,
            onSurface = TextPrimary,
            surfaceVariant = sv,
            onSurfaceVariant = osv,
            surfaceContainerLowest = ladder.lowest,
            surfaceContainerLow = ladder.low,
            surfaceContainer = ladder.container,
            surfaceContainerHigh = ladder.high,
            surfaceContainerHighest = ladder.highest,
            surfaceDim = ladder.dim,
            surfaceBright = ladder.bright,
            outline = outlineFrom(osv, baseSurface, OUTLINE_MIX),
            outlineVariant = outlineFrom(osv, baseSurface, OUTLINE_VARIANT_MIX),
            inverseSurface = Color(0xFF322F35),
            inverseOnSurface = Color(0xFFF5EFF7),
            // 浅色 scheme 里的 inversePrimary 应是深色模式那个浅色主色。
            inversePrimary = primaryDark,
            error = ErrorRed,
            onError = TextOnPrimary,
            onErrorContainer = onContainer,
        )
    }
}

/** `outline` 从 `onSurfaceVariant` 朝 `surface` 靠拢的比例。 */
private const val OUTLINE_MIX = 0.25f

/** `outlineVariant` 的靠拢比例 —— 比 `outline` 更靠近底色一档。 */
private const val OUTLINE_VARIANT_MIX = 0.72f

/**
 * 从 `onSurfaceVariant` 朝 `surface` 插值，得到 `outline` / `outlineVariant`。
 *
 * 方向不用判断：浅色主题里 `surface` 是白的，插值即提亮；深色主题里 `surface` 是深灰，
 * 同一条式子自动变成压暗。M3 基线里 `outline` 比 `onSurfaceVariant` 浅、`outlineVariant`
 * 更浅，正是这个走向。比例按 M3 基线校准过：[OUTLINE_MIX] 得 `#76737B`（基线 `#79747E`），
 * [OUTLINE_VARIANT_MIX] 得 `#CCCBCE`（基线 `#CAC4D0`）。
 */
private fun outlineFrom(onSurfaceVariant: Color, surface: Color, fraction: Float): Color =
    lerp(onSurfaceVariant, surface, fraction)

/**
 * `surfaceContainerLowest` ~ `Highest` 五档，外加 `surfaceDim` / `surfaceBright`。
 *
 * M3 的这套阶梯本来就是「同一中性色相排在不同 tone 上」，而 LoveScore 只给了阶梯里的
 * 两档（`surface` / `surfaceContainer`）。这里以这两档为锚点、按同一思路线性铺开：
 * 浅色主题往黑走、深色主题往白走 —— 方向由 [dark] 显式给定，不靠猜。
 *
 * 之所以不能放着 M3 基线不管：基线的 `surfaceContainerHighest = #E6E0E9` 是带紫的浅灰，
 * 而 Card 默认就取它 —— 在薄荷绿主题上会冒出一张紫灰色卡片。
 */
private class SurfaceLadder(
    val lowest: Color,
    val low: Color,
    val container: Color,
    val high: Color,
    val highest: Color,
    val dim: Color,
    val bright: Color,
) {
    companion object {
        /** 阶梯往哪个方向延伸：浅色主题越往下越暗，深色主题越往上越亮。 */
        private val FAINT_STEP = 0.06f

        fun of(surface: Color, container: Color, dark: Boolean): SurfaceLadder {
            val away = if (dark) Color.White else Color.Black
            return SurfaceLadder(
                // 浅色主题下 surface 本就是最亮的一档，Lowest 没得再亮，就是它自己。
                lowest = if (dark) lerp(surface, Color.Black, 0.12f) else surface,
                low = lerp(surface, container, 0.5f),
                container = container,
                high = lerp(container, away, FAINT_STEP),
                highest = lerp(container, away, FAINT_STEP * 2f),
                dim = if (dark) lerp(surface, Color.Black, 0.35f) else lerp(surface, container, 0.6f),
                bright = if (dark) lerp(surface, Color.White, 0.22f) else surface,
            )
        }
    }
}

// ============================ 八套色板 ============================

private val PinkSpec = PaletteSpec(
    primary = PinkPrimary,
    primaryDark = DarkPink,
    primaryContainer = PinkPrimaryLight,
    primaryContainerDark = PinkPrimaryDark,
    secondary = RoseSecondary,
    secondaryContainer = PinkPrimaryLight,
    tertiary = RoseSecondaryLight,
    tertiaryContainer = Color(0xFFF8BBD9),
    // 粉 → 薰衣草
    tertiaryContainerDark = Color(0xFF7E57C2),
    onTertiaryContainer = Color(0xFF2B1B3D),
    background = CreamBackground,
    surface = WhiteSurface,
    surfaceContainer = Color(0xFFFDF2F5),
    surfaceContainerDark = Color(0xFF382029),
    surfaceVariant = Color(0xFFFCE4EC),
    surfaceVariantDark = Color(0xFF4A2A35),
    onSurfaceVariant = TextSecondary,
)

private val SkySpec = PaletteSpec(
    primary = SkyPrimary,
    primaryDark = DarkSky,
    primaryContainer = SkyPrimaryLight,
    primaryContainerDark = SkyPrimaryDark,
    secondary = DeepSeaSecondary,
    secondaryContainer = Color(0xFFB3E5FC),
    tertiary = DeepSeaSecondaryLight,
    tertiaryContainer = Color(0xFFB2EBF2),
    // 天蓝 → 青绿
    tertiaryContainerDark = Color(0xFF26C6DA),
    onTertiaryContainer = Color(0xFF00363D),
    background = SkyBackground,
    surface = WhiteSurface,
    surfaceContainer = Color(0xFFF0F8FE),
    surfaceContainerDark = Color(0xFF15232F),
    surfaceVariant = Color(0xFFE3F2FD),
    surfaceVariantDark = Color(0xFF1A2E40),
    onSurfaceVariant = TextSecondary,
)

private val NeutralSpec = PaletteSpec(
    primary = NeutralPrimary,
    primaryDark = DarkNeutral,
    primaryContainer = NeutralPrimaryLight,
    primaryContainerDark = NeutralPrimaryDark,
    secondary = SlateSecondary,
    secondaryContainer = MistTertiary,
    tertiary = SlateSecondaryLight,
    tertiaryContainer = Color(0xFFDCE4E8),
    // 中性灰 → 偏冷的钢蓝灰。这套本就无色彩倾向，两道光斑差别最小，是刻意的。
    tertiaryContainerDark = Color(0xFF78909C),
    onTertiaryContainer = Color(0xFF1F2A30),
    background = NeutralBackground,
    surface = WhiteSurface,
    surfaceContainer = Color(0xFFF5F7F8),
    surfaceContainerDark = Color(0xFF2A2E32),
    surfaceVariant = Color(0xFFECEFF1),
    surfaceVariantDark = Color(0xFF3A3F44),
    onSurfaceVariant = TextSecondary,
)

private val MintSpec = PaletteSpec(
    primary = MintPrimary,
    primaryDark = DarkMint,
    primaryContainer = MintPrimaryLight,
    primaryContainerDark = MintPrimaryDark,
    secondary = ForestSecondary,
    secondaryContainer = Color(0xFFC8E6C9),
    tertiary = ForestSecondaryLight,
    tertiaryContainer = Color(0xFFDCEDC8),
    // 薄荷 → 黄绿
    tertiaryContainerDark = Color(0xFF9CCC65),
    onTertiaryContainer = Color(0xFF23361A),
    background = NeutralBackground,
    surface = WhiteSurface,
    surfaceContainer = Color(0xFFF1F8E9),
    surfaceContainerDark = Color(0xFF182A1B),
    surfaceVariant = Color(0xFFE8F5E9),
    surfaceVariantDark = Color(0xFF1E3A22),
    onSurfaceVariant = TextSecondary,
)

private val PurpleSpec = PaletteSpec(
    primary = PurplePrimary,
    primaryDark = DarkPurple,
    primaryContainer = PurplePrimaryLight,
    primaryContainerDark = PurplePrimaryDark,
    secondary = VioletSecondary,
    secondaryContainer = Color(0xFFEDE7F6),
    tertiary = VioletSecondaryLight,
    tertiaryContainer = Color(0xFFF0D9F5),
    // 暮色紫 → 兰紫
    tertiaryContainerDark = Color(0xFFCE93D8),
    onTertiaryContainer = Color(0xFF33123D),
    background = NeutralBackground,
    surface = WhiteSurface,
    surfaceContainer = Color(0xFFF3E5F5),
    surfaceContainerDark = Color(0xFF231938),
    surfaceVariant = Color(0xFFEDE7F6),
    surfaceVariantDark = Color(0xFF2E1F47),
    onSurfaceVariant = TextSecondary,
)

private val OrangeSpec = PaletteSpec(
    primary = OrangePrimary,
    primaryDark = DarkOrange,
    primaryContainer = OrangePrimaryLight,
    primaryContainerDark = OrangePrimaryDark,
    secondary = AmberSecondary,
    secondaryContainer = Color(0xFFFFE0B2),
    tertiary = AmberSecondaryLight,
    tertiaryContainer = Color(0xFFFFE0B2),
    // 日出橙 → 杏黄
    tertiaryContainerDark = Color(0xFFFFB74D),
    onTertiaryContainer = Color(0xFF3A2400),
    background = NeutralBackground,
    surface = WhiteSurface,
    surfaceContainer = Color(0xFFFFF3E0),
    surfaceContainerDark = Color(0xFF382010),
    surfaceVariant = Color(0xFFFFE0B2),
    surfaceVariantDark = Color(0xFF4A2A14),
    onSurfaceVariant = TextSecondary,
)

private val BrownSpec = PaletteSpec(
    primary = BrownPrimary,
    primaryDark = DarkBrown,
    primaryContainer = BrownPrimaryLight,
    primaryContainerDark = BrownPrimaryDark,
    secondary = CaramelSecondary,
    secondaryContainer = Color(0xFFEFEBE9),
    tertiary = CaramelSecondaryLight,
    tertiaryContainer = Color(0xFFE5D5C9),
    // 沙漠棕 → 陶土
    tertiaryContainerDark = Color(0xFFA1887F),
    onTertiaryContainer = Color(0xFF2E211A),
    background = NeutralBackground,
    surface = WhiteSurface,
    surfaceContainer = Color(0xFFF5F0EE),
    surfaceContainerDark = Color(0xFF2A211D),
    surfaceVariant = Color(0xFFEFEBE9),
    surfaceVariantDark = Color(0xFF3A2D27),
    onSurfaceVariant = TextSecondary,
)

private val AuroraSpec = PaletteSpec(
    primary = AuroraPrimary,
    primaryDark = DarkAurora,
    primaryContainer = AuroraPrimaryLight,
    primaryContainerDark = AuroraPrimaryDark,
    secondary = IrisSecondary,
    secondaryContainer = Color(0xFFF3E5F5),
    tertiary = IrisSecondaryLight,
    tertiaryContainer = Color(0xFFC9D6FF),
    // 炫彩 → 蓝紫（与玫红主色拉成 shader 预设里那条玫红→蓝紫色域）
    tertiaryContainerDark = Color(0xFF7986CB),
    onTertiaryContainer = Color(0xFF14204A),
    background = AuroraBackground,
    surface = WhiteSurface,
    surfaceContainer = Color(0xFFFDF3F8),
    surfaceContainerDark = Color(0xFF231938),
    surfaceVariant = Color(0xFFEDE7F6),
    surfaceVariantDark = Color(0xFF2E1F47),
    onSurfaceVariant = TextSecondary,
)

private val Specs: Map<ThemePalette, PaletteSpec> = mapOf(
    ThemePalette.PINK to PinkSpec,
    ThemePalette.SKY to SkySpec,
    ThemePalette.NEUTRAL to NeutralSpec,
    ThemePalette.MINT to MintSpec,
    ThemePalette.PURPLE to PurpleSpec,
    ThemePalette.ORANGE to OrangeSpec,
    ThemePalette.BROWN to BrownSpec,
    ThemePalette.AURORA to AuroraSpec,
)

// 每套明暗各建一次，实例因此是复用的 —— 主题重组时 ColorScheme 身份稳定。
private val LightSchemes: Map<ThemePalette, ColorScheme> =
    Specs.mapValues { (_, spec) -> spec.toColorScheme(dark = false) }

private val DarkSchemes: Map<ThemePalette, ColorScheme> =
    Specs.mapValues { (_, spec) -> spec.toColorScheme(dark = true) }

/**
 * 取出某套固定色板在指定深浅模式下的 [ColorScheme]。
 *
 * [ThemePalette.DYNAMIC] 返回 null —— 它要走 Material You 动态取色，那条路需要 Context，
 * 由 [ScabbardTheme] 自己处理。
 */
internal fun fixedColorSchemeFor(palette: ThemePalette, dark: Boolean): ColorScheme? {
    val table = if (dark) DarkSchemes else LightSchemes
    return table[palette]
}
