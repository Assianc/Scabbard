package com.assiance.memo.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.assiance.memo.AudioWaveView
import com.assiance.memo.PhotoViewDialog
import com.assiance.memo.R
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** 媒体列表里音频项的路径前缀，与旧 ImageAdapter / AddMemoActivity 保持一致。 */
const val MEMO_AUDIO_PREFIX = "audio:"

private const val TAG = "MemoMedia"

/** 图片项的显示边长，与旧 image_item.xml 的 350dp 一致。 */
private val MEMO_IMAGE_SIZE = 350.dp

private val memoImageRequestOptions = RequestOptions()
    .override(Target.SIZE_ORIGINAL)
    .fitCenter()
    .diskCacheStrategy(DiskCacheStrategy.ALL)
    .placeholder(R.drawable.ic_image_placeholder)
    .error(R.drawable.ic_image_error)

// ---------------------------------------------------------------------------
// 权限 / 选择器 / 录音 的公共辅助（原 AddMemoActivity 与 MemoDetailActivity 中是同一套代码）
// ---------------------------------------------------------------------------

/** 13 及以上用 READ_MEDIA_IMAGES，否则用 READ_EXTERNAL_STORAGE。 */
fun memoStoragePermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

fun memoImagePickerIntent(): Intent = Intent(Intent.ACTION_GET_CONTENT).apply {
    type = "image/*"
    addCategory(Intent.CATEGORY_OPENABLE)
    putExtra(Intent.EXTRA_LOCAL_ONLY, true)
    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*"))
}

private fun launchMemoImagePicker(context: Context, launcher: ActivityResultLauncher<Intent>) {
    try {
        launcher.launch(memoImagePickerIntent())
    } catch (e: Exception) {
        Log.e(TAG, "启动图片选择器失败", e)
        Toast.makeText(context, "无法启动图片选择器", Toast.LENGTH_SHORT).show()
    }
}

/** 选中图片后复制到应用私有目录，与旧 handleSelectedImage 完全一致。 */
private fun copyPickedImage(context: Context, uri: Uri, onAdded: (String) -> Unit) {
    try {
        val fileName = "image_${System.currentTimeMillis()}.jpg"
        val destinationFile = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destinationFile).use { output ->
                input.copyTo(output)
            }
        }
        onAdded(destinationFile.absolutePath)
    } catch (e: Exception) {
        Log.e(TAG, "处理所选图片失败", e)
        Toast.makeText(context, "无法处理所选图片", Toast.LENGTH_SHORT).show()
    }
}

/** 图片加载：与旧 ImageAdapter.loadImage 相同（含权限判断与占位 / 失败图）。 */
private fun loadMemoImage(imageView: ImageView, path: String, context: Context) {
    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        memoStoragePermission()
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasPermission) {
        Log.e(TAG, "没有存储权限，无法加载图片: $path")
        Glide.with(context).load(R.drawable.ic_image_error).into(imageView)
        return
    }

    val file = File(path)
    if (!file.exists()) {
        Log.e(TAG, "图片文件不存在: $path")
        Glide.with(context).load(R.drawable.ic_image_error).into(imageView)
        return
    }

    val thumbnailRequest: RequestBuilder<android.graphics.drawable.Drawable> =
        Glide.with(context).load(R.drawable.ic_image_placeholder)

    Glide.with(context)
        .load(file)
        .apply(memoImageRequestOptions)
        .thumbnail(thumbnailRequest)
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(imageView)
}

/** 图片预览：仍然走原来的 PhotoViewDialog（全屏、缩放 / 拖动 / 点击关闭）。 */
private fun showMemoFullImage(context: Context, path: String) {
    val file = File(path)
    if (!file.exists()) {
        Toast.makeText(context, "图片文件不存在", Toast.LENGTH_SHORT).show()
        return
    }
    PhotoViewDialog(context, path).show()
}

/**
 * 录音：逻辑照搬旧 AddMemoActivity / MemoDetailActivity 中的 MediaRecorder 部分，
 * 只是把状态暴露成 Compose 可观察的形式。
 */
@Stable
class MemoAudioRecorder(private val context: Context) {

    /** 长按 FAB 切换的“录音模式 / 照片模式”。 */
    var isRecordingMode by mutableStateOf(false)
        private set

    var isRecording by mutableStateOf(false)
        private set

    private var recorder: MediaRecorder? = null
    private var audioFilePath: String? = null
    private var recordingStartTime = 0L

    /**
     * 与旧 toggleRecordingMode() 对应：切换模式；切回照片模式时若正在录音则停止。
     * @return 切换后的模式
     */
    fun toggleRecordingMode(): Boolean {
        isRecordingMode = !isRecordingMode
        if (!isRecordingMode && isRecording) {
            stopRecording()
        }
        return isRecordingMode
    }

    /** 录音权限被拒绝时回到照片模式（旧 onRequestPermissionsResult 的行为）。 */
    fun exitRecordingMode() {
        isRecordingMode = false
    }

