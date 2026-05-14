package com.navigo.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ShortcutEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class NaviGoDatabase : RoomDatabase() {
    abstract fun shortcutDao(): ShortcutDao

    companion object {
        fun build(context: Context): NaviGoDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                NaviGoDatabase::class.java,
                "navigo.db",
            ).build()
    }
}
