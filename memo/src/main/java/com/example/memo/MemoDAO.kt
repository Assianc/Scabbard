package com.example.memo

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import java.text.SimpleDateFormat
import java.util.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MemoDAO(context: Context) {

    private val dbHelper: MemoDatabaseHelper = MemoDatabaseHelper(context)
    private val gson = Gson()

    // 插入新的备忘录
    fun insertMemo(title: String, content: String, imagePaths: List<String> = emptyList()): Long {
        val db = dbHelper.writableDatabase
        var result: Long = -1
        try {
            val values = ContentValues().apply {
                put(MemoDatabaseHelper.COLUMN_TITLE, title)
                put(MemoDatabaseHelper.COLUMN_CONTENT, content)
                put(MemoDatabaseHelper.COLUMN_UPDATE_TIME, System.currentTimeMillis())
                put(MemoDatabaseHelper.COLUMN_IMAGE_PATHS, gson.toJson(imagePaths))
            }
            result = db.insert(MemoDatabaseHelper.TABLE_NAME, null, values)
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            db.close()
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
                val updateTimeMillis = it.getLong(it.getColumnIndexOrThrow(MemoDatabaseHelper.COLUMN_UPDATE_TIME))
                val imagePathsJson = try {
                    it.getString(it.getColumnIndexOrThrow(MemoDatabaseHelper.COLUMN_IMAGE_PATHS))
                } catch (e: Exception) {
                    try {
                        val oldPath = it.getString(it.getColumnIndexOrThrow(MemoDatabaseHelper.COLUMN_IMAGE_PATH))
                        if (oldPath != null) "[\""+ oldPath +"\"]" else null
                    } catch (e: Exception) {
                        null
                    }
                }
                
                val listType = object : TypeToken<ArrayList<String>>() {}.type
                val imagePaths = if (imagePathsJson != null) {
                    try {
                        gson.fromJson<ArrayList<String>>(imagePathsJson, listType)
                    } catch (e: Exception) {
                        ArrayList()
                    }
                } else {
                    ArrayList()
                }

                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val formattedUpdateTime = sdf.format(Date(updateTimeMillis))

                memoList.add(Memo(id, title, content, timestamp, formattedUpdateTime, imagePaths))
            }
        }

        db.close()
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
    fun updateMemo(id: Int, title: String, content: String, imagePaths: List<String>) {
        val db = dbHelper.writableDatabase
        try {
            val values = ContentValues().apply {
                put(MemoDatabaseHelper.COLUMN_TITLE, title)
                put(MemoDatabaseHelper.COLUMN_CONTENT, content)
                put(MemoDatabaseHelper.COLUMN_UPDATE_TIME, System.currentTimeMillis())
                put(MemoDatabaseHelper.COLUMN_IMAGE_PATHS, gson.toJson(imagePaths))
            }
            db.update(MemoDatabaseHelper.TABLE_NAME, values, 
                "${MemoDatabaseHelper.COLUMN_ID}=?", arrayOf(id.toString()))
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            db.close()
        }
    }
}
