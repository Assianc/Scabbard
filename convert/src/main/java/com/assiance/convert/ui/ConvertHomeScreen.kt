package com.assiance.convert.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.nadeemiqbal.liquidglass.liquidGlass

/**
 * 「晨昏」首页 —— 一个平铺的功能入口列表。
 *
 * 这一层是二级菜单页：页面本身不再分「标题 + 条目」的小组，后续功能直接往
 * [Column] 里追加 [MenuEntryCard] 即可。条目数量已经超过一屏，所以整列必须可滚动 ——
 * 否则屏幕外的功能既看不到也点不到。
 *
 * 页面底色保持透明，让全 App 的背景层透出来，与 memo / alm 各页一致。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConvertHomeScreen(
    onNavigate: (route: String) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("晨昏") },
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CONVERT_TOOLS.forEach { tool ->
                MenuEntryCard(
                    icon = tool.icon,
                    title = tool.title,
                    subtitle = tool.subtitle,
                    onClick = { onNavigate(tool.route) },
                )
            }
        }
    }
}

/**
 * 菜单条目卡。材质与首页入口卡一致（液态玻璃），但多了副标题与右侧箭头 ——
 * 这里表达的是「进入下一层」，不是模块入口。
 */
@Composable
private fun MenuEntryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val shape = CardDefaults.shape
    val glassState = rememberConvertGlassState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // 先把命中区裁进圆角内，否则圆角外的空矩形会吞掉列表手势。
            .clip(shape)
            .liquidGlass(
                state = glassState,
                shape = shape,
                tint = MaterialTheme.colorScheme.surfaceContainer
                    .copy(alpha = GLASS_TINT_ALPHA),
                grain = glassGrainFor(glassState.quality),
            )
            // liquidGlass 只裁渲染，触摸命中仍由整条 modifier 链决定，所以这里再裁一次。
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
