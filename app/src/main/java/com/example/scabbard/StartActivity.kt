package com.example.scabbard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import com.example.memo.MainActivityMemo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.scabbard.update.UpdateChecker
import android.widget.ImageView
import com.example.scabbard.utils.IconManager

open class StartActivity : AppCompatActivity() {

    private lateinit var startButton: Button
    private lateinit var memoButton: Button
    private lateinit var moreButton: Button
    private val updateChecker = UpdateChecker()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_allocator)

        // 更新当前图标
        findViewById<ImageView>(R.id.app_icon)?.setImageResource(
            IconManager.getCurrentIconResourceId(this)
        )

        startButton = findViewById(R.id.start_button)
        memoButton = findViewById(R.id.memo_button)
        moreButton = findViewById(R.id.toolbar)

        // 设置更多按钮点击事件
        moreButton.setOnClickListener { view ->
            showPopupMenu(view)
        }

        startButton.setOnClickListener {
            val intent = Intent(this@StartActivity, MainActivity::class.java)
            startActivity(intent)
        }

        memoButton.setOnClickListener {
            val intent = Intent(this@StartActivity, MainActivityMemo::class.java)
            startActivity(intent)
        }
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.menu_more, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_about -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                    true
                }
                R.id.menu_appearance -> {
                    startActivity(Intent(this, AppearanceActivity::class.java))
                    true
                }
                R.id.menu_update -> {
                    checkForUpdates()
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    private fun checkForUpdates() {
        CoroutineScope(Dispatchers.Main).launch {
            val updateInfo = updateChecker.checkForUpdates(this@StartActivity)

            updateInfo?.let { info ->
                val currentVersion = updateChecker.getCurrentVersion(this@StartActivity)

                if (updateChecker.shouldUpdate(info.latestVersion, currentVersion)) {
                    updateChecker.showUpdateDialog(
                        context = this@StartActivity,
                        updateInfo = info,
                        onConfirm = {},
                        onCancel = {}
                    )
                }
            }
        }
    }
}

