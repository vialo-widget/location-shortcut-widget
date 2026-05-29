package com.vialo.app.data.repo

import com.vialo.app.data.db.ShortcutDao
import com.vialo.app.data.db.toDomain
import com.vialo.app.data.db.toEntity
import com.vialo.app.data.model.Shortcut
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single-writer repository over [ShortcutDao]. The Compose layer collects
 * [shortcuts] as state; CRUD operations are suspend functions launched from
 * ViewModels.
 *
 * Domain↔entity mapping lives in [com.vialo.app.data.db] (see ShortcutEntity.kt)
 * so the rest of the app never touches Room types.
 */
class ShortcutRepository(private val dao: ShortcutDao) {

    val shortcuts: Flow<List<Shortcut>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun get(id: String): Shortcut? = dao.findById(id)?.toDomain()

    /** Snapshot of every stored shortcut, sort-order ascending. Used by the
     *  duplicate-check helpers that need to see the whole set at a point in
     *  time (cheap — the table is bounded to a handful of rows). */
    suspend fun list(): List<Shortcut> = dao.getAll().map { it.toDomain() }

    suspend fun add(shortcut: Shortcut) = dao.insert(shortcut.toEntity())

    suspend fun addAll(shortcuts: List<Shortcut>) =
        dao.insertAll(shortcuts.map { it.toEntity() })

    suspend fun update(shortcut: Shortcut) = dao.update(shortcut.toEntity())

    suspend fun delete(id: String) = dao.deleteById(id)

    suspend fun reorder(orderedIds: List<String>) = dao.applyOrdering(orderedIds)

    /** Returns the number of rows removed. */
    suspend fun pruneExpired(nowMillis: Long = System.currentTimeMillis()): Int =
        dao.deleteExpired(nowMillis)

    suspend fun count(): Int = dao.count()
}
