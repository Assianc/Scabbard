package com.example.memo;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MemoAdapter extends RecyclerView.Adapter<MemoAdapter.MemoViewHolder> {

    private List<Memo> memoList;
    private Context context;
    private static final int MAX_CONTENT_LENGTH = 50;
    private boolean isMultiSelectMode = false; // 是否处于多选模式
    private List<Memo> selectedItems = new ArrayList<>(); // 保存被选中的备忘录
    private MemoDAO memoDAO;

    public MemoAdapter(List<Memo> memoList, Context context, MemoDAO memoDAO) { // 修改构造函数
        this.memoList = memoList;
        this.context = context;
        this.memoDAO = memoDAO; // 初始化 MemoDAO
    }

    @NonNull
    @Override
    public MemoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.memo_item, parent, false);
        return new MemoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemoViewHolder holder, int position) {
        Memo memo = memoList.get(position);
        holder.titleTextView.setText(memo.getTitle());

        // 显示更新时间
        holder.updateTimeTextView.setText("上次更新: " + memo.getUpdateTime());

        // 限制内容显示前50字符，并设置最大行数为2行
        String content = memo.getContent();
        String displayContent = content.length() > MAX_CONTENT_LENGTH
                ? content.substring(0, MAX_CONTENT_LENGTH) + "..."
                : content;
        holder.contentTextView.setText(displayContent);
        holder.contentTextView.setMaxLines(2);

        // 多选模式下显示 CheckBox，并设置选中状态
        holder.checkBox.setVisibility(isMultiSelectMode ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(selectedItems.contains(memo));

        // 设置点击事件，跳转到 MemoDetailActivity 显示完整信息
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MemoDetailActivity.class);
            intent.putExtra("memo_title", memo.getTitle());
            intent.putExtra("memo_content", memo.getContent());
            intent.putExtra("memo_update_time",memo.getUpdateTime()+ " 修改"); // 传递更新时间

            context.startActivity(intent);
        });

        holder.checkBox.setVisibility(isMultiSelectMode ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(selectedItems.contains(memo));

        holder.contentTextView.setOnClickListener(new View.OnClickListener() {
            private boolean isExpanded = false;

            @Override
            public void onClick(View v) {
                isExpanded = !isExpanded;
                holder.contentTextView.setText(isExpanded ? content : displayContent);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!isMultiSelectMode) {
                isMultiSelectMode = true;
                if (context instanceof MainActivityMemo) {
                    ((MainActivityMemo) context).toggleDeleteButton(true);
                }
                notifyDataSetChanged();
            }
            return true;
        });

        holder.checkBox.setOnClickListener(v -> {
            if (holder.checkBox.isChecked()) {
                selectedItems.add(memo);
            } else {
                selectedItems.remove(memo);
            }
        });
    }

    @Override
    public int getItemCount() {
        return memoList.size();
    }

    // 批量删除选中的备忘录
    public void deleteSelectedMemos() {
        for (Memo memo : selectedItems) {
            memoDAO.deleteMemo(memo.getId()); // 使用 memoDAO 实例删除数据库中的记录
        }
        memoList.removeAll(selectedItems);
        selectedItems.clear();
        isMultiSelectMode = false;
        notifyDataSetChanged();

        if (context instanceof MainActivityMemo) {
            ((MainActivityMemo) context).toggleDeleteButton(false); // 隐藏删除按钮
        }
    }


    // 退出多选模式
    public void exitMultiSelectMode() {
        isMultiSelectMode = false;
        selectedItems.clear();
        notifyDataSetChanged();
        if (context instanceof MainActivityMemo) {
            ((MainActivityMemo) context).toggleDeleteButton(false); // 隐藏删除按钮
        }
    }

    // 判断是否处于多选模式
    public boolean isMultiSelectMode() {
        return isMultiSelectMode;
    }

    public static class MemoViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView contentTextView;
        CheckBox checkBox;
        TextView updateTimeTextView;

        public MemoViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.memo_title);
            contentTextView = itemView.findViewById(R.id.memo_content);
            checkBox = itemView.findViewById(R.id.checkbox); // 添加一个 CheckBox
            updateTimeTextView = itemView.findViewById(R.id.memo_update_time); // 绑定到布局
        }
    }
}
