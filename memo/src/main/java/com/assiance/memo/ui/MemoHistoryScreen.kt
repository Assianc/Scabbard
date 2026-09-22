package com.assiance.memo.ui

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.assiance.memo.MemoDAO
import com.assiance.memo.MemoHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 历史记录页，对应旧 MemoHistoryActivity。
 *
 * @param memoId 备忘录 id
 * @param onRestored 恢复版本成功后回调（由导航层用 previousBackStackEntry.savedStateHandle 回传给详情页）
 * @param onClose 关闭本页
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoHistoryScreen(
    memoId: Int,
    onRestored: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val dao = remember { MemoDAO(context) }
    val scope = rememberCoroutineScope()

    var historyList by remember { mutableStateOf<List<MemoHistory>>(emptyList()) }
    var pendingDeleteIndex by remember { mutableStateOf<Int?>(null) }
    var pendingRestore by remember { mutableStateOf<MemoHistory?>(null) }

    LaunchedEffect(memoId) {
        if (memoId == -1) {
            Toast.makeText(context, "无法加载历史记录", Toast.LENGTH_SHORT).show()
            onClose()
            return@LaunchedEffect
        }
        historyList = withContext(Dispatchers.IO) { dao.getMemoHistory(memoId) }
    }

    fun restoreHistory(history: MemoHistory) {
        scope.launch {
            withContext(Dispatchers.IO) {
                dao.updateMemo(
                    id = memoId,
                    title = history.oldTitle,
                    content = history.oldContent,
                    imagePaths = history.oldImagePaths,
                )
            }
            Toast.makeText(context, "已恢复到选中的版本", Toast.LENGTH_SHORT).show()
            onRestored()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("历史记录") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(historyList, key = { it.id }) { history ->
                MemoHistoryItem(
                    history = history,
                    onRestoreClick = { pendingRestore = history },
                    onLongClick = { pendingDeleteIndex = historyList.indexOf(history) },
                )
            }
        }
    }

    pendingDeleteIndex?.let { index ->
        val history = historyList.getOrNull(index)
        AlertDialog(
            onDismissRequest = { pendingDeleteIndex = null },
            title = { Text("删除历史记录") },
            text = { Text("确定要删除这条历史记录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    if (history != null) {
                        scope.launch {
                            withContext(Dispatchers.IO) { dao.deleteMemoHistory(history.id) }
                            historyList = historyList.filterNot { it.id == history.id }
                        }
                    }
                    pendingDeleteIndex = null
                }) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteIndex = null }) {
                    Text("取消")
                }
            },
        )
    }

    pendingRestore?.let { history ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("恢复确认") },
            text = { Text("确定要恢复到这个版本吗？当前版本将被保存到历史记录中。") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestore = null
                    restoreHistory(history)
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) {
                    Text("取消")
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MemoHistoryItem(
    history: MemoHistory,
    onRestoreClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val hasImages = history.oldImagePaths.any { !it.startsWith(MEMO_AUDIO_PREFIX) }
    val hasAudio = history.oldImagePaths.any { it.startsWith(MEMO_AUDIO_PREFIX) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = history.modifyTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = history.oldTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = history.oldContent,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (hasImages || hasAudio) {
                Text(
                    text = "此处图文和音频不支持历史记录查阅",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Button(
                onClick = onRestoreClick,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp),
            ) {
                Text("恢复此版本")
            }
        }
    }
}
