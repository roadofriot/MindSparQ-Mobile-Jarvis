package com.mindsparq.jarvis.core.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

class DatabaseDriverFactory(private val context: Context) {
    fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(JarvisDatabase.Schema, context, "jarvis.db")
    }

    companion object {
        fun createDatabase(context: Context): JarvisDatabase {
            val driver = DatabaseDriverFactory(context).createDriver()
            return JarvisDatabase(driver)
        }
    }
}

