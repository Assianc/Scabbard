package com.assiance.memo

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
import android.graphics.Paint

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
    private lateinit var titleFontButton: ImageButton
    private var currentTitleFontName = "DEFAULT"
    private lateinit var boldButton: ImageButton
    private lateinit var italicButton: ImageButton
    private lateinit var underlineButton: ImageButton
    private lateinit var titleBoldButton: ImageButton
    private lateinit var titleItalicButton: ImageButton
    private lateinit var titleUnderlineButton: ImageButton
    private var titleBoldState = false
    private var titleItalicState = false
    private var contentBoldState = false
    private var contentItalicState = false

    companion object {
        private val FONTS = mutableListOf<Typeface>()
        private val FONT_NAMES = arrayOf(
            "默认字体",
            "宋体",
            "仿宋",
            "黑体",
            "楷体",
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

        // 初始化标题字体按钮
        titleFontButton = findViewById(R.id.title_font_button)
        titleFontButton.setOnClickListener {
            showTitleFontSelectionDialog()
        }
        
        // 默认隐藏标题字体按钮
        titleFontButton.visibility = View.GONE

        // 获取并设置标题字体
        currentTitleFontName = intent.getStringExtra("memo_title_font_name") ?: "DEFAULT"
        applyTitleFont(currentTitleFontName)

        // 初始化内容样式按钮
        boldButton = findViewById(R.id.bold_button)
        italicButton = findViewById(R.id.italic_button)
        underlineButton = findViewById(R.id.underline_button)

        // 初始化标题样式按钮
        titleBoldButton = findViewById(R.id.title_bold_button)
        titleItalicButton = findViewById(R.id.title_italic_button)
        titleUnderlineButton = findViewById(R.id.title_underline_button)

        // 恢复样式状态
        val titleStyle = intent.getIntExtra("memo_title_style", Typeface.NORMAL)
        val contentStyle = intent.getIntExtra("memo_content_style", Typeface.NORMAL)
        val titleUnderline = intent.getBooleanExtra("memo_title_underline", false)
        val contentUnderline = intent.getBooleanExtra("memo_content_underline", false)

        // 初始化样式状态
        titleBoldState = (titleStyle and Typeface.BOLD) != 0
        titleItalicState = (titleStyle and Typeface.ITALIC) != 0
        contentBoldState = (contentStyle and Typeface.BOLD) != 0
        contentItalicState = (contentStyle and Typeface.ITALIC) != 0

        // 应用样式
        applyTitleStyles()
        applyContentStyles()
        
        // 应用下划线并强制重绘
        titleEditText.paint.isUnderlineText = titleUnderline
        titleEditText.text = titleEditText.text
        titleEditText.invalidate()

        contentEditText.paint.isUnderlineText = contentUnderline
        contentEditText.text = contentEditText.text
        contentEditText.invalidate()
        
        // 设置按钮点击事件
        titleBoldButton.setOnClickListener { 
            titleBoldState = !titleBoldState
            applyTitleStyles()
        }
        
        titleItalicButton.setOnClickListener { 
            titleItalicState = !titleItalicState
            applyTitleStyles()
        }
        
        titleUnderlineButton.setOnClickListener {
            titleEditText.paint.isUnderlineText = !titleEditText.paint.isUnderlineText
            // 强制重绘整个 EditText
            titleEditText.text = titleEditText.text
            titleEditText.invalidate()
        }

        boldButton.setOnClickListener { 
            contentBoldState = !contentBoldState
            applyContentStyles()
        }
        
        italicButton.setOnClickListener { 
            contentItalicState = !contentItalicState
            applyContentStyles()
        }
        
        underlineButton.setOnClickListener {
            contentEditText.paint.isUnderlineText = !contentEditText.paint.isUnderlineText
            // 强制重绘整个 EditText
            contentEditText.text = contentEditText.text
            contentEditText.invalidate()
        }
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
        titleFontButton.visibility = if (edit) View.VISIBLE else View.GONE
        
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

        // 显示/隐藏内容样式按钮
        boldButton.visibility = if (edit) View.VISIBLE else View.GONE
        italicButton.visibility = if (edit) View.VISIBLE else View.GONE
        underlineButton.visibility = if (edit) View.VISIBLE else View.GONE

        // 显示/隐藏标题样式按钮
        titleBoldButton.visibility = if (edit) View.VISIBLE else View.GONE
        titleItalicButton.visibility = if (edit) View.VISIBLE else View.GONE
        titleUnderlineButton.visibility = if (edit) View.VISIBLE else View.GONE
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
            
            // 计算当前样式
            var titleStyle = Typeface.NORMAL
            if (titleBoldState) titleStyle = titleStyle or Typeface.BOLD
            if (titleItalicState) titleStyle = titleStyle or Typeface.ITALIC
            
            var contentStyle = Typeface.NORMAL
            if (contentBoldState) contentStyle = contentStyle or Typeface.BOLD
            if (contentItalicState) contentStyle = contentStyle or Typeface.ITALIC

            memoDAO.updateMemo(
                id = memoId,
                title = newTitle,
                content = newContent,
                imagePaths = imagePaths,
                fontName = currentFontName,
                titleFontName = currentTitleFontName,
                titleStyle = titleStyle,
                contentStyle = contentStyle,
                titleUnderline = titleEditText.paint.isUnderlineText,
                contentUnderline = contentEditText.paint.isUnderlineText
            )
            updateTimeTextView.text = "刚刚更新"
        }
    }

    private fun initFonts() {
        FONTS.clear()
        FONTS.add(Typeface.DEFAULT) // 默认字体
        
        // 从 res/font 目录加载字体
        try {
            FONTS.add(ResourcesCompat.getFont(this, R.font.simsunch)!!) // 宋体
            FONTS.add(ResourcesCompat.getFont(this, R.font.fasimsunch)!!) //仿宋
            FONTS.add(ResourcesCompat.getFont(this, R.font.simheich)!!) // 黑体
            FONTS.add(ResourcesCompat.getFont(this, R.font.simkaich)!!) // 楷体

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

    private fun showTitleFontSelectionDialog() {
        val currentIndex = FONT_NAMES.indexOf(currentTitleFontName).takeIf { it != -1 } ?: 0
        var selectedIndex = currentIndex

        AlertDialog.Builder(this)
            .setTitle("选择标题字体")
            .setSingleChoiceItems(FONT_NAMES, currentIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton("确定") { _, _ ->
                if (selectedIndex != currentIndex) {
                    currentTitleFontName = FONT_NAMES[selectedIndex]
                    applyTitleFont(currentTitleFontName)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun applyTitleFont(fontName: String) {
        val index = FONT_NAMES.indexOf(fontName)
        if (index >= 0 && index < FONTS.size) {
            titleEditText.typeface = FONTS[index]
        }
    }

    private fun applyTitleStyles() {
        try {
            // 获取当前字体
            val currentTypeface = titleEditText.typeface ?: Typeface.DEFAULT
            
            // 计算新样式
            val newStyle = when {
                titleBoldState && titleItalicState -> Typeface.BOLD_ITALIC
                titleBoldState -> Typeface.BOLD
                titleItalicState -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }

            // 创建新的 Typeface
            val newTypeface = Typeface.create(currentTypeface, newStyle)
            titleEditText.typeface = newTypeface
            titleEditText.invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applyContentStyles() {
        try {
            // 获取当前字体
            val currentTypeface = contentEditText.typeface ?: Typeface.DEFAULT
            
            // 计算新样式
            val newStyle = when {
                contentBoldState && contentItalicState -> Typeface.BOLD_ITALIC
                contentBoldState -> Typeface.BOLD
                contentItalicState -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }

            // 创建新的 Typeface
            val newTypeface = Typeface.create(currentTypeface, newStyle)
            contentEditText.typeface = newTypeface
            contentEditText.invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
