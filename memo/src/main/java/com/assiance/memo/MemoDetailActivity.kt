package com.assiance.memo

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

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
    private lateinit var fontSizeButton: ImageButton
    private lateinit var titleFontSizeButton: ImageButton
    private var currentTitleFontSize = 32f // 默认标题字体大小
    private var currentContentFontSize = 16f // 默认内容字体大小
    private lateinit var titleFontSizeInput: EditText
    private lateinit var contentFontSizeInput: EditText
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>

    companion object {
        private val FONTS = mutableListOf<Typeface>()
        private val FONT_NAMES = arrayOf(
            "默认字体",
            "宋体",
            "仿宋",
            "黑体",
            "楷体",
        )

        private val FONT_SIZES = arrayOf(
            Pair("初号", 42f),
            Pair("小初", 36f),
            Pair("一号", 26f),
            Pair("小一", 24f),
            Pair("二号", 22f),
            Pair("小二", 18f),
            Pair("三号", 16f),
            Pair("小三", 15f),
            Pair("四号", 14f),
            Pair("小四", 12f),
            Pair("五号", 10.5f),
            Pair("小五", 9f),
            Pair("六号", 7.5f),
            Pair("小六", 6.5f)
        )

        private const val HISTORY_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 设置窗口背景为透明
        window.setBackgroundDrawableResource(android.R.color.transparent)

        // 启用转场动画
        window.requestFeature(android.view.Window.FEATURE_ACTIVITY_TRANSITIONS)
        window.sharedElementEnterTransition = android.transition.TransitionInflater
            .from(this)
            .inflateTransition(R.transition.memo_transition)
        window.sharedElementReturnTransition = android.transition.TransitionInflater
            .from(this)
            .inflateTransition(R.transition.memo_transition)

        // 禁用默认的进入和退出过渡动画
        window.enterTransition = null
        window.exitTransition = null

        super.onCreate(savedInstanceState)

        // 在初始化其他内容之前，先确保数据库是最新的
        val dbHelper = MemoDatabaseHelper(this)
        dbHelper.writableDatabase.close()

        setContentView(R.layout.activity_memo_detail)
        // 初始化所有视图
        initializeViews()

        // 初始化 ActivityResultLauncher
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val selectedImageUri = result.data?.data
                selectedImageUri?.let { uri ->
                    val path = getRealPathFromURI(uri)
                    imagePaths.add(path)
                    imageAdapter.notifyItemInserted(imagePaths.size - 1)
                }
            }
        }

        // 获取传递过来的数据
        memoId = intent.getIntExtra("memo_id", -1)
        val title = intent.getStringExtra("memo_title") ?: ""
        val content = intent.getStringExtra("memo_content") ?: ""
        val updateTime = intent.getStringExtra("memo_update_time") ?: ""

        // 设置数据
        titleEditText.setText(title)
        contentEditText.setText(content)
        updateTimeTextView.text = updateTime

        // 设置事件监听器
        setupEventListeners()

        // 初始化字体
        initFonts()

        // 应用保存的设置
        applyStoredSettings()

        requestStoragePermission()

        // 在 onCreate 方法中添加历史记录按钮
        val historyButton = findViewById<ImageButton>(R.id.history_button)
        historyButton.setOnClickListener {
            val intent = Intent(this, MemoHistoryActivity::class.java)
            intent.putExtra("memo_id", memoId)
            startActivityForResult(intent, HISTORY_REQUEST_CODE)
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES), PERMISSION_REQUEST_CODE)
            }
        } else {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // 权限已授予，可以访问存储
            } else {
                Toast.makeText(this, "读取外部存储权限被拒绝，无法加载图片。", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initializeViews() {
        // 初始化基本视图
        titleEditText = findViewById(R.id.memo_detail_title)
        contentEditText = findViewById(R.id.memo_detail_content)
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

        // 初始化图片列表
        imagesRecyclerView = findViewById(R.id.images_recycler_view)
        imagesRecyclerView.layoutManager = LinearLayoutManager(this)

        // 初始化 DAO
        memoDAO = MemoDAO(this)

        // 设置默认可见性
        addImageButton.visibility = View.GONE
        fontButton.visibility = View.GONE
        titleFontButton.visibility = View.GONE
        titleFontSizeInput.visibility = View.GONE
        contentFontSizeInput.visibility = View.GONE

        // 添加历史记录按钮到工具栏
        val historyButton = findViewById<ImageButton>(R.id.history_button)
        historyButton.setOnClickListener {
            val intent = Intent(this, MemoHistoryActivity::class.java)
            intent.putExtra("memo_id", memoId)
            startActivityForResult(intent, HISTORY_REQUEST_CODE)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
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
        // 初始化图片列表
        imageAdapter = ImageAdapter(
            this,
            imagePaths,
            isEditMode = false
        ) { position ->
            // 长按删除图片
            imagePaths.removeAt(position)
            imageAdapter.notifyItemRemoved(position)
            // 通知适配器数据集已更改
            imageAdapter.notifyItemRangeChanged(position, imagePaths.size)
        }
        imagesRecyclerView.adapter = imageAdapter
        // 获取并显示现有图片
        intent.getStringArrayListExtra("memo_image_paths")?.let { paths ->
            imagePaths.addAll(paths)
            imageAdapter.notifyDataSetChanged()
        }
        // 初始化字体按钮
        fontButton.setOnClickListener {
            showFontSelectionDialog()
        }

        // 初始化标题字体按钮
        titleFontButton.setOnClickListener {
            showTitleFontSelectionDialog()
        }
        // 初始化内容样式按钮
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

    @SuppressLint("SetTextI18n")
    private fun applyStoredSettings() {
        // 应用保存的字体设置
        currentFontName = intent.getStringExtra("memo_font_name") ?: "DEFAULT"
        currentTitleFontName = intent.getStringExtra("memo_title_font_name") ?: "DEFAULT"
        applyFont(currentFontName)
        applyTitleFont(currentTitleFontName)

        // 应用保存的样式设置
        val titleStyle = intent.getIntExtra("memo_title_style", Typeface.NORMAL)
        val contentStyle = intent.getIntExtra("memo_content_style", Typeface.NORMAL)
        val titleUnderline = intent.getBooleanExtra("memo_title_underline", false)
        val contentUnderline = intent.getBooleanExtra("memo_content_underline", false)

        // 设置字体大小
        currentTitleFontSize = intent.getFloatExtra("memo_title_font_size", 32f)
        currentContentFontSize = intent.getFloatExtra("memo_content_font_size", 16f)
        titleEditText.textSize = currentTitleFontSize
        contentEditText.textSize = currentContentFontSize

        // 更新输入框的显示值
        titleFontSizeInput.setText(currentTitleFontSize.toInt().toString())
        contentFontSizeInput.setText(currentContentFontSize.toInt().toString())

        // 恢复样式状态
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
        
        // 为编辑相关的按钮设置动画
        val editButtons = listOf(
            saveButton,
            addImageButton,
            fontButton,
            titleFontButton,
            boldButton,
            italicButton,
            underlineButton,
            titleBoldButton,
            titleItalicButton,
            titleUnderlineButton,
            fontSizeButton,
            titleFontSizeButton,
            titleFontSizeInput,
            contentFontSizeInput
        )
        
        // 为预览模式的按钮设置动画
        val viewButtons = listOf(editButton)

        // 使用 ValueAnimator 控制透明度变化
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 200L
            interpolator = FastOutSlowInInterpolator()
            
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                if (edit) {
                    editButtons.forEach { it.alpha = value }
                    viewButtons.forEach { it.alpha = 1 - value }
                } else {
                    editButtons.forEach { it.alpha = 1 - value }
                    viewButtons.forEach { it.alpha = value }
                }
            }
            
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    if (edit) {
                        editButtons.forEach { it.visibility = View.VISIBLE }
                    } else {
                        viewButtons.forEach { it.visibility = View.VISIBLE }
                    }
                }
                
                override fun onAnimationEnd(animation: Animator) {
                    if (edit) {
                        viewButtons.forEach { it.visibility = View.GONE }
                    } else {
                        editButtons.forEach { it.visibility = View.GONE }
                    }
                }
            })
            start()
        }

        // 使用 ValueAnimator 处理文本颜色变化
        ValueAnimator.ofArgb(
            if (edit) getColor(R.color.view_mode_text) else getColor(R.color.edit_mode_text),
            if (edit) getColor(R.color.edit_mode_text) else getColor(R.color.view_mode_text)
        ).apply {
            duration = 200L
            interpolator = FastOutSlowInInterpolator()
            
            addUpdateListener { animator ->
                val color = animator.animatedValue as Int
                titleEditText.setTextColor(color)
                contentEditText.setTextColor(color)
            }
            start()
        }

        // 在动画开始前设置状态
        titleEditText.isEnabled = edit
        contentEditText.isEnabled = edit
        
        // 更新 ImageAdapter 的编辑模式状态
        imageAdapter.updateEditMode(edit) { position ->
            if (edit) {
                showDeleteConfirmationDialog(position)
            }
        }
    }

    private fun showDeleteConfirmationDialog(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("删除图片")
            .setMessage("确定要删除这张图片吗？")
            .setPositiveButton("确定") { dialog, _->
                // 添加安全检查
                if (position >= 0 && position < imagePaths.size) {
                    imagePaths.removeAt(position)
                    imageAdapter.notifyItemRemoved(position)
                    // 通知适配器数据集已更改
                    imageAdapter.notifyItemRangeChanged(position, imagePaths.size)
                }
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog,_ ->
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
        pickImageLauncher.launch(intent)
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
                contentUnderline = contentEditText.paint.isUnderlineText,
                titleFontSize = currentTitleFontSize,
                contentFontSize = currentContentFontSize
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
            .setPositiveButton("确定") { _, _->
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
            .setPositiveButton("确定") { _, _->
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

    @SuppressLint("SetTextI18n")
    private fun showFontSizeMenu(editText: EditText, isTitle: Boolean) {
        val popupMenu = PopupMenu(this, editText)

        // 添加预设选项
        FONT_SIZES.forEach { (name, size) ->
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
                    contentEditText.textSize = size
                    editText.setText(size.toInt().toString())
                }
            }
            true
        }
        // 显示菜单
        popupMenu.show()
    }

    // 修改 handleFontSizeInput 方法，添加 Toast 提醒
    @SuppressLint("SetTextI18n")
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
                        contentEditText.textSize = size
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

    override fun finish() {
        // 在结束活动前清空文本内容
        titleEditText.text = null
        contentEditText.text = null
        updateTimeTextView.text = null
        
        // 清除所有视图
        val rootView = findViewById<View>(android.R.id.content)
        if (rootView is ViewGroup) {
            rootView.removeAllViews()
        }
        
        super.finish()
        // 应用退出动画
        overridePendingTransition(R.anim.slide_down_enter, R.anim.slide_down_exit)
    }

    override fun onBackPressed() {
        // 在返回前清空文本内容
        titleEditText.text = null
        contentEditText.text = null
        updateTimeTextView.text = null
        
        // 清除所有视图
        val rootView = findViewById<View>(android.R.id.content)
        if (rootView is ViewGroup) {
            rootView.removeAllViews()
        }
        
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_down_enter, R.anim.slide_down_exit)
    }

    // 添加 onActivityResult 方法
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == HISTORY_REQUEST_CODE && resultCode == RESULT_OK) {
            // 刷新备忘录内容
            val memo = memoDAO.getMemoById(memoId)
            memo?.let {
                titleEditText.setText(it.title)
                contentEditText.setText(it.content)
                // 更新其他UI元素...
            }
        }
    }
}