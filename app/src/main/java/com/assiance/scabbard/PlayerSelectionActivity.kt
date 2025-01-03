package com.assiance.scabbard

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class PlayerSelectionActivity : AppCompatActivity() {
    private lateinit var playerNameInput: EditText
    private lateinit var playerListView: ListView
    private lateinit var addPlayerButton: Button
    private lateinit var doneButton: Button
    private lateinit var selectAllButton: Button
    private var playerList: ArrayList<String> = arrayListOf()
    private lateinit var playerAdapter: ArrayAdapter<String>
    private var isAllSelected = false // 用于跟踪全选状态

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_selection)

        playerNameInput = findViewById(R.id.player_name_input)
        playerListView = findViewById(R.id.player_list_view)
        addPlayerButton = findViewById(R.id.add_player_button)
        doneButton = findViewById(R.id.done_button)
        selectAllButton = findViewById(R.id.select_all_button)

        // 从 MainActivity 获取当前的玩家列表
        intent.getStringArrayListExtra("currentPlayers")?.let {
            playerList.addAll(it)
        }

        // 初始化玩家列表适配器
        playerAdapter = ArrayAdapter(
            this, android.R.layout.simple_list_item_multiple_choice,
            playerList
        )
        playerListView.adapter = playerAdapter
        playerListView.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        // 添加玩家按钮点击事件
        addPlayerButton.setOnClickListener {
            val playerName = playerNameInput.text.toString()
            if (playerName.isNotEmpty()) {
                playerList.add(playerName)
                playerAdapter.notifyDataSetChanged()
                playerNameInput.setText("")
            } else {
                Toast.makeText(this, "请输入玩家名称", Toast.LENGTH_SHORT).show()
            }
        }

        // 全选按钮事件
        selectAllButton.setOnClickListener {
            if (isAllSelected) {
                for (i in playerList.indices) {
                    playerListView.setItemChecked(i, false)
                }
                isAllSelected = false
                selectAllButton.text = "全选"
            } else {
                for (i in playerList.indices) {
                    playerListView.setItemChecked(i, true)
                }
                isAllSelected = true
                selectAllButton.text = "取消全选"
            }
        }

        // 设置 ListView 长按删除玩家功能
        playerListView.setOnItemLongClickListener { _, _, position, _ ->
            showDeleteConfirmationDialog(position)
            true
        }

        // 完成按钮点击事件，返回主界面
        doneButton.setOnClickListener {
            val selectedPlayers = arrayListOf<String>()
            for (i in 0 until playerListView.count) {
                if (playerListView.isItemChecked(i)) {
                    selectedPlayers.add(playerList[i])
                }
            }
            if (selectedPlayers.isEmpty()) {
                Toast.makeText(this, "请选择至少一名玩家", Toast.LENGTH_SHORT).show()
            } else {
                val resultIntent = Intent()
                resultIntent.putStringArrayListExtra("selectedPlayers", selectedPlayers)
                resultIntent.putStringArrayListExtra("allPlayers", playerList)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    // 显示确认删除对话框
    private fun showDeleteConfirmationDialog(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("删除元素")
            .setMessage("你确定要删除该分类元素吗")
            .setPositiveButton("是") { _, _ ->
                playerList.removeAt(position)
                playerAdapter.notifyDataSetChanged()
            }
            .setNegativeButton("否", null)
            .show()
    }
}


//fwhwbuifi wif iwfhgwioq GFOPIFWGQOIG FQOIUG