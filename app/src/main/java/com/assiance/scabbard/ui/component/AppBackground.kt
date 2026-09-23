package com.assiance.scabbard.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.assiance.scabbard.data.AppSettings
import com.assiance.scabbard.ui.effect.bgEffectShader
import com.assiance.scabbard.ui.effect.rememberBgEffectState
import com.assiance.scabbard.ui.theme.ThemePalette
import io.github.nadeemiqbal.liquidglass.LiquidGlassState
import io.github.nadeemiqbal.liquidglass.liquidGlassSource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 供玻璃面采样背板用的共享 [LiquidGlassState]，由 [com.assiance.scabbard.ui.ScabbardApp]
 * 在 App 根 provide。
 *
 * **为什么必须是同一个实例**：liquid-glass 的模糊、饱和提升、折射全部挂在 `state.backdrop`
 * 这个 `GraphicsLayer` 上，而那个 layer 只由 `Modifier.liquidGlassSource(state)` 录制
 * （库的 `drawBackdrop` 开头是 `if (!recorded) return`）。传进来的 state 和录制用的 state
 * 不是同一个实例，layer 就永远是空的 —— 玻璃静默退化成"纯 tint + 描边"，不报错、不崩，
 * 只是不模糊。本项目此前正是这个状态。
 *
 * `null` 表示没有背板可采（预览、单测、脱离 App 根复用某个组件），消费方自行
 * `rememberLiquidGlassState()` 兜底即可，不必判空到处传。
 */
val LocalLiquidGlassState = staticCompositionLocalOf<LiquidGlassState?> { null }

/**
 * 全 App 背景层：跟着当前主题走的一层缓动背景。
 *
 * 位置：铺在 [com.assiance.scabbard.ui.ScabbardApp] 的 NavHost **之下**，z-order 最底层。
 * 因此各页面的 Scaffold 必须用 `containerColor = Color.Transparent`、TopAppBar 必须用
 * `topAppBarColors(containerColor = Color.Transparent)`，背景才透得出来 —— 漏掉任何一处
 * 都会在该页留下一条实色横带。
 *
 * **它同时是玻璃的背板来源**：本函数内部挂 [liquidGlassSource]，把这一层画出来的像素录进
 * [LocalLiquidGlassState] 的 `backdrop`。所以背板的登记是**跟着背景层走的、不靠调用方记得**
 * —— 这正是此前漏掉的那一步（见 [LocalLiquidGlassState] 的说明）。state 为 null（没有
 * provide）或处于 `Fallback` 档时，[liquidGlassSource] 是恒等变换、不分配任何离屏层。
 *
 * 画什么由当前主题色板决定：
 * - [ThemePalette.AURORA] → 全屏炫彩流光 shader（[AuroraBackground]）；
 * - 其余（含默认的 DYNAMIC）→ 双色游移光斑（[AuroraGlowBackground]）。
 *
 * 两条路的分工是刻意的：色板决定"控件配色 + 背景形态"，而双色光斑自己的**颜色**另有
 * 一套预设（[com.assiance.scabbard.ui.theme.AuroraPreset]），两者解耦 —— 用户完全可能
 * 想要「薄荷绿的控件 + 暖色背景」。炫彩那套没有这个自由度，它的颜色就写在 shader 预设里。
 */
@Composable
fun AppBackground(modifier: Modifier = Modifier) {
    val palette by AppSettings.themePalette.collectAsState()
    val preset by AppSettings.auroraPreset.collectAsState()
    val veilEnabled by AppSettings.auroraVeilEnabled.collectAsState()

    // 把自己登记成玻璃的背板。挂在这一层而不是让调用方传 modifier：背景层就是背板本身，
    // 由它自己认领这件事，才不会有"忘了挂"的形态 —— 忘了不会报错，只会悄悄不模糊。
    val glassState = LocalLiquidGlassState.current
    val backdropModifier =
        if (glassState != null) modifier.liquidGlassSource(glassState) else modifier

    if (palette == ThemePalette.AURORA) {
        AuroraBackground(veilEnabled = veilEnabled, modifier = backdropModifier)
    } else {
        AuroraGlowBackground(
            primaryGlow = preset.primaryGlow,
            secondaryGlow = preset.secondaryGlow,
            modifier = backdropModifier,
        )
    }
}

