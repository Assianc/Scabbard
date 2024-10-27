package com.example.memo

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import java.text.SimpleDateFormat
import java.util.*

class MemoDAO(context: Context) {

    private val dbHelper: MemoDatabaseHelper = MemoDatabaseHelper(context)

    // 插入新的备忘录
    fun insertMemo(title: String, content: String): Long {
        val db = dbHelper.writableDatabase
        var result: Long = -1
        try {
            val values = ContentValues().apply {
                put(MemoDatabaseHelper.COLUMN_TITLE, title)
                put(MemoDatabaseHelper.COLUMN_CONTENT, content)
                put(MemoDatabaseHelper.COLUMN_UPDATE_TIME, System.currentTimeMillis()) // 添加更新时间
            }
            result = db.insert(MemoDatabaseHelper.TABLE_NAME, null, values)
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            db.close() // 确保在操作后关闭数据库
        }
        return result
    }

    // 查询所有备忘录
    fun getAllMemos(): List<Memo> {
        val memoList = mutableListOf<Memo>()
        val db = dbHelper.readableDatabase

        val cursor: Cursor? = db.query(
            MemoDatabaseHelper.TABLE_NAME,
            null, null, null, null, null,
            "${MemoDatabaseHelper.COLUMN_TIMESTAMP} DESC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow(MemoDatabaseHelper.COLUMN_ID))
                val title = it.getString(it.getColumnIndexOrThrow(MemoDatabaseHelper.COLUMN_TITLE))
                val content = it.getString(it.getColumnIndexOrThrow(MemoDatabaseHelper.COLUMN_CONTENT))
                val timestamp = it.getString(it.getColumnIndexOrThrow(MemoDatabaseHelper.COLUMN_TIMESTAMP))

                // 获取更新时间并进行格式化
                val updateTimeMillis = it.getLong(it.getColumnIndexOrThrow(MemoDatabaseHelper.COLUMN_UPDATE_TIME))
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val formattedUpdateTime = sdf.format(Date(updateTimeMillis))

                memoList.add(Memo(id, title, content, timestamp, formattedUpdateTime)) // 将格式化后的更新时间传入 Memo
            }
        } ?: run {
            println("Cursor is null")
        }

        db.close() // 确保在操作后关闭数据库
        return memoList
    }

    // 删除指定ID的备忘录
    fun deleteMemo(id: Int) {
        val db = dbHelper.writableDatabase
        try {
            db.delete(MemoDatabaseHelper.TABLE_NAME, "${MemoDatabaseHelper.COLUMN_ID}=?", arrayOf(id.toString()))
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            db.close()
        }
    }

    // 更新备忘录
    fun updateMemo(id: Int, title: String, content: String) {
        val db = dbHelper.writableDatabase
        try {
            val values = ContentValues().apply {
                put(MemoDatabaseHelper.COLUMN_TITLE, title)
                put(MemoDatabaseHelper.COLUMN_CONTENT, content)
                put(MemoDatabaseHelper.COLUMN_UPDATE_TIME, System.currentTimeMillis()) // 更新当前时间戳
            }
            db.update(MemoDatabaseHelper.TABLE_NAME, values, "${MemoDatabaseHelper.COLUMN_ID}=?", arrayOf(id.toString()))
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            db.close()
        }
    }
}
