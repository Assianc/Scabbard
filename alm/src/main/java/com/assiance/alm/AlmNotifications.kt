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

    /**
     * 待办到点提醒的通知。
     *
     * **这条通知在原实现里并不存在**：模块内只有渠道常量 [AlmContract.TODO_CHANNEL_ID]，
     * 以及 `TodoReminderReceiver` 停止分支里 cancel ID 2/3/4 的痕迹，没有任何实现。
     * 所以文案是新增的，但**没有新造句子**——标题直接用待办自己的标题、正文用待办自己的
     * 描述，都是用户在待办里填过的内容（`AlmRepository.setReminder()` 已把它们放进广播
     * extra）。唯一新写下的字面量是标题为空时的兜底「待办提醒」。
     *
     * 形态与闹钟通知一致：HIGH 优先级、autoCancel、点击回 alm 首页。
     * 小图标用 `ic_todo`（迁移时随被删的 Activity 一起删掉了，本次从 7d31835 原样取回，
     * 内容未改）。**不用 `ic_notification`**：memo 模块已经定义了一个同名资源，虽然
     * `nonTransitiveRClass=true` 下各模块引用各自 R 不会互相覆盖，但没必要让两个模块
     * 出现同名资源；`ic_todo` 在语义上也更贴。
     *
     * 未做两点（原实现无任何可依据的方案，按最简处理）：
     * - 不加 BigTextStyle，长描述在通知里会被截断，展开可见；
     * - 不给「开始/截止 × 提前/准时」分配不同的通知 ID，一个待办同一时刻只留一条提醒通知。
     *   停止提醒时 `TodoReminderReceiver` 会撤掉 ID 2/3/4，覆盖本函数的 ID。
     */
    fun showTodoReminder(context: Context, title: String?, description: String?) {
        val pendingIntent = AlmHostNavigation.hostRoutePendingIntent(
            context = context,
            requestCode = AlmContract.TODO_NOTIFICATION_ID,
            route = AlmRoutes.HOME,
        )

        val builder = NotificationCompat.Builder(context, AlmContract.TODO_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_todo)
            .setContentTitle(title?.takeIf { it.isNotBlank() } ?: DEFAULT_TODO_TITLE)
            .setContentText(description.orEmpty())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(AlmContract.TODO_NOTIFICATION_ID, builder.build())
    }

    /** 待办标题为空时的兜底。写法与备忘侧 `ReminderReceiver` 对 title 的处理一致。 */
    private const val DEFAULT_TODO_TITLE = "待办提醒"
}
