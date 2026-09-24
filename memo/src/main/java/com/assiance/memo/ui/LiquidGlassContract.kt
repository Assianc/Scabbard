package com.assiance.memo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.nadeemiqbal.liquidglass.LiquidGlassQuality
import io.github.nadeemiqbal.liquidglass.LiquidGlassState
import io.github.nadeemiqbal.liquidglass.rememberLiquidGlassState

/*
 * ============================ 跨模块的液态玻璃契约 ============================
 *
 * 这个文件是全 App 玻璃效果的**单一来源**：背板登记用的 state、玻璃面的默认参数、
 * 以及消费方取 state 的唯一入口。app（主模块）与 memo（辅助模块）都从这里取，
 * 三处玻璃面（首页入口卡、备忘录记录卡、关于页作者行）因此只有一份定义。
 *
 * **它为什么在 memo 而不在 app**：依赖方向是 app → memo，memo 看不到 app 的任何符号。
 * 而玻璃面又必须拿到宿主 provide 的那个 state，所以「声明」得放在依赖链下游、
 * 由上游 app 去 provide 实例 —— 这样两侧都能看到同一个 CompositionLocal，
 * 且方向不变（仍是 app 依赖 memo，没有反向，也没有新增共享模块）。
 */

/**
 * 供玻璃面采样背板用的共享 [LiquidGlassState]，由宿主 App 在根上 provide。
 *
 * **为什么必须是同一个实例**：liquid-glass 的模糊、饱和提升、折射全部挂在 `state.backdrop`
 * 这个 `GraphicsLayer` 上，而那个 layer 只由 `Modifier.liquidGlassSource(state)` 录制
 * （库的 `drawBackdrop` 开头是 `if (!recorded) return`）。传进来的 state 和录制用的 state
 * 不是同一个实例，layer 就永远是空的 —— 玻璃静默退化成"纯 tint + 描边"，不报错、不崩，
 * 只是不模糊。本项目此前正是这个状态。
 *
 * `null` 表示没有背板可采（预览、单测、脱离 App 根复用某个组件），消费方用
 * [rememberSharedGlassState] 兜底即可，不必判空到处传。
 */
val LocalLiquidGlassState = staticCompositionLocalOf<LiquidGlassState?> { null }

/**
 * 玻璃卡 tint 的不透明度 —— 玻璃底色这一层唯一的浓度旋钮。
 *
 * 施加在 `liquidGlass` 的 `tint` 上（取当前色板的 `surfaceContainer`）。调低更透、玻璃感
 * 更强；调高更接近一张实底 Card。
 *
 * 2026-09-23 从 0.6 收到 0.3：0.6 时背板只剩四成参与、且深色下 `surfaceContainerDark` 很暗，
 * 卡片与旁边背景明暗分层明显，读作一块实心板。
 */
const val GLASS_TINT_ALPHA = 0.3f

/**
 * 玻璃**面上**的颗粒强度 —— 毛玻璃那层"沙粒感"。
 *
 * 走 `liquidGlass` 的 `grain` 参数，颗粒画在 tint 与描边之后、内容之前，是玻璃**材质自身**
 * 的纹理，不随背板移动。（加在背板上是另一回事 —— 那是"一块带噪点的背景透过来"，
 * 噪点跟着背景转，不是毛玻璃。）
 *
 * 库把颗粒铺成 1:1 的 64×64 像素 tile（`drawNoiseTile` 不缩放），每个颗粒就是 1 物理像素，
 * 在高密度屏上细得只有"质感"、看不出"块"。
 *
 * 取 0.04 是库自己标定的值（`grain` 参数 KDoc：「A subtle 0.04 matches the iOS-26
 * frosted-glass feel」），不自己拍。**但只在非 Fallback 档画**，见 [glassGrainFor]。
 */
const val GLASS_GRAIN = 0.04f

/**
 * 按当前档位决定颗粒强度：[LiquidGlassQuality.Fallback] 档一颗不画。
 *
 * 颗粒是"毛玻璃"这个材质的一部分，跟模糊同进退 —— 模糊关掉后玻璃退回一块干净的半透明色板，
 * 再留一层沙粒就成了"没有磨砂的磨砂玻璃"，说不通。库自己也是这个口径：Fallback 的定位写的是
 * "a translucent tint with an edge sheen"，颗粒不在其中。
 *
 * 判据取**实际档位**而不是那个开关的设置值：开关关掉、设备内存不足被自动降级、系统版本
 * 低于 31 —— 三种情况在这里是同一个值，不用判三遍。
 */
fun glassGrainFor(quality: LiquidGlassQuality): Float =
    if (quality == LiquidGlassQuality.Fallback) 0f else GLASS_GRAIN

/**
 * 取当前生效的玻璃 state —— 所有玻璃面都走这个函数，不要自己 `rememberLiquidGlassState()`。
 *
 * 优先用宿主 provide 的共享实例（**背板录制在它身上，换成别的就永远不模糊**，见
 * [LocalLiquidGlassState]）；没有 provide 时（预览、单测）退回一份自持的。
 *
 * **两个 `rememberLiquidGlassState` 都必须无条件求值**：`?:` 右侧的 @Composable 是条件调用，
 * 两侧来回切换会让 Compose 的 slot table 错位 —— 冷启动只走一条路径时看不出来，在设置里
 * 热切换开关时才踩得到。兜底那份显式取 [LiquidGlassQuality.Fallback]：没有背板时本来也不该
 * 模糊，而 Fallback 档不分配任何离屏层，所以这次"多算一遍"是零代价的。
 */
@Composable
fun rememberSharedGlassState(): LiquidGlassState {
    val shared = LocalLiquidGlassState.current
    val fallback = rememberLiquidGlassState(quality = LiquidGlassQuality.Fallback)
    return shared ?: fallback
}
