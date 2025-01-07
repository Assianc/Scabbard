package com.assiance.memo

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.assiance.memo.databinding.ActivityAddMemoBinding

class AddMemoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddMemoBinding
    private lateinit var memoDAO: MemoDAO
    private val imagePaths = mutableListOf<String>()
    private lateinit var imageAdapter: ImageAdapter
    
    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddMemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        memoDAO = MemoDAO(this)

        // 设置图片列表
        binding.imagesRecyclerView.layoutManager = LinearLayoutManager(this)
        imageAdapter = ImageAdapter(
            this,
            imagePaths,
            isEditMode = true
        ) { position ->
            // 长按删除图片
            imagePaths.removeAt(position)
            imageAdapter.notifyItemRemoved(position)
        }
        binding.imagesRecyclerView.adapter = imageAdapter

        // 添加图片按钮点击事件
        binding.addImageButton.setOnClickListener {
            openImagePicker()
        }

        // 保存按钮点击事件
        binding.buttonSave.setOnClickListener {
            saveMemo()
        }
    }

    private fun openImagePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES), PERMISSION_REQUEST_CODE)
                return
            }
        } else {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
                return
            }
        }
        
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            val selectedImageUri = data.data
            selectedImageUri?.let { uri ->
                val path = getRealPathFromURI(uri)
                imagePaths.add(path)
                imageAdapter.notifyItemInserted(imagePaths.size - 1)
            }
        }
    }

    private fun getRealPathFromURI(uri: Uri): String {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, projection, null, null, null)
        val columnIndex = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor?.moveToFirst()
        val path = cursor?.getString(columnIndex ?: 0) ?: ""
        cursor?.close()
        return path
    }

    private fun saveMemo() {
        val title = binding.editTextTitle.text.toString()
        val content = binding.editTextContent.text.toString()
        
        if (title.isNotEmpty() || content.isNotEmpty()) {
            memoDAO.insertMemo(
                title = title,
                content = content,
                imagePaths = imagePaths,
                fontName = "DEFAULT",
                titleFontName = "DEFAULT",
                titleStyle = Typeface.NORMAL,
                contentStyle = Typeface.NORMAL,
                titleUnderline = false,
                contentUnderline = false,
                titleFontSize = 32f,
                contentFontSize = 16f
            )
            finish()
        }
    }
}
