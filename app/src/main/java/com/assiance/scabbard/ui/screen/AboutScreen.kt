package com.assiance.scabbard.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.assiance.scabbard.R
import com.assiance.scabbard.data.AppSettings
import com.assiance.scabbard.ui.component.GradientTitle

/**
 * 关于页。对应改造前的 `AboutActivity`。
 *
 * 文案、行序、点击目标逐条照搬，包括那条**看起来像 bug、但确实是有意为之**的映射：
 * 显示名 "bx" 的那一行点开的是 `github.com/cuxt`；而 "sy" 那一行（Provider & Designer）
 * 在原来就**没有绑任何点击事件**，这里也不给它加 —— 没有依据的"顺手修好"就是加戏。
 *
 * 行序也保持原样：Assianc → bx → sy（布局里 id 叫 dev_2 / dev_1 / dev_3，顺序和 id 无关）。
 *
 * 原来靠 ACTION_ICON_CHANGED / ACTION_GRADIENT_CHANGED 广播来刷新图标与渐变，
 * 现在直接订阅 [AppSettings]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    val appIconRes by AppSettings.appIconRes.collectAsStateWithLifecycle()
    val gradientStyle by AppSettings.gradientStyle.collectAsStateWithLifecycle()

    // 原来读不到版本号时兜底 "Version 未知"
    val versionText = remember {
        try {
            val versionName = context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName
            "Version $versionName"
        } catch (_: Exception) {
            "Version 未知"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (appIconRes != 0) {
                Spacer(modifier = Modifier.height(24.dp))
                Image(
                    painter = painterResource(appIconRes),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(80.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            GradientTitle(
                text = stringResource(R.string.app_name),
                style = gradientStyle,
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                ),
                textAlign = TextAlign.Center,
            )

            Text(
                text = versionText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(32.dp))
            SectionLabel("What's this")

            Text(
                text = "Scabbard 剑鞘 —— 愿你我成为生活的高手",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionLabel("Developers")

            Spacer(modifier = Modifier.height(12.dp))

            // 行序照搬布局：Assianc → bx → sy
            DeveloperRow(
                avatarRes = R.mipmap.ic_add,
                name = "Assianc",
                role = "Developer & Designer",
                onClick = { openGitHub(context, "Assianc") },
            )
            Spacer(modifier = Modifier.height(12.dp))
            DeveloperRow(
                avatarRes = R.mipmap.bx,
                name = "bx",
                role = "Developer & Designer",
                // 显示名是 bx，跳转目标却是 cuxt —— 原实现如此，不改
                onClick = { openGitHub(context, "cuxt") },
            )
            Spacer(modifier = Modifier.height(12.dp))
            DeveloperRow(
                avatarRes = R.mipmap.sy,
                name = "sy",
                role = "Provider & Designer",
                // 原来这一行没有点击事件，保持不可点击
                onClick = null,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    )
}

@Composable
private fun DeveloperRow(
    avatarRes: Int,
    name: String,
    role: String,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            // 原布局里三行都是白底，这里换成主题容器色，深色模式下才不刺眼
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            painter = painterResource(avatarRes),
            contentDescription = name,
            modifier = Modifier.size(48.dp),
        )
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = role,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

private fun openGitHub(context: android.content.Context, username: String) {
    try {
        context.startActivity(
            android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                "https://github.com/$username".toUri(),
            )
        )
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "无法打开链接", android.widget.Toast.LENGTH_SHORT).show()
    }
}
