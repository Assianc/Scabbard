package com.assiance.convert.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TextFields
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
        title = "命名风格",
        subtitle = "snake_case / camelCase / kebab-case 互转",
        icon = Icons.Default.TextFields,
        route = ConvertRoutes.NAMING_CASE,
    ),
    ConvertTool(
        title = "存储单位",
        subtitle = "B / KB / MB / GB / TB，支持 1024 与 1000",
        icon = Icons.Default.Storage,
        route = ConvertRoutes.STORAGE_UNIT,
    ),
)
