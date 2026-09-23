package com.assiance.scabbard.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 「分道」：把选中的元素随机分成 2 / 3 / 4 组。对应改造前的 `MainActivity`。
 *
 * 分组算法原样照搬（先 shuffle 再切分，余数摊到前面的组），分类数只有 2/3/4 三档，
 * 二分类之外的 C、D 区域在**还没分过组**时按分类数显隐——分过一次之后就不再自动显隐，
 * 这是原来 `isTeamsAllocated` 的语义。
 *
 * 状态用普通 remember 而非 rememberSaveable：原来的 MainActivity 也没做状态保存，
 * 旋屏即回到示例元素，保持同样的行为。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllocateScreen(
    selectedPlayers: List<String>,
    onBack: () -> Unit,
    onPickPlayers: () -> Unit,
) {
    val context = LocalContext.current

    val classificationOptions = listOf("二分类", "三分类", "四分类")
    var classificationIndex by remember { mutableIntStateOf(0) }
    var expanded by remember { mutableStateOf(false) }
    var teamsAllocated by remember { mutableStateOf(false) }
    var teamTexts by remember { mutableStateOf(List(4) { "" }) }
    var visibleTeamCount by remember { mutableIntStateOf(2) }

    val classificationType = classificationIndex + 2

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分 道") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                OutlinedTextField(
                    value = classificationOptions[classificationIndex],
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("分类方式") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    classificationOptions.forEachIndexed { index, label ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                classificationIndex = index
                                expanded = false
                                // 原 updateClassificationViews()：只在还没分过组时调整显隐
                                if (!teamsAllocated) {
                                    visibleTeamCount = index + 2
                                }
                            },
                        )
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = if (selectedPlayers.isEmpty()) {
                    "未选择任何元素"
                } else {
                    "已选择: ${selectedPlayers.joinToString(", ")}"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onPickPlayers,
                    modifier = Modifier.weight(1f),
                ) { Text("选择分类元素") }

                Button(
                    onClick = {
                        if (selectedPlayers.isEmpty()) {
                            Toast.makeText(context, "请选择玩家进行分类", Toast.LENGTH_SHORT).show()
                        } else {
                            val teams = shuffleIntoTeams(selectedPlayers, classificationType)
                            teamTexts = List(4) { index ->
                                teams.getOrNull(index)
                                    ?.let { team ->
                                        "${TEAM_LABELS[index]}: ${team.joinToString(", ")}"
                                    }
                                    .orEmpty()
                            }
                            visibleTeamCount = teams.size
                            teamsAllocated = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("开始分类") }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                teamTexts.take(visibleTeamCount).forEach { text ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    ) {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 先打乱再按份数切分，余数摊给前面的组。算法与原
 * `partitionToThree` / `partitionToFour` 一致。
 */
private fun shuffleIntoTeams(players: List<String>, teamCount: Int): List<List<String>> {
    val shuffled = players.shuffled()
    if (teamCount == 2) {
        val half = shuffled.size / 2
        return listOf(shuffled.take(half), shuffled.drop(half))
    }

    val base = shuffled.size / teamCount
    val teams = mutableListOf<List<String>>()
    var rest = shuffled
    for (index in 0 until teamCount - 1) {
        // 原实现里余数的判定基数逐轮变小：(size - 已分走) % (剩余组数)
        val remainingGroups = teamCount - index
        val take = base + if (rest.size % remainingGroups > 0) 1 else 0
        teams += rest.take(take)
        rest = rest.drop(take)
    }
    teams += rest
    return teams
}

private val TEAM_LABELS = listOf("区域 A", "区域 B", "区域 C", "区域 D")
