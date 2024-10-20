package com.example.allocator;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_ADD_PLAYER = 1;
    private Button allocateButton, addPlayerButton;
    private TextView teamAText, teamBText, teamCText, teamDText;
    private Spinner classificationSpinner;
    private List<String> players;
    private int classificationType = 2; // 默认二分类
    private List<String> selectedPlayers; // 保存选中的玩家列表
    private boolean isTeamsAllocated = false;  // 标志是否已经进行过分类



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 获取控件
        classificationSpinner = findViewById(R.id.classificationSpinner);
        allocateButton = findViewById(R.id.allocate_button);
        addPlayerButton = findViewById(R.id.add_player_button);
        teamAText = findViewById(R.id.team_a_text);
        teamBText = findViewById(R.id.team_b_text);
        teamCText = findViewById(R.id.team_c_text);
        teamDText = findViewById(R.id.team_d_text);

        // 初始化玩家列表
        players = new ArrayList<>();
        players.add("吴");
        players.add("韩");
        players.add("祖力");
        players.add("功桑");
        players.add("展斌");
        players.add("欧桑");
        players.add("陆勇诺");
        players.add("赵志辉");
        players.add("聂宏旭");
        players.add("6舅");
        players.add("付俊杰");
        players.add("黄贵豪");
        players.add("孜巴努尔");
        players.add("侯欢格");

        // 设置 Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.classification_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        classificationSpinner.setAdapter(adapter);

        // 监听分类类型选择
        classificationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedClassification = parent.getItemAtPosition(position).toString();

                // 只更新分类类型，但不立即改变分类区域的可见性
                if (selectedClassification.equals("二分类")) {
                    classificationType = 2;
                } else if (selectedClassification.equals("三分类")) {
                    classificationType = 3;
                } else if (selectedClassification.equals("四分类")) {
                    classificationType = 4;
                }

                // 如果没有执行过分类操作，保持当前的分类区域状态不变
                if (!isTeamsAllocated) {
                    teamCText.setVisibility(View.GONE);
                    teamDText.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                classificationType = 2;  // 默认二分类
            }
        });



        // 添加玩家按钮
        addPlayerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PlayerSelectionActivity.class);
                intent.putStringArrayListExtra("currentPlayers", new ArrayList<>(players)); // 传递当前的玩家列表
                startActivityForResult(intent, REQUEST_CODE_ADD_PLAYER); // 启动玩家选择界面
            }
        });
        //每次点击“分配”按钮时，确保使用选中的玩家列表
        allocateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                allocateTeams(selectedPlayers); // 使用当前选中的玩家进行分类
            }
        });

    }

    // 接收玩家选择界面返回的数据
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_PLAYER && resultCode == RESULT_OK && data != null) {
            ArrayList<String> updatedPlayers = data.getStringArrayListExtra("allPlayers");
            ArrayList<String> selectedPlayersList = data.getStringArrayListExtra("selectedPlayers");

            if (updatedPlayers != null) {
                players.clear();
                players.addAll(updatedPlayers); // 更新所有玩家列表
            }

            // 更新选中的玩家
            if (selectedPlayersList != null) {
                selectedPlayers = new ArrayList<>(selectedPlayersList); // 保存选中的玩家
            }
        }
    }

    // 修改 allocateTeams 方法，接受选中的玩家作为参数
    // 格式化玩家列表，将玩家名用逗号分隔
// 格式化玩家列表，将玩家名用逗号分隔
    private String formatPlayerList(List<String> team) {
        return String.join(", ", team); // 使用逗号分隔玩家名字
    }

    // 修改后的 allocateTeams 方法，增加了空检查
    private void allocateTeams(List<String> selectedPlayers) {
        if (selectedPlayers == null || selectedPlayers.isEmpty()) {
            // 如果没有选中的玩家，弹出提示并返回
            Toast.makeText(this, "请选择玩家进行分类", Toast.LENGTH_SHORT).show();
            return;
        }

        // 正常的分类逻辑
        Collections.shuffle(selectedPlayers);  // 打乱玩家顺序

        // 清空之前的分类区域文本
        teamAText.setText("");
        teamBText.setText("");
        teamCText.setText("");
        teamDText.setText("");

        switch (classificationType) {
            case 2:
                int halfSize = selectedPlayers.size() / 2;
                List<String> teamA = selectedPlayers.subList(0, halfSize);
                List<String> teamB = selectedPlayers.subList(halfSize, selectedPlayers.size());
                teamAText.setText("区域 A: " + formatPlayerList(teamA));
                teamBText.setText("区域 B: " + formatPlayerList(teamB));
                break;

            case 3:
                int totalPlayers = selectedPlayers.size();
                int teamA3Size = totalPlayers / 3 + (totalPlayers % 3 > 0 ? 1 : 0);
                int teamB3Size = (totalPlayers - teamA3Size) / 2 + ((totalPlayers - teamA3Size) % 2 > 0 ? 1 : 0);
                int teamC3Size = totalPlayers - teamA3Size - teamB3Size;

                List<String> teamA3 = selectedPlayers.subList(0, teamA3Size);
                List<String> teamB3 = selectedPlayers.subList(teamA3Size, teamA3Size + teamB3Size);
                List<String> teamC3 = selectedPlayers.subList(teamA3Size + teamB3Size, totalPlayers);

                teamAText.setText("区域 A: " + formatPlayerList(teamA3));
                teamBText.setText("区域 B: " + formatPlayerList(teamB3));
                teamCText.setText("区域 C: " + formatPlayerList(teamC3));

                teamCText.setVisibility(View.VISIBLE);
                break;

            case 4:
                totalPlayers = selectedPlayers.size();
                int teamA4Size = totalPlayers / 4 + (totalPlayers % 4 > 0 ? 1 : 0);
                int teamB4Size = (totalPlayers - teamA4Size) / 3 + ((totalPlayers - teamA4Size) % 3 > 0 ? 1 : 0);
                int teamC4Size = (totalPlayers - teamA4Size - teamB4Size) / 2 + ((totalPlayers - teamA4Size - teamB4Size) % 2 > 0 ? 1 : 0);
                int teamD4Size = totalPlayers - teamA4Size - teamB4Size - teamC4Size;

                List<String> teamA4 = selectedPlayers.subList(0, teamA4Size);
                List<String> teamB4 = selectedPlayers.subList(teamA4Size, teamA4Size + teamB4Size);
                List<String> teamC4 = selectedPlayers.subList(teamA4Size + teamB4Size, teamA4Size + teamB4Size + teamC4Size);
                List<String> teamD4 = selectedPlayers.subList(teamA4Size + teamB4Size + teamC4Size, totalPlayers);

                teamAText.setText("区域 A: " + formatPlayerList(teamA4));
                teamBText.setText("区域 B: " + formatPlayerList(teamB4));
                teamCText.setText("区域 C: " + formatPlayerList(teamC4));
                teamDText.setText("区域 D: " + formatPlayerList(teamD4));

                teamCText.setVisibility(View.VISIBLE);
                teamDText.setVisibility(View.VISIBLE);
                break;
        }

        // 根据分类类型动态隐藏或显示多余的分类区域
        if (classificationType == 2) {
            teamCText.setVisibility(View.GONE);
            teamDText.setVisibility(View.GONE);
        } else if (classificationType == 3) {
            teamDText.setVisibility(View.GONE);
        }
    }



}
