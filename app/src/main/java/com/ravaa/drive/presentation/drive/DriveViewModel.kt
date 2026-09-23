package com.ravaa.drive.presentation.drive

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.ravaa.drive.data.api.DriveApi
import com.ravaa.drive.data.api.DriveFile
import com.ravaa.drive.data.api.DriveFolder
import com.ravaa.drive.data.api.DriveListResponse
import com.ravaa.drive.data.api.StorageStats
import com.ravaa.drive.data.repository.DriveRepository
import com.ravaa.drive.data.sync.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

private enum class View { FOLDER, STARRED, RECENT, TRASH }

@HiltViewModel
class DriveViewModel @Inject constructor(
    private val api: DriveApi,
    private val repo: DriveRepository,
    val imageLoader: ImageLoader,
    private val baseUrl: String,
    @ApplicationContext private val ctx: Context
) : ViewModel() {
    fun thumbUrl(id: String): String = "${baseUrl}api/files/$id/thumb"

    private val _files = MutableStateFlow<List<Any>>(emptyList())
    val files = _files.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    /** null = ok; "SESSION_EXPIRED" = 401 → arahkan ke login; lainnya = pesan error. */
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    /** Stack folder untuk breadcrumb (kosong = root). */
    private val _stack = MutableStateFlow<List<DriveFolder>>(emptyList())
    val stack = _stack.asStateFlow()

    private val _storage = MutableStateFlow<StorageStats?>(null)
    val storage = _storage.asStateFlow()

    /** true = offline (tampilkan banner + baca cache). */
    private val _offline = MutableStateFlow(false)
    val isOffline = _offline.asStateFlow()

    val pendingCount = repo.pendingCount

    fun clearError() { _error.value = null }

    private var view: View = View.FOLDER
    private var observeJob: Job? = null

    init {
        // Pantau koneksi: kembali online → sync antrean otomatis
        viewModelScope.launch {
            repo.observeOnline().collectLatest { online ->
                _offline.value = !online
                if (online) {
                    try { repo.sync() } catch (e: Exception) { }
                }
            }
        }
        observe()
        load(null)
        loadStorage()
    }

    private fun applyList(res: DriveListResponse) {
        if (res.success) {
            _files.value = (res.data?.folders ?: emptyList()) + (res.data?.files ?: emptyList())
            _error.value = null
        } else {
            _error.value = res.error ?: "Gagal memuat"
        }
    }

    private fun applyFailure(e: Exception) {
        _error.value = if (e is HttpException && e.code() == 401) "SESSION_EXPIRED" else e.message ?: "Error"
    }

    private fun observe() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            val flow = when (view) {
                View.FOLDER -> repo.itemsIn(currentFolderId())
                View.STARRED -> repo.starred()
                View.RECENT -> repo.recent()
                View.TRASH -> repo.trash()
            }
            flow.catch { applyFailure(Exception(it.message)) }.collectLatest { _files.value = it }
        }
    }

    /** Isi folder (null = root). Baca cache dulu, lalu refresh bila online. */
    fun load(folderId: String? = null) {
        view = View.FOLDER
        observe()
        viewModelScope.launch {
            _loading.value = true
            try {
                repo.pullFolder(folderId)
            } catch (e: Exception) {
                if (!repo.isOnline()) _offline.value = true
                else applyFailure(e)
            }
            _loading.value = false
        }
    }

    fun search(q: String) {
        viewModelScope.launch {
            if (q.trim().length < 2) { load(null); return@launch }
            _loading.value = true
            try {
                if (repo.isOnline()) applyList(api.search(q.trim()))
                else {
                    repo.searchOffline(q.trim()).collectLatest { _files.value = it }
                    _offline.value = true
                }
            } catch (e: Exception) { applyFailure(e) }
            _loading.value = false
        }
    }

    fun loadStarred() {
        view = View.STARRED
        observe()
        viewModelScope.launch {
            _loading.value = true
            try { repo.pullStarred() } catch (e: Exception) {
                if (!repo.isOnline()) _offline.value = true else applyFailure(e)
            }
            _loading.value = false
        }
    }

    fun loadRecent() {
        view = View.RECENT
        observe()
        _loading.value = false
    }

    fun loadShared() {
        viewModelScope.launch {
            _loading.value = true
            try { applyList(api.shared()) } catch (e: Exception) { applyFailure(e) }
            _loading.value = false
        }
    }

    fun loadTrash() {
        view = View.TRASH
        observe()
        viewModelScope.launch {
            _loading.value = true
            try { repo.pullTrash() } catch (e: Exception) {
                if (!repo.isOnline()) _offline.value = true else applyFailure(e)
            }
            _loading.value = false
        }
    }

    fun loadPhotos() {
        viewModelScope.launch {
            _loading.value = true
            try { applyList(api.photos()) } catch (e: Exception) { applyFailure(e) }
            _loading.value = false
        }
    }

    fun createFolder(name: String, parentId: String? = null) {
        viewModelScope.launch {
            if (!repo.isOnline()) {
                _error.value = "Offline — hubungkan internet untuk buat folder"
                return@launch
            }
            _loading.value = true
            try {
                val res = api.createFolder(mapOf("name" to name, "parentId" to parentId))
                if (res.success) {
                    res.data?.let {
                        // Masukkan langsung ke cache agar langsung terlihat
                    }
                    load(currentFolderId())
                } else _error.value = res.error
            } catch (e: Exception) { applyFailure(e) }
            _loading.value = false
        }
    }

    /** Navigasi folder (breadcrumb). */
    fun openFolder(folder: DriveFolder) {
        _stack.value = _stack.value + folder
        load(folder.id)
    }

    fun openCrumb(index: Int) {
        // index -1 = root
        _stack.value = if (index < 0) emptyList() else _stack.value.take(index + 1)
        load(currentFolderId())
    }

    fun currentFolderId(): String? = _stack.value.lastOrNull()?.id

    fun refresh() {
        SyncWorker.syncNow(ctx)
        load(currentFolderId())
        loadStorage()
    }

    fun loadStorage() {
        viewModelScope.launch {
            try {
                val res = api.storageStats()
                if (res.success) _storage.value = res.data
            } catch (e: Exception) { /* storage bar opsional, jangan ganggu UI */ }
        }
    }

    fun toggleStar(file: DriveFile) {
        viewModelScope.launch {
            try {
                repo.setStarred(file.id, !file.isStarred)
            } catch (e: Exception) { applyFailure(e) }
        }
    }

    fun renameFile(id: String, name: String) {
        viewModelScope.launch {
            try { repo.rename(id, name, "file") } catch (e: Exception) { applyFailure(e) }
        }
    }

    fun trashFile(id: String) {
        viewModelScope.launch {
            try { repo.trash(id, "file") } catch (e: Exception) { applyFailure(e) }
        }
    }

    /** Folder: DELETE = pindah ke trash (perilaku Google Drive). */
    fun trashFolder(id: String) {
        viewModelScope.launch {
            try { repo.trash(id, "folder") } catch (e: Exception) { applyFailure(e) }
        }
    }

    fun renameFolder(id: String, name: String) {
        viewModelScope.launch {
            try { repo.rename(id, name, "folder") } catch (e: Exception) { applyFailure(e) }
        }
    }

    fun restoreFile(id: String) {
        viewModelScope.launch {
            try { repo.restore(id) } catch (e: Exception) { applyFailure(e) }
        }
    }

    fun deletePermanent(id: String) {
        viewModelScope.launch {
            try { repo.deletePermanent(id) } catch (e: Exception) { applyFailure(e) }
        }
    }

    // ---------- Move ----------

    private val _moveFolders = MutableStateFlow<List<DriveFolder>>(emptyList())
    val moveFolders = _moveFolders.asStateFlow()

    fun loadMoveFolders(parentId: String?) {
        viewModelScope.launch {
            try {
                val res = api.listFiles(parentId)
                if (res.success) _moveFolders.value = res.data?.folders ?: emptyList()
            } catch (e: Exception) { /* dialog tetap tampil dengan yang ada */ }
        }
    }

    fun moveItem(id: String, targetId: String?) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val res = api.bulkMove(mapOf("ids" to listOf(id), "folderId" to targetId))
                if (res.success) load(currentFolderId()) else _error.value = res.error
            } catch (e: Exception) { applyFailure(e) }
            _loading.value = false
        }
    }

    // ---------- Make a copy ----------

    fun copyItem(id: String, isFolder: Boolean) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val res = api.copyItems(mapOf("ids" to listOf(mapOf("id" to id, "type" to if (isFolder) "folder" else "file"))))
                if (res.success) load(currentFolderId()) else _error.value = res.error
            } catch (e: Exception) { applyFailure(e) }
            _loading.value = false
        }
    }

    // ---------- Share link ----------

    /** Buat link publik + kembalikan URL lengkap, atau throw. */
    suspend fun makeShareLink(id: String, isFolder: Boolean): String {
        val body = if (isFolder) mapOf("folderId" to id, "permission" to "view", "visibility" to "LINK")
        else mapOf("fileId" to id, "permission" to "view", "visibility" to "LINK")
        val res = api.shareLink(body)
        val token = res.data?.share?.shareToken
        if (!res.success || token.isNullOrBlank()) throw IllegalStateException(res.error ?: "Gagal membuat link")
        return "${baseUrl}s/$token"
    }

    // ---------- Download ke Download/Ravaa Drive ----------

    /** Unduh via API ke folder Download, kembalikan nama file. Throw bila gagal/offline. */
    suspend fun downloadToPublic(id: String, name: String, mime: String?): String {
        if (!repo.isOnline()) throw IllegalStateException("Offline — hubungkan internet untuk mengunduh")
        val body = api.download(id)
        val resolver = ctx.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, mime?.takeIf { it.contains("/") } ?: "application/octet-stream")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Ravaa Drive")
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Tidak bisa membuat file tujuan")
        resolver.openOutputStream(uri)?.use { out ->
            body.byteStream().use { input -> input.copyTo(out) }
        } ?: throw IllegalStateException("Tidak bisa menulis file")
        return name
    }

    // ---------- Version history ----------

    private val _versions = MutableStateFlow<List<com.ravaa.drive.data.api.FileVersion>>(emptyList())
    val versions = _versions.asStateFlow()

    fun loadVersions(id: String) {
        viewModelScope.launch {
            try {
                val res = api.versions(id)
                _versions.value = if (res.success) res.data?.versions ?: emptyList() else emptyList()
                if (!res.success) _error.value = res.error
            } catch (e: Exception) { applyFailure(e) }
        }
    }

    fun restoreVersion(fileId: String, versionId: String) {
        viewModelScope.launch {
            try {
                val res = api.restoreVersion(fileId, versionId)
                if (res.success) {
                    loadVersions(fileId)
                    load(currentFolderId())
                } else _error.value = res.error
            } catch (e: Exception) { applyFailure(e) }
        }
    }

    /** Multi-select ala Google Drive (long-press). Kosong = mode normal. */
    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds = _selectedIds.asStateFlow()

    fun toggleSelect(id: String) {
        _selectedIds.value = if (id in _selectedIds.value) _selectedIds.value - id else _selectedIds.value + id
    }

    fun clearSelection() { _selectedIds.value = emptySet() }

    fun deleteSelected() {
        val ids = _selectedIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                val items = _files.value
                for (id in ids) {
                    val f = items.firstOrNull {
                        (it as? DriveFile)?.id == id || (it as? DriveFolder)?.id == id
                    }
                    when (f) {
                        is DriveFolder -> repo.trash(id, "folder")
                        is DriveFile -> repo.trash(id, "file")
                        else -> {}
                    }
                }
                _selectedIds.value = emptySet()
                load(currentFolderId())
            } catch (e: Exception) { applyFailure(e) }
        }
    }
}
