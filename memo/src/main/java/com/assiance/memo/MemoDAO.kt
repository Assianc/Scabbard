package com.assiance.memo

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
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
                } catch (_: Exception) {
                    try {
                        val oldPath = it.getString(it.getColumnIndexOrThrow(MemoDatabaseHelper.COLUMN_IMAGE_PATH))
                        if (oldPath != null) "[\"$oldPath\"]" else null
                    } catch (_: Exception) {
                        null
                    }
                }
                
                val listType = object : TypeToken<ArrayList<String>>() {}.type
                val imagePaths = if (imagePathsJson != null) {
                    try {
                        gson.fromJson<ArrayList<String>>(imagePathsJson, listType)
                    } catch (_: Exception) {
                        ArrayList()
                    }
                } else {
                    ArrayList()
                }

                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val formattedUpdateTime = sdf.format(Date(updateTimeMillis))

                val fontName = try {
                    it.getString(it.getColumnIndexOrThrow(MemoDatabaseHelper.COLUMN_FONT_NAME))
                } catch (_: Exception) {
                    "DEFAULT"
                }

                val titleFontName = try {
                    it.getString(it.getColumnIndexOrThrow(MemoDatabaseHelper.COLUMN_TITLE_FONT_NAME))
                } catch (_: Exception) {
                    "DEFAULT"
                }

                val titleStyle = try {
                    it.getInt(it.getColumnIndexOrThrow(MemoDatabaseHelper.COLUMN_TITLE_STYLE))
                } catch (_: Exception) { 0 }
                
                val contentStyle = try {
                    it.getInt(it.getColumnIndexOrThrow(MemoDatabaseHelper.COLUMN_CONTENT_STYLE))
                } catch (_: Exception) { 0 }
                
                val titleUnderline = try {
                    it.getInt(it.getColumnIndexOrThrow(MemoDatabaseHelper.COLUMN_TITLE_UNDERLINE)) == 1
                } catch (_: Exception) { false }
                
                val contentUnderline = try {
                    it.getInt(it.getColumnIndexOrThrow(MemoDatabaseHelper.COLUMN_CONTENT_UNDERLINE)) == 1
                } catch (_: Exception) { false }

                val titleFontSize = try {
                    it.getFloat(it.getColumnIndexOrThrow(MemoDatabaseHelper.COLUMN_TITLE_FONT_SIZE))
                } catch (_: Exception) { 32f }

                val contentFontSize = try {
                    it.getFloat(it.getColumnIndexOrThrow(MemoDatabaseHelper.COLUMN_CONTENT_FONT_SIZE))
                } catch (_: Exception) { 16f }

                memoList.add(Memo(
                    id = id,
                    title = title,
                    content = content,
                    timestamp = timestamp,
                    updateTime = formattedUpdateTime,
                    imagePaths = imagePaths,
                    fontName = fontName,
                    titleFontName = titleFontName,
                    titleStyle = titleStyle,
                    contentStyle = contentStyle,
                    titleUnderline = titleUnderline,
                    contentUnderline = contentUnderline,
                    titleFontSize = titleFontSize,
                    contentFontSize = contentFontSize
                ))
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
    fun updateMemo(
        id: Int,
        title: String,
        content: String,
        imagePaths: List<String>,
        fontName: String = "DEFAULT",
        titleFontName: String = "DEFAULT",
        titleStyle: Int = 0,
        contentStyle: Int = 0,
        titleUnderline: Boolean = false,
        contentUnderline: Boolean = false,
        titleFontSize: Float = 32f,
        contentFontSize: Float = 16f
    ) {
        val db = dbHelper.writableDatabase
        try {
            val values = ContentValues().apply {
                put(MemoDatabaseHelper.COLUMN_TITLE, title)
                put(MemoDatabaseHelper.COLUMN_CONTENT, content)
                put(MemoDatabaseHelper.COLUMN_UPDATE_TIME, System.currentTimeMillis())
                put(MemoDatabaseHelper.COLUMN_IMAGE_PATHS, gson.toJson(imagePaths))
                put(MemoDatabaseHelper.COLUMN_FONT_NAME, fontName)
                put(MemoDatabaseHelper.COLUMN_TITLE_FONT_NAME, titleFontName)
                put(MemoDatabaseHelper.COLUMN_TITLE_STYLE, titleStyle)
                put(MemoDatabaseHelper.COLUMN_CONTENT_STYLE, contentStyle)
                put(MemoDatabaseHelper.COLUMN_TITLE_UNDERLINE, if (titleUnderline) 1 else 0)
                put(MemoDatabaseHelper.COLUMN_CONTENT_UNDERLINE, if (contentUnderline) 1 else 0)
                put(MemoDatabaseHelper.COLUMN_TITLE_FONT_SIZE, titleFontSize)
                put(MemoDatabaseHelper.COLUMN_CONTENT_FONT_SIZE, contentFontSize)
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
