package com.assiance.scabbard.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.assiance.alm.ui.AlmRoutes
import com.assiance.alm.ui.almGraph
import com.assiance.convert.ui.ConvertRoutes
import com.assiance.convert.ui.convertGraph
import com.assiance.memo.ui.LocalLiquidGlassState
import com.assiance.memo.ui.MemoRoutes
import com.assiance.memo.ui.memoGraph
import com.assiance.scabbard.data.AppSettings
import com.assiance.scabbard.ui.component.AppBackground
import com.assiance.scabbard.ui.screen.AboutScreen
import com.assiance.scabbard.ui.screen.AllocateScreen
import com.assiance.scabbard.ui.screen.AppearanceScreen
import com.assiance.scabbard.ui.screen.HomeScreen
import com.assiance.scabbard.ui.screen.PlayerSelectionScreen
import com.assiance.scabbard.ui.screen.SplashScreen
import io.github.nadeemiqbal.liquidglass.LiquidGlassQuality
import io.github.nadeemiqbal.liquidglass.rememberLiquidGlassState
import io.github.nadeemiqbal.liquidglass.rememberPlatformLiquidGlassQuality

/**
 * 全 App 的导航图。改造前是 3 个模块共十几个 Activity 互相 startActivity，
 * 现在全部收进这一个 NavHost。
 *
 * 模块边界保持原样：app 是唯一主模块，memo / alm 是辅助模块，依赖方向永远是
 * **app → memo/alm**。两个模块各自通过 [memoGraph] / [almGraph] 把自己的路由挂进来，
 * 模块内部怎么跳转由模块自己决定，app 只提供两个入口 —— 和改造前 app 只 startActivity
 * 进它们的入口页是同一个粒度。
 *
 * 「分道」那两个元素列表挂在这一层而不是 AllocateScreen 内部：原来的
 * `MainActivity.onActivityResult` 是拿 Activity 实例的字段存这两个列表的，
 * 选择页改完回传、分类页读同一份，所以状态的生命周期必须比单个页面长。
 */
