package com.assiance.scabbard

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.assiance.alm.AlmHostNavigation
import com.assiance.scabbard.ui.ScabbardApp
import com.assiance.scabbard.ui.theme.ScabbardTheme

/**
 * 全 App 唯一的 Activity。
 *
 * 改造前是 9 个 Activity（外加 memo / alm 两个模块各自的 Activity）互相 startActivity 跳转，
 * 现在统一由这里的 NavHost 承载。清单里 15 个 activity-alias 的 targetActivity 全部指向本类，
 * 别名本身的 android:name 一个都没改 —— IconManager 是按别名名字开关组件的，
 * 而别名名字又存进了老用户的 app_settings.current_icon，改名会让他们的图标设置失效。
 *
 * **launchMode 是 singleTop**（写在清单里）：alm 的闹钟通知会在点击时带一个路由 extra
 * 打回来（见 [AlmHostNavigation]），singleTop 让这个 Intent 落到已有的实例上，
 * 于是路由被 push 到现有 NavHost 的返回栈顶 —— 用户看完闹钟按返回能退回原来那一页。
 * 这正是改造前的行为：那边是直接 `Intent(this, MainActivityAlm::class.java)`
 * 新起一个 Activity 叠在当前界面之上，返回同样退得回去；区别只在于这里不再多一个 Activity 实例。
 *
 * 启动页不会因此被跳过或重复出现：冷启动带路由时 [ScabbardApp] 直接把它当起始页，
 * 不带路由时照旧从启动页开始 —— 和改造前点通知直接进 MainActivityAlm、
 * 点图标先看开场动画是同一个区分。
 */
class ScabbardActivity : ComponentActivity() {

    /**
     * 外部入口（目前只有 alm 的闹钟通知）带进来的目标路由。
     * 用 State 而不是普通字段：热启动走 onNewIntent 时靠它触发重组，
     * 让已经建好的 NavHost 去 navigate，而不是重建整张图。
     */
    private val deepLinkRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        deepLinkRoute.value = intent?.getStringExtra(AlmHostNavigation.EXTRA_ROUTE)
        setContent {
            ScabbardTheme {
                ScabbardApp(deepLinkRoute = deepLinkRoute.value)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkRoute.value = intent.getStringExtra(AlmHostNavigation.EXTRA_ROUTE)
    }
}
