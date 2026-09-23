package com.ravaa.drive.data.repository

import com.ravaa.drive.data.api.DriveApi
import com.ravaa.drive.data.api.DriveFile
import com.ravaa.drive.data.api.DriveFolder
import com.ravaa.drive.data.db.AppDatabase
import com.ravaa.drive.data.db.CachedItem
import com.ravaa.drive.data.db.PendingOp
import com.ravaa.drive.data.sync.ConnectivityObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first untuk metadata Drive.
 * - Baca: selalu dari cache Room (langsung tampil, offline OK).
 * - Tulis saat online: API dulu, lalu cache. Saat offline/gagal: cache optimistis + antre ke outbox.
 * - Sync: putar ulang outbox FIFO, lalu pull ulang dari server (server menang).
 */
@Singleton
class DriveRepository @Inject constructor(
    private val api: DriveApi,
    private val db: AppDatabase,
    private val connectivity: ConnectivityObserver
) {
    fun isOnline(): Boolean = connectivity.isOnlineNow()
    fun observeOnline(): Flow<Boolean> = connectivity.isOnline

    val pendingCount: Flow<Int> = db.outbox().count()

    // ---------- Baca (cache) ----------

    fun itemsIn(folderId: String?): Flow<List<Any>> =
        db.cache().itemsIn(folderId).map { it.map(::toModel) }

    fun starred(): Flow<List<Any>> =
        db.cache().starred().map { it.map(::toModel) }

    fun recent(): Flow<List<Any>> =
        db.cache().recent().map { it.map(::toModel) }

    fun trash(): Flow<List<Any>> =
        db.cache().trash().map { it.map(::toModel) }

    fun searchOffline(q: String): Flow<List<Any>> =
        db.cache().search(q).map { it.map(::toModel) }

    private fun toModel(c: CachedItem): Any =
        if (c.kind == "folder") {
            DriveFolder(c.id, c.name, c.parentId, null, c.updatedAt)
        } else {
            DriveFile(c.id, c.name, null, c.size, c.folderId, null, c.isStarred, c.updatedAt, null)
        }

    // ---------- Pull (server → cache) ----------

    /** Pull satu folder (null = root). Throw bila offline/gagal. */
    suspend fun pullFolder(folderId: String?) {
        val res = api.listFiles(folderId)
        if (!res.success) throw IllegalStateException(res.error ?: "Gagal memuat")
        val items = mutableListOf<CachedItem>()
        res.data?.folders?.forEach { f ->
            items.add(CachedItem(f.id, f.name, "folder", null, 0L, null, f.parentId, false, false, f.updatedAt))
        }
        res.data?.files?.forEach { f ->
            items.add(CachedItem(f.id, f.name, "file", f.mimeType, f.fileSize, f.folderId, null, f.isStarred, false, f.updatedAt))
        }
        db.cache().upsertAll(items)
    }

    suspend fun pullTrash() {
        val res = api.trash()
        if (!res.success) throw IllegalStateException(res.error ?: "Gagal memuat trash")
        val items = mutableListOf<CachedItem>()
        res.data?.folders?.forEach { f ->
            items.add(CachedItem(f.id, f.name, "folder", null, 0L, null, f.parentId, false, true, f.updatedAt))
        }
        res.data?.files?.forEach { f ->
            items.add(CachedItem(f.id, f.name, "file", f.mimeType, f.fileSize, f.folderId, null, f.isStarred, true, f.updatedAt))
        }
        db.cache().upsertAll(items)
    }

    suspend fun pullStarred() {
        val res = api.starred()
        if (!res.success) throw IllegalStateException(res.error ?: "Gagal memuat")
        val items = mutableListOf<CachedItem>()
        res.data?.folders?.forEach { f ->
            items.add(CachedItem(f.id, f.name, "folder", null, 0L, null, f.parentId, false, false, f.updatedAt))
        }
        res.data?.files?.forEach { f ->
            items.add(CachedItem(f.id, f.name, "file", f.mimeType, f.fileSize, f.folderId, null, true, false, f.updatedAt))
        }
        db.cache().upsertAll(items)
    }

    // ---------- Tulis ----------

    suspend fun setStarred(id: String, starred: Boolean) {
        if (isOnline()) {
            val res = api.updateFile(id, mapOf("isStarred" to starred))
            if (!res.success) throw IllegalStateException(res.error ?: "Gagal")
            db.cache().setStarred(id, starred)
        } else {
            db.cache().setStarred(id, starred)
            db.outbox().add(PendingOp(op = "STAR", itemId = id, payload = "{\"starred\":$starred}"))
        }
    }

    suspend fun rename(id: String, name: String, kind: String) {
        if (isOnline()) {
            val res = if (kind == "folder") api.renameFolder(id, mapOf("name" to name))
            else api.updateFile(id, mapOf("name" to name))
            if (!res.success) throw IllegalStateException(res.error ?: "Gagal")
            db.cache().rename(id, name)
        } else {
            db.cache().rename(id, name)
            db.outbox().add(PendingOp(op = "RENAME", itemId = id, payload = JSONObject().put("name", name).put("kind", kind).toString()))
        }
    }

    suspend fun trash(id: String, kind: String) {
        if (isOnline()) {
            val res = if (kind == "folder") api.deleteFolder(id)
            else api.updateFile(id, mapOf("isTrashed" to true))
            if (!res.success) throw IllegalStateException(res.error ?: "Gagal")
            db.cache().setTrashed(id, true)
        } else {
            db.cache().setTrashed(id, true)
            db.outbox().add(PendingOp(op = "TRASH", itemId = id, payload = JSONObject().put("kind", kind).toString()))
        }
    }

    suspend fun restore(id: String) {
        if (!isOnline()) throw IllegalStateException("Offline — hubungkan internet untuk restore")
        val res = api.updateFile(id, mapOf("isTrashed" to false))
        if (!res.success) throw IllegalStateException(res.error ?: "Gagal")
        db.cache().setTrashed(id, false)
    }

    suspend fun deletePermanent(id: String) {
        if (!isOnline()) throw IllegalStateException("Offline — hubungkan internet untuk hapus permanen")
        val res = api.deleteFile(id)
        if (!res.success) throw IllegalStateException(res.error ?: "Gagal")
        db.cache().delete(id)
    }

    // ---------- Sync (didahulukan outbox, lalu pull) ----------

    suspend fun sync() {
        if (!isOnline()) return
        // 1. Putar ulang antrean FIFO
        for (op in db.outbox().all()) {
            try {
                replay(op)
                db.outbox().remove(op.seq)
            } catch (e: Exception) {
                // Berhenti di op pertama yang gagal — coba lagi lain waktu (urutan terjaga)
                break
            }
        }
        // 2. Pull ulang tampilan utama
        try { pullFolder(null) } catch (e: Exception) { }
        try { pullTrash() } catch (e: Exception) { }
    }

    private suspend fun replay(op: PendingOp) {
        val payload = op.payload?.let { JSONObject(it) }
        when (op.op) {
            "STAR" -> {
                val starred = payload?.optBoolean("starred", true) ?: true
                val res = api.updateFile(op.itemId, mapOf("isStarred" to starred))
                if (!res.success) throw IllegalStateException(res.error)
                db.cache().setStarred(op.itemId, starred)
            }
            "RENAME" -> {
                val name = payload?.optString("name", "") ?: ""
                val kind = payload?.optString("kind", "file") ?: "file"
                if (name.isBlank()) return
                val res = if (kind == "folder") api.renameFolder(op.itemId, mapOf("name" to name))
                else api.updateFile(op.itemId, mapOf("name" to name))
                if (!res.success) throw IllegalStateException(res.error)
                db.cache().rename(op.itemId, name)
            }
            "TRASH" -> {
                val kind = payload?.optString("kind", "file") ?: "file"
                val res = if (kind == "folder") api.deleteFolder(op.itemId)
                else api.updateFile(op.itemId, mapOf("isTrashed" to true))
                if (!res.success) throw IllegalStateException(res.error)
                db.cache().setTrashed(op.itemId, true)
            }
        }
    }
}
