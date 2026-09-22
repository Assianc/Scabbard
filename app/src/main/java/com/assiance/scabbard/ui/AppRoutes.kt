package com.assiance.scabbard.ui

/**
 * app 主模块自己的路由表。
 *
 * 跨模块入口不在这里：memo 用 [com.assiance.memo.ui.MemoRoutes.LIST]，
 * alm 用 [com.assiance.alm.ui.AlmRoutes.HOME]，各自模块自带导航图，
 * app 只负责把它们挂进同一个 NavHost。
 */
object AppRoutes {
    const val SPLASH = "app/splash"
    const val HOME = "app/home"
    const val ALLOCATE = "app/allocate"
    const val PLAYER_SELECT = "app/player_select"
    const val APPEARANCE = "app/appearance"
    const val ABOUT = "app/about"

    /**
     * 分类元素选择页回传结果的键。
     * 沿用改造前 PlayerSelectionActivity 的 Intent extra 名，语义不变，
     * 现在走 NavController 的 savedStateHandle。
     */
    const val RESULT_ALL_PLAYERS = "allPlayers"
    const val RESULT_SELECTED_PLAYERS = "selectedPlayers"
}
