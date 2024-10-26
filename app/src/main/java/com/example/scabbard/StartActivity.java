package com.example.scabbard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.example.memo.MainActivityMemo;


import androidx.appcompat.app.AppCompatActivity;


public class StartActivity extends AppCompatActivity {

    private Button startButton;
    private Button memoButton; // 云端按钮

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        startButton = findViewById(R.id.start_button);
        memoButton = findViewById(R.id.memo_button); // 初始化新增的按钮


        // 设置按钮点击事件，点击跳转到团队分配界面
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StartActivity.this, MainActivity.class);
                startActivity(intent);
//                finish(); // 结束当前 Activity
            }
        });

        // 设置备忘录按钮点击事件，跳转到备忘录界面
        memoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StartActivity.this, MainActivityMemo.class);
                startActivity(intent);
//                finish(); // 结束当前 Activity
            }
        });
    }
}