    /** 与旧 startRecording() 对应：已在录音则先停止。 */
    fun startRecording() {
        if (isRecording) {
            stopRecording()
            return
        }
        try {
            val fileName = "audio_${System.currentTimeMillis()}.mp3"
            val audioFile = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), fileName)
            audioFilePath = audioFile.absolutePath

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFilePath)
                prepare()
                start()
            }

            isRecording = true
            recordingStartTime = System.currentTimeMillis()
            Toast.makeText(context, "开始录音...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "录音失败", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 与旧 stopRecording() 对应：内部完成 Toast 提示。
     * @return 录音文件路径；未在录音或停止失败时为 null
     */
    fun stopRecording(): String? {
        if (!isRecording) return null
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            isRecording = false

            val duration = (System.currentTimeMillis() - recordingStartTime) / 1000
            Toast.makeText(context, "录音完成，时长: ${duration}秒", Toast.LENGTH_SHORT).show()
            audioFilePath
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "停止录音失败", Toast.LENGTH_SHORT).show()
            null
        }
    }

    /**
     * 页面销毁时兜底停止录音（旧实现的 onDestroy -> stopRecording()）。
     * 此时界面已经不存在，因此不再 Toast、也不再往列表里追加。
     */
    fun release() {
        if (!isRecording) return
        try {
            recorder?.stop()
        } catch (_: Exception) {
            // 没有录到数据时 stop() 会抛异常，忽略即可
        }
        try {
            recorder?.release()
        } catch (_: Exception) {
            // ignore
        }
        recorder = null
        isRecording = false
    }
}

/**
 * 相机 / 录音 FAB：短按选图或开始 / 停止录音，长按切换录音模式。
 * 对应旧 AddMemoActivity.setupClickListeners + toggleRecordingMode + 权限处理。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MemoMediaCaptureFab(
    recorder: MemoAudioRecorder,
    onMediaAdded: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentOnMediaAdded by rememberUpdatedState(onMediaAdded)

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedImageUri = result.data?.data
            if (selectedImageUri != null) {
                copyPickedImage(context, selectedImageUri) { currentOnMediaAdded(it) }
            } else {
                Toast.makeText(context, "未能获取所选图片", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchMemoImagePicker(context, imagePickerLauncher)
        } else {
            Toast.makeText(context, "需要存储权限才能选择图片", Toast.LENGTH_SHORT).show()
        }
    }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "需要录音权限才能录制音频", Toast.LENGTH_SHORT).show()
            recorder.exitRecordingMode()
        }
    }

    val icon: ImageVector = when {
        recorder.isRecording -> Icons.Default.Pause
        recorder.isRecordingMode -> Icons.Default.Mic
        else -> Icons.Default.PhotoCamera
    }
    val description = when {
        recorder.isRecording -> "停止录音"
        recorder.isRecordingMode -> "开始录音"
        else -> "添加图片"
    }

    fun onShortClick() {
        if (!recorder.isRecordingMode) {
            val permission = memoStoragePermission()
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                launchMemoImagePicker(context, imagePickerLauncher)
            } else {
                val activity = context.findActivity()
                if (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    Toast.makeText(context, "需要存储权限才能选择图片", Toast.LENGTH_LONG).show()
                }
                storagePermissionLauncher.launch(permission)
            }
        } else if (recorder.isRecording) {
            recorder.stopRecording()?.let { currentOnMediaAdded("$MEMO_AUDIO_PREFIX$it") }
        } else {
            recorder.startRecording()
        }
    }

    fun onLongClick() {
        if (recorder.toggleRecordingMode()) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            Toast.makeText(context, "录音模式已启用", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "照片模式已启用", Toast.LENGTH_SHORT).show()
        }
    }

    // 用 Surface 而不是 M3 的 FloatingActionButton，因为后者不支持长按
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 6.dp,
        modifier = modifier
            .size(56.dp)
            .combinedClickable(onClick = { onShortClick() }, onLongClick = { onLongClick() }),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description)
        }
    }
}

// ---------------------------------------------------------------------------
// 音频播放（原 ImageAdapter 的播放逻辑）
// ---------------------------------------------------------------------------

/** 一次只播一条：对应原 ImageAdapter 的 currentPlayingPosition / currentMediaPlayer。 */
@Stable
class MemoAudioPlayback {

    var playingIndex by mutableIntStateOf(-1)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var positionMs by mutableIntStateOf(0)
        private set

    /** 供波形刷新使用；不是状态，读它不会触发重组。 */
    var player: MediaPlayer? = null
        private set

    /**
     * 旧 ImageAdapter 的播放按钮逻辑：
     * 正在播本条 -> 暂停；否则停掉上一条并从头开始播本条（旧实现暂停后再播也是从头开始）。
     */
    fun onPlayPauseClick(index: Int, audioPath: String) {
        if (playingIndex == index && isPlaying) {
            pause()
        } else {
            start(index, audioPath)
        }
    }

    fun pause() {
        player?.pause()
        isPlaying = false
    }

