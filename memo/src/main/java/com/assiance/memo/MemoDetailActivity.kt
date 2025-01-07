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
import android.view.inputmethod.EditorInfo
import android.widget.PopupMenu
import android.view.Menu
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import android.view.ViewGroup

class MemoDetailActivity : AppCompatActivity() {

    private lateinit var titleEditText: EditText
    private lateinit var updateTimeTextView: TextView
    private lateinit var editButton: ImageButton
    private lateinit var saveButton: Button
    private lateinit var memoDAO: MemoDAO
    private var memoId: Int = -1
    private var isEditMode = false
    private lateinit var addImageButton: FloatingActionButton
    private val PICK_IMAGE_REQUEST = 1
    private val PERMISSION_REQUEST_CODE = 100
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
    private lateinit var fontSizeButton: ImageButton
    private lateinit var titleFontSizeButton: ImageButton
    private var currentTitleFontSize = 32f // 默认标题字体大小
    private var currentContentFontSize = 16f // 默认内容字体大小
    private lateinit var titleFontSizeInput: EditText
    private lateinit var contentFontSizeInput: EditText
    private lateinit var contentRecyclerView: RecyclerView
    private lateinit var mixedContentAdapter: MixedContentAdapter
    private val contentsList = mutableListOf<MemoContent>()
    private var contentUnderlineState = false

    companion object {
        // 删除 FONTS 和 FONT_NAMES
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 在初始化其他内容之前，先确保数据库是最新的
        val dbHelper = MemoDatabaseHelper(this)
        dbHelper.writableDatabase.close()
        
        setContentView(R.layout.activity_memo_detail)

        // 初始化所有视图
        initializeViews()
        
        // 初始化混合内容列表
        contentRecyclerView = findViewById(R.id.content_recycler_view)
        contentRecyclerView.layoutManager = LinearLayoutManager(this)
        
        // 获取传递过来的数据
        memoId = intent.getIntExtra("memo_id", -1)
        val title = intent.getStringExtra("memo_title") ?: ""
        val content = intent.getStringExtra("memo_content") ?: ""
        val updateTime = intent.getStringExtra("memo_update_time") ?: ""

        // 设置数据
        titleEditText.setText(title)
        
        // 将内容按换行符分割，每段创建一个文本内容
        if (content.isNotEmpty()) {
            content.split("\n").forEach { paragraph ->
                if (paragraph.isNotEmpty()) {
                    contentsList.add(MemoContent.TextContent(paragraph))
                }
            }
        }
        
        // 如果内容为空，添加一个空的文本内容
        if (contentsList.isEmpty()) {
            contentsList.add(MemoContent.TextContent(""))
        }

        updateTimeTextView.text = updateTime

        // 添加现有图片，每张图片后面添加一个空的文本内容
        intent.getStringArrayListExtra("memo_image_paths")?.forEach { path ->
            contentsList.add(MemoContent.ImageContent(path))
            contentsList.add(MemoContent.TextContent(
                text = "",
                fontName = currentFontName,
                style = if (contentBoldState && contentItalicState) Typeface.BOLD_ITALIC 
                        else if (contentBoldState) Typeface.BOLD 
                        else if (contentItalicState) Typeface.ITALIC 
                        else Typeface.NORMAL,
                fontSize = currentContentFontSize
            ))
        }

        // 初始化适配器
        mixedContentAdapter = MixedContentAdapter(
            this,
            contentsList,
            isEditMode,
            currentFontName,  // 添加字体名称
            if (contentBoldState && contentItalicState) Typeface.BOLD_ITALIC 
            else if (contentBoldState) Typeface.BOLD 
            else if (contentItalicState) Typeface.ITALIC 
            else Typeface.NORMAL,  // 添加样式
            currentContentFontSize,  // 添加字体大小
            onImageClick = { path ->
                showFullImage(path)
            },
            onImageLongClick = { position ->
                if (isEditMode) {
                    showDeleteConfirmationDialog(position)
                }
            },
            onTextChange = { position, newText ->
                (contentsList[position] as? MemoContent.TextContent)?.let {
                    contentsList[position] = MemoContent.TextContent(newText)
                }
            }
        )
        
        contentRecyclerView.adapter = mixedContentAdapter

        // 设置事件监听器
        setupEventListeners()
        
        // 初始化字体
        FontUtils.initFonts(this)
        
        // 应用保存的设置
        applyStoredSettings()
    }

