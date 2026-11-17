package com.assiance.memo.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/** memo 对外的路由名。宿主 NavHost 只需知道 [LIST]，模块内部路由都由本文件注册。 */
object MemoRoutes {
    const val LIST = "memo/list"

    /** 新建备忘录 */
    const val ADD = "memo/add"

    /** 详情，参数为备忘录 id */
    const val DETAIL = "memo/detail/{memoId}"

    /** 历史记录，参数为备忘录 id */
    const val HISTORY = "memo/history/{memoId}"

    const val ARG_MEMO_ID = "memoId"

    /** 历史页恢复版本后回传给详情页的信号 key（替代原来的 setResult(RESULT_OK)） */
    const val RESULT_RESTORED = "memo_restored"

    fun detail(memoId: Int): String = "memo/detail/$memoId"

    fun history(memoId: Int): String = "memo/history/$memoId"
}

/** memo 模块的导航图，宿主在 NavHost 中调用一次即可。 */
fun NavGraphBuilder.memoGraph(navController: NavController) {
    composable(MemoRoutes.LIST) {
        MemoListScreen(
            onOpenDetail = { memoId -> navController.navigate(MemoRoutes.detail(memoId)) },
            onAddMemo = { navController.navigate(MemoRoutes.ADD) },
        )
    }

    composable(MemoRoutes.ADD) {
        MemoAddScreen(onDone = { navController.popBackStack() })
    }

    composable(
        route = MemoRoutes.DETAIL,
        arguments = listOf(navArgument(MemoRoutes.ARG_MEMO_ID) { type = NavType.IntType }),
    ) { entry ->
        val memoId = entry.arguments?.getInt(MemoRoutes.ARG_MEMO_ID) ?: -1
        // 历史页恢复版本后（见下方 HISTORY）会往这里塞 true
        val restored by entry.savedStateHandle
            .getStateFlow(MemoRoutes.RESULT_RESTORED, false)
            .collectAsState()
        MemoDetailScreen(
            memoId = memoId,
            restoredSignal = restored,
            onRestoredSignalConsumed = { entry.savedStateHandle[MemoRoutes.RESULT_RESTORED] = false },
            onOpenHistory = { id -> navController.navigate(MemoRoutes.history(id)) },
        )
    }

    composable(
        route = MemoRoutes.HISTORY,
        arguments = listOf(navArgument(MemoRoutes.ARG_MEMO_ID) { type = NavType.IntType }),
    ) { entry ->
        val memoId = entry.arguments?.getInt(MemoRoutes.ARG_MEMO_ID) ?: -1
        MemoHistoryScreen(
            memoId = memoId,
            onRestored = {
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set(MemoRoutes.RESULT_RESTORED, true)
                navController.popBackStack()
            },
            onClose = { navController.popBackStack() },
        )
    }
}
