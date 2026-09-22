package com.assiance.memo.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.assiance.memo.Memo
import com.assiance.memo.MemoDAO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 与旧 MemoAdapter.MAX_CONTENT_LENGTH 一致。 */
private const val MEMO_LIST_CONTENT_LENGTH = 50

/**
 * 备忘录列表页，对应旧 MainActivityMemo。
 *
 * @param onOpenDetail 打开指定备忘录
 * @param onAddMemo 新建备忘录
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoListScreen(
    onOpenDetail: (Int) -> Unit,
    onAddMemo: () -> Unit,
) {
    val context = LocalContext.current
    val dao = remember { MemoDAO(context) }
    val scope = rememberCoroutineScope()

    var memos by remember { mutableStateOf<List<Memo>>(emptyList()) }
    var multiSelectMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Int>() }

    fun loadMemos() {
        scope.launch {
            try {
                val newMemos = withContext(Dispatchers.IO) { dao.getAllMemos() }
                memos = newMemos
            } catch (e: Exception) {
                e.printStackTrace()
                if (e !is IllegalStateException && memos.isEmpty()) {
                    Toast.makeText(context, "数据加载异常，请重试", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 对齐旧 onResume：每次回到本页都重新拉取（首次进入也会拉一次）
    MemoResumeEffect { loadMemos() }

    // 多选模式下先退出多选，与旧 OnBackPressedCallback 行为一致
    BackHandler(enabled = multiSelectMode) {
        multiSelectMode = false
        selectedIds.clear()
    }

    fun deleteSelectedMemos() {
        val ids = selectedIds.toList()
        scope.launch {
            try {
                withContext(Dispatchers.IO) { ids.forEach { dao.deleteMemo(it) } }
                memos = memos.filterNot { it.id in ids }
                selectedIds.clear()
                multiSelectMode = false
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("备忘录") })
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (multiSelectMode) {
                    FloatingActionButton(onClick = { deleteSelectedMemos() }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除选中的备忘录")
                    }
                }
                FloatingActionButton(onClick = onAddMemo) {
                    Icon(Icons.Default.Add, contentDescription = "添加备忘录")
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(memos, key = { it.id }) { memo ->
                MemoListItem(
                    memo = memo,
                    multiSelectMode = multiSelectMode,
                    selected = memo.id in selectedIds,
                    onToggleSelect = {
                        if (memo.id in selectedIds) selectedIds.remove(memo.id) else selectedIds.add(memo.id)
                    },
                    onClick = { onOpenDetail(memo.id) },
                    onLongClick = {
                        if (!multiSelectMode) multiSelectMode = true
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MemoListItem(
    memo: Memo,
    multiSelectMode: Boolean,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    // 旧 MemoAdapter：内容超过 50 字截断，点内容可展开 / 收起
    var expanded by remember(memo.id) { mutableStateOf(false) }

    val hasImages = memo.imagePaths.any { !it.startsWith(MEMO_AUDIO_PREFIX) }
    val hasAudio = memo.imagePaths.any { it.startsWith(MEMO_AUDIO_PREFIX) }
    val indicatorText = when {
        hasImages && hasAudio -> "（图文音频）"
        hasImages -> "（图文）"
        hasAudio -> "（音频）"
        else -> null
    }
    val displayContent = if (memo.content.length > MEMO_LIST_CONTENT_LENGTH) {
        memo.content.take(MEMO_LIST_CONTENT_LENGTH) + "..."
    } else {
        memo.content
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Column(modifier = Modifier.padding(26.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = memo.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = memoFontFamily(memo.titleFontName),
                        fontWeight = memoFontWeight(memo.titleStyle),
                        fontStyle = memoFontStyle(memo.titleStyle),
                        textDecoration = memoTextDecoration(memo.titleUnderline),
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (multiSelectMode) {
                    Checkbox(checked = selected, onCheckedChange = { onToggleSelect() })
                }
            }

            if (indicatorText != null) {
                Text(
                    text = indicatorText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            Text(
                text = if (expanded) memo.content else displayContent,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = memoFontFamily(memo.fontName),
                    fontWeight = memoFontWeight(memo.contentStyle),
                    fontStyle = memoFontStyle(memo.contentStyle),
                    textDecoration = memoTextDecoration(memo.contentUnderline),
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable { expanded = !expanded },
            )

            Text(
                text = "上次更新: ${memo.updateTime}",
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp),
            )
        }
    }
}

/** 与旧 Activity 的 onResume 对齐：注册后立刻执行一次（若已处于 RESUMED），之后每次回到 RESUMED 再执行。 */
@Composable
private fun MemoResumeEffect(onResume: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnResume by rememberUpdatedState(onResume)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) currentOnResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            currentOnResume()
        }
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
