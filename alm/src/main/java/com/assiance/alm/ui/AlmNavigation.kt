package com.assiance.alm.ui

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import io.github.nadeemiqbal.liquidglass.LiquidGlassState

/**
 * alm 模块对外的**唯一入口**。
 *
 * 宿主（app 的单 Activity NavHost）只需要：
 * ```
 * NavHost(navController, startDestination = ...) {
 *     almGraph(navController)
 * }
 * ```
 * 内部路由全部由 alm 自己注册、自己用 `navController.navigate(...)` 跳转，
 * 不使用 Intent / startActivity（铃音选择器等**外部系统 UI** 除外）。
 */
object AlmRoutes {

    // ------------------------------------------------------------ 路由名
    /** 首页（闹钟 / 待办）。宿主进入 alm 模块的入口。 */
    const val HOME = "alm/home"

    /** 历史记录（原 CalendarActivity）。 */
    const val CALENDAR = "alm/calendar"

    /** 闹钟设置（原 AlarmSettingActivity）。 */
    const val ALARM_SETTING = "alm/alarm/setting"

    /** 待办设置（原 TodoSettingActivity）。 */
    const val TODO_SETTING = "alm/todo/setting"

    // ------------------------------------------------------------ 路由 pattern
    const val PATTERN_ALARM_SETTING =
        "alm/alarm/setting?alarmId={alarmId}&alarmTime={alarmTime}&ringtoneUri={ringtoneUri}"

    const val PATTERN_TODO_SETTING =
        "alm/todo/setting?todoId={todoId}&title={title}&description={description}" +
            "&startTime={startTime}&dueTime={dueTime}" +
            "&startRingtoneUri={startRingtoneUri}&dueRingtoneUri={dueRingtoneUri}"

    // ------------------------------------------------------------ 参数名
    const val ARG_ALARM_ID = "alarmId"
    const val ARG_ALARM_TIME = "alarmTime"
    const val ARG_RINGTONE_URI = "ringtoneUri"
    const val ARG_TODO_ID = "todoId"
    const val ARG_TODO_TITLE = "title"
    const val ARG_TODO_DESCRIPTION = "description"
    const val ARG_TODO_START_TIME = "startTime"
    const val ARG_TODO_DUE_TIME = "dueTime"
    const val ARG_TODO_START_RINGTONE_URI = "startRingtoneUri"
    const val ARG_TODO_DUE_RINGTONE_URI = "dueRingtoneUri"

    // ------------------------------------------------------------ 构造路由
    /**
     * 构造闹钟设置路由。对应原 `AlarmSettingActivity` 的三个 extra：
     * `alarm_id` / `alarm_time` / `ringtone_uri`。
     */
    fun alarmSetting(
        alarmId: Int = -1,
        alarmTime: Long = -1L,
        ringtoneUri: String? = null,
    ): String = buildString {
        append(ALARM_SETTING)
        append("?$ARG_ALARM_ID=$alarmId")
        append("&$ARG_ALARM_TIME=$alarmTime")
        if (ringtoneUri != null) {
            append("&$ARG_RINGTONE_URI=${Uri.encode(ringtoneUri)}")
        }
    }

    /**
     * 构造待办设置路由。对应原 `TodoSettingActivity` 的各个 extra。
     *
     * 原 `MainActivityAlm.editTodo()` 还会传 `todo_advance_minutes`，但 `TodoSettingActivity`
     * 读出来的 `advanceMinutes` 从未被使用（死字段），所以这里不再传递。
     */
    fun todoSetting(
        todoId: Int = -1,
        title: String = "",
        description: String = "",
        startTime: Long = -1L,
        dueTime: Long = -1L,
        startRingtoneUri: String? = null,
        dueRingtoneUri: String? = null,
    ): String = buildString {
        append(TODO_SETTING)
        append("?$ARG_TODO_ID=$todoId")
        append("&$ARG_TODO_TITLE=${Uri.encode(title)}")
        append("&$ARG_TODO_DESCRIPTION=${Uri.encode(description)}")
        append("&$ARG_TODO_START_TIME=$startTime")
        append("&$ARG_TODO_DUE_TIME=$dueTime")
        if (startRingtoneUri != null) {
            append("&$ARG_TODO_START_RINGTONE_URI=${Uri.encode(startRingtoneUri)}")
        }
        if (dueRingtoneUri != null) {
            append("&$ARG_TODO_DUE_RINGTONE_URI=${Uri.encode(dueRingtoneUri)}")
        }
    }
}

/**
 * 把 alm 的所有路由注册到宿主的 NavHost 上。
 *
 * 宿主 NavGraph 里只调用这一次；alm 内部再次跳转（首页 → 设置页 / 历史记录）由 alm 自己完成。
 */
