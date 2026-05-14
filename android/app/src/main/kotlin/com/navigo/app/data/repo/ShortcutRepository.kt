package com.navigo.app.data.repo

import com.navigo.app.data.db.ShortcutDao
import com.navigo.app.data.db.toDomain
import com.navigo.app.data.db.toEntity
import com.navigo.app.data.model.Shortcut
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single-writer repository over [ShortcutDao]. The Compose layer collects
 * [shortcuts] as state; CRUD operations are suspend functions launched from
 * ViewModels.
 *
 * Domain↔entity mapping lives in [com.navigo.app.data.db] (see ShortcutEntity.kt)
 * so the rest of the app never touches Room types.
 */
class ShortcutRepository(private val dao: ShortcutDao) {

    val shortcuts: Flow<List<Shortcut>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun get(id: String): Shortcut? = dao.findById(id)?.toDomain()

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
