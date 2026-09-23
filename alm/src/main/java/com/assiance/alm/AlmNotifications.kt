package com.assiance.alm

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.assiance.alm.ui.AlmHostNavigation
import com.assiance.alm.ui.AlmRoutes

/**
 * alm 的通知。
 *
 * 这里只有一条通知——「闹钟时间到了！」——它的内容、渠道、优先级、图标、autoCancel
 * 全部与原 `MainActivityAlm.showNotification()` 一致。
 *
 * 唯一的改动：点击通知原来拉起 `MainActivityAlm`（Activity 已删除），现在改成
 * [AlmHostNavigation] 约定的「指向宿主单 Activity + 带路由 extra」的 PendingIntent，
 * 路由为 [AlmRoutes.HOME]。
 *
 * 说明：原实现里这段代码是**不可达的死代码**（它挂在一个从未 registerReceiver 的
 * Activity 内部 receiver 上，所以这条通知实际上从来没有弹出过）。
 *
 * 迁移时先保持了同样的可达性；**2026-09-23 用户明确要求接通到点通知**，于是在
 * [AlarmReceiver] 的 `ALARM_ACTION` 分支（真实触发点）显式调用本方法，并在
 * `ALARM_STOP_ACTION` 分支撤销这条通知（原实现没有撤销代码，因为通知从没弹出过）。
 * 除这两个调用点外，本方法的内容仍是原 `MainActivityAlm.showNotification()` 的原样搬运，
 * 文案一个字都没有新增。
 */
object AlmNotifications {

    fun showAlarmTriggered(context: Context) {
        val pendingIntent = AlmHostNavigation.hostRoutePendingIntent(
            context = context,
            requestCode = 0,
            route = AlmRoutes.HOME,
        )

        val builder = NotificationCompat.Builder(context, AlmContract.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("冬去鸟鸣时")
            .setContentText("闹钟时间到了！")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(AlmContract.NOTIFICATION_ID, builder.build())
    }
}
