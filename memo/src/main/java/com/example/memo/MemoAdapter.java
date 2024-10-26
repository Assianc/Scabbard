package com.example.memo;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MemoAdapter extends RecyclerView.Adapter<MemoAdapter.MemoViewHolder> {

    private List<Memo> memoList;
    private Context context;


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
        holder.contentTextView.setText(memo.getContent());

        // 设置长按删除监听器
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // 弹出确认对话框
                new AlertDialog.Builder(context)
                        .setTitle("删除备忘录")
                        .setMessage("确定要删除这个备忘录吗？")
                        .setPositiveButton("确定", (dialog, which) -> {
                            // 删除Memo并通知适配器刷新
                            deleteMemo(holder.getAdapterPosition());
                        })
                        .setNegativeButton("取消", null)
                        .show();
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return memoList.size();
    }

    // 删除Memo的方法
    private void deleteMemo(int position) {
        memoList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, memoList.size());
    }

    public static class MemoViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView contentTextView;

        public MemoViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.memo_title);
            contentTextView = itemView.findViewById(R.id.memo_content);
        }
    }
}
