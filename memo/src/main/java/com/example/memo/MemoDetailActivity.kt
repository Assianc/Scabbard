package com.example.memo

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.core.content.res.ResourcesCompat

class MemoDetailActivity : AppCompatActivity() {

    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var updateTimeTextView: TextView
    private lateinit var editButton: ImageButton
    private lateinit var saveButton: Button
    private lateinit var memoDAO: MemoDAO
    private var memoId: Int = -1
    private var isEditMode = false
    private lateinit var addImageButton: FloatingActionButton
    private val PICK_IMAGE_REQUEST = 1
    private val PERMISSION_REQUEST_CODE = 100
    private lateinit var imagesRecyclerView: RecyclerView
    private lateinit var imageAdapter: ImageAdapter
    private val imagePaths = mutableListOf<String>()
    private lateinit var fontButton: ImageButton
    private var currentFontName = "DEFAULT"

    companion object {
        private val FONTS = mutableListOf<Typeface>()
        private val FONT_NAMES = arrayOf(
            "默认字体",
            "宋体",
            "宋体（港繁）",
            "宋体（繁）",
            "仿宋",
            "仿宋（港繁）",
            "仿宋（繁）",
            "黑体",
            "黑体（港繁）",
            "黑体（繁）",
            "楷体",
            "楷体（港繁）",
            "楷体（繁）",
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memo_detail)

        // 初始化视图
        titleEditText = findViewById(R.id.memo_detail_title)
        contentEditText = findViewById(R.id.memo_detail_content)
        updateTimeTextView = findViewById(R.id.memo_detail_update_time)
        editButton = findViewById(R.id.edit_button)
        saveButton = findViewById(R.id.save_button)
        addImageButton = findViewById(R.id.add_image_button)
        memoDAO = MemoDAO(this)

        // 获取传递过来的数据
        memoId = intent.getIntExtra("memo_id", -1)
        val title = intent.getStringExtra("memo_title") ?: ""
        val content = intent.getStringExtra("memo_content") ?: ""
        val updateTime = intent.getStringExtra("memo_update_time") ?: ""

        // 设置数据
        titleEditText.setText(title)
        contentEditText.setText(content)
        updateTimeTextView.text = updateTime

        // 设置编辑按钮点击事件
        editButton.setOnClickListener {
            toggleEditMode(true)
        }

        // 设置保存按钮点击事件
        saveButton.setOnClickListener {
            saveMemo()
            toggleEditMode(false)
        }
        
        // 默认隐藏添加图片按钮
        addImageButton.visibility = View.GONE

        // 添加图片按钮点击事件
        addImageButton.setOnClickListener {
            openImagePicker()
        }

        // 初始化图片列表
        imagesRecyclerView = findViewById(R.id.images_recycler_view)
        imagesRecyclerView.layoutManager = LinearLayoutManager(this)
        imageAdapter = ImageAdapter(
            this, 
            imagePaths,
            isEditMode = false
        ) { position ->
            // 长按删除图片
            imagePaths.removeAt(position)
            imageAdapter.notifyItemRemoved(position)
        }
        imagesRecyclerView.adapter = imageAdapter

        // 获取并显示现有图片
        intent.getStringArrayListExtra("memo_image_paths")?.let { paths ->
            imagePaths.addAll(paths)
            imageAdapter.notifyDataSetChanged()
        }

        // 初始化字体按钮
        fontButton = findViewById(R.id.font_button)
        fontButton.setOnClickListener {
            showFontSelectionDialog()
        }
        
        // 默认隐藏字体按钮
        fontButton.visibility = View.GONE

        // 初始化字体
        initFonts()

        // 获取并设置字体
        currentFontName = intent.getStringExtra("memo_font_name") ?: "DEFAULT"
        applyFont(currentFontName)
    }

