package com.assiance.alm.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.nadeemiqbal.liquidglass.LiquidGlassQuality
import io.github.nadeemiqbal.liquidglass.LiquidGlassState
import io.github.nadeemiqbal.liquidglass.rememberLiquidGlassState

/*
 * ========================== alm 侧的液态玻璃契约 ==========================
 *
 * 这里的内容与 memo 的 `com.assiance.memo.ui.LiquidGlassContract` 是**同一个契约的
 * 两份声明**，参数值刻意保持一致（tint 0.3 / grain 0.04）。不是想写两遍，是模块边界
 * 使然：依赖方向永远是 app → memo/alm，memo 与 alm 平级、互相看不到对方的符号，
 * 而"新增共享模块"被明确排除。
 *
 * 分工是「**实例由宿主给，声明由各模块自己持有**」：宿主 app 同时依赖 memo 与 alm，
 * 它从 memo 侧取到那份已经在录背板的 state，再原样交给 [almGraph]。只要两边拿到的是
 * 同一个实例，玻璃就采得到背板；换成两个实例就永远不模糊（原因见 [LocalAlmGlassState]）。
 */

/**
 * 宿主经 `almGraph(navController, glassState)` 传进来的共享 [LiquidGlassState]。
 *
 * **为什么必须是同一个实例**：liquid-glass 的模糊、饱和提升、折射全部挂在 `state.backdrop`
 * 这个 `GraphicsLayer` 上，而那个 layer 只由 `Modifier.liquidGlassSource(state)` 录制
 * （库的 `drawBackdrop` 开头是 `if (!recorded) return`）。alm 自己 `rememberLiquidGlassState()`
 * 新建一个的话，它的 backdrop 永远是空的 —— 玻璃静默退化成"纯 tint + 描边"，不报错、不崩，
 * 只是不模糊。alm 以前读不到宿主的 state，缺的正是这条传递链。
 *
 * `null` 表示没有背板可采（预览、单测、脱离宿主单独跑 alm 的某个页面），消费方用
 * [rememberAlmGlassState] 兜底即可，不必判空到处传。
 */
val LocalAlmGlassState = staticCompositionLocalOf<LiquidGlassState?> { null }

/**
 * 玻璃卡 tint 的不透明度。与 memo 侧 `GLASS_TINT_ALPHA` 同值，理由见文件头。
 *
 * 取值 0.3 是 2026-09-23 定下的（0.6 时背板只剩四成参与、深色下读作一块实心板），
 * 这里只是复述同一个值，不是另立一份口径。
 */
const val GLASS_TINT_ALPHA = 0.3f

/**
 * 玻璃**面上**的颗粒强度。与 memo 侧 `GLASS_GRAIN` 同值，理由见文件头。
 *
 * 0.04 是库自己标定的值（`grain` 参数 KDoc：「A subtle 0.04 matches the iOS-26
 * frosted-glass feel」）。**只在非 Fallback 档画**，见 [glassGrainFor]。
 */
const val GLASS_GRAIN = 0.04f

/**
 * 按当前档位决定颗粒强度：[LiquidGlassQuality.Fallback] 档一颗不画。
 *
 * 颗粒是"毛玻璃"这个材质的一部分，跟模糊同进退 —— 模糊关掉后玻璃退回一块干净的半透明
 * 色板，再留一层沙粒就成了"没有磨砂的磨砂玻璃"。库自己也是这个口径：Fallback 的定位写的是
 * "a translucent tint with an edge sheen"，颗粒不在其中。
 *
 * 判据取**实际档位**而不是那个开关的设置值：开关关掉、设备内存不足被自动降级、系统版本
 * 低于 31 —— 三种情况在这里是同一个值，不用判三遍。
 */
fun glassGrainFor(quality: LiquidGlassQuality): Float =
    if (quality == LiquidGlassQuality.Fallback) 0f else GLASS_GRAIN

/**
 * 取当前生效的玻璃 state —— alm 的所有玻璃面都走这个函数，不要自己 `rememberLiquidGlassState()`。
 *
 * 优先用宿主经 [LocalAlmGlassState] provide 的共享实例（**背板录制在它身上**，换成别的
 * 就永远不模糊）；没有 provide 时（预览、单测）退回一份自持的。
 *
 * **两个 `rememberLiquidGlassState` 都必须无条件求值**：`?:` 右侧的 @Composable 是条件调用，
 * 两侧来回切换会让 Compose 的 slot table 错位 —— 冷启动只走一条路径时看不出来，在设置里
 * 热切换开关时才踩得到。兜底那份显式取 [LiquidGlassQuality.Fallback]：没有背板时本来也不该
 * 模糊，而 Fallback 档不分配任何离屏层，所以这次"多算一遍"是零代价的。
 */
@Composable
fun rememberAlmGlassState(): LiquidGlassState {
    val shared = LocalAlmGlassState.current
    val fallback = rememberLiquidGlassState(quality = LiquidGlassQuality.Fallback)
    return shared ?: fallback
}
