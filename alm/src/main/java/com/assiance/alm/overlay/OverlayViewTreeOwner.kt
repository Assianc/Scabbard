package com.assiance.alm.overlay

import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * 两个悬浮窗 Service 用的「人工 ViewTree owner」。
 *
 * 悬浮窗是 WindowManager overlay，外面没有 Activity，所以 ComposeView 在
 * `onAttachedToWindow` 时找不到 `ViewTreeLifecycleOwner`（会直接抛
 * "ViewTreeLifecycleOwner not found from ..."），必须自己按标准做法挂一套：
 * [LifecycleRegistry] 由 Service 手动驱动，[SavedStateRegistry] 手动 restore。
 *
 * 注意：**只服务于悬浮窗**。走宿主 NavHost 的 4 个屏幕由宿主 Activity 提供 owner，
 * 不要用这个类。
 */
internal class OverlayViewTreeOwner :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    /** Service.onCreate 时调用。 */
    fun onCreate() {
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    /** 视图加入 WindowManager 前调用。 */
    fun onStart() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    /** 视图加入 WindowManager 后调用。 */
    fun onResume() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    /** Service.onDestroy 时调用（视图已从 WindowManager 移除之后）。 */
    fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }

    /** 把三个 owner 挂到 ComposeView 上；必须在 `addView` 之前调用。 */
    fun attachTo(view: View) {
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
    }
}

/**
 * 造一个「自带 ViewTree owner + 自带极小主题」的 ComposeView，供悬浮窗使用。
 *
 * @param owner 由 Service 持有并驱动生命周期。
 * @param content 悬浮窗内容，会被 [AlmOverlayTheme] 包住。
 */
internal fun createOverlayComposeView(
    context: Context,
    owner: OverlayViewTreeOwner,
    content: @Composable () -> Unit,
): ComposeView = ComposeView(context).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
    owner.attachTo(this)
    setContent {
        AlmOverlayTheme {
            content()
        }
    }
}
