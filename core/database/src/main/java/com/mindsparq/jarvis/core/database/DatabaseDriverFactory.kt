package com.mindsparq.jarvis.core.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

class DatabaseDriverFactory(private val context: Context) {
    fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = JarvisDatabase.Schema,
            context = context,
            name = "jarvis.db",
            callback = object : AndroidSqliteDriver.Callback(JarvisDatabase.Schema) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.setForeignKeyConstraintsEnabled(true)
                }
            }
        )
    }

    companion object {
        fun createDatabase(context: Context): JarvisDatabase {
            val driver = DatabaseDriverFactory(context).createDriver()
            return JarvisDatabase(driver)
        }
    }
}