    private fun toggleEditMode(edit: Boolean) {
        isEditMode = edit
        titleEditText.isEnabled = edit
        contentEditText.isEnabled = edit
        
        val textColor = if (edit) {
            getColor(R.color.edit_mode_text)
        } else {
            getColor(R.color.view_mode_text)
        }
        
        titleEditText.setTextColor(textColor)
        contentEditText.setTextColor(textColor)
        
        editButton.visibility = if (edit) View.GONE else View.VISIBLE
        saveButton.visibility = if (edit) View.VISIBLE else View.GONE
        addImageButton.visibility = if (edit) View.VISIBLE else View.GONE
        fontButton.visibility = if (edit) View.VISIBLE else View.GONE
        
        // 更新 ImageAdapter 的编辑模式
        imageAdapter = ImageAdapter(
            this, 
            imagePaths,
            isEditMode = edit
        ) { position ->
            // 长按时显示确认对话框
            showDeleteConfirmationDialog(position)
        }
        imagesRecyclerView.adapter = imageAdapter
    }

    private fun showDeleteConfirmationDialog(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("删除图片")
            .setMessage("确定要删除这张图片吗？")
            .setPositiveButton("确定") { dialog, _ ->
                // 用户点击确定后删除图片
                imagePaths.removeAt(position)
                imageAdapter.notifyItemRemoved(position)
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun openImagePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 及以上版本
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES), PERMISSION_REQUEST_CODE)
                return
            }
        } else {
            // Android 13 以下版本
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
                return
            }
        }
        
        // 如果已经有权限，打开图片选择器
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
        if (memoId != -1) {
            val newTitle = titleEditText.text.toString()
            val newContent = contentEditText.text.toString()
            memoDAO.updateMemo(memoId, newTitle, newContent, imagePaths, currentFontName)
            updateTimeTextView.text = "刚刚更新"
        }
    }

    private fun initFonts() {
        FONTS.clear()
        FONTS.add(Typeface.DEFAULT) // 默认字体
        
        // 从 res/font 目录加载字体
        try {
            FONTS.add(ResourcesCompat.getFont(this, R.font.simsunch)!!) // 宋体
            FONTS.add(ResourcesCompat.getFont(this, R.font.simsunhk)!!) //宋体 港繁
            FONTS.add(ResourcesCompat.getFont(this, R.font.simsunpro)!!) // 宋体 繁
            FONTS.add(ResourcesCompat.getFont(this, R.font.fasimsunch)!!) //仿宋
            FONTS.add(ResourcesCompat.getFont(this, R.font.fasimsunhk)!!)
            FONTS.add(ResourcesCompat.getFont(this, R.font.fasimsunpro)!!)
            FONTS.add(ResourcesCompat.getFont(this, R.font.simheich)!!) // 黑体
            FONTS.add(ResourcesCompat.getFont(this, R.font.simheihk)!!)
            FONTS.add(ResourcesCompat.getFont(this, R.font.simheipro)!!)
            FONTS.add(ResourcesCompat.getFont(this, R.font.simkaich)!!) // 楷体
            FONTS.add(ResourcesCompat.getFont(this, R.font.simkaihk)!!)
            FONTS.add(ResourcesCompat.getFont(this, R.font.simkaipro)!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applyFont(fontName: String) {
        val index = FONT_NAMES.indexOf(fontName)
        if (index >= 0 && index < FONTS.size) {
            contentEditText.typeface = FONTS[index]
        }
    }

    private fun showFontSelectionDialog() {
        val currentIndex = FONT_NAMES.indexOf(currentFontName).takeIf { it != -1 } ?: 0
        var selectedIndex = currentIndex

        AlertDialog.Builder(this)
            .setTitle("选择字体")
            .setSingleChoiceItems(FONT_NAMES, currentIndex) { _, which ->
                // 只记录选择的位置，不立即应用
                selectedIndex = which
            }
            .setPositiveButton("确定") { _, _ ->
                // 用户点击确定后才应用字体
                if (selectedIndex != currentIndex) {
                    currentFontName = FONT_NAMES[selectedIndex]
                    applyFont(currentFontName)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
