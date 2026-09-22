package com.assiance.memo.ui

import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.assiance.memo.MemoDAO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 备忘录详情页（查看 / 编辑），对应旧 MemoDetailActivity。
 *
 * @param memoId 备忘录 id，详情数据由本页按 id 从库里取
 * @param restoredSignal 历史页恢复版本后的回传信号（替代原来的 setResult(RESULT_OK)）
 * @param onRestoredSignalConsumed 信号已消费
 * @param onOpenHistory 打开历史记录页
 */
@Composable
fun MemoDetailScreen(
    memoId: Int,
    restoredSignal: Boolean,
    onRestoredSignalConsumed: () -> Unit,
    onOpenHistory: (Int) -> Unit,
) {
    val context = LocalContext.current
    val dao = remember { MemoDAO(context) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val mediaPaths = remember(memoId) { mutableStateListOf<String>() }

    var title by remember(memoId) { mutableStateOf("") }
    var content by remember(memoId) { mutableStateOf("") }
    var updateTimeText by remember(memoId) { mutableStateOf("") }

    var editMode by remember(memoId) { mutableStateOf(false) }

    var titleFontName by remember(memoId) { mutableStateOf("DEFAULT") }
    var contentFontName by remember(memoId) { mutableStateOf("DEFAULT") }
    var titleBoldState by remember(memoId) { mutableStateOf(false) }
    var titleItalicState by remember(memoId) { mutableStateOf(false) }
    var contentBoldState by remember(memoId) { mutableStateOf(false) }
    var contentItalicState by remember(memoId) { mutableStateOf(false) }
    var titleUnderline by remember(memoId) { mutableStateOf(false) }
    var contentUnderline by remember(memoId) { mutableStateOf(false) }
    var titleFontSize by remember(memoId) { mutableFloatStateOf(MEMO_TITLE_FONT_SIZE_DEFAULT) }
    var contentFontSize by remember(memoId) { mutableFloatStateOf(MEMO_CONTENT_FONT_SIZE_DEFAULT) }

    var titleSizeInput by remember(memoId) { mutableStateOf("") }
    var contentSizeInput by remember(memoId) { mutableStateOf("") }

    var showTitleFontDialog by remember { mutableStateOf(false) }
    var showContentFontDialog by remember { mutableStateOf(false) }
    var showTitleSizeMenu by remember { mutableStateOf(false) }
    var showContentSizeMenu by remember { mutableStateOf(false) }
    var pendingRemoveIndex by remember { mutableStateOf<Int?>(null) }

    val recorder = remember(memoId) { MemoAudioRecorder(context) }
    DisposableEffect(recorder) {
        onDispose { recorder.release() }
    }

    // 进入详情页时申请存储权限（与旧 requestStoragePermission() 一致）
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(memoId) {
        val permission = memoStoragePermission()
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            storagePermissionLauncher.launch(permission)
        }
    }

    // 首次加载：只加载一次，之后的编辑不会被打断
    LaunchedEffect(memoId) {
        val memo = withContext(Dispatchers.IO) { dao.getMemoById(memoId) } ?: return@LaunchedEffect
        title = memo.title
        content = memo.content
        updateTimeText = "${memo.updateTime} 修改"
        mediaPaths.clear()
        mediaPaths.addAll(memo.imagePaths)
        titleFontName = memo.titleFontName
        contentFontName = memo.fontName
        titleBoldState = memo.titleStyle and android.graphics.Typeface.BOLD != 0
        titleItalicState = memo.titleStyle and android.graphics.Typeface.ITALIC != 0
        contentBoldState = memo.contentStyle and android.graphics.Typeface.BOLD != 0
        contentItalicState = memo.contentStyle and android.graphics.Typeface.ITALIC != 0
        titleUnderline = memo.titleUnderline
        contentUnderline = memo.contentUnderline
        titleFontSize = memo.titleFontSize
        contentFontSize = memo.contentFontSize
        titleSizeInput = memo.titleFontSize.toInt().toString()
        contentSizeInput = memo.contentFontSize.toInt().toString()
    }

    // 历史页恢复了某个版本：只刷新标题与内容（与旧 onActivityResult 一致）
    LaunchedEffect(restoredSignal) {
        if (!restoredSignal) return@LaunchedEffect
        val memo = withContext(Dispatchers.IO) { dao.getMemoById(memoId) }
        if (memo != null) {
            title = memo.title
            content = memo.content
        }
        onRestoredSignalConsumed()
    }

    fun saveMemo() {
        val newTitle = title
        val newContent = content
        val titleStyle = memoStyleBits(titleBoldState, titleItalicState)
        val contentStyle = memoStyleBits(contentBoldState, contentItalicState)
        val paths = mediaPaths.toList()
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    dao.updateMemo(
                        id = memoId,
                        title = newTitle,
                        content = newContent,
                        imagePaths = paths,
                        fontName = contentFontName,
                        titleFontName = titleFontName,
                        titleStyle = titleStyle,
                        contentStyle = contentStyle,
                        titleUnderline = titleUnderline,
                        contentUnderline = contentUnderline,
                        titleFontSize = titleFontSize,
                        contentFontSize = contentFontSize,
                    )
                }
                updateTimeText = "刚刚更新"
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun applyFontSize(size: Float, isTitle: Boolean) {
        if (isTitle) {
            titleFontSize = size
            titleSizeInput = size.toInt().toString()
        } else {
            contentFontSize = size
            contentSizeInput = size.toInt().toString()
        }
    }

    /** 与旧 handleFontSizeInput 一致：6.5~42 之外或非法输入都提示并回滚显示值。 */
    fun commitFontSizeInput(isTitle: Boolean) {
        val input = if (isTitle) titleSizeInput else contentSizeInput
        val current = if (isTitle) titleFontSize else contentFontSize
        val parsed = input.toFloatOrNull()
        when {
            parsed == null -> {
                Toast.makeText(context, "请输入有效的字号", Toast.LENGTH_SHORT).show()
                if (isTitle) titleSizeInput = current.toInt().toString() else contentSizeInput = current.toInt().toString()
            }

            parsed < MEMO_FONT_SIZE_MIN -> {
                Toast.makeText(context, "字号不能小于 6.5", Toast.LENGTH_SHORT).show()
                if (isTitle) titleSizeInput = current.toInt().toString() else contentSizeInput = current.toInt().toString()
            }

            parsed > MEMO_FONT_SIZE_MAX -> {
                Toast.makeText(context, "字号不能大于 42", Toast.LENGTH_SHORT).show()
                if (isTitle) titleSizeInput = current.toInt().toString() else contentSizeInput = current.toInt().toString()
            }

            else -> if (isTitle) titleFontSize = parsed else contentFontSize = parsed
        }
    }

    val titleTextStyle: TextStyle = MaterialTheme.typography.titleLarge.copy(
        fontFamily = memoFontFamily(titleFontName),
        fontWeight = if (titleBoldState) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (titleItalicState) FontStyle.Italic else FontStyle.Normal,
        textDecoration = memoTextDecoration(titleUnderline),
        fontSize = titleFontSize.sp,
        color = MaterialTheme.colorScheme.onSurface,
    )

    val contentTextStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(
        fontFamily = memoFontFamily(contentFontName),
        fontWeight = if (contentBoldState) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (contentItalicState) FontStyle.Italic else FontStyle.Normal,
        textDecoration = memoTextDecoration(contentUnderline),
        fontSize = contentFontSize.sp,
        color = MaterialTheme.colorScheme.onSurface,
    )

    // 旧实现用 ValueAnimator 做 200ms 淡入淡出
    val editControlsAlpha by animateFloatAsState(
        targetValue = if (editMode) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "memoDetailEditControls",
    )
    val viewControlsAlpha by animateFloatAsState(
        targetValue = if (editMode) 0f else 1f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "memoDetailViewControls",
    )

    Scaffold(
        floatingActionButton = {
            if (editControlsAlpha > 0.01f) {
                MemoMediaCaptureFab(
                    recorder = recorder,
                    onMediaAdded = { mediaPaths.add(it) },
                    modifier = Modifier.alpha(editControlsAlpha),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 26.dp, vertical = 8.dp),
        ) {
            // 标题工具栏（编辑态显示）
            if (editControlsAlpha > 0.01f) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .alpha(editControlsAlpha),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { showTitleFontDialog = true }) {
                        Icon(Icons.Default.TextFields, contentDescription = "标题字体")
                    }
                    IconButton(onClick = { titleBoldState = !titleBoldState }) {
                        Icon(Icons.Default.FormatBold, contentDescription = "标题加粗")
                    }
                    IconButton(onClick = { titleItalicState = !titleItalicState }) {
                        Icon(Icons.Default.FormatItalic, contentDescription = "标题斜体")
                    }
                    IconButton(onClick = { titleUnderline = !titleUnderline }) {
                        Icon(Icons.Default.FormatUnderlined, contentDescription = "标题下划线")
                    }
                    Box {
                        IconButton(onClick = { showTitleSizeMenu = true }) {
                            Icon(Icons.Default.FormatSize, contentDescription = "标题字体大小")
                        }
                        MemoFontSizeMenu(
                            expanded = showTitleSizeMenu,
                            onDismiss = { showTitleSizeMenu = false },
                            onPick = { applyFontSize(it, true) },
                        )
                    }
                    MemoFontSizeInput(
                        value = titleSizeInput,
                        onValueChange = { titleSizeInput = it },
                        onCommit = { commitFontSizeInput(true) },
                        focusManager = focusManager,
                    )
                }
            }

            // 标题 + 编辑 / 保存
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    enabled = editMode,
                    singleLine = false,
                    textStyle = titleTextStyle,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                )
                if (viewControlsAlpha > 0.01f) {
                    IconButton(
                        onClick = {
                            editMode = true
                        },
                        modifier = Modifier.alpha(viewControlsAlpha),
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                }
                if (editControlsAlpha > 0.01f) {
                    Button(
                        onClick = {
                            saveMemo()
                            editMode = false
                        },
                        modifier = Modifier.alpha(editControlsAlpha),
                    ) {
                        Text("保存")
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 20.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            // 内容工具栏（编辑态显示）
            if (editControlsAlpha > 0.01f) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .alpha(editControlsAlpha),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { showContentFontDialog = true }) {
                        Icon(Icons.Default.FontDownload, contentDescription = "字体")
                    }
                    IconButton(onClick = { contentBoldState = !contentBoldState }) {
                        Icon(Icons.Default.FormatBold, contentDescription = "加粗")
                    }
                    IconButton(onClick = { contentItalicState = !contentItalicState }) {
                        Icon(Icons.Default.FormatItalic, contentDescription = "斜体")
                    }
                    IconButton(onClick = { contentUnderline = !contentUnderline }) {
                        Icon(Icons.Default.FormatUnderlined, contentDescription = "下划线")
                    }
                    Box {
                        IconButton(onClick = { showContentSizeMenu = true }) {
                            Icon(Icons.Default.FormatSize, contentDescription = "字体大小")
                        }
                        MemoFontSizeMenu(
                            expanded = showContentSizeMenu,
                            onDismiss = { showContentSizeMenu = false },
                            onPick = { applyFontSize(it, false) },
                        )
                    }
                    MemoFontSizeInput(
                        value = contentSizeInput,
                        onValueChange = { contentSizeInput = it },
                        onCommit = { commitFontSizeInput(false) },
                        focusManager = focusManager,
                    )
                }
            }

            // 正文 + 图片 / 音频
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                BasicTextField(
                    value = content,
                    onValueChange = { content = it },
                    enabled = editMode,
                    textStyle = contentTextStyle,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp),
                )
                MemoMediaList(
                    items = mediaPaths,
                    editable = editMode,
                    onRequestRemove = { pendingRemoveIndex = it },
                )
            }

            // 更新时间 + 历史记录
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = updateTimeText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { onOpenHistory(memoId) }) {
                    Icon(Icons.Default.History, contentDescription = "历史记录")
                }
            }
        }
    }

    if (showTitleFontDialog) {
        MemoFontDialog(
            dialogTitle = "选择标题字体",
            current = titleFontName,
            onConfirm = { titleFontName = it },
            onDismiss = { showTitleFontDialog = false },
        )
    }

    if (showContentFontDialog) {
        MemoFontDialog(
            dialogTitle = "选择字体",
            current = contentFontName,
            onConfirm = { contentFontName = it },
            onDismiss = { showContentFontDialog = false },
        )
    }

    pendingRemoveIndex?.let { index ->
        AlertDialog(
            onDismissRequest = { pendingRemoveIndex = null },
            title = { Text("删除图片") },
            text = { Text("确定要删除这张图片吗？") },
            confirmButton = {
                TextButton(onClick = {
                    if (index >= 0 && index < mediaPaths.size) {
                        mediaPaths.removeAt(index)
                    }
                    pendingRemoveIndex = null
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoveIndex = null }) {
                    Text("取消")
                }
            },
        )
    }
}

