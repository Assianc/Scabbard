package com.assiance.convert.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.assiance.convert.ui.tools.LunarScreen
import com.assiance.convert.ui.tools.TimeZoneScreen
import io.github.nadeemiqbal.liquidglass.LiquidGlassState

/**
 * convert 模块对外的入口。
 *
 * 宿主只需要知道 [HOME]：首页（「晨昏」）→ 内部菜单 → 功能页的跳转全部由 convert
 * 自己用 `navController.navigate(...)` 完成，与 memo / alm 的分工一致。
 */
object ConvertRoutes {

    /** 首页（晨昏）。宿主进入 convert 模块的入口。 */
    const val HOME = "convert/home"

    /** 定时任务 cron 表达式转换。 */
    const val CRON = "convert/cron"

    /** 时区对照。 */
    const val TIME_ZONE = "convert/timezone"

    /** 农历 ↔ 公历。 */
    const val LUNAR = "convert/lunar"
}

/**
 * 把 convert 的所有路由注册到宿主的 NavHost 上。
 *
 * [glassState] 必须是宿主背板录制所用的**同一个**实例，否则玻璃永远不模糊
 * （原因见 [LocalConvertGlassState]）。
 */
fun NavGraphBuilder.convertGraph(
    navController: NavController,
    glassState: LiquidGlassState,
) {
    composable(ConvertRoutes.HOME) {
        CompositionLocalProvider(LocalConvertGlassState provides glassState) {
            ConvertHomeScreen(
                onNavigate = { route -> navController.navigate(route) },
                onBack = { navController.popBackStack() },
            )
        }
    }

    composable(ConvertRoutes.CRON) {
        CompositionLocalProvider(LocalConvertGlassState provides glassState) {
            CronConverterScreen(onBack = { navController.popBackStack() })
        }
    }

    composable(ConvertRoutes.TIME_ZONE) {
        CompositionLocalProvider(LocalConvertGlassState provides glassState) {
            TimeZoneScreen(onBack = { navController.popBackStack() })
        }
    }

    composable(ConvertRoutes.LUNAR) {
        CompositionLocalProvider(LocalConvertGlassState provides glassState) {
            LunarScreen(onBack = { navController.popBackStack() })
        }
    }
}
