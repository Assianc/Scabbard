package com.assiance.convert.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 「晨昏」首页的一条功能入口。
 *
 * 菜单由数据驱动而不是在 [ConvertHomeScreen] 里逐张写卡片：新增功能时这里追加一项、
 * 到 [ConvertRoutes] 补一个路由常量、在 [convertGraph] 里注册一个 composable 即可。
 * 三处都是纯追加，多个功能并行开发时合并冲突只落在追加点附近。
 */
internal data class ConvertTool(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
)

/** 首页功能清单，按展示顺序排列。 */
internal val CONVERT_TOOLS = listOf(
    ConvertTool(
        title = "Cron 表达式",
        subtitle = "选择时间，生成定时任务表达式",
        icon = Icons.Default.Schedule,
        route = ConvertRoutes.CRON,
    ),
    ConvertTool(
        title = "时间戳转换",
        subtitle = "时间戳与日期时间互转",
        icon = Icons.Default.AccessTime,
        route = ConvertRoutes.TIMESTAMP,
    ),
    ConvertTool(
        title = "日期差计算",
        subtitle = "两个日期相差多少天",
        icon = Icons.Default.DateRange,
        route = ConvertRoutes.DATE_DIFF,
    ),
    ConvertTool(
        title = "单位换算",
        subtitle = "温度、长度、重量",
        icon = Icons.Default.Straighten,
        route = ConvertRoutes.UNITS,
    ),
    ConvertTool(
        title = "颜色格式",
        subtitle = "HEX、RGB、HSL 互转",
        icon = Icons.Default.Palette,
        route = ConvertRoutes.COLOR,
    ),
)
