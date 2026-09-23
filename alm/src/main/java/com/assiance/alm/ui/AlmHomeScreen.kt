package com.assiance.alm.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import com.assiance.alm.AlarmData
import com.assiance.alm.AlmReceivers
import com.assiance.alm.AlmRepository
import com.assiance.alm.TodoData
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * alm 首页（原 `MainActivityAlm`）。
 *
 * 与原实现的对应关系：
 * - 顶部日期/时钟：原来是 `Handler.postDelayed` 每秒刷新，这里换成 `LaunchedEffect` + `delay(1000)`。
 * - 「闹钟 / 待办」两个标签：原来是两个 MaterialButton + 手写的指示器动画，这里换成 M3 的 [TabRow]。
 * - 两个列表：仍然复用原有的 `AlarmAdapter` / `TodoAdapter`（通过 `AndroidView` 桥接），
 *   侧滑删除、长按、开关、已完成不能侧滑等行为全部来自原 Adapter。
 * - 右下角的「新增」按钮：换成 [DraggableAddFab]（Compose 拖动），行为对齐见该文件注释。
 * - 生命周期：原来在 `onResume` 里 `loadAlarms()` + `loadTodos()`，这里用 `LifecycleEventEffect`。
 * - 广播注册与通知渠道：原来在 `onCreate` 里做一次，这里在首次组合时做一次，见 [AlmReceivers]。
 */
@Composable
internal fun AlmHomeScreen(navController: NavController) {
    val context = LocalContext.current

    var alarms by remember { mutableStateOf<List<AlarmData>>(emptyList()) }
    var todos by remember { mutableStateOf<List<TodoData>>(emptyList()) }
    var currentTab by rememberSaveable { mutableStateOf(TAB_ALARM) }

    fun refresh() {
        alarms = AlmRepository.loadAlarms(context)
        todos = AlmRepository.loadTodos(context)
    }

    // 通知渠道 + 动态注册广播（等价于原 Activity 的 onCreate）
    LaunchedEffect(Unit) {
        AlmRepository.createAlarmNotificationChannel(context)
        AlmReceivers.ensureRegistered(context)
        refresh()
    }

    // 等价于原 Activity 的 onResume
    LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        refresh()
    }

    // ---------------------------------------------------------------- 待删除确认
    var pendingDeleteAlarm by remember { mutableStateOf<AlarmData?>(null) }
    var pendingDeleteTodo by remember { mutableStateOf<TodoData?>(null) }

    pendingDeleteAlarm?.let { alarm ->
        AlertDialog(
            onDismissRequest = { pendingDeleteAlarm = null },
            title = { Text("删除闹钟") },
            text = { Text("确定要删除这个闹钟吗？") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeleteAlarm = null
                    AlmRepository.cancelAlarm(context, alarm)
                    alarms = alarms - alarm
                    AlmRepository.saveAlarms(context, alarms)
                    Toast.makeText(context, "闹钟已删除", Toast.LENGTH_SHORT).show()
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteAlarm = null }) { Text("取消") }
            },
        )
    }

    pendingDeleteTodo?.let { todo ->
        AlertDialog(
            onDismissRequest = { pendingDeleteTodo = null },
            title = { Text("删除待办") },
            text = { Text("确定要删除这个待办事项吗？") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeleteTodo = null
                    AlmRepository.cancelTodoReminder(context, todo.id)
                    todos = todos - todo
                    AlmRepository.saveTodos(context, todos)
                    Toast.makeText(context, "待办已删除", Toast.LENGTH_SHORT).show()
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTodo = null }) { Text("取消") }
            },
        )
    }

    // ---------------------------------------------------------------- 悬浮窗权限
    var showOverlayDialog by rememberSaveable { mutableStateOf(false) }
    var overlayAsked by rememberSaveable { mutableStateOf(false) }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(context)
        ) {
            Toast.makeText(
                context,
                "需要悬浮窗权限来显示闹钟提醒，请在设置中开启",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!overlayAsked &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(context)
        ) {
            overlayAsked = true
            showOverlayDialog = true
        }
    }

    if (showOverlayDialog) {
        AlertDialog(
            onDismissRequest = { showOverlayDialog = false },
            title = { Text("需要悬浮窗权限") },
            text = { Text("为了在闹钟响起时显示提醒窗口，需要您授予悬浮窗权限。") },
            confirmButton = {
                TextButton(onClick = {
                    showOverlayDialog = false
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}"),
                    )
                    overlayPermissionLauncher.launch(intent)
                }) { Text("去设置") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showOverlayDialog = false
                    Toast.makeText(
                        context,
                        "未授予悬浮窗权限，闹钟提醒可能无法正常显示",
                        Toast.LENGTH_LONG,
                    ).show()
                }) { Text("取消") }
            },
        )
    }

    // ---------------------------------------------------------------- 时钟
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000L)
        }
    }
    val dateFormatter = remember { SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINESE) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.CHINESE) }
    val calendar = remember(now) { Calendar.getInstance().apply { timeInMillis = now } }

    // ---------------------------------------------------------------- 界面
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .onSizeChanged { containerSize = it },
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = dateFormatter.format(calendar.time),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = timeFormatter.format(calendar.time),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            // 原实现：点时间进历史记录；失败时 Toast 提示
                            try {
                                navController.navigate(AlmRoutes.CALENDAR)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "打开日历失败: ${e.message}",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                e.printStackTrace()
                            }
                        },
                    )
                }

                TabRow(selectedTabIndex = currentTab) {
                    Tab(
                        selected = currentTab == TAB_ALARM,
                        onClick = { currentTab = TAB_ALARM },
                        text = { Text("闹钟") },
                    )
                    Tab(
                        selected = currentTab == TAB_TODO,
                        onClick = { currentTab = TAB_TODO },
                        text = { Text("待办") },
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    // 原实现是两个列表都装在 ViewFlipper 里同时保活、切标签只切显示，
                    // 所以各自的滚动位置都在。这里等价处理：两个列表都常驻组合树，只切可见性。
                    //
                    // 必须「常驻」的原因：AndroidView 一旦离开组合，承载的 View 会被从视图树
                    // 摘掉（compose-ui 的 AndroidViewHolder.onDetach → removeAllViewsInLayout），
                    // 再次进入时 factory 会重新造一个 RecyclerView，LinearLayoutManager 的
                    // 滚动位置随之丢失。
                    // 隐藏用 alpha 而不是 size(0)/GONE：alpha 只落在绘制层，完全不碰 View 的
                    // measure/layout，RecyclerView 不受任何打扰（且 alpha == 1f 时 Compose
                    // 直接返回原 Modifier，可见的那份连图层都不会多出来）。
                    // zIndex 让可见的那份压在上层：两层都会进命中路径，但上层转发给自己那个
                    // Android View 后会把指针事件整个消费掉，隐藏那份在 PointerInteropFilter
                    // 里检测到已消费就停止转发，所以不会出现「滑到看不见的列表上」。
                    KeepAlivePane(visible = currentTab == TAB_TODO) {
                        TodoListView(
                            todos = todos,
                            onDeleteClick = { pendingDeleteTodo = it },
                            onToggleClick = { todo, isCompleted ->
                                val index = todos.indexOf(todo)
                                if (index != -1) {
                                    val updated = todo.copy(
                                        isCompleted = isCompleted,
                                        completedTime = if (isCompleted) {
                                            System.currentTimeMillis()
                                        } else {
                                            null
                                        },
                                    )
                                    if (isCompleted) {
                                        AlmRepository.cancelTodoReminder(context, todo.id)
                                    }
                                    // 与原实现一致：按完成状态、截止时间重排后再落库
                                    todos = todos.toMutableList().also { list ->
                                        list[index] = updated
                                        list.sortWith(
                                            compareBy<TodoData> { it.isCompleted }
                                                .thenBy { it.dueTime ?: Long.MAX_VALUE }
                                        )
                                    }
                                    AlmRepository.saveTodos(context, todos)
                                }
                            },
                            onTodoClick = { todo ->
                                navController.navigate(
                                    AlmRoutes.todoSetting(
                                        todoId = todo.id,
                                        title = todo.title,
                                        description = todo.description,
                                        startTime = todo.startTime ?: -1L,
                                        dueTime = todo.dueTime ?: -1L,
                                        startRingtoneUri = todo.startRingtoneUri,
                                        dueRingtoneUri = todo.dueRingtoneUri,
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    KeepAlivePane(visible = currentTab == TAB_ALARM) {
                        AlarmListView(
                            alarms = alarms,
                            onDeleteClick = { pendingDeleteAlarm = it },
                            onToggleClick = { alarm, isEnabled ->
                                val index = alarms.indexOf(alarm)
                                if (index != -1) {
                                    val updated = alarm.copy(isEnabled = isEnabled)
                                    if (isEnabled) {
                                        AlmRepository.setAlarm(context, updated)
                                    } else {
                                        AlmRepository.cancelAlarm(context, updated)
                                    }
                                    alarms = alarms.toMutableList().also { it[index] = updated }
                                    AlmRepository.saveAlarms(context, alarms)
                                }
                            },
                            onAlarmClick = { alarm ->
                                navController.navigate(
                                    AlmRoutes.alarmSetting(
                                        alarmId = alarm.id,
                                        alarmTime = alarm.timeInMillis,
                                        ringtoneUri = alarm.ringtoneUri,
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            DraggableAddFab(
                isAlarmTab = currentTab == TAB_ALARM,
                containerSize = containerSize,
                onClick = {
                    navController.navigate(
                        if (currentTab == TAB_ALARM) {
                            AlmRoutes.alarmSetting()
                        } else {
                            AlmRoutes.todoSetting()
                        }
                    )
                },
                modifier = Modifier.align(Alignment.TopStart),
            )
        }
    }
}

/**
 * 常驻组合树、只切可见性的容器 —— 对应原 `ViewFlipper` 的语义。
 *
 * 与 `when (tab) { ... }` 的区别：内容始终留在组合里，`AndroidView` 不会走到 onDetach，
 * 承载的 `RecyclerView` 因此不离开视图树，`layoutManager` 的滚动位置不会被重建丢掉。
 *
 * 隐藏用 `alpha` 而不是 `size(0.dp)`/`GONE`：后者会改变 View 的测量结果，RecyclerView 会被
 * 以 0 尺寸重新布局，滚动位置反而保不住。
 */
@Composable
private fun KeepAlivePane(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(if (visible) 1f else 0f)
            .alpha(if (visible) 1f else 0f),
    ) {
        content()
    }
}

private const val TAB_ALARM = 0
private const val TAB_TODO = 1
