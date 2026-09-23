package com.ravaa.drive.presentation.upload

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravaa.drive.data.api.DriveApi
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.util.UUID
import javax.inject.Inject
import kotlin.math.min

data class UploadItem(val id: String, val name: String, val size: Long, val progress: Int = 0, val status: String = "pending", val error: String? = null)

private const val SINGLE_LIMIT = 10L * 1024 * 1024
private const val CHUNK_SIZE = 2 * 1024 * 1024

/**
 * Upload coroutine-based: ≤10MB sekali jalan, selebihnya chunked 2MB resumable
 * (offset ditanya dulu ke GET /api/files/chunk). Pause/resume/cancel per file.
 */
@HiltViewModel
class UploadViewModel @Inject constructor(
    private val api: DriveApi,
    @ApplicationContext private val ctx: Context
) : ViewModel() {
    private val _uploads = MutableStateFlow<List<UploadItem>>(emptyList())
    val uploads = _uploads.asStateFlow()

    private val jobs = mutableMapOf<String, Job>()
    private val uris = mutableMapOf<String, Uri>()
    private val folderIds = mutableMapOf<String, String?>()
    private val paused = mutableSetOf<String>()
    private val cancelled = mutableSetOf<String>()

    fun addFiles(files: List<UploadItem>) { _uploads.value = _uploads.value + files }

    /** Masukkan antrean dari file picker. folderId null = root. */
    fun enqueue(uri: Uri, folderId: String? = null) {
        val name = queryName(uri) ?: "upload-${System.currentTimeMillis()}"
        val size = querySize(uri)
        val id = UUID.randomUUID().toString()
        uris[id] = uri
        folderIds[id] = folderId
        _uploads.value = _uploads.value + UploadItem(id, name, size)
        start(id)
    }

    fun pause(id: String) {
        paused.add(id)
        jobs[id]?.cancel()
        set(id) { it.copy(status = "paused") }
    }

    fun resume(id: String) {
        val cur = _uploads.value.firstOrNull { it.id == id } ?: return
        if (cur.status != "paused" && cur.status != "error") return
        set(id) { it.copy(status = "pending") }
        start(id)
    }

    fun cancelAll() {
        val ids = _uploads.value.map { it.id }
        cancelled.addAll(ids)
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        uris.keys.removeAll(ids)
        folderIds.keys.removeAll(ids)
        paused.clear()
        _uploads.value = _uploads.value.filter { it.status == "success" || it.status == "error" }
    }

    private fun start(id: String) {
        jobs[id]?.cancel()
        paused.remove(id)
        cancelled.remove(id)
        set(id) { it.copy(status = "uploading") }
        // I/O file + chunk di IO thread (Main = ANR untuk file besar)
        jobs[id] = viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = uris[id] ?: throw IllegalStateException("file hilang")
                val item = _uploads.value.firstOrNull { it.id == id } ?: return@launch
                android.util.Log.d("Upload", "start ${item.name} size=${item.size}")
                if (item.size in 1..SINGLE_LIMIT) uploadSingle(id, uri, item)
                else uploadChunked(id, uri, item)
                if (isActive && id !in cancelled) {
                    android.util.Log.d("Upload", "success ${item.name}")
                    set(id) { it.copy(progress = 100, status = "success", error = null) }
                    // Animasi selesai — hilangkan kartu setelah 3 detik
                    viewModelScope.launch {
                        delay(3000)
                        _uploads.value = _uploads.value.filter { it.id != id || it.status != "success" }
                        uris.remove(id)
                        folderIds.remove(id)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = if (e is HttpException && e.code() == 401) "Sesi habis, login ulang" else (e.message ?: "Error")
                android.util.Log.e("Upload", "failed: $msg", e)
                set(id) { it.copy(status = "error", error = msg) }
            } finally {
                jobs.remove(id)
            }
        }
    }

    private suspend fun uploadSingle(id: String, uri: Uri, item: UploadItem) {
        val mime = ctx.contentResolver.getType(uri) ?: "application/octet-stream"
        val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("tidak bisa dibaca")
        val part = MultipartBody.Part.createFormData("file", item.name, bytes.toRequestBody(mime.toMediaType()))
        val folderPart = folderIds[id]?.let { it.toRequestBody("text/plain".toMediaType()) }
        val res = api.upload(part, folderPart)
        if (!res.success) throw IllegalStateException(res.error ?: "Upload gagal")
    }

    private suspend fun uploadChunked(id: String, uri: Uri, item: UploadItem) {
        val mime = ctx.contentResolver.getType(uri) ?: "application/octet-stream"
        val uploadId = UUID.randomUUID().toString()
        val totalChunks = ((item.size + CHUNK_SIZE - 1) / CHUNK_SIZE).toInt().coerceAtLeast(1)
        var offset = try { api.chunkStatus(uploadId).data?.offset ?: 0L } catch (e: Exception) { 0L }
        var index = (offset / CHUNK_SIZE).toInt().coerceIn(0, totalChunks - 1)
        val folderPart = folderIds[id]?.let { it.toRequestBody("text/plain".toMediaType()) }
        val namePart = item.name.toRequestBody("text/plain".toMediaType())
        val idPart = uploadId.toRequestBody("text/plain".toMediaType())

        ctx.contentResolver.openInputStream(uri)?.use { stream ->
            var skipped = 0L
            while (skipped < offset) skipped += stream.skip(offset - skipped)
            val buf = ByteArray(CHUNK_SIZE)
            while (index < totalChunks) {
                if (id in paused || id in cancelled) {
                    set(id) { it.copy(status = if (id in paused) "paused" else it.status) }
                    return
                }
                var read = 0
                while (read < CHUNK_SIZE) {
                    val n = stream.read(buf, read, CHUNK_SIZE - read)
                    if (n == -1) break
                    read += n
                }
                if (read <= 0) break
                val chunkBytes = buf.copyOf(read)
                val part = MultipartBody.Part.createFormData("chunk", item.name, chunkBytes.toRequestBody(mime.toMediaType()))
                val res = api.uploadChunk(
                    part, idPart,
                    index.toString().toRequestBody("text/plain".toMediaType()),
                    totalChunks.toString().toRequestBody("text/plain".toMediaType()),
                    namePart, folderPart
                )
                if (!res.success) throw IllegalStateException(res.error ?: "Chunk $index gagal")
                offset += read
                index++
                val pct = min(99, ((offset * 100) / item.size.coerceAtLeast(1)).toInt())
                set(id) { it.copy(progress = pct) }
            }
        } ?: throw IllegalStateException("tidak bisa dibaca")
    }

    private fun set(id: String, f: (UploadItem) -> UploadItem) {
        _uploads.value = _uploads.value.map { if (it.id == id) f(it) else it }
    }

    private fun queryName(uri: Uri): String? {
        ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0) return c.getString(i)
            }
        }
        return uri.lastPathSegment?.substringAfterLast("/")
    }

    private fun querySize(uri: Uri): Long {
        ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val i = c.getColumnIndex(OpenableColumns.SIZE)
                if (i >= 0) return c.getLong(i).coerceAtLeast(0L)
            }
        }
        return 0L
    }
}
