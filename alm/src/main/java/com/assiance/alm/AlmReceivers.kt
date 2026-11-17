package com.assiance.alm

import android.content.Context
import android.content.IntentFilter
import android.os.Build

/**
 * alm 两个广播接收者的**动态注册**（原 `MainActivityAlm.onCreate` 里做的那一次）。
 *
 * 为什么单独拎出来并且只注册一次：
 * - 原实现在 Activity 的 `onCreate` 里注册，且 Activity 是常驻的，所以注册的生命周期约等于进程；
 * - 现在首页只是一个 Composable，离开导航栈就会退出组合，如果在 `onDispose` 里反注册，
 *   用户在「历史记录 / 设置」页时闹钟到点就收不到广播了（这是必须保住的既有行为）。
 *
 * 因此这里用 application context 注册，**只在进程内注册一次**、不反注册——与原行为的可见效果一致。
 * receiver 实例是单例/长生命周期对象，不会泄漏 Activity 上下文。
 *
 * 注册的三个闹钟 action 与两个待办 action 与原实现逐个一致（见 [AlmContract]）。
 */
internal object AlmReceivers {

    private var registered = false

    private val todoReminderReceiver = TodoReminderReceiver()

    fun ensureRegistered(context: Context) {
        if (registered) return

        val appContext = context.applicationContext

        val alarmFilter = IntentFilter().apply {
            addAction(AlmContract.ALARM_ACTION)
            addAction(AlmContract.ALARM_STOP_ACTION)
            addAction(AlmContract.ALARM_STATUS_CHANGED_ACTION)
        }
        val todoFilter = IntentFilter().apply {
            addAction(AlmContract.TODO_REMINDER_ACTION)
            addAction(AlmContract.TODO_REMINDER_STOP_ACTION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(
                AlarmReceiver.getInstance(),
                alarmFilter,
                Context.RECEIVER_NOT_EXPORTED
            )
            appContext.registerReceiver(
                todoReminderReceiver,
                todoFilter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            appContext.registerReceiver(AlarmReceiver.getInstance(), alarmFilter)
            appContext.registerReceiver(todoReminderReceiver, todoFilter)
        }

        registered = true
    }
}
