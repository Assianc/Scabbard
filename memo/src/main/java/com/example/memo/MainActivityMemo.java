package com.example.memo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainActivityMemo extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MemoAdapter memoAdapter;
    private MemoDAO memoDAO;
    private List<Memo> memoList;
    private FloatingActionButton deleteButton;
    private float dX, dY;
    private int lastAction;
    private static final long ANIMATION_DURATION = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_memo);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        memoDAO = new MemoDAO(this);
        memoList = memoDAO.getAllMemos();

        // 设置适配器
        memoAdapter = new MemoAdapter(memoList, this, memoDAO); // 传递 memoDAO 实例
        recyclerView.setAdapter(memoAdapter);


        FloatingActionButton fab = findViewById(R.id.fab);
        deleteButton = findViewById(R.id.delete_button);
        deleteButton.setVisibility(View.GONE); // 初始状态隐藏删除按钮

        fab.setOnClickListener(v -> {
            // 跳转到添加备忘录界面
            Intent intent = new Intent(MainActivityMemo.this, AddMemoActivity.class);
            startActivity(intent);
        });

        // 设置点击事件，删除选中的备忘录
        deleteButton.setOnClickListener(v -> {
            memoAdapter.deleteSelectedMemos();
            toggleDeleteButton(false); // 删除完成后隐藏按钮
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
                                    .x(finalX)
                                    .setDuration(ANIMATION_DURATION)
                                    .start();
                        } else if (lastAction == MotionEvent.ACTION_DOWN) {
                            view.performClick();
                        }
                        return true;

                    default:
                        return false;
                }
            }
        });
    }

    // 控制删除按钮的显示/隐藏
    public void toggleDeleteButton(boolean show) {
        deleteButton.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 重新加载数据库中的备忘录数据，确保删除或添加操作后的数据是最新的
        memoList.clear();
        memoList.addAll(memoDAO.getAllMemos());  // 从数据库重新加载最新的备忘录列表
        memoAdapter.notifyDataSetChanged();      // 通知适配器更新显示
    }

    @Override
    public void onBackPressed() {
        if (memoAdapter.isMultiSelectMode()) {
            memoAdapter.exitMultiSelectMode(); // 退出多选模式
        } else {
            super.onBackPressed();
        }
    }

}
