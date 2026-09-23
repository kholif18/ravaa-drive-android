package com.ravaa.drive.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** Cache metadata file/folder untuk mode offline (isi file = fase berikutnya). */
@Entity(tableName = "cached_items")
data class CachedItem(
    @PrimaryKey val id: String,
    val name: String,
    val kind: String, // "file" | "folder"
    val mimeType: String? = null,
    val size: Long = 0L,
    val folderId: String? = null,
    val parentId: String? = null,
    val isStarred: Boolean = false,
    val isTrashed: Boolean = false,
    val updatedAt: String? = null,
    val cachedAt: Long = System.currentTimeMillis()
)

/** Antrean operasi offline (outbox): diputar ulang saat online, FIFO. */
@Entity(tableName = "pending_ops")
data class PendingOp(
    @PrimaryKey(autoGenerate = true) val seq: Long = 0,
    val op: String, // STAR | RENAME | TRASH | UNTRASH
    val itemId: String,
    val payload: String? = null, // JSON kecil, mis. {"name":"..."} / {"starred":true}
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface CacheDao {
    @Query("SELECT * FROM cached_items WHERE folderId IS :fid AND isTrashed = 0 ORDER BY kind DESC, name ASC")
    fun itemsIn(fid: String?): Flow<List<CachedItem>>

    @Query("SELECT * FROM cached_items WHERE isTrashed = 0 AND isStarred = 1 ORDER BY name ASC")
    fun starred(): Flow<List<CachedItem>>

    @Query("SELECT * FROM cached_items WHERE isTrashed = 0 ORDER BY updatedAt DESC LIMIT 50")
    fun recent(): Flow<List<CachedItem>>

    @Query("SELECT * FROM cached_items WHERE isTrashed = 1 ORDER BY updatedAt DESC")
    fun trash(): Flow<List<CachedItem>>

    @Query("SELECT * FROM cached_items WHERE isTrashed = 0 AND name LIKE '%' || :q || '%' ORDER BY name ASC LIMIT 50")
    fun search(q: String): Flow<List<CachedItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CachedItem>)

    @Query("UPDATE cached_items SET isStarred = :starred WHERE id = :id")
    suspend fun setStarred(id: String, starred: Boolean)

    @Query("UPDATE cached_items SET name = :name WHERE id = :id")
    suspend fun rename(id: String, name: String)

    @Query("UPDATE cached_items SET isTrashed = :trashed WHERE id = :id")
    suspend fun setTrashed(id: String, trashed: Boolean)

    @Query("DELETE FROM cached_items WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM cached_items")
    suspend fun clear()
}

@Dao
interface PendingOpDao {
    @Query("SELECT * FROM pending_ops ORDER BY seq ASC")
    suspend fun all(): List<PendingOp>

    @Insert
    suspend fun add(op: PendingOp)

    @Query("DELETE FROM pending_ops WHERE seq = :seq")
    suspend fun remove(seq: Long)

    @Query("SELECT COUNT(*) FROM pending_ops")
    fun count(): Flow<Int>
}

@Database(entities = [CachedItem::class, PendingOp::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cache(): CacheDao
    abstract fun outbox(): PendingOpDao
}
