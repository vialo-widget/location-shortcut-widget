package com.vialo.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ShortcutEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class VialoDatabase : RoomDatabase() {
    abstract fun shortcutDao(): ShortcutDao

    companion object {
        fun build(context: Context): VialoDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                VialoDatabase::class.java,
                "vialo.db",
            ).build()
    }
}
