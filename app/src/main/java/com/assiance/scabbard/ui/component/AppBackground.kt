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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.assiance.memo.ui.LocalLiquidGlassState
import com.assiance.scabbard.data.AppSettings
import com.assiance.scabbard.ui.effect.bgEffectShader
import com.assiance.scabbard.ui.effect.rememberBgEffectState
import com.assiance.scabbard.ui.theme.ThemePalette
import io.github.nadeemiqbal.liquidglass.liquidGlassSource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

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
 * - 其余（含默认的 DYNAMIC）→ 双色旋转背景（[AuroraGlowBackground]）。
 *
 * 两条路的分工是刻意的：色板决定"控件配色 + 背景形态"，而双色渐变自己的**颜色**另有
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
 * 双色旋转背景。
 *
 * 底色是一条纵向渐变，其上压一道**两端分别是 [primaryGlow] 与 [secondaryGlow] 的线性渐变**，
 * 这道渐变的**方向**绕屏幕中心匀速旋转 —— 观感是"两种色彩在转"，而不是两颗光在游移。
 * 端点传 null 时分别回落到当前色板的 `primaryContainer` / `tertiaryContainer`。
 *
 * 动效是一个 [drawBehind]，没有子节点、不触发重组 —— 每帧只重绘这一层。
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

    // 12s 转一圈。线性渐变的方向转过 180° 时两端色就已经换过位（视觉上等价于整圈），
    // 但仍按 0~360 写整圈：量纲直白，读代码时不用在脑子里把"半圈"再翻回去。
    val transition = rememberInfiniteTransition(label = "bg-rotate")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "bg-rotate-angle",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // 底色：surfaceContainer → surface 纵向渐变。
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(scheme.surfaceContainer, scheme.surface),
                    )
                )
                val rad = angle * (PI / 180.0)
                val cx = size.width / 2f
                val cy = size.height / 2f
                // 半长取对角线的一半：线性渐变按"每个点在渐变线上的投影"取色，跨度只有盖住
                // 整条对角线，转到任意角度才不会在四角露出没上色的底色。
                val half = hypot(size.width, size.height) / 2f
                val dx = (half * cos(rad)).toFloat()
                val dy = (half * sin(rad)).toFloat()
                // alpha 施加在 drawRect 上、而不是先写进两个颜色里：这样插值发生在**不透明色**
                // 之间，色相不被透明度稀释，混出来的中间色更干净。
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(primary, secondary),
                        start = Offset(cx - dx, cy - dy),
                        end = Offset(cx + dx, cy + dy),
                    ),
                    alpha = ROTATE_ALPHA,
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
 * shader 不可用（Android 12 及以下 / AGSL 编译失败）时，静默退回双色旋转背景 ——
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
 * 双色渐变压在底色上的整体不透明度 —— 这里是唯一的浓度旋钮。
 *
 * 施加在 `drawRect` 的 `alpha` 参数上，而不是先写进两个颜色里：见 [AuroraGlowBackground] 内的说明。
 *
 * 调高更醒目、更像"主题色在流动"；调低则一点点退回纯粹的 surface 底色。
 * 取 0.5 是"看得出来是双色、又仍然读作背景"的中间值。
 */
private const val ROTATE_ALPHA = 0.5f
