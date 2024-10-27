package com.example.memo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.memo.AddMemoActivity;
import com.example.memo.Memo;
import com.example.memo.MemoAdapter;
import com.example.memo.MemoDAO;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainActivityMemo extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MemoAdapter memoAdapter;
    private MemoDAO memoDAO;
    private List<Memo> memoList;
    private float dX, dY;
    private int lastAction;
    private static final long ANIMATION_DURATION = 300; // 动画持续时间

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_memo);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        memoDAO = new MemoDAO(this);
        memoList = memoDAO.getAllMemos();

        // 设置适配器
        memoAdapter = new MemoAdapter(memoList, this); // 传递Context
        recyclerView.setAdapter(memoAdapter);

        FloatingActionButton fab = findViewById(R.id.fab);
        FloatingActionButton deleteButton = findViewById(R.id.delete_button);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 跳转到添加备忘录界面
                Intent intent = new Intent(MainActivityMemo.this, AddMemoActivity.class);
                startActivity(intent);
            }
        });

        // 设置点击事件，删除选中的备忘录
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                memoAdapter.deleteSelectedMemos();
                deleteButton.setVisibility(View.GONE); // 删除完成后隐藏按钮
            }
        });

        // 设置 FloatingActionButton 可拖动
        fab.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        lastAction = MotionEvent.ACTION_DOWN;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        view.setX(event.getRawX() + dX);
                        view.setY(event.getRawY() + dY);
                        lastAction = MotionEvent.ACTION_MOVE;
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (lastAction == MotionEvent.ACTION_MOVE) {
                            // 松手后将按钮移动到最近的屏幕侧边
                            float finalX = (view.getX() + view.getWidth() / 2) >= (getWindow().getDecorView().getWidth() / 2)
                                    ? getWindow().getDecorView().getWidth() - view.getWidth() // 右侧
                                    : 0; // 左侧

                            view.animate()
                                    .x(finalX) // 将X轴坐标设为左右两侧的某个值
                                    .setDuration(ANIMATION_DURATION) // 动画时长
                                    .start();
                        } else if (lastAction == MotionEvent.ACTION_DOWN) {
                            view.performClick();
                        }
                        return true;

                    default:
                        return false;
                }
            }

            public void toggleDeleteButton(boolean show) {
                deleteButton.setVisibility(show ? View.VISIBLE : View.GONE);
            }


        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        memoList.clear();
        memoList.addAll(memoDAO.getAllMemos());
        memoAdapter.notifyDataSetChanged();
    }

    public void toggleDeleteButton(boolean b) {
    }
}

