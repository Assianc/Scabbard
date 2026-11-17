package com.assiance.convert.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.nadeemiqbal.liquidglass.LiquidGlassQuality
import io.github.nadeemiqbal.liquidglass.LiquidGlassState
import io.github.nadeemiqbal.liquidglass.rememberLiquidGlassState

/*
 * ========================= convert 侧的液态玻璃契约 =========================
 *
 * 这里的内容与 memo 的 `com.assiance.memo.ui.LiquidGlassContract`、alm 的
 * `AlmGlassContract` 是**同一个契约的第三份声明**，参数值刻意与它们保持一致
 * （tint 0.3 / grain 0.04）。不是想写三遍，是模块边界使然：依赖方向永远是
 * app → memo/alm/convert，三个辅助模块彼此平级、互相看不到对方的符号，
 * 而「新增共享模块」被明确排除。
 *
 * 分工沿用 alm 的既有约定：「**实例由宿主给，声明由各模块自己持有**」。宿主 app
 * 从 memo 侧取到那份已经在录背板的 state，再原样交给 convertGraph；只要拿到的是
 * 同一个实例，玻璃就采得到背板，换成另一个实例就永远不模糊。
 */

/**
 * 宿主经 `convertGraph(navController, glassState)` 传进来的共享 [LiquidGlassState]。
 *
 * **为什么必须是同一个实例**：liquid-glass 的模糊、饱和提升、折射全部挂在
 * `state.backdrop` 这个 GraphicsLayer 上，而那个 layer 只由 `Modifier.liquidGlassSource(state)`
 * 录制。convert 自己新建一个的话，backdrop 永远是空的，玻璃静默退化成
 * 「纯 tint + 描边」—— 不报错、不崩，只是不模糊。机制详见 memo 的 LiquidGlassContract。
 *
 * `null` 表示没有背板可采（预览、单测），消费方用 [rememberConvertGlassState] 兜底即可。
 */
val LocalConvertGlassState = staticCompositionLocalOf<LiquidGlassState?> { null }

/** 玻璃卡 tint 的不透明度。与 memo / alm 同值，理由见各自契约文件头。 */
const val GLASS_TINT_ALPHA = 0.3f

/** 玻璃面上的颗粒强度。与 memo / alm 同值（库标定的 0.04）。 */
const val GLASS_GRAIN = 0.04f

/** 按实际档位决定颗粒强度：Fallback 档一颗不画，与 memo / alm 一致。 */
fun glassGrainFor(quality: LiquidGlassQuality): Float =
    if (quality == LiquidGlassQuality.Fallback) 0f else GLASS_GRAIN

/**
 * 取当前生效的玻璃 state —— convert 的所有玻璃面都走这个函数，不要自己
 * `rememberLiquidGlassState()`。
 *
 * **两个 `rememberLiquidGlassState` 都必须无条件求值**：`?:` 右侧的 @Composable 是条件调用，
 * 两侧来回切换会让 Compose 的 slot table 错位。兜底那份显式取 Fallback：没有背板时本来
 * 也不该模糊，而 Fallback 档不分配任何离屏层，所以「多算一遍」是零代价的。
 */
@Composable
fun rememberConvertGlassState(): LiquidGlassState {
    val shared = LocalConvertGlassState.current
    val fallback = rememberLiquidGlassState(quality = LiquidGlassQuality.Fallback)
    return shared ?: fallback
}
