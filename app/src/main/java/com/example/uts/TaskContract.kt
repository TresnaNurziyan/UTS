package com.example.uts

import android.provider.BaseColumns

object TaskContract {
    const val DB_NAME = "com.aziflaj.todolist.db"
    const val DB_VERSION = 1

    object TaskEntry : BaseColumns {
        val TABLE = "tasks"
        val COL_TASK_TITLE = "title"
        val _ID = BaseColumns._ID
    }
}