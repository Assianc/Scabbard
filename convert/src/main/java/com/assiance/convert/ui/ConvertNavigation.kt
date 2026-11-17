package com.assiance.convert.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.assiance.convert.ui.tools.ColorScreen
import com.assiance.convert.ui.tools.DateDiffScreen
import com.assiance.convert.ui.tools.EncodingScreen
import com.assiance.convert.ui.tools.LunarScreen
import com.assiance.convert.ui.tools.NamingCaseScreen
import com.assiance.convert.ui.tools.NumberBaseScreen
import com.assiance.convert.ui.tools.StorageUnitScreen
import com.assiance.convert.ui.tools.TimeZoneScreen
import com.assiance.convert.ui.tools.TimestampScreen
import com.assiance.convert.ui.tools.UnitConvertScreen
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

    /** 时间戳 ↔ 日期时间。 */
    const val TIMESTAMP = "convert/timestamp"

    /** 两个日期相差的自然日数。 */
    const val DATE_DIFF = "convert/date_diff"

    /** 时区对照。 */
    const val TIME_ZONE = "convert/timezone"

    /** 农历 ↔ 公历。 */
    const val LUNAR = "convert/lunar"

    /** 进制转换。 */
    const val NUMBER_BASE = "convert/number_base"

    /** URL / Base64 / JSON 编码转换。 */
    const val ENCODING = "convert/encoding"

    /** 命名风格转换。 */
    const val NAMING_CASE = "convert/naming_case"

    /** 存储单位换算。 */
    const val STORAGE_UNIT = "convert/storage_unit"

    /** 生活单位换算。 */
    const val UNITS = "convert/units"

    /** 颜色格式转换。 */
    const val COLOR = "convert/color"
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

    composable(ConvertRoutes.TIMESTAMP) {
        CompositionLocalProvider(LocalConvertGlassState provides glassState) {
            TimestampScreen(onBack = { navController.popBackStack() })
        }
    }

    composable(ConvertRoutes.DATE_DIFF) {
        CompositionLocalProvider(LocalConvertGlassState provides glassState) {
            DateDiffScreen(onBack = { navController.popBackStack() })
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

    composable(ConvertRoutes.NUMBER_BASE) {
        CompositionLocalProvider(LocalConvertGlassState provides glassState) {
            NumberBaseScreen(onBack = { navController.popBackStack() })
        }
    }

    composable(ConvertRoutes.ENCODING) {
        CompositionLocalProvider(LocalConvertGlassState provides glassState) {
            EncodingScreen(onBack = { navController.popBackStack() })
        }
    }

    composable(ConvertRoutes.NAMING_CASE) {
        CompositionLocalProvider(LocalConvertGlassState provides glassState) {
            NamingCaseScreen(onBack = { navController.popBackStack() })
        }
    }

    composable(ConvertRoutes.STORAGE_UNIT) {
        CompositionLocalProvider(LocalConvertGlassState provides glassState) {
            StorageUnitScreen(onBack = { navController.popBackStack() })
        }
    }

    composable(ConvertRoutes.UNITS) {
        CompositionLocalProvider(LocalConvertGlassState provides glassState) {
            UnitConvertScreen(onBack = { navController.popBackStack() })
        }
    }

    composable(ConvertRoutes.COLOR) {
        CompositionLocalProvider(LocalConvertGlassState provides glassState) {
            ColorScreen(onBack = { navController.popBackStack() })
        }
    }
}
