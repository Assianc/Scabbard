package com.assiance.memo.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.assiance.memo.MemoDAO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 新建备忘录，对应旧 AddMemoActivity。
 *
 * @param onDone 保存完成或返回时关闭本页
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoAddScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { MemoDAO(context) }
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val mediaPaths = remember { mutableStateListOf<String>() }

    val recorder = remember { MemoAudioRecorder(context) }
    DisposableEffect(recorder) {
        onDispose { recorder.release() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新建备忘录") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        bottomBar = {
            Surface {
                Button(
                    onClick = {
                        val newTitle = title
                        val newContent = content
                        val paths = mediaPaths.toList()
                        scope.launch {
                            withContext(Dispatchers.IO) { dao.insertMemo(newTitle, newContent, paths) }
                            onDone()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text("保存")
                }
            }
        },
        floatingActionButton = {
            MemoMediaCaptureFab(
                recorder = recorder,
                onMediaAdded = { mediaPaths.add(it) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("标题") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("内容") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            // 与旧 AddMemoActivity 一致：长按直接删除，没有二次确认
            MemoMediaList(
                items = mediaPaths,
                editable = true,
                onRequestRemove = { index ->
                    if (index >= 0 && index < mediaPaths.size) mediaPaths.removeAt(index)
                },
                modifier = Modifier.padding(bottom = 96.dp),
            )
        }
    }
}
