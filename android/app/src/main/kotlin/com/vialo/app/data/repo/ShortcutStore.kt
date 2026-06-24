package com.vialo.app.data.repo

import com.vialo.app.data.model.Shortcut
import com.vialo.app.data.model.toDomain
import com.vialo.app.data.model.toRemote
import com.vialo.app.data.pairing.ApiResult
import com.vialo.app.data.pairing.VialoApi
import com.vialo.app.service.notification.ExpiryNotifier
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Persistence façade used by Add / Edit ViewModels.
 *
 * Two implementations:
 *   - [LocalShortcutStore] writes to the user's own [ShortcutRepository] and
 *     manages local expiry notifications. Used by the existing Home flow.
 *   - [RemoteShortcutStore] mirrors mutations to a paired caree's snapshot
 *     via [VialoApi] PATCH calls. Used by the carer-side Add / Edit
 *     screens.
 *
 * Keeping both behind one interface lets the existing Add / Edit screens
 * stay structurally unchanged — the only difference is which store is
 * injected at the nav-host level.
 */
interface ShortcutStore {
    suspend fun list(): List<Shortcut>
    suspend fun get(id: String): Shortcut?
    suspend fun add(shortcut: Shortcut)
    suspend fun update(shortcut: Shortcut)
    suspend fun delete(id: String)
}

class LocalShortcutStore(
    private val repo: ShortcutRepository,
    private val notifier: ExpiryNotifier,
) : ShortcutStore {

    override suspend fun list(): List<Shortcut> = repo.list()
    override suspend fun get(id: String): Shortcut? = repo.get(id)

    override suspend fun add(shortcut: Shortcut) {
        repo.add(shortcut)
        notifier.schedule(shortcut)
    }

    override suspend fun update(shortcut: Shortcut) {
        repo.update(shortcut)
        // The schedule() impl cancels any prior alarm for the same id before
        // re-arming, so an explicit cancel here would be redundant.
        notifier.schedule(shortcut)
    }

    override suspend fun delete(id: String) {
        repo.delete(id)
        notifier.cancel(id)
    }
}

/**
 * Mutates a paired caree's snapshot on the server. Holds an in-memory cache
 * loaded lazily on first call so duplicate checks (which want a full list)
 * don't pay a round-trip for every keystroke after the first.
 *
 * Each mutation rewrites the whole list — matches the server's last-write-
 * wins contract on `caree_state.shortcuts_json`. The server fans out a
 * `shortcut_changed` push to the caree and other carers; this device's
 * UI updates optimistically and skips the push (chunk 4's FCM handler
 * is keyed on caree_device_id ≠ self for refresh logic).
 *
 * Not thread-safe across instances — each carer-side screen creates its
 * own. The internal mutex serialises mutations on a single instance so
 * back-to-back saves don't trample each other's snapshot.
 */
class RemoteShortcutStore(
    private val api: VialoApi,
    private val careeDeviceId: String,
) : ShortcutStore {

    private val cache: MutableList<Shortcut> = mutableListOf()
    private var loaded: Boolean = false
    private val mutex = Mutex()

    private suspend fun loadIfNeeded() {
        if (loaded) return
        when (val r = api.getCareeState(careeDeviceId)) {
            is ApiResult.Success -> {
                cache.clear()
                cache.addAll(r.value.shortcuts.map { it.toDomain() })
                loaded = true
            }
            is ApiResult.Failure ->
                throw RemoteStoreException("couldn't load: ${r.message}")
        }
    }

    override suspend fun list(): List<Shortcut> = mutex.withLock {
        loadIfNeeded()
        cache.toList()
    }

    override suspend fun get(id: String): Shortcut? = mutex.withLock {
        loadIfNeeded()
        cache.firstOrNull { it.id == id }
    }

    override suspend fun add(shortcut: Shortcut) = mutex.withLock {
        loadIfNeeded()
        cache.add(shortcut)
        push()
    }

    override suspend fun update(shortcut: Shortcut) = mutex.withLock {
        loadIfNeeded()
        val idx = cache.indexOfFirst { it.id == shortcut.id }
        if (idx >= 0) cache[idx] = shortcut else cache.add(shortcut)
        push()
    }

    override suspend fun delete(id: String) = mutex.withLock {
        loadIfNeeded()
        cache.removeAll { it.id == id }
        push()
    }

    private suspend fun push() {
        val r = api.pushCareeShortcuts(careeDeviceId, cache.map { it.toRemote() })
        if (r is ApiResult.Failure) {
            // Roll-back semantics aren't worth it here — the cache is in-
            // memory only, and the next list()/get() through a fresh
            // instance reloads from the server. Propagate so the VM can
            // surface the error to the user.
            throw RemoteStoreException("push failed: ${r.message}")
        }
    }
}

class RemoteStoreException(message: String) : RuntimeException(message)