@Composable
fun ScabbardApp(deepLinkRoute: String? = null) {
    val navController = rememberNavController()

    // 原 MainActivity 每被新建一次就重置成这四个示例元素
    var allPlayers by remember { mutableStateOf(DEFAULT_PLAYERS) }
    var selectedPlayers by remember { mutableStateOf(emptyList<String>()) }

    // 通知点击等外部入口带进来的路由。解析不出来的话退回启动页。
    val resolvedDeepLink = remember { resolveDeepLinkRoute(deepLinkRoute) }
    val startDestination = resolvedDeepLink ?: AppRoutes.SPLASH

    // 热启动：Activity 还在（singleTop + onNewIntent），图早就建好了，
    // 这时候只能往现有返回栈上 push，不能改 startDestination。
    // 冷启动时解析结果已经等于 startDestination，这里自然什么都不做。
    LaunchedEffect(deepLinkRoute) {
        val route = resolveDeepLinkRoute(deepLinkRoute)
        if (route != null && route != startDestination) {
            navController.navigate(route)
        }
    }

    // 玻璃的共享 state：背板（AppBackground）与玻璃面（首页入口卡）必须拿到**同一个实例**，
    // 否则 backdrop 那个 GraphicsLayer 永远是空的、玻璃不会模糊（原因见
    // [LocalLiquidGlassState]）。创建在这里而不是首页：背板的生命周期是全 App 的，跟着
    // 首页走会让每次进出首页都重建离屏层。
    //
    // 外观设置里的「液态玻璃模糊」开关即在这里落地 —— 关掉就是把档位强制成 Fallback，
    // 库的 Fallback 档不分配 GraphicsLayer（`rememberLiquidGlassState` 里 `layer = null`），
    // 于是背板录制与模糊一起消失，玻璃退回纯 tint + 描边。
    val glassBlurEnabled by AppSettings.glassBlurEnabled.collectAsStateWithLifecycle()
    // 平台档位**无条件**求值，再交给 if 去选：@Composable 调用不能放进 if 分支 —— 分支
    // 切换会让 Compose 的 slot table 错位（冷启动只走一条路径时看不出来，在设置里热切换
    // 开关时才踩得到）。rememberPlatformLiquidGlassQuality 内部是 `remember(context)` 缓存
    // 的一次 ActivityManager 查询，无条件调用没有额外代价，拿到的值不用也不亏。
    val platformQuality = rememberPlatformLiquidGlassQuality()
    val glassState = rememberLiquidGlassState(
        quality = if (glassBlurEnabled) platformQuality else LiquidGlassQuality.Fallback,
    )

    CompositionLocalProvider(LocalLiquidGlassState provides glassState) {
        // 背景层铺在 NavHost 之下。它是全 App 唯一的背景来源，各页面靠把 Scaffold 与
        // TopAppBar 的 containerColor 设成 Transparent 把底透出来 —— 漏改一处就会在该页
        // 留下一条实色横带。首页入口卡的液态玻璃要靠这层背景的空间层次才看得出来。
        Box(modifier = Modifier.fillMaxSize()) {
            AppBackground()

            NavHost(navController = navController, startDestination = startDestination) {
                composable(AppRoutes.SPLASH) {
                    SplashScreen(
                        onFinished = {
                            navController.navigate(AppRoutes.HOME) {
                                popUpTo(AppRoutes.SPLASH) { inclusive = true }
                            }
                        },
                    )
                }

                composable(AppRoutes.HOME) {
                    HomeScreen(
                        onOpenAllocate = {
                            // 对应原 MainActivity 新建实例时的字段初始化：每次从首页进去都是这四个
                            allPlayers = DEFAULT_PLAYERS
                            selectedPlayers = emptyList()
                            navController.navigate(AppRoutes.ALLOCATE)
                        },
                        onOpenMemo = { navController.navigate(MemoRoutes.LIST) },
                        onOpenAlm = { navController.navigate(AlmRoutes.HOME) },
                        onOpenConvert = { navController.navigate(ConvertRoutes.HOME) },
                        onOpenAppearance = { navController.navigate(AppRoutes.APPEARANCE) },
                        onOpenAbout = { navController.navigate(AppRoutes.ABOUT) },
                    )
                }

                composable(AppRoutes.ALLOCATE) {
                    AllocateScreen(
                        selectedPlayers = selectedPlayers,
                        onBack = { navController.popBackStack() },
                        onPickPlayers = { navController.navigate(AppRoutes.PLAYER_SELECT) },
                    )
                }

                composable(AppRoutes.PLAYER_SELECT) {
                    PlayerSelectionScreen(
                        initialPlayers = allPlayers,
                        onBack = { navController.popBackStack() },
                        onDone = { all, selected ->
                            allPlayers = all
                            selectedPlayers = selected
                            navController.popBackStack()
                        },
                    )
                }

                composable(AppRoutes.APPEARANCE) {
                    AppearanceScreen(onBack = { navController.popBackStack() })
                }

                composable(AppRoutes.ABOUT) {
                    AboutScreen(onBack = { navController.popBackStack() })
                }

                // 辅助模块自带的导航图。
                //
                // memo 的玻璃面不用额外传：它自己的 LocalLiquidGlassState 就是上面那个
                // CompositionLocalProvider 提供的，模块内天然可见。
                // alm 与 memo 平级、看不到 memo 的符号，所以要把**同一个** state 实例显式交给它 ——
                // 玻璃的 backdrop 录在这个实例上，给成另一个实例就永远不模糊（见 AlmGlassContract）。
                memoGraph(navController)
                almGraph(navController, glassState)
                // convert 与 memo/alm 平级，同样跨不过模块边界拿到 memo 的契约符号，
                // 所以把**同一个** state 实例显式交给它（原因同 alm，见 ConvertGlassContract）。
                convertGraph(navController, glassState)
            }
        }
    }
}

/** 与原 MainActivity 里那份示例元素一致。 */
private val DEFAULT_PLAYERS = listOf("示例元素1", "示例元素2", "示例元素3", "示例元素4")

/**
 * 把模块打过来的深链路由解析成一条**确定注册得上**的路由，认不出来就返回 null（退回启动页）。
 *
 * 目前有两个来源，都是改造前就存在的直达行为，不能丢：
 * - alm 的闹钟通知：原来直接拉起 `MainActivityAlm`；
 * - memo 的备忘提醒：原来直接拉起 `MemoDetailActivity` 并带上 `memo_id`。
 * 两者现在都改成"打开宿主 + 带一个路由 extra"，宿主在这里落地。
 *
 * 做成显式解析而不是"有 extra 就拿去 startDestination"：路由名或参数非法时，
 * Navigation 会在建图 / 解析参数阶段直接抛异常崩掉，这里挡一道能让它安静地退回启动页。
 */
private fun resolveDeepLinkRoute(route: String?): String? = when {
    route == null -> null
    route == AlmRoutes.HOME -> AlmRoutes.HOME
    route.startsWith(MEMO_DETAIL_PREFIX) ->
        route.removePrefix(MEMO_DETAIL_PREFIX)
            .toIntOrNull()
            ?.let { MemoRoutes.detail(it) }

    else -> null
}

/** `memo/detail/{memoId}` → 前缀 `memo/detail/`，用来从直达路由里剥出 memoId。 */
private val MEMO_DETAIL_PREFIX = MemoRoutes.DETAIL.substringBefore("{")
