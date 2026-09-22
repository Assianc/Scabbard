package com.assiance.alm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * alm 模块的数据层：闹钟 / 待办的持久化、精确闹钟的注册与取消、通知渠道创建。
 *
 * 本文件里的每个方法都是从被删除的 4 个 Activity 里逐行搬出来的，业务逻辑没有改动
 * （包括原有的既有缺陷，见交付报告）。唯一的结构性变化是把 `Activity` 上下文换成了
 * 方法参数 `context`。
 */
object AlmRepository {

    // ================================================================ 通知渠道

    /** 原 `MainActivityAlm.createNotificationChannel()`。 */
    fun createAlarmNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "闹钟通知"
            val descriptionText = "冬去鸟鸣时闹钟通知"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(AlmContract.CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // ================================================================ 闹钟：读写

    /**
     * 原 `MainActivityAlm.loadAlarms()`（去掉 adapter 更新与那句空的 updateAlarmStatus）。
     * 返回按 timeInMillis 升序排好的列表。
     */
    fun loadAlarms(context: Context): List<AlarmData> {
        val alarms = mutableListOf<AlarmData>()
        val prefs = context.getSharedPreferences(AlmContract.ALARM_PREFS, Context.MODE_PRIVATE)
        val alarmsJson = prefs.getString(AlmContract.ALARM_LIST_KEY, "[]")
        try {
            val jsonArray = JSONArray(alarmsJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                // 获取重复日期数组
                val repeatDaysArray = obj.optJSONArray("repeatDays")
                val repeatDays = BooleanArray(7) { true } // 默认每天重复
                if (repeatDaysArray != null) {
                    for (j in 0 until repeatDaysArray.length()) {
                        repeatDays[j] = repeatDaysArray.getBoolean(j)
                    }
                }

                alarms.add(
                    AlarmData(
                        id = obj.getInt("id"),
                        timeInMillis = obj.getLong("timeInMillis"),
                        isEnabled = obj.getBoolean("isEnabled"),
                        ringtoneUri = obj.optString("ringtoneUri", null),
                        repeatDays = repeatDays
                    )
                )
            }
            // 按时间排序
            alarms.sortBy { it.timeInMillis }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return alarms
    }

    /**
     * 原 `MainActivityAlm.saveAlarms()`。
     *
     * 注意：与原实现一致，这里**没有**写 `repeatDays` 字段。
     */
    fun saveAlarms(context: Context, alarms: List<AlarmData>) {
        val jsonArray = JSONArray()
        alarms.forEach { alarm ->
            val obj = JSONObject().apply {
                put("id", alarm.id)
                put("timeInMillis", alarm.timeInMillis)
                put("isEnabled", alarm.isEnabled)
                put("ringtoneUri", alarm.ringtoneUri)
            }
            jsonArray.put(obj)
        }

        context.getSharedPreferences(AlmContract.ALARM_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(AlmContract.ALARM_LIST_KEY, jsonArray.toString())
            .apply()
    }

    /** 原 `MainActivityAlm.setAlarm()`：切换开关时注册下一次闹钟。 */
    fun setAlarm(context: Context, alarm: AlarmData) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(AlmContract.ALARM_ACTION).apply {
            `package` = context.packageName
            putExtra("ringtone_uri", alarm.ringtoneUri)
            putExtra("alarm_id", alarm.id)
            putExtra("is_repeating", alarm.repeatDays.any { it })
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = alarm.timeInMillis
            // 如果时间已过，设置为下一个有效日期
            if (timeInMillis <= System.currentTimeMillis()) {
                // 检查是否有重复日期
                val hasRepeatDays = alarm.repeatDays.any { it }
                if (hasRepeatDays) {
                    // 找到下一个重复的日期
                    val today = (get(Calendar.DAY_OF_WEEK) + 5) % 7
                    var daysToAdd = 1
                    for (i in 1..7) {
                        val checkDay = (today + i) % 7
                        if (alarm.repeatDays[checkDay]) {
                            daysToAdd = i
                            break
                        }
                    }
                    add(Calendar.DAY_OF_YEAR, daysToAdd)
                } else {
                    // 非重复闹钟，只往后延一天
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        }

        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent),
                pendingIntent
            )
            Log.d(
                "Alarm",
                "设置闹钟：${
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(calendar.time)
                }"
            )
        } catch (e: Exception) {
            Log.e("Alarm", "设置闹钟失败", e)
            Toast.makeText(context, "设置闹钟失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /** 原 `MainActivityAlm.cancelAlarm()`。 */
    fun cancelAlarm(context: Context, alarm: AlarmData) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(AlmContract.ALARM_ACTION).apply {
            `package` = context.packageName
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /** 原 `AlarmSettingActivity.setNextAlarm()`。 */
    fun setNextAlarmFromSetting(context: Context, alarm: AlarmData) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance().apply {
            timeInMillis = alarm.timeInMillis
        }

        // 检查是否有任何重复日期被选中
        val hasRepeatDays = alarm.repeatDays.any { it }

        if (!hasRepeatDays) {
            // 如果没有选择重复日期，只响铃一次
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        } else {
            // 找到下一个需要响铃的日期
            var daysToAdd = 0
            val today = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 转换为周一为0的索引

            for (i in 0..7) {
                val checkDay = (today + i) % 7
                if (alarm.repeatDays[checkDay]) {
                    daysToAdd = i
                    break
                }
            }
            calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
        }

        val intent = Intent(AlmContract.ALARM_ACTION).apply {
            `package` = context.packageName
            putExtra("ringtone_uri", alarm.ringtoneUri)
            putExtra("is_repeating", hasRepeatDays) // 添加是否重复的标志
            putExtra("alarm_id", alarm.id)           // 传递当前闹钟的 ID
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent),
            pendingIntent
        )

        // 显示提示信息
        val message = if (hasRepeatDays) {
            "已设置重复闹钟"
        } else {
            val dateFormat = SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault())
            "已设置单次闹钟：${dateFormat.format(calendar.time)}"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * 原 `AlarmSettingActivity.setAlarm()` 里写 SharedPreferences 的那一段：
     * 编辑时先删掉旧记录，再追加新记录（带 repeatDays）。
     */
    fun upsertAlarm(context: Context, alarm: AlarmData, isEditing: Boolean) {
        val prefs = context.getSharedPreferences(AlmContract.ALARM_PREFS, Context.MODE_PRIVATE)
        val alarmsJson = prefs.getString(AlmContract.ALARM_LIST_KEY, "[]")
        val jsonArray = JSONArray(alarmsJson)

        // 如果是编辑现有闹钟，先删除旧的
        if (isEditing) {
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.getInt("id") == alarm.id) {
                    jsonArray.remove(i)
                    break
                }
            }
        }

        // 添加新的闹钟数据
        jsonArray.put(JSONObject().apply {
            put("id", alarm.id)
            put("timeInMillis", alarm.timeInMillis)
            put("isEnabled", alarm.isEnabled)
            put("ringtoneUri", alarm.ringtoneUri)
            put("repeatDays", JSONArray(alarm.repeatDays.toList()))
        })

        prefs.edit()
            .putString(AlmContract.ALARM_LIST_KEY, jsonArray.toString())
            .apply()
    }

    /** 原 `AlarmSettingActivity` onCreate 中「编辑模式读回 repeatDays」的那一段。 */
    fun loadRepeatDays(context: Context, alarmId: Int): BooleanArray? {
        val prefs = context.getSharedPreferences(AlmContract.ALARM_PREFS, Context.MODE_PRIVATE)
        val alarmsJson = prefs.getString(AlmContract.ALARM_LIST_KEY, "[]")
        try {
            val jsonArray = JSONArray(alarmsJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.getInt("id") == alarmId) {
                    val jsonRepeat = obj.optJSONArray("repeatDays")
                    if (jsonRepeat != null && jsonRepeat.length() == 7) {
                        return BooleanArray(7) { jsonRepeat.getBoolean(it) }
                    }
                    return null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    // ================================================================ 待办：读写

    /**
     * 原 `MainActivityAlm.loadTodos()`（去掉 adapter 更新）。
     * 会把「已过期且未完成」的待办自动标记为完成并写回。
     */
    fun loadTodos(context: Context): List<TodoData> {
        val todos = mutableListOf<TodoData>()
        val prefs = context.getSharedPreferences(AlmContract.TODO_PREFS, Context.MODE_PRIVATE)
        val todosJson = prefs.getString(AlmContract.TODO_LIST_KEY, "[]")
        try {
            val jsonArray = JSONArray(todosJson)
            val currentTime = System.currentTimeMillis()
            var hasChanges = false

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val dueTime = obj.optLong("dueTime").takeIf { it != 0L }
                val isCompleted = obj.getBoolean("isCompleted")
                val completedTime = obj.optLong("completedTime").takeIf { it > 0L }

                // 如果待办已过期且未完成，自动标记为完成
                val shouldComplete = dueTime != null &&
                    dueTime < currentTime &&
                    !isCompleted

                if (shouldComplete) {
                    obj.put("isCompleted", true)
                    obj.put("completedTime", dueTime) // 设置完成时间为截止时间
                    hasChanges = true
                }

                todos.add(
                    TodoData(
                        id = obj.getInt("id"),
                        title = obj.getString("title"),
                        description = obj.optString("description", ""),
                        startTime = obj.optLong("startTime").takeIf { it != 0L },
                        dueTime = dueTime,
                        isCompleted = shouldComplete || isCompleted,
                        startRingtoneUri = obj.optString("startRingtoneUri", null),
                        dueRingtoneUri = obj.optString("dueRingtoneUri", null),
                        completedTime = if (shouldComplete) dueTime else completedTime // 设置完成时间
                    )
                )
            }

            // 如果有待办被自动完成，保存更新
            if (hasChanges) {
                prefs.edit()
                    .putString(AlmContract.TODO_LIST_KEY, jsonArray.toString())
                    .apply()
            }

            // 按完成状态、开始时间和截止时间排序
            todos.sortWith(
                compareBy<TodoData> { it.isCompleted }
                    .thenBy { it.startTime ?: Long.MAX_VALUE }
                    .thenBy { it.dueTime ?: Long.MAX_VALUE }
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return todos
    }

    /**
     * 原 `MainActivityAlm.saveTodos()`（`toggleTodo()` 里那段内联的 JSON 写入与它完全一致，
     * 因此合并成这一个方法）。
     *
     * 注意：与原实现一致，这里**没有**写 `createdTime` 字段。
     */
    fun saveTodos(context: Context, todos: List<TodoData>) {
        val jsonArray = JSONArray()
        todos.forEach { todo ->
            val obj = JSONObject().apply {
                put("id", todo.id)
                put("title", todo.title)
                put("description", todo.description)
                put("startTime", todo.startTime ?: 0L)
                put("dueTime", todo.dueTime ?: 0L)
                put("isCompleted", todo.isCompleted)
                put("startRingtoneUri", todo.startRingtoneUri)
                put("dueRingtoneUri", todo.dueRingtoneUri)
                put("completedTime", todo.completedTime ?: 0L)  // 添加完成时间的保存
            }
            jsonArray.put(obj)
        }

        context.getSharedPreferences(AlmContract.TODO_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(AlmContract.TODO_LIST_KEY, jsonArray.toString())
            .apply()
    }

    /** 原 `TodoSettingActivity.saveTodo()` 里读回原状态的那一段。 */
    data class TodoSnapshot(
        val isCompleted: Boolean,
        val startTime: Long?,
        val dueTime: Long?,
        val completedTime: Long?,
        val createdTime: Long,
    )

    /** 原 `TodoSettingActivity.saveTodo()` 中「获取原有待办的完成状态和时间」。 */
    fun readTodoSnapshot(context: Context, todoId: Int): TodoSnapshot {
        var originalIsCompleted = false
        var originalStartTime: Long? = null
        var originalDueTime: Long? = null
        var originalCompletedTime: Long? = null
        var originalCreatedTime: Long = System.currentTimeMillis()  // 默认为当前时间

        if (todoId != -1) {
            val prefs = context.getSharedPreferences(AlmContract.TODO_PREFS, Context.MODE_PRIVATE)
            val todosJson = prefs.getString(AlmContract.TODO_LIST_KEY, "[]")
            val jsonArray = JSONArray(todosJson)

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.getInt("id") == todoId) {
                    originalIsCompleted = obj.getBoolean("isCompleted")
                    originalStartTime = obj.getLong("startTime").takeIf { it != 0L }
                    originalDueTime = obj.getLong("dueTime").takeIf { it != 0L }
                    originalCompletedTime = obj.optLong("completedTime").takeIf { it > 0L }
                    originalCreatedTime = obj.optLong("createdTime", obj.getInt("id").toLong())
                    break
                }
            }
        }

        return TodoSnapshot(
            isCompleted = originalIsCompleted,
            startTime = originalStartTime,
            dueTime = originalDueTime,
            completedTime = originalCompletedTime,
            createdTime = originalCreatedTime,
        )
    }

    /** 原 `TodoSettingActivity.saveTodo()` 里写 SharedPreferences 的那一段。 */
    fun upsertTodo(context: Context, todo: TodoData, isEditing: Boolean) {
        val prefs = context.getSharedPreferences(AlmContract.TODO_PREFS, Context.MODE_PRIVATE)
        val todosJson = prefs.getString(AlmContract.TODO_LIST_KEY, "[]")
        val jsonArray = JSONArray(todosJson)

        // 如果是编辑现有待办，先删除旧的
        if (isEditing) {
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.getInt("id") == todo.id) {
                    jsonArray.remove(i)
                    break
                }
            }
        }

        // 添加新的待办数据
        jsonArray.put(JSONObject().apply {
            put("id", todo.id)
            put("title", todo.title)
            put("description", todo.description)
            put("startTime", todo.startTime ?: 0L)
            put("dueTime", todo.dueTime ?: 0L)
            put("isCompleted", todo.isCompleted)
            put("startRingtoneUri", todo.startRingtoneUri)
            put("dueRingtoneUri", todo.dueRingtoneUri)
            put("completedTime", todo.completedTime ?: 0L)
            put("createdTime", todo.createdTime)
        })

        prefs.edit()
            .putString(AlmContract.TODO_LIST_KEY, jsonArray.toString())
            .apply()
    }

    // ================================================================ 待办：提醒

    /** 原 `TodoSettingActivity.setTodoReminder()`。 */
    fun setTodoReminder(
        context: Context,
        todo: TodoData,
        startAdvanceMinutes: Int,
        dueAdvanceMinutes: Int,
    ) {
        // 如果待办已完成，不设置任何提醒
        if (todo.isCompleted) {
            return
        }

        Log.d("TodoReminder", "开始设置提醒：${todo.title}")

        // 处理开始时间提醒
        todo.startTime?.let { startTime ->
            if (startTime > System.currentTimeMillis() && startAdvanceMinutes != -1) {
                // 设置开始时间的提前提醒
                if (startAdvanceMinutes > 0) {
                    val advanceTime = startTime - (startAdvanceMinutes * 60 * 1000)
                    if (advanceTime > System.currentTimeMillis()) {
                        setReminder(context, todo, advanceTime, true, false)
                    }
                }
                // 设置开始时间提醒
                setReminder(context, todo, startTime, false, false)
            }
        }

        // 处理截止时间提醒
        todo.dueTime?.let { dueTime ->
            if (dueTime > System.currentTimeMillis() && dueAdvanceMinutes != -1) {
                // 设置截止时间的提前提醒
                if (dueAdvanceMinutes > 0) {
                    val advanceTime = dueTime - (dueAdvanceMinutes * 60 * 1000)
                    if (advanceTime > System.currentTimeMillis()) {
                        setReminder(context, todo, advanceTime, true, true)
                    }
                }
                // 设置截止时间提醒
                setReminder(context, todo, dueTime, false, true)
            }
        }
    }

    /** 原 `TodoSettingActivity.setReminder()`。 */
    private fun setReminder(
        context: Context,
        todo: TodoData,
        reminderTime: Long,
        isAdvance: Boolean,
        isDueReminder: Boolean,
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(AlmContract.TODO_REMINDER_ACTION).apply {
            `package` = context.packageName
            putExtra("todo_title", todo.title)
            putExtra("todo_description", todo.description)
            putExtra("is_advance", isAdvance)
            putExtra("is_due_reminder", isDueReminder)
            putExtra("ringtone_uri", if (isDueReminder) todo.dueRingtoneUri else todo.startRingtoneUri)
        }

        // 修改请求码的生成方式，确保每个提醒都有唯一的请求码
        val requestCode = when {
            !isDueReminder -> {
                if (isAdvance) todo.id + 2000000 else todo.id + 3000000  // 区分开始时间的提前提醒和准时提醒
            }

            isAdvance -> todo.id + 1000000  // 截止时间的提前提醒
            else -> todo.id                  // 截止时间的准时提醒
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAlarmClock(  // 使用 setAlarmClock 替代 setExactAndAllowWhileIdle
                    AlarmManager.AlarmClockInfo(reminderTime, pendingIntent),
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val reminderTimeStr = sdf.format(reminderTime)
            val reminderType = when {
                !isDueReminder -> if (isAdvance) "开始时间提前" else "开始时间"
                isAdvance -> "截止时间提前"
                else -> "截止时间"
            }
            Log.d("TodoReminder", "成功设置${reminderType}提醒：${todo.title} - $reminderTimeStr")
            Toast.makeText(context, "已设置${reminderType}提醒", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("TodoReminder", "设置提醒失败", e)
            Toast.makeText(context, "设置提醒失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /** 原 `cancelTodoReminder()`（MainActivityAlm 与 TodoSettingActivity 里两份完全相同）。 */
    fun cancelTodoReminder(context: Context, todoId: Int) {
        // 取消所有类型的提醒
        cancelSingleReminder(context, todoId)           // 取消截止时间提醒
        cancelSingleReminder(context, todoId + 1000000) // 取消截止时间提前提醒
        cancelSingleReminder(context, todoId + 2000000) // 取消开始时间提前提醒
        cancelSingleReminder(context, todoId + 3000000) // 取消开始时间准时提醒
    }

    /** 原 `cancelSingleReminder()`。 */
    fun cancelSingleReminder(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(AlmContract.TODO_REMINDER_ACTION).apply {
            `package` = context.packageName
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    // ================================================================ 历史记录数据

    /** 原 `CalendarActivity.loadData()` 中「收集闹钟 + 待办、合并排序」的那一段。 */
    fun loadHistoryItems(context: Context): List<HistoryAdapter.HistoryItem> {
        // 加载闹钟数据
        val prefs = context.getSharedPreferences(AlmContract.ALARM_PREFS, Context.MODE_PRIVATE)
        val alarmsJson = prefs.getString(AlmContract.ALARM_LIST_KEY, "[]")
        val alarms = try {
            val jsonArray = JSONArray(alarmsJson)
            (0 until jsonArray.length()).map { i ->
                val obj = jsonArray.getJSONObject(i)
                val repeatDaysArray = obj.optJSONArray("repeatDays")
                val repeatDays = BooleanArray(7) { true }
                if (repeatDaysArray != null) {
                    for (j in 0 until repeatDaysArray.length()) {
                        repeatDays[j] = repeatDaysArray.getBoolean(j)
                    }
                }
                AlarmData(
                    id = obj.getInt("id"),
                    timeInMillis = obj.getLong("timeInMillis"),
                    isEnabled = obj.getBoolean("isEnabled"),
                    ringtoneUri = obj.optString("ringtoneUri", null),
                    repeatDays = repeatDays
                )
            }
        } catch (e: Exception) {
            emptyList()
        }

        // 生成未来7天的闹钟时间，从当天凌晨开始
        val startDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 7)  // 只看未来7天
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val alarmTimes = mutableListOf<HistoryAdapter.HistoryItem>()

        // 遍历每个闹钟
        alarms.forEach { alarm ->
            val alarmCalendar = Calendar.getInstance().apply {
                timeInMillis = alarm.timeInMillis
            }

            // 如果是重复闹钟，生成该闹钟在 [startDate, endDate] 每天对应的时间
            if (alarm.repeatDays.any { it }) {
                // 使用 startDate 的副本作为循环起始
                val currentCal = Calendar.getInstance().apply {
                    timeInMillis = startDate.timeInMillis
                }
                while (currentCal.timeInMillis <= endDate.timeInMillis) {
                    val dayOfWeek =
                        (currentCal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 转换为周一为0的索引
                    if (alarm.repeatDays[dayOfWeek]) {
                        // 创建该日期的闹钟时间
                        val alarmTime = Calendar.getInstance().apply {
                            timeInMillis = currentCal.timeInMillis // 基础日期
                            set(Calendar.HOUR_OF_DAY, alarmCalendar.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, alarmCalendar.get(Calendar.MINUTE))
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        // 添加在 [startDate, endDate] 范围内的闹钟
                        if (alarmTime.timeInMillis in startDate.timeInMillis..endDate.timeInMillis) {
                            alarmTimes.add(
                                HistoryAdapter.HistoryItem.AlarmItem(
                                    AlarmData(
                                        id = alarm.id,
                                        timeInMillis = alarmTime.timeInMillis,
                                        isEnabled = alarm.isEnabled,
                                        ringtoneUri = alarm.ringtoneUri,
                                        repeatDays = alarm.repeatDays
                                    )
                                )
                            )
                        }
                    }
                    currentCal.add(Calendar.DAY_OF_YEAR, 1)
                }
            } else if (alarm.timeInMillis in startDate.timeInMillis until endDate.timeInMillis) {
                // 非重复闹钟：添加在 [startDate, endDate) 内的闹钟，无论是否已过
                alarmTimes.add(HistoryAdapter.HistoryItem.AlarmItem(alarm))
            }
        }

        // 加载待办数据
        val todosPrefs = context.getSharedPreferences(AlmContract.TODO_PREFS, Context.MODE_PRIVATE)
        val todosJson = todosPrefs.getString(AlmContract.TODO_LIST_KEY, "[]")
        val todos = mutableListOf<TodoData>()

        try {
            val jsonArray = JSONArray(todosJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val startTime = obj.optLong("startTime")
                val dueTime = obj.optLong("dueTime")
                val isCompleted = obj.getBoolean("isCompleted")
                val completedTime = obj.optLong("completedTime").takeIf { it > 0 }
                val createdTime = obj.optLong("createdTime", obj.getInt("id").toLong())

                // 已完成的待办直接添加
                if (isCompleted) {
                    todos.add(
                        TodoData(
                            id = obj.getInt("id"),
                            title = obj.getString("title"),
                            description = obj.getString("description"),
                            startTime = if (startTime > 0) startTime else null,
                            dueTime = if (dueTime > 0) dueTime else null,
                            isCompleted = isCompleted,
                            startRingtoneUri = obj.optString("startRingtoneUri", null),
                            dueRingtoneUri = obj.optString("dueRingtoneUri", null),
                            completedTime = completedTime,
                            createdTime = createdTime
                        )
                    )
                }
                // 未完成的待办，只要满足以下任一条件就添加：
                // 1. 无时间限制
                // 2. 有开始时间且在未来7天内
                // 3. 有截止时间且未过期
                else if (
                    (startTime == 0L && dueTime == 0L) || // 无时间限制
                    (startTime > 0 && startTime < endDate.timeInMillis) || // 有开始时间且在未来7天内
                    (dueTime > 0 && dueTime > System.currentTimeMillis()) // 有截止时间且未过期
                ) {
                    todos.add(
                        TodoData(
                            id = obj.getInt("id"),
                            title = obj.getString("title"),
                            description = obj.getString("description"),
                            startTime = if (startTime > 0) startTime else null,
                            dueTime = if (dueTime > 0) dueTime else null,
                            isCompleted = isCompleted,
                            startRingtoneUri = obj.optString("startRingtoneUri", null),
                            dueRingtoneUri = obj.optString("dueRingtoneUri", null),
                            completedTime = completedTime,
                            createdTime = createdTime
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 合并所有数据并按时间排序
        val historyItems = mutableListOf<HistoryAdapter.HistoryItem>()
        historyItems.addAll(alarmTimes)
        historyItems.addAll(todos.map { HistoryAdapter.HistoryItem.TodoItem(it) })

        // 修改排序逻辑，考虑完成时间
        historyItems.sortWith(
            compareBy({
                // 第一个排序键：待办为 0，闹铃为 1
                when (it) {
                    is HistoryAdapter.HistoryItem.TodoItem -> 0
                    is HistoryAdapter.HistoryItem.AlarmItem -> 1
                }
            }, {
                // 第二个排序键：各自的时间属性
                when (it) {
                    is HistoryAdapter.HistoryItem.TodoItem -> {
                        val todo = it.todo
                        when {
                            todo.isCompleted -> todo.completedTime ?: todo.dueTime ?: Long.MAX_VALUE
                            todo.startTime != null -> todo.startTime
                            todo.dueTime != null -> todo.dueTime
                            else -> System.currentTimeMillis()
                        }
                    }

                    is HistoryAdapter.HistoryItem.AlarmItem -> it.alarm.timeInMillis
                }
            })
        )

        return historyItems
    }

    /** 原 `CalendarActivity` 的删除历史记录逻辑（闹钟 / 待办两种）。 */
    fun deleteHistoryAlarm(context: Context, alarmId: Int) {
        val prefs = context.getSharedPreferences(AlmContract.ALARM_PREFS, Context.MODE_PRIVATE)
        val alarmsJsonString = prefs.getString(AlmContract.ALARM_LIST_KEY, "[]")
        val jsonArray = JSONArray(alarmsJsonString)
        val newJsonArray = JSONArray()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            if (obj.getInt("id") != alarmId) {
                newJsonArray.put(obj)
            }
        }
        val success = prefs.edit()
            .putString(AlmContract.ALARM_LIST_KEY, newJsonArray.toString())
            .commit()
        Log.d("CalendarScreen", "删除闹钟历史记录, commit: $success, 新数据: $newJsonArray")
    }

    /** 原 `CalendarActivity` 的删除历史记录逻辑（待办）。 */
    fun deleteHistoryTodo(context: Context, todoId: Int) {
        val prefs = context.getSharedPreferences(AlmContract.TODO_PREFS, Context.MODE_PRIVATE)
        val todosJsonString = prefs.getString(AlmContract.TODO_LIST_KEY, "[]")
        val jsonArray = JSONArray(todosJsonString)
        val newJsonArray = JSONArray()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            if (obj.getInt("id") != todoId) {
                newJsonArray.put(obj)
            }
        }
        val success = prefs.edit()
            .putString(AlmContract.TODO_LIST_KEY, newJsonArray.toString())
            .commit()
        Log.d("CalendarScreen", "删除待办历史记录, commit: $success, 新数据: $newJsonArray")
    }
}
