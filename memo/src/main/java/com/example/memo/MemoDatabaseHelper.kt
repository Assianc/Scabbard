package com.example.memo

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MemoDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_UPDATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE $TABLE_NAME RENAME COLUMN $COLUMN_IMAGE_PATH TO $COLUMN_IMAGE_PATHS")
            } catch (e: Exception) {
                try {
                    db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_IMAGE_PATHS TEXT")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        if (oldVersion < 4) {
            try {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_FONT_NAME TEXT DEFAULT 'DEFAULT'")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private const val DATABASE_NAME = "memo.db"
        private const val DATABASE_VERSION = 4

        // 表和列名
        const val TABLE_NAME = "memo"
        const val COLUMN_ID = "_id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_CONTENT = "content"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_UPDATE_TIME = "update_time"
        const val COLUMN_IMAGE_PATH = "image_path"
        const val COLUMN_IMAGE_PATHS = "image_paths"
        const val COLUMN_FONT_NAME = "font_name"

        // 创建表语句
        val CREATE_TABLE = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT,
                $COLUMN_CONTENT TEXT,
                $COLUMN_TIMESTAMP TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                $COLUMN_UPDATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                $COLUMN_IMAGE_PATHS TEXT,
                $COLUMN_FONT_NAME TEXT DEFAULT 'DEFAULT'
            )
        """.trimIndent()
    }
}
