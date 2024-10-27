package com.example.memo;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MemoAdapter extends RecyclerView.Adapter<MemoAdapter.MemoViewHolder> {

    private List<Memo> memoList;
    private Context context;
    private static final int MAX_CONTENT_LENGTH = 50;
    private boolean isMultiSelectMode = false; // 是否处于多选模式
    private List<Memo> selectedItems = new ArrayList<>(); // 保存被选中的备忘录

    public MemoAdapter(List<Memo> memoList, Context context) {
        this.memoList = memoList;
        this.context = context;
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

        // 限制内容显示前50字符，并设置最大行数为2行
        String content = memo.getContent();
        String displayContent = content.length() > MAX_CONTENT_LENGTH
                ? content.substring(0, MAX_CONTENT_LENGTH) + "..."
                : content;
        holder.contentTextView.setText(displayContent);
        holder.contentTextView.setMaxLines(2);

        // 如果是多选模式，显示 CheckBox；否则隐藏
        holder.checkBox.setVisibility(isMultiSelectMode ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(selectedItems.contains(memo));

        // 设置内容点击事件，切换内容展开/收起
        holder.contentTextView.setOnClickListener(new View.OnClickListener() {
            private boolean isExpanded = false;

            @Override
            public void onClick(View v) {
                isExpanded = !isExpanded;
                holder.contentTextView.setText(isExpanded ? content : displayContent);
            }
        });

        // 长按启用多选模式
        holder.itemView.setOnLongClickListener(v -> {
            isMultiSelectMode = true;
            notifyDataSetChanged();
            return true;
        });

        holder.itemView.setOnLongClickListener(v -> {
            isMultiSelectMode = true;
            if (context instanceof MainActivityMemo) {
                ((MainActivityMemo) context).toggleDeleteButton(true); // 显示删除按钮
            }
            notifyDataSetChanged();
            return true;
        });


        // 处理多选状态切换
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
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

    // 批量删除选中 Memo
    public void deleteSelectedMemos() {
        memoList.removeAll(selectedItems);
        selectedItems.clear();
        isMultiSelectMode = false;
        notifyDataSetChanged();
        if (context instanceof MainActivityMemo) {
            ((MainActivityMemo) context).toggleDeleteButton(false); // 隐藏删除按钮
        }
    }


    public static class MemoViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView contentTextView;
        CheckBox checkBox;

        public MemoViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.memo_title);
            contentTextView = itemView.findViewById(R.id.memo_content);
            checkBox = itemView.findViewById(R.id.checkbox); // 添加一个 CheckBox
        }
    }
}