/** 字号输入框：只允许数字、最多两位，失焦或回车时提交校验。 */
@Composable
private fun MemoFontSizeInput(
    value: String,
    onValueChange: (String) -> Unit,
    onCommit: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
) {
    var hasBeenFocused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = { new -> onValueChange(new.filter { it.isDigit() }.take(2)) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                onCommit()
                focusManager.clearFocus()
            },
        ),
        modifier = Modifier
            .padding(start = 4.dp)
            .size(width = 48.dp, height = 40.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
            .onFocusChanged { state ->
                if (state.isFocused) {
                    hasBeenFocused = true
                } else if (hasBeenFocused) {
                    onCommit()
                }
            }
            .padding(horizontal = 4.dp, vertical = 8.dp),
    )
}

/** 字号预设菜单，条目文案与旧 PopupMenu 相同：`名称 (数值pt)`。 */
@Composable
private fun MemoFontSizeMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onPick: (Float) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        MEMO_FONT_SIZES.forEach { (name, size) ->
            DropdownMenuItem(
                text = { Text("$name (${size}pt)") },
                onClick = {
                    onPick(size)
                    onDismiss()
                },
            )
        }
    }
}

/** 字体选择对话框：与旧实现一致，点“确定”且选项发生变化时才生效。 */
@Composable
private fun MemoFontDialog(
    dialogTitle: String,
    current: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column {
                MEMO_FONT_NAMES.forEach { name ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    ) {
                        RadioButton(
                            selected = selected == name,
                            onClick = { selected = name },
                        )
                        Text(
                            text = name,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (selected != current) {
                    onConfirm(selected)
                }
                onDismiss()
            }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}
