package com.vialo.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ShortcutEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class VialoDatabase : RoomDatabase() {
    abstract fun shortcutDao(): ShortcutDao

    companion object {
        /** v1 → v2: add `transportMode` column. Existing rows default to
         *  DRIVE, matching the old single-mode behaviour. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE shortcuts ADD COLUMN transportMode TEXT NOT NULL DEFAULT 'DRIVE'",
                )
            }
        }

        fun build(context: Context): VialoDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                VialoDatabase::class.java,
                "vialo.db",
            )
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