fun NavGraphBuilder.almGraph(
    navController: NavController,
    glassState: LiquidGlassState,
) {
    composable(AlmRoutes.HOME) {
        CompositionLocalProvider(LocalAlmGlassState provides glassState) {
            AlmHomeScreen(navController = navController)
        }
    }

    composable(
        route = AlmRoutes.PATTERN_ALARM_SETTING,
        arguments = listOf(
            navArgument(AlmRoutes.ARG_ALARM_ID) {
                type = NavType.IntType
                defaultValue = -1
            },
            navArgument(AlmRoutes.ARG_ALARM_TIME) {
                type = NavType.LongType
                defaultValue = -1L
            },
            navArgument(AlmRoutes.ARG_RINGTONE_URI) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        ),
    ) { entry ->
        CompositionLocalProvider(LocalAlmGlassState provides glassState) {
            AlarmSettingScreen(
                navController = navController,
                alarmId = entry.arguments?.getInt(AlmRoutes.ARG_ALARM_ID) ?: -1,
                alarmTime = entry.arguments?.getLong(AlmRoutes.ARG_ALARM_TIME) ?: -1L,
                ringtoneUri = entry.arguments?.getString(AlmRoutes.ARG_RINGTONE_URI),
            )
        }
    }

    composable(
        route = AlmRoutes.PATTERN_TODO_SETTING,
        arguments = listOf(
            navArgument(AlmRoutes.ARG_TODO_ID) {
                type = NavType.IntType
                defaultValue = -1
            },
            navArgument(AlmRoutes.ARG_TODO_TITLE) {
                type = NavType.StringType
                defaultValue = ""
            },
            navArgument(AlmRoutes.ARG_TODO_DESCRIPTION) {
                type = NavType.StringType
                defaultValue = ""
            },
            navArgument(AlmRoutes.ARG_TODO_START_TIME) {
                type = NavType.LongType
                defaultValue = -1L
            },
            navArgument(AlmRoutes.ARG_TODO_DUE_TIME) {
                type = NavType.LongType
                defaultValue = -1L
            },
            navArgument(AlmRoutes.ARG_TODO_START_RINGTONE_URI) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument(AlmRoutes.ARG_TODO_DUE_RINGTONE_URI) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        ),
    ) { entry ->
        CompositionLocalProvider(LocalAlmGlassState provides glassState) {
            TodoSettingScreen(
                navController = navController,
                todoId = entry.arguments?.getInt(AlmRoutes.ARG_TODO_ID) ?: -1,
                initialTitle = entry.arguments?.getString(AlmRoutes.ARG_TODO_TITLE) ?: "",
                initialDescription =
                    entry.arguments?.getString(AlmRoutes.ARG_TODO_DESCRIPTION) ?: "",
                initialStartTime = entry.arguments?.getLong(AlmRoutes.ARG_TODO_START_TIME) ?: -1L,
                initialDueTime = entry.arguments?.getLong(AlmRoutes.ARG_TODO_DUE_TIME) ?: -1L,
                initialStartRingtoneUri =
                    entry.arguments?.getString(AlmRoutes.ARG_TODO_START_RINGTONE_URI),
                initialDueRingtoneUri =
                    entry.arguments?.getString(AlmRoutes.ARG_TODO_DUE_RINGTONE_URI),
            )
        }
    }

    composable(AlmRoutes.CALENDAR) {
        CompositionLocalProvider(LocalAlmGlassState provides glassState) {
            CalendarScreen(navController = navController)
        }
    }
}

/**
 * # 通知 / Receiver 拉起界面时的路由约定
 *
 * alm 里唯一一处「点通知要回到界面」的地方，是闹钟触发时的通知（见 [com.assiance.alm.AlmNotifications]）。
 * 宿主已经是单 Activity + NavHost，所以 alm 不再拉起任何 Activity，而是发一个
 * **指向宿主自身**的 PendingIntent，并把要跳转的路由放在 fixed key 的 extra 里：
 *
 * ```
 * Intent(宿主启动 Intent)
 *     .putExtra("com.assiance.host.extra.ROUTE", "alm/home")
 *     .addFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP)
 * ```
 *
 * extra 键 `com.assiance.host.extra.ROUTE` 是**宿主与各业务模块共用的字面量约定**
 * （memo 的备忘提醒走同一条直达链路），模块看不到主模块常量，所以两边各写一份字面量：
 *
 * > **[EXTRA_ROUTE] 的值必须与 `com.assiance.scabbard.ScabbardActivity.EXTRA_ROUTE` 完全一致**，
 * > 任何一侧改动都要同步另一侧。
 *
 * 宿主侧行为（已定）：
 * - 冷启动：把该路由直接当作 `startDestination`（不回开场动画）；
 * - 热启动：`ScabbardActivity` 为 `launchMode="singleTop"`，走 `onNewIntent` → 在现有返回栈上
 *   `navigate(route)`（所以看完闹钟页按返回能退回原来那页）。
 * - 宿主目前只放行 `alm/home` 一条 alm 直达路由（显式解析，非法路由退回启动页而不是崩在建图阶段）。
 *   alm 发的通知路由就是 [AlmRoutes.HOME]，与之一致。
 *
 * 注意：alm 不依赖 app，拿不到宿主的类名，所以这里用「当前 launcher 启动 Intent」定位宿主，
 * 再附上路由 extra。app 侧如果更希望用显式 ComponentName，可以在其入口处覆盖这一层。
 */
object AlmHostNavigation {

    /**
     * 路由信息放在这个 extra 里，取值是 [AlmRoutes] 中的路由名。
     *
     * **必须与 `com.assiance.scabbard.ScabbardActivity.EXTRA_ROUTE` 保持同值**
     * （`"com.assiance.host.extra.ROUTE"`）——两边是字面量各写一份的约定。
     */
    const val EXTRA_ROUTE = "com.assiance.host.extra.ROUTE"

    /** 构造「回到宿主并跳转到 [route]」的 Intent。 */
    fun hostRouteIntent(context: Context, route: String): Intent {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(context.packageName)
        return launchIntent.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_ROUTE, route)
        }
    }

    /** 构造「回到宿主并跳转到 [route]」的 PendingIntent（通知点击用）。 */
    fun hostRoutePendingIntent(
        context: Context,
        requestCode: Int,
        route: String,
    ): PendingIntent = PendingIntent.getActivity(
        context,
        requestCode,
        hostRouteIntent(context, route),
        PendingIntent.FLAG_IMMUTABLE
    )
}
