package com.vialo.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortcutDao {

    @Query("SELECT * FROM shortcuts ORDER BY sortOrder ASC, createdAtMillis ASC")
    fun observeAll(): Flow<List<ShortcutEntity>>

    @Query("SELECT * FROM shortcuts ORDER BY sortOrder ASC, createdAtMillis ASC")
    suspend fun getAll(): List<ShortcutEntity>

    @Query("SELECT * FROM shortcuts WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ShortcutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ShortcutEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ShortcutEntity>)

    @Update
    suspend fun update(entity: ShortcutEntity)

    @Query("DELETE FROM shortcuts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM shortcuts WHERE expiresAtMillis IS NOT NULL AND expiresAtMillis < :nowMillis")
    suspend fun deleteExpired(nowMillis: Long): Int

    /**
     * Apply a new ordering by id. Missing ids are left alone; unknown ids
     * are ignored — callers don't have to fetch the table first.
     */
    @Transaction
    suspend fun applyOrdering(orderedIds: List<String>) {
        orderedIds.forEachIndexed { index, id -> setSortOrder(id, index) }
    }

    @Query("UPDATE shortcuts SET sortOrder = :order WHERE id = :id")
    suspend fun setSortOrder(id: String, order: Int)

    @Query("SELECT COUNT(*) FROM shortcuts")
    suspend fun count(): Int
}