    private fun initializeViews() {
        // 初始化基本视图
        titleEditText = findViewById(R.id.memo_detail_title)
        updateTimeTextView = findViewById(R.id.memo_detail_update_time)
        editButton = findViewById(R.id.edit_button)
        saveButton = findViewById(R.id.save_button)
        addImageButton = findViewById(R.id.add_image_button)
        
        // 初始化字体相关按钮
        fontButton = findViewById(R.id.font_button)
        titleFontButton = findViewById(R.id.title_font_button)
        boldButton = findViewById(R.id.bold_button)
        italicButton = findViewById(R.id.italic_button)
        underlineButton = findViewById(R.id.underline_button)
        titleBoldButton = findViewById(R.id.title_bold_button)
        titleItalicButton = findViewById(R.id.title_italic_button)
        titleUnderlineButton = findViewById(R.id.title_underline_button)
        fontSizeButton = findViewById(R.id.font_size_button)
        titleFontSizeButton = findViewById(R.id.title_font_size_button)
        
        // 初始化字体大小输入框
        titleFontSizeInput = findViewById(R.id.title_font_size_input)
        contentFontSizeInput = findViewById(R.id.content_font_size_input)
        
        // 初始化 DAO
        memoDAO = MemoDAO(this)
        
        // 设置默认可见性
        addImageButton.visibility = View.GONE
        fontButton.visibility = View.GONE
        titleFontButton.visibility = View.GONE
        titleFontSizeInput.visibility = View.GONE
        contentFontSizeInput.visibility = View.GONE
    }

