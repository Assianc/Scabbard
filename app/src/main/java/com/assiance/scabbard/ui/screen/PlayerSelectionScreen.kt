package com.assiance.scabbard.ui.screen

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * 分类元素选择页。对应改造前的 `PlayerSelectionActivity`。
 *
 * 交互照搬：输入框加元素（空输入提示"请输入玩家名称"）、多选、全选/取消全选、
 * 长按弹确认框删除、完成后校验"至少选一个"。
 *
 * 一处照搬的小脾气：全选按钮的文案只跟着"全选"这个开关走，手动一个个勾满不会变成"取消全选"。
 *
 * 改造前结果通过 Intent extra（`selectedPlayers` / `allPlayers`）回传给 MainActivity，
 * 现在由 [onDone] 直接回调，键名语义不变。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSelectionScreen(
    initialPlayers: List<String>,
    onBack: () -> Unit,
    onDone: (allPlayers: List<String>, selectedPlayers: List<String>) -> Unit,
) {
    val context = LocalContext.current

    var players by remember { mutableStateOf(initialPlayers) }
    var selectedIndices by remember { mutableStateOf(emptySet<Int>()) }
    var input by remember { mutableStateOf("") }
    var allSelected by remember { mutableStateOf(false) }
    var pendingDeleteIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择分类元素") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("元素名称") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = {
                    if (input.isEmpty()) {
                        Toast.makeText(context, "请输入玩家名称", Toast.LENGTH_SHORT).show()
                    } else {
                        players = players + input
                        input = ""
                        allSelected = false
                    }
                }) { Text("添加") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = {
                    allSelected = !allSelected
                    selectedIndices = if (allSelected) players.indices.toSet() else emptySet()
                }) { Text(if (allSelected) "取消全选" else "全选") }

                Button(
                    onClick = {
                        if (selectedIndices.isEmpty()) {
                            Toast.makeText(context, "请选择至少一名玩家", Toast.LENGTH_SHORT).show()
                        } else {
                            onDone(
                                players,
                                selectedIndices.sorted().map { players[it] },
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("完成") }
            }

            HorizontalDivider()

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(players) { index, player ->
                    PlayerRow(
                        name = player,
                        checked = index in selectedIndices,
                        onToggle = {
                            selectedIndices = if (index in selectedIndices) {
                                selectedIndices - index
                            } else {
                                selectedIndices + index
                            }
                        },
                        onLongClick = { pendingDeleteIndex = index },
                    )
                }
            }
        }
    }

    pendingDeleteIndex?.let { index ->
        AlertDialog(
            onDismissRequest = { pendingDeleteIndex = null },
            title = { Text("删除元素") },
            text = { Text("你确定要删除该分类元素吗") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeleteIndex = null
                    players = players.toMutableList().apply { removeAt(index) }
                    // 删除后下标的位移：被删的去掉，比它大的减一
                    selectedIndices = selectedIndices.mapNotNull { selected ->
                        when {
                            selected < index -> selected
                            selected == index -> null
                            else -> selected - 1
                        }
                    }.toSet()
                    if (players.isEmpty()) allSelected = false
                }) { Text("是") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteIndex = null }) { Text("否") }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayerRow(
    name: String,
    checked: Boolean,
    onToggle: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onToggle, onLongClick = onLongClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Text(text = name, style = MaterialTheme.typography.bodyLarge)
    }
}