    private fun start(index: Int, audioPath: String) {
        stop()
        try {
            player = MediaPlayer().apply {
                setDataSource(audioPath)
                prepare()
                start()
                setOnCompletionListener { stop() }
            }
            playingIndex = index
            isPlaying = true
            positionMs = 0
        } catch (e: Exception) {
            e.printStackTrace()
            stop()
        }
    }

    fun seekTo(milliseconds: Int) {
        player?.seekTo(milliseconds)
        positionMs = milliseconds
    }

    fun refreshPosition() {
        player?.let { if (it.isPlaying) positionMs = it.currentPosition }
    }

    fun stop() {
        player?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (_: Exception) {
                // ignore
            }
            it.release()
        }
        player = null
        playingIndex = -1
        isPlaying = false
        positionMs = 0
    }

    fun release() = stop()
}

private fun formatMemoTime(milliseconds: Int): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

// ---------------------------------------------------------------------------
// 媒体列表（原 ImageAdapter：图片项 + 音频项）
// ---------------------------------------------------------------------------

/**
 * 备忘录里的图片 / 音频列表。
 *
 * @param items 路径列表，音频项以 [MEMO_AUDIO_PREFIX] 开头
 * @param editable 是否处于编辑态：编辑态下长按条目可删除（是否二次确认由调用方决定）
 * @param onRequestRemove 长按条目回调，参数为下标
 */
@Composable
fun MemoMediaList(
    items: List<String>,
    editable: Boolean,
    onRequestRemove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playback = remember { MemoAudioPlayback() }
    DisposableEffect(Unit) {
        onDispose { playback.release() }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { index, path ->
            key(index, path) {
                val longPress: (() -> Unit)? = if (editable) {
                    { onRequestRemove(index) }
                } else {
                    null
                }
                if (path.startsWith(MEMO_AUDIO_PREFIX)) {
                    MemoAudioItem(
                        index = index,
                        audioPath = path.removePrefix(MEMO_AUDIO_PREFIX),
                        playback = playback,
                        onLongPress = longPress,
                    )
                } else {
                    MemoImageItem(path = path, onLongPress = longPress)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MemoImageItem(path: String, onLongPress: (() -> Unit)?) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            ImageView(ctx).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
        },
        update = { imageView -> loadMemoImage(imageView, path, context) },
        modifier = Modifier
            .padding(4.dp)
            .size(MEMO_IMAGE_SIZE)
            .combinedClickable(
                onClick = { showMemoFullImage(context, path) },
                onLongClick = onLongPress,
            ),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MemoAudioItem(
    index: Int,
    audioPath: String,
    playback: MemoAudioPlayback,
    onLongPress: (() -> Unit)?,
) {
    val context = LocalContext.current
    val waveView = remember { AudioWaveView(context) }
    var durationMs by remember(audioPath) { mutableIntStateOf(0) }
    var dragging by remember(audioPath) { mutableStateOf(false) }
    var dragValue by remember(audioPath) { mutableFloatStateOf(0f) }

    val isCurrent = playback.playingIndex == index
    val isPlayingThis = isCurrent && playback.isPlaying
    val positionMs = if (isCurrent) playback.positionMs else 0

    // 总时长：与旧 setupAudioPlayer 一致，临时 MediaPlayer prepare 后取 duration
    LaunchedEffect(audioPath) {
        withContext(Dispatchers.IO) {
            try {
                val tempPlayer = MediaPlayer()
                tempPlayer.setDataSource(audioPath)
                tempPlayer.prepare()
                val duration = tempPlayer.duration
                tempPlayer.release()
                durationMs = duration
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "音频加载失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 播放位置刷新 + 波形刷新（旧 startSeekBarUpdate，100ms 一次）
    LaunchedEffect(isPlayingThis, index) {
        while (isPlayingThis && playback.playingIndex == index && playback.isPlaying) {
            playback.refreshPosition()
            waveView.updateWaveform(playback.player)
            delay(100)
        }
    }

    // 旧实现里 setPlaying(true/false) 跟着播放状态走
    LaunchedEffect(isPlayingThis) {
        waveView.setPlaying(isPlayingThis)
    }
    DisposableEffect(Unit) {
        onDispose { waveView.setPlaying(false) }
    }

    val rowModifier = if (onLongPress != null) {
        Modifier.combinedClickable(onClick = {}, onLongClick = onLongPress)
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .then(rowModifier)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { playback.onPlayPauseClick(index, audioPath) }
            ) {
                Icon(
                    imageVector = if (isPlayingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "播放/暂停",
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                AndroidView(
                    factory = { waveView },
                    modifier = Modifier.fillMaxSize(),
                )
                Slider(
                    value = if (dragging) dragValue else positionMs.toFloat(),
                    onValueChange = {
                        dragging = true
                        dragValue = it
                    },
                    onValueChangeFinished = {
                        playback.seekTo(dragValue.toInt())
                        dragging = false
                    },
                    valueRange = 0f..(if (durationMs > 0) durationMs.toFloat() else 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 60.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatMemoTime(if (dragging) dragValue.toInt() else positionMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = formatMemoTime(durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
