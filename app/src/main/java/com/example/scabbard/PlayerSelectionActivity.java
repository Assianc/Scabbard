package com.example.scabbard;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class PlayerSelectionActivity extends AppCompatActivity {

    private EditText playerNameInput;
    private ListView playerListView;
    private Button addPlayerButton, doneButton, selectAllButton;
    private ArrayList<String> playerList;
    private ArrayAdapter<String> playerAdapter;
    private boolean isAllSelected = false;  // 用于跟踪全选状态

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_selection);

        playerNameInput = findViewById(R.id.player_name_input);
        playerListView = findViewById(R.id.player_list_view);
        addPlayerButton = findViewById(R.id.add_player_button);
        doneButton = findViewById(R.id.done_button);
        selectAllButton = findViewById(R.id.select_all_button);  // 新增的全选按钮

        // 从 MainActivity 获取当前的玩家列表
        playerList = getIntent().getStringArrayListExtra("currentPlayers");
        if (playerList == null) {
            playerList = new ArrayList<>();
        }

        // 初始化玩家列表适配器
        playerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, playerList);
        playerListView.setAdapter(playerAdapter);
        playerListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE); // 设置多选模式

        // 添加玩家按钮点击事件
        addPlayerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String playerName = playerNameInput.getText().toString();
                if (!playerName.isEmpty()) {
                    playerList.add(playerName);
                    playerAdapter.notifyDataSetChanged();
                    playerNameInput.setText(""); // 清空输入框
                } else {
                    Toast.makeText(PlayerSelectionActivity.this, "请输入玩家名称", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 全选按钮事件
        selectAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isAllSelected) {
                    // 取消所有选中的项目
                    for (int i = 0; i < playerList.size(); i++) {
                        playerListView.setItemChecked(i, false);  // 设置为未选中
                    }
                    isAllSelected = false;  // 更新状态为未全选
                    selectAllButton.setText("全选");  // 更新按钮文字
                } else {
                    // 全选所有项目
                    for (int i = 0; i < playerList.size(); i++) {
                        playerListView.setItemChecked(i, true);  // 设置为选中
                    }
                    isAllSelected = true;  // 更新状态为全选
                    selectAllButton.setText("取消全选");  // 更新按钮文字
                }
            }
        });


        // 设置 ListView 长按删除玩家功能
        playerListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showDeleteConfirmationDialog(position);
                return true;
            }
        });

        // 完成按钮点击事件，返回主界面
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<String> selectedPlayers = new ArrayList<>();

                // 获取已选中的玩家
                for (int i = 0; i < playerListView.getCount(); i++) {
                    if (playerListView.isItemChecked(i)) {
                        selectedPlayers.add(playerList.get(i));
                    }
                }

                if (selectedPlayers.isEmpty()) {
                    Toast.makeText(PlayerSelectionActivity.this, "请选择至少一名玩家", Toast.LENGTH_SHORT).show();
                } else {
                    Intent resultIntent = new Intent();
                    resultIntent.putStringArrayListExtra("selectedPlayers", selectedPlayers);
                    resultIntent.putStringArrayListExtra("allPlayers", playerList); // 传递所有玩家列表
                    setResult(RESULT_OK, resultIntent);
                    finish(); // 返回主界面
                }
            }
        });

    }

    // 显示确认删除对话框
    private void showDeleteConfirmationDialog(final int position) {
        new AlertDialog.Builder(this)
                .setTitle("删除元素")
                .setMessage("你确定要删除该分类元素吗")
                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 删除选中的玩家
                        playerList.remove(position);
                        playerAdapter.notifyDataSetChanged();
                    }
                })
                .setNegativeButton("否", null)
                .show();
    }
}