/**
 * 双色游移光斑背景。
 *
 * 底色是一条纵向渐变，上面叠两颗缓慢画圆游移、各自呼吸的径向光斑：
 * 主光斑取 [primaryGlow]（null 时取当前色板的 `primaryContainer`），
 * 副光斑取 [secondaryGlow]（null 时取 `tertiaryContainer`）。
 *
 * 动效是一个 [drawBehind]，没有子节点、不触发重组 —— 每帧只重绘这一层。
 *
 * 光斑半径必须收在屏幕短边的一小段里（见 [GLOW_RADIUS_FRACTION]），否则动效等于没有。
 * 曾经写的是 `size.maxDimension`：1080×2400 的屏上渐变跨度 2400px，而圆心只在 ±0.32 屏内
 * 游移几百像素，整屏被匀匀染上一层淡色、游移落在这个大缓坡上完全看不出来。半径收到短边
 * 量级后峰值集中，游移才读得出是"光在动"；同时远离光斑处更接近干净底色，压在背景上的
 * 正文反而更清楚 —— 比调高透明度划算。
 */
@Composable
private fun AuroraGlowBackground(
    primaryGlow: Color?,
    secondaryGlow: Color?,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    // 预设给的是写死色，null 才回落到色板 —— 这样「跟随主题」是默认行为，而选了具体预设
    // 的人会看到背景与控件脱钩，正是他要的。
    val primary = primaryGlow ?: scheme.primaryContainer
    val secondary = secondaryGlow ?: scheme.tertiaryContainer

    // 缓慢游移：约 12s 一圈，径向光斑的圆心在背景上画圆，制造"光在流动"的观感。
    val transition = rememberInfiniteTransition(label = "bg-flow")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "bg-flow-angle",
    )
    // 光斑透明度轻微呼吸（0.35 ~ 0.55），避免恒定高光显得死板。
    val breathe by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bg-flow-breathe",
    )
    // 副光斑自己的呼吸（0.16 ~ 0.30）。周期取 8s 而不是跟主光斑一样的 6s：
    // 两者周期相同会同步明暗，看上去还是一颗光斑在忽大忽小；错开才像两层极光各自流动。
    val breatheSecond by transition.animateFloat(
        initialValue = SECOND_BREATHE_MIN,
        targetValue = SECOND_BREATHE_MAX,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bg-flow-breathe-second",
    )

    val rad = angle * (PI / 180.0)
    // 副光斑与主光斑反相（差 180°）：两颗光斑沿对角线此消彼长地游移，
    // 屏幕上任何时刻都有一明一暗两处色相，而不是两颗一起漂。
    val radSecond = rad + PI

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // 用 DrawScope 的实际宽高把归一化游移坐标换算成真实 px 中心，
                // 避免 fraction 被当成 px 用导致光斑偏到左上角。
                val cx = size.width * (0.5f + ORBIT_RADIUS_FRACTION * cos(rad).toFloat())
                val cy = size.height * (0.5f + ORBIT_RADIUS_FRACTION * sin(rad).toFloat())
                // 副光斑的游移半径略大于主光斑，两颗圆心不会长期重合
                val cxSecond =
                    size.width * (0.5f + SECOND_ORBIT_RADIUS_FRACTION * cos(radSecond).toFloat())
                val cySecond =
                    size.height * (0.5f + SECOND_ORBIT_RADIUS_FRACTION * sin(radSecond).toFloat())
                // 底色：surfaceContainer → surface 纵向渐变。
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(scheme.surfaceContainer, scheme.surface),
                    )
                )
                // 主光斑：径向渐变，圆心游移、浓度呼吸。半径按短边取，见 GLOW_RADIUS_FRACTION。
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(primary.copy(alpha = breathe), Color.Transparent),
                        radius = size.minDimension * GLOW_RADIUS_FRACTION,
                        center = Offset(cx, cy),
                    )
                )
                // 副光斑：比主光斑更大更淡，拉开色相层次。
                // 盖在主光斑之上：两者在圆心附近叠出第三种过渡色，边缘各自淡出。
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(secondary.copy(alpha = breatheSecond), Color.Transparent),
                        radius = size.minDimension * SECOND_GLOW_RADIUS_FRACTION,
                        center = Offset(cxSecond, cySecond),
                    )
                )
            }
    )
}

/**
 * 「炫彩流光」主题的全屏背景：主题 surface 铺底 + 全屏炫彩 shader。
 *
 * **层序与移植来源一致：surface 在下、炫彩在上，不能反过来。**
 * 这不是随手定的 —— 深色预设每组色份的第 4 位就是 alpha（`OS3_PHONE_DARK` 是 0.4~0.6），
 * shader 输出的正是预乘 alpha 的 vec4，本来就该和底下的实色混出层次，所以下面必须先有
 * 一层可混的实色。反过来「炫彩打底 + surface 压在上面」等于把炫彩整体乘掉一大半，
 * 颜色被洗得发灰 —— 那正是它看着不如原版显眼的原因。
 *
 * [veilEnabled] 是给「觉得炫彩太跳、正文压不住」的用户留的退路：打开就在炫彩之上再补
 * 一层主题 surface，把对比收回去。默认关闭，即默认给原版浓度。
 *
 * shader 不可用（Android 12 及以下 / AGSL 编译失败）时，静默退回双色游移光斑 ——
 * 背景永远不该是空白或崩溃。
 */
@Composable
private fun AuroraBackground(
    veilEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val state = rememberBgEffectState()

    if (!state.supported) {
        AuroraGlowBackground(primaryGlow = null, secondaryGlow = null, modifier = modifier)
        return
    }

    val scheme = MaterialTheme.colorScheme
    // 遮罩浓度的两档是分开定的。
    //
    // 深色那档一开始定成 0.62，实测**把炫彩抹没了**：深色预设的色份 alpha 本就只有 0.4~0.6，
    // 再压 62% 的 surface 上去，剩下的 38% 在暗底上已经低到看不出色相，整屏退化成一片死灰紫。
    // 遮罩的语义是"收浓度让正文好读"，不是"关掉背景"，所以收到 0.45 —— 色相还在，浓度下来了。
    val isDark = scheme.surface.luminance() < 0.5f

    Box(modifier = modifier.fillMaxSize()) {
        // 底色：给炫彩的预乘 alpha 一个可混的实色面，深色预设尤其依赖它。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scheme.surface)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .bgEffectShader(state)
        )
        // 可选遮罩：收住炫彩的浓度，让压在它上面的玻璃卡片和正文更好读。
        if (veilEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scheme.surface.copy(alpha = if (isDark) 0.45f else 0.55f))
            )
        }
    }
}

/**
 * 光斑半径占屏幕短边的比例。
 *
 * 取 0.6 的用意：光斑直径略大于屏宽，边缘自然淡出、不切出硬边；而圆心游移幅度
 * （[ORBIT_RADIUS_FRACTION]）与半径同量级，光斑能明显地在屏幕上挪动。
 * 调大 → 越来越像均匀铺色，动效随之消失；调小 → 变成一颗清晰的光球，离"低调背景"的定位就远了。
 */
private const val GLOW_RADIUS_FRACTION = 0.6f

/**
 * 光斑圆心的游移半径，占屏幕宽/高的比例（±0.32 屏）。
 * 与 [GLOW_RADIUS_FRACTION] 同量级时光斑挪动最明显。
 */
private const val ORBIT_RADIUS_FRACTION = 0.32f

/**
 * 副光斑半径占屏幕短边的比例。
 *
 * 比主光斑的 [GLOW_RADIUS_FRACTION] 大得多，是刻意的：副光斑的作用是拉开**色相**层次，
 * 不是再点一颗亮斑。铺得越大、越淡，越像一层漫开的色雾；若跟主光斑同尺寸同浓度，
 * 两颗光斑只会互相抵消成一块更亮的大斑，色相层反而不见了。
 */
private const val SECOND_GLOW_RADIUS_FRACTION = 0.85f

/**
 * 副光斑圆心的游移半径。比主光斑略大（0.38 vs 0.32），保证两颗圆心不会长期重合 ——
 * 重合时两个径向渐变叠在一起，双色的效果就没了。
 */
private const val SECOND_ORBIT_RADIUS_FRACTION = 0.38f

/** 副光斑呼吸区间的下沿。比主光斑更低，避免两层色雾把背景提得过亮。 */
private const val SECOND_BREATHE_MIN = 0.16f

/** 副光斑呼吸区间的上沿。 */
private const val SECOND_BREATHE_MAX = 0.30f
