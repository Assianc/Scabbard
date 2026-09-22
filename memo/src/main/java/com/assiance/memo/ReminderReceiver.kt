package com.assiance.memo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.assiance.memo.ui.MemoRoutes

/**
 * 宿主深链路由的 extra 键。
 *
 * 约定值，与 `com.assiance.scabbard.ScabbardActivity.EXTRA_ROUTE` 必须一致 ——
 * 依赖方向是 app → 模块，memo 看不到主模块的常量，只能按字面量写一份。改一边就要改两边。
 */
private const val HOST_ROUTE_EXTRA = "com.assiance.host.extra.ROUTE"

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "备忘提醒"
        val content = intent.getStringExtra("content") ?: ""
        val memoId = intent.getLongExtra("memo_id", -1)

        // 创建通知渠道（Android 8.0及以上需要）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "memo_reminder",
                "备忘录提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "备忘录提醒通知"
                enableLights(true)
                lightColor = Color.GREEN
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        // 详情页已并入宿主的单 Activity NavHost，memo 模块内不再有自己的 Activity，
        // 所以改成“打开宿主的启动 Intent + 带一个目标路由 extra”，由宿主落到对应页面。
        // 路由名沿用模块自己的路由表，宿主在 ScabbardActivity.EXTRA_ROUTE 上收。
        //
        // flags 与改造前一致（NEW_TASK + CLEAR_TASK）：原来点提醒也是清掉原任务、
        // 以详情页为根新起一个 Activity，返回即退出；宿主那边同样走冷启动路径，行为不变。
        val detailIntent = (
            context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setPackage(context.packageName)
                }
            ).apply {
            putExtra(HOST_ROUTE_EXTRA, MemoRoutes.detail(memoId.toInt()))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 
            memoId.toInt(), 
            detailIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 创建通知
        val notification = NotificationCompat.Builder(context, "memo_reminder")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(Color.parseColor("#4CAF50"))
            .build()

        // 显示通知
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
} 