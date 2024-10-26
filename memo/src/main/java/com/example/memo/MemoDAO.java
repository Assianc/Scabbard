package com.example.memo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

public class MemoDAO {

    private MemoDatabaseHelper dbHelper;

    public MemoDAO(Context context) {
        dbHelper = new MemoDatabaseHelper(context);
    }

    // 插入新的备忘录
    public long insertMemo(String title, String content) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(MemoDatabaseHelper.COLUMN_TITLE, title);
        values.put(MemoDatabaseHelper.COLUMN_CONTENT, content);
        return db.insert(MemoDatabaseHelper.TABLE_NAME, null, values);
    }

    // 查询所有备忘录
    public List<Memo> getAllMemos() {
        List<Memo> memoList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                MemoDatabaseHelper.TABLE_NAME,
                null, null, null, null, null,
                MemoDatabaseHelper.COLUMN_TIMESTAMP + " DESC"
        );
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(MemoDatabaseHelper.COLUMN_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(MemoDatabaseHelper.COLUMN_TITLE));
                String content = cursor.getString(cursor.getColumnIndexOrThrow(MemoDatabaseHelper.COLUMN_CONTENT));
                String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(MemoDatabaseHelper.COLUMN_TIMESTAMP));
                memoList.add(new Memo(id, title, content, timestamp));
            }
            cursor.close();
        }
        return memoList;
    }

    // 删除备忘录
    public void deleteMemo(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(MemoDatabaseHelper.TABLE_NAME, MemoDatabaseHelper.COLUMN_ID + "=?", new String[]{String.valueOf(id)});
    }

    // 更新备忘录
    public void updateMemo(int id, String title, String content) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(MemoDatabaseHelper.COLUMN_TITLE, title);
        values.put(MemoDatabaseHelper.COLUMN_CONTENT, content);
        db.update(MemoDatabaseHelper.TABLE_NAME, values, MemoDatabaseHelper.COLUMN_ID + "=?", new String[]{String.valueOf(id)});
    }
}