    private fun setupEventListeners() {
        // 设置编辑按钮点击事件
        editButton.setOnClickListener {
            toggleEditMode(true)
        }

        // 设置保存按钮点击事件
        saveButton.setOnClickListener {
            saveMemo()
            toggleEditMode(false)
        }
        
        // 添加图片按钮点击事件
        addImageButton.setOnClickListener {
            openImagePicker()
        }

        // 初始化字体按钮
        fontButton.setOnClickListener {
            showFontSelectionDialog()
        }
        
        // 初始化标题字体按钮
        titleFontButton.setOnClickListener {
            showTitleFontSelectionDialog()
        }

        // 修改内容样式按钮的点击事件
        boldButton.setOnClickListener { 
            contentBoldState = !contentBoldState
            applyContentStyles()
        }
        
        italicButton.setOnClickListener { 
            contentItalicState = !contentItalicState
            applyContentStyles()
        }
        
        underlineButton.setOnClickListener {
            mixedContentAdapter.updateUnderline(!contentUnderlineState)
            contentUnderlineState = !contentUnderlineState
        }

        // 修改字体大小按钮点击事件
        fontSizeButton.setOnClickListener {
            showFontSizeMenu(contentFontSizeInput, false)
        }

        titleFontSizeButton.setOnClickListener {
            showFontSizeMenu(titleFontSizeInput, true)
        }

        // 修改输入框点击事件
        contentFontSizeInput.setOnClickListener {
            showFontSizeMenu(it as EditText, false)
        }

        titleFontSizeInput.setOnClickListener {
            showFontSizeMenu(it as EditText, true)
        }

        // 保持原有的输入监听，用于手动输入数值
        titleFontSizeInput.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                handleFontSizeInput(v as EditText, true)
                v.clearFocus()
                true
            } else {
                false
            }
        }

        contentFontSizeInput.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                handleFontSizeInput(v as EditText, false)
                v.clearFocus()
                true
            } else {
                false
            }
        }

        // 在失去焦点时应用字体大小
        titleFontSizeInput.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                handleFontSizeInput(v as EditText, true)
            }
        }

        contentFontSizeInput.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                handleFontSizeInput(v as EditText, false)
            }
        }
    }

    private fun applyStoredSettings() {
        // 应用标题设置
        currentTitleFontName = intent.getStringExtra("memo_title_font_name") ?: "DEFAULT"
        applyTitleFont(currentTitleFontName)
        
        titleBoldState = (intent.getIntExtra("memo_title_style", Typeface.NORMAL) and Typeface.BOLD) != 0
        titleItalicState = (intent.getIntExtra("memo_title_style", Typeface.NORMAL) and Typeface.ITALIC) != 0
        applyTitleStyles()
        
        titleEditText.paint.isUnderlineText = intent.getBooleanExtra("memo_title_underline", false)
        
        // 应用内容设置
        currentFontName = intent.getStringExtra("memo_font_name") ?: "DEFAULT"
        
        contentBoldState = (intent.getIntExtra("memo_content_style", Typeface.NORMAL) and Typeface.BOLD) != 0
        contentItalicState = (intent.getIntExtra("memo_content_style", Typeface.NORMAL) and Typeface.ITALIC) != 0
        
        // 使用适配器更新内容样式
        mixedContentAdapter.updateFont(currentFontName)
        mixedContentAdapter.updateStyle(
            if (contentBoldState && contentItalicState) Typeface.BOLD_ITALIC
            else if (contentBoldState) Typeface.BOLD
            else if (contentItalicState) Typeface.ITALIC
            else Typeface.NORMAL
        )
        
        // 设置字体大小
        currentTitleFontSize = intent.getFloatExtra("memo_title_font_size", 32f)
        currentContentFontSize = intent.getFloatExtra("memo_content_font_size", 16f)
        
        titleEditText.textSize = currentTitleFontSize
        mixedContentAdapter.updateFontSize(currentContentFontSize)
        
        // 更新输入框显示的值
        titleFontSizeInput.setText(currentTitleFontSize.toInt().toString())
        contentFontSizeInput.setText(currentContentFontSize.toInt().toString())
    }

    private fun toggleEditMode(edit: Boolean) {
        isEditMode = edit
        titleEditText.isEnabled = edit
        editButton.visibility = if (edit) View.GONE else View.VISIBLE
        saveButton.visibility = if (edit) View.VISIBLE else View.GONE
        addImageButton.visibility = if (edit) View.VISIBLE else View.GONE
        fontButton.visibility = if (edit) View.VISIBLE else View.GONE
        titleFontButton.visibility = if (edit) View.VISIBLE else View.GONE
        boldButton.visibility = if (edit) View.VISIBLE else View.GONE
        italicButton.visibility = if (edit) View.VISIBLE else View.GONE
        underlineButton.visibility = if (edit) View.VISIBLE else View.GONE
        titleBoldButton.visibility = if (edit) View.VISIBLE else View.GONE
        titleItalicButton.visibility = if (edit) View.VISIBLE else View.GONE
        titleUnderlineButton.visibility = if (edit) View.VISIBLE else View.GONE
        fontSizeButton.visibility = if (edit) View.VISIBLE else View.GONE
        titleFontSizeButton.visibility = if (edit) View.VISIBLE else View.GONE
        
        // 更新适配器的编辑模式状态
        mixedContentAdapter = MixedContentAdapter(
            this,
            contentsList,
            edit,
            currentFontName,
            if (contentBoldState && contentItalicState) Typeface.BOLD_ITALIC 
            else if (contentBoldState) Typeface.BOLD 
            else if (contentItalicState) Typeface.ITALIC 
            else Typeface.NORMAL,
            currentContentFontSize,
            onImageClick = { path -> showFullImage(path) },
            onImageLongClick = { position ->
                if (edit) {
                    showDeleteConfirmationDialog(position)
                }
            },
            onTextChange = { position, newText ->
                (contentsList[position] as? MemoContent.TextContent)?.let {
                    contentsList[position] = MemoContent.TextContent(newText)
                }
            }
        )
        contentRecyclerView.adapter = mixedContentAdapter
        
        // 添加字体大小输入框的可见性控制
        titleFontSizeInput.visibility = if (edit) View.VISIBLE else View.GONE
        contentFontSizeInput.visibility = if (edit) View.VISIBLE else View.GONE
        titleFontSizeInput.isEnabled = edit
        contentFontSizeInput.isEnabled = edit
    }

    private fun showDeleteConfirmationDialog(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("删除确认")
            .setMessage("确定要删除这张图片吗？")
            .setPositiveButton("确定") { _, _ ->
                contentsList.removeAt(position)
                mixedContentAdapter.notifyItemRemoved(position)
                mixedContentAdapter.notifyItemRangeChanged(position, contentsList.size)
            }
            .setNegativeButton("取消", null)
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
                // 添加图片内容
                contentsList.add(MemoContent.ImageContent(path))
                // 添加新的文本内容
                contentsList.add(MemoContent.TextContent(
                    text = "",
                    fontName = currentFontName,
                    style = if (contentBoldState && contentItalicState) Typeface.BOLD_ITALIC 
                            else if (contentBoldState) Typeface.BOLD 
                            else if (contentItalicState) Typeface.ITALIC 
                            else Typeface.NORMAL,
                    fontSize = currentContentFontSize
                ))
                // 通知适配器整个范围的更新
                mixedContentAdapter.notifyItemRangeInserted(contentsList.size - 2, 2)
                // 滚动到新添加的文本位置
                contentRecyclerView.post {
                    contentRecyclerView.smoothScrollToPosition(contentsList.size - 1)
                }
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
            
            // 收集所有文本内容
            val textContents = contentsList
                .filterIsInstance<MemoContent.TextContent>()
                .joinToString("\n") { it.text }
            
            // 收集所有图片路径
            val imagePaths = contentsList
                .filterIsInstance<MemoContent.ImageContent>()
                .map { it.imagePath }

            memoDAO.updateMemo(
                id = memoId,
                title = newTitle,
                content = textContents,
                imagePaths = imagePaths,
                fontName = currentFontName,
                titleFontName = currentTitleFontName,
                titleStyle = if (titleBoldState && titleItalicState) Typeface.BOLD_ITALIC 
                            else if (titleBoldState) Typeface.BOLD 
                            else if (titleItalicState) Typeface.ITALIC 
                            else Typeface.NORMAL,
                contentStyle = if (contentBoldState && contentItalicState) Typeface.BOLD_ITALIC 
                              else if (contentBoldState) Typeface.BOLD 
                              else if (contentItalicState) Typeface.ITALIC 
                              else Typeface.NORMAL,
                titleUnderline = titleEditText.paint.isUnderlineText,
                contentUnderline = false,  // 内容的下划线状态现在由适配器管理
                titleFontSize = currentTitleFontSize,
                contentFontSize = currentContentFontSize
            )
            updateTimeTextView.text = "刚刚更新"
        }
    }

    private fun applyFont(fontName: String) {
        currentFontName = fontName
        mixedContentAdapter.updateFont(fontName)
    }

    private fun showFontSelectionDialog() {
        val currentIndex = FontUtils.FONT_NAMES.indexOf(currentFontName).takeIf { it != -1 } ?: 0
        var selectedIndex = currentIndex

        AlertDialog.Builder(this)
            .setTitle("选择字体")
            .setSingleChoiceItems(FontUtils.FONT_NAMES, currentIndex) { _, which ->
                // 只记录选择的位置，不立即应用
                selectedIndex = which
            }
            .setPositiveButton("确定") { _, _ ->
                // 用户点击确定后才应用字体
                if (selectedIndex != currentIndex) {
                    currentFontName = FontUtils.FONT_NAMES[selectedIndex]
                    applyFont(currentFontName)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showTitleFontSelectionDialog() {
        val currentIndex = FontUtils.FONT_NAMES.indexOf(currentTitleFontName).takeIf { it != -1 } ?: 0
        var selectedIndex = currentIndex

        AlertDialog.Builder(this)
            .setTitle("选择标题字体")
            .setSingleChoiceItems(FontUtils.FONT_NAMES, currentIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton("确定") { _, _ ->
                if (selectedIndex != currentIndex) {
                    currentTitleFontName = FontUtils.FONT_NAMES[selectedIndex]
                    applyTitleFont(currentTitleFontName)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun applyTitleFont(fontName: String) {
        val index = FontUtils.FONT_NAMES.indexOf(fontName)
        if (index >= 0 && index < FontUtils.FONTS.size) {
            titleEditText.typeface = FontUtils.FONTS[index]
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
        val newStyle = when {
            contentBoldState && contentItalicState -> Typeface.BOLD_ITALIC
            contentBoldState -> Typeface.BOLD
            contentItalicState -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        mixedContentAdapter.updateStyle(newStyle)
    }

    private fun showFontSizeMenu(editText: EditText, isTitle: Boolean) {
        val currentSize = if (isTitle) currentTitleFontSize else currentContentFontSize
        
        val popupMenu = PopupMenu(this, editText)
        
        FontUtils.FONT_SIZES.forEach { (name, size) ->
            popupMenu.menu.add(Menu.NONE, View.NO_ID, Menu.NONE, "$name (${size}pt)")
        }

        // 设置点击监听
        popupMenu.setOnMenuItemClickListener { menuItem ->
            // 从选项中提取大小并应用
            val sizeStr = menuItem.title.toString()
                .substringAfter("(")
                .substringBefore("pt")
            val size = sizeStr.toFloatOrNull()
            if (size != null) {
                if (isTitle) {
                    currentTitleFontSize = size
                    titleEditText.textSize = size
                    editText.setText(size.toInt().toString())
                } else {
                    currentContentFontSize = size
                    mixedContentAdapter.updateFontSize(size)  // 使用适配器更新内容字体大小
                    editText.setText(size.toInt().toString())
                }
            }
            true
        }

        // 显示菜单
        popupMenu.show()
    }

    // 修改 handleFontSizeInput 方法，添加 Toast 提醒
    private fun handleFontSizeInput(editText: EditText, isTitle: Boolean) {
        val size = editText.text.toString().toFloatOrNull()
        if (size != null) {
            when {
                size < 6.5f -> {
                    Toast.makeText(this, "字号不能小于 6.5", Toast.LENGTH_SHORT).show()
                    editText.setText((if (isTitle) currentTitleFontSize else currentContentFontSize).toInt().toString())
                }
                size > 42f -> {
                    Toast.makeText(this, "字号不能大于 42", Toast.LENGTH_SHORT).show()
                    editText.setText((if (isTitle) currentTitleFontSize else currentContentFontSize).toInt().toString())
                }
                else -> {
                    if (isTitle) {
                        currentTitleFontSize = size
                        titleEditText.textSize = size
                    } else {
                        currentContentFontSize = size
                        mixedContentAdapter.updateFontSize(size)  // 使用适配器更新内容字体大小
                    }
                }
            }
        } else {
            // 如果输入无效，恢复原来的值并提示
            Toast.makeText(this, "请输入有效的字号", Toast.LENGTH_SHORT).show()
            editText.setText(
                (if (isTitle) currentTitleFontSize else currentContentFontSize)
                    .toInt().toString()
            )
        }
        // 隐藏输入法
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editText.windowToken, 0)
    }

    private fun showFullImage(imagePath: String) {
        // 创建一个对话框来显示全屏图片
        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            .create()
        
        // 创建一个 ImageView
        val imageView = ImageView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        // 使用 Glide 加载图片
        Glide.with(this)
            .load(imagePath)
            .override(2048, 2048)  // 设置更大的尺寸用于全屏显示
            .into(imageView)

        // 点击图片关闭对话框
        imageView.setOnClickListener {
            dialog.dismiss()
        }

        // 设置对话框内容并显示
        dialog.setView(imageView)
        dialog.show()
    }

    private fun addNewTextContent() {
        contentsList.add(MemoContent.TextContent(
            text = "",
            fontName = currentFontName,
            style = if (contentBoldState && contentItalicState) Typeface.BOLD_ITALIC 
                    else if (contentBoldState) Typeface.BOLD 
                    else if (contentItalicState) Typeface.ITALIC 
                    else Typeface.NORMAL,
            fontSize = currentContentFontSize
        ))
        val position = contentsList.size - 1
        mixedContentAdapter.notifyItemInserted(position)
        // 滚动到新添加的文本位置
        contentRecyclerView.post {
            contentRecyclerView.smoothScrollToPosition(position)
        }
    }
}
