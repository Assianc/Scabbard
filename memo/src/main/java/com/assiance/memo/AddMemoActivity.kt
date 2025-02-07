package com.assiance.memo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.assiance.memo.databinding.ActivityAddMemoBinding
import java.io.File
import java.io.FileOutputStream

class AddMemoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddMemoBinding
    private lateinit var memoDAO: MemoDAO
    private val imagePaths = mutableListOf<String>()
    private lateinit var imageAdapter: ImageAdapter
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private var isRecordingMode = false
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var audioFilePath: String? = null
    private var recordingStartTime: Long = 0

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val RECORD_AUDIO_PERMISSION_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        memoDAO = MemoDAO(this)
        setupImagePicker()
        setupRecyclerView()
        setupClickListeners()
    }

    private fun setupImagePicker() {
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val selectedImageUri = result.data?.data
                if (selectedImageUri != null) {
                    handleSelectedImage(selectedImageUri)
                } else {
                    Toast.makeText(this, "未能获取所选图片", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleSelectedImage(uri: Uri) {
        try {
            // 将图片复制到应用私有目录
            val fileName = "image_${System.currentTimeMillis()}.jpg"
            val destinationFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName)

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }

            // 使用应用私有目录中的文件路径
            val path = destinationFile.absolutePath
            imagePaths.add(path)
            imageAdapter.notifyItemInserted(imagePaths.size - 1)

        } catch (e: Exception) {
            Log.e("AddMemoActivity", "处理所选图片失败", e)
            Toast.makeText(this, "无法处理所选图片", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        binding.imagesRecyclerView.layoutManager = LinearLayoutManager(this)
        imageAdapter = ImageAdapter(
            context = this,
            imagePaths = imagePaths,
            isEditMode = true
        ) { position ->
            imagePaths.removeAt(position)
            imageAdapter.notifyItemRemoved(position)
        }
        binding.imagesRecyclerView.adapter = imageAdapter
    }

    private fun setupClickListeners() {
        binding.addImageButton.setOnClickListener {
            if (!isRecordingMode) {
                checkPermissionAndOpenPicker()
            } else {
                startRecording()
            }
        }

        binding.addImageButton.setOnLongClickListener {
            toggleRecordingMode()
            true
        }

        binding.buttonSave.setOnClickListener {
            saveMemo()
        }
    }

    private fun toggleRecordingMode() {
        isRecordingMode = !isRecordingMode
        if (isRecordingMode) {
            // 切换到录音模式: 设置录音图标，并检查录音权限
            binding.addImageButton.setImageResource(android.R.drawable.ic_btn_speak_now)
            checkRecordPermission()
            Toast.makeText(this, "录音模式已启用", Toast.LENGTH_SHORT).show()
        } else {
            // 切换回照片模式: 设置相机图标
            binding.addImageButton.setImageResource(android.R.drawable.ic_menu_camera)
            if (isRecording) {
                // 如果当前正在录音，则停止录音
                stopRecording()
            }
            Toast.makeText(this, "照片模式已启用", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkRecordPermission() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_CODE
            )
        }
    }

    private fun startRecording() {
        if (isRecording) {
            stopRecording()
            return
        }

        try {
            val fileName = "audio_${System.currentTimeMillis()}.mp3"
            val audioFile = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), fileName)
            audioFilePath = audioFile.absolutePath

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFilePath)
                prepare()
                start()
            }

            isRecording = true
            recordingStartTime = System.currentTimeMillis()
            binding.addImageButton.setImageResource(android.R.drawable.ic_media_pause)
            
            // 显示录音提示
            Toast.makeText(this, "开始录音...", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "录音失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        if (!isRecording) return

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            
            // 计算录音时长
            val duration = (System.currentTimeMillis() - recordingStartTime) / 1000
            Toast.makeText(this, "录音完成，时长: ${duration}秒", Toast.LENGTH_SHORT).show()
            
            // 将录音文件路径添加到列表
            audioFilePath?.let { path ->
                imagePaths.add("audio:$path")
                imageAdapter.notifyItemInserted(imagePaths.size - 1)
            }
            
            binding.addImageButton.setImageResource(android.R.drawable.ic_btn_speak_now)
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "停止录音失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissionAndOpenPicker() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED -> {
                openImagePicker()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(this, "需要存储权限才能选择图片", Toast.LENGTH_LONG).show()
                requestPermissions(arrayOf(permission), PERMISSION_REQUEST_CODE)
            }
            else -> {
                requestPermissions(arrayOf(permission), PERMISSION_REQUEST_CODE)
            }
        }
    }

    private fun openImagePicker() {
        // 使用 ACTION_GET_CONTENT 而不是 ACTION_PICK
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            // 添加多个可能的数据源
            putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            // 允许选择所有图片类型
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*"))
        }

        try {
            pickImageLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e("AddMemoActivity", "启动图片选择器失败", e)
            Toast.makeText(this, "无法启动图片选择器", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openImagePicker()
                } else {
                    Toast.makeText(this, "需要存储权限才能选择图片", Toast.LENGTH_SHORT).show()
                }
            }
            RECORD_AUDIO_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "需要录音权限才能录制音频", Toast.LENGTH_SHORT).show()
                    isRecordingMode = false
                    binding.addImageButton.setImageResource(android.R.drawable.ic_menu_camera)
                }
            }
        }
    }

    private fun saveMemo() {
        val title = binding.editTextTitle.text.toString()
        val content = binding.editTextContent.text.toString()

        memoDAO.insertMemo(title, content, imagePaths)
        finish()
    }

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }
}