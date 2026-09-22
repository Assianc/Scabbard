package com.assiance.scabbard.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.assiance.alm.ui.AlmRoutes
import com.assiance.alm.ui.almGraph
import com.assiance.memo.ui.MemoRoutes
import com.assiance.memo.ui.memoGraph
import com.assiance.scabbard.ui.screen.AboutScreen
import com.assiance.scabbard.ui.screen.AllocateScreen
import com.assiance.scabbard.ui.screen.AppearanceScreen
import com.assiance.scabbard.ui.screen.HomeScreen
import com.assiance.scabbard.ui.screen.PlayerSelectionScreen
import com.assiance.scabbard.ui.screen.SplashScreen

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

    // 通知点击等外部入口带进来的路由。只认白名单里的，避免路由名拼错导致建图直接崩。
    val startDestination = remember {
        when {
            deepLinkRoute != null && deepLinkRoute in DEEP_LINK_ROUTES -> deepLinkRoute
            else -> AppRoutes.SPLASH
        }
    }

    // 热启动：Activity 还在（singleTop + onNewIntent），图早就建好了，
    // 这时候只能往现有返回栈上 push，不能改 startDestination。
    // 冷启动时 deepLinkRoute 已经等于 startDestination，这里自然什么都不做。
    LaunchedEffect(deepLinkRoute) {
        val route = deepLinkRoute
        if (route != null && route in DEEP_LINK_ROUTES && route != startDestination) {
            navController.navigate(route)
        }
    }

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

        // 辅助模块自带的导航图
        memoGraph(navController)
        almGraph(navController)
    }
}

/** 与原 MainActivity 里那份示例元素一致。 */
private val DEFAULT_PLAYERS = listOf("示例元素1", "示例元素2", "示例元素3", "示例元素4")

/**
 * 允许作为起始页的「外部直达路由」白名单。
 *
 * 目前只有 alm 的闹钟通知会这么做（见 `AlmHostNavigation`）：改造前点通知是直接拉起
 * `MainActivityAlm`，用户不会看到开场动画，所以这里也不该先闪一下启动页。
 *
 * 新增直达路由时记得往这里加 —— 刻意做成白名单而不是"有 extra 就 startDestination = extra"：
 * 路由名写错时 Navigation 是在建图阶段抛异常崩掉的，白名单能让它退回启动页。
 */
private val DEEP_LINK_ROUTES = setOf(AlmRoutes.HOME)
