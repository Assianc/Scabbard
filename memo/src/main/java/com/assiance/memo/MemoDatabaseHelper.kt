package com.assiance.memo

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MemoDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_MEMO_TABLE)
        db.execSQL(CREATE_HISTORY_TABLE)
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
            } catch (_: Exception) {
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
        if (oldVersion < 5) {
            try {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_TITLE_FONT_NAME TEXT DEFAULT 'DEFAULT'")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (oldVersion < 6) {
            try {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_TITLE_STYLE INTEGER DEFAULT 0")
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_CONTENT_STYLE INTEGER DEFAULT 0")
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_TITLE_UNDERLINE INTEGER DEFAULT 0")
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_CONTENT_UNDERLINE INTEGER DEFAULT 0")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (oldVersion < 7) {
            try {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_TITLE_FONT_SIZE REAL DEFAULT 32")
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_CONTENT_FONT_SIZE REAL DEFAULT 16")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (oldVersion < 9) {
            try {
                val cursor = db.rawQuery("PRAGMA table_info(${TABLE_NAME})", null)
                val columnNames = mutableListOf<String>()
                cursor.use {
                    while (it.moveToNext()) {
                        columnNames.add(it.getString(1))
                    }
                }

                if (!columnNames.contains(COLUMN_TITLE_FONT_SIZE)) {
                    db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_TITLE_FONT_SIZE REAL DEFAULT 32")
                }
                if (!columnNames.contains(COLUMN_CONTENT_FONT_SIZE)) {
                    db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_CONTENT_FONT_SIZE REAL DEFAULT 16")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (oldVersion < 10) {
            try {
                val cursor = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                    arrayOf(HISTORY_TABLE_NAME)
                )
                val tableExists = cursor.count > 0
                cursor.close()

                if (!tableExists) {
                    db.execSQL(CREATE_HISTORY_TABLE)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private const val DATABASE_NAME = "memo.db"
        private const val DATABASE_VERSION = 11

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
        const val COLUMN_TITLE_FONT_NAME = "title_font_name"
        const val COLUMN_TITLE_STYLE = "title_style"
        const val COLUMN_CONTENT_STYLE = "content_style"
        const val COLUMN_TITLE_UNDERLINE = "title_underline"
        const val COLUMN_CONTENT_UNDERLINE = "content_underline"
        const val COLUMN_TITLE_FONT_SIZE = "title_font_size"
        const val COLUMN_CONTENT_FONT_SIZE = "content_font_size"

        // 添加历史记录表相关常量
        const val HISTORY_TABLE_NAME = "memo_history"
        const val COLUMN_MEMO_ID = "memo_id"
        const val COLUMN_OLD_TITLE = "old_title"
        const val COLUMN_OLD_CONTENT = "old_content"
        const val COLUMN_OLD_IMAGE_PATHS = "old_image_paths"
        const val COLUMN_MODIFY_TIME = "modify_time"

        // 创建表语句 - 移除 const 修饰符
        private val CREATE_MEMO_TABLE = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT,
                $COLUMN_CONTENT TEXT,
                $COLUMN_TIMESTAMP TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                $COLUMN_UPDATE_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                $COLUMN_IMAGE_PATHS TEXT,
                $COLUMN_FONT_NAME TEXT DEFAULT 'DEFAULT',
                $COLUMN_TITLE_FONT_NAME TEXT DEFAULT 'DEFAULT',
                $COLUMN_TITLE_STYLE INTEGER DEFAULT 0,
                $COLUMN_CONTENT_STYLE INTEGER DEFAULT 0,
                $COLUMN_TITLE_UNDERLINE INTEGER DEFAULT 0,
                $COLUMN_CONTENT_UNDERLINE INTEGER DEFAULT 0,
                $COLUMN_TITLE_FONT_SIZE REAL DEFAULT 32,
                $COLUMN_CONTENT_FONT_SIZE REAL DEFAULT 16
            )
        """.trimIndent()

        // 添加创建历史记录表的 SQL 语句 - 移除 const 修饰符
        private val CREATE_HISTORY_TABLE = """
            CREATE TABLE $HISTORY_TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_MEMO_ID INTEGER,
                $COLUMN_OLD_TITLE TEXT,
                $COLUMN_OLD_CONTENT TEXT,
                $COLUMN_OLD_IMAGE_PATHS TEXT,
                $COLUMN_MODIFY_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY($COLUMN_MEMO_ID) REFERENCES $TABLE_NAME($COLUMN_ID) ON DELETE CASCADE
            )
        """.trimIndent()
    }
}
