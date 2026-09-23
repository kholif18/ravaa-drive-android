package com.ravaa.drive.presentation.viewer

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.ravaa.drive.data.api.DriveApi
import com.ravaa.drive.data.datastore.TokenDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

sealed interface ViewerState {
    data object Loading : ViewerState
    data class Downloading(val progress: Int) : ViewerState
    data class Ready(val file: File) : ViewerState
    data class External(val file: File) : ViewerState
    data class Error(val msg: String) : ViewerState
}

/** Unduh sekali ke cache, lalu tampilkan sesuai tipe (gambar/PDF/video internal, office via aplikasi luar). */
@HiltViewModel
class ViewerViewModel @Inject constructor(
    private val api: DriveApi,
    private val ds: TokenDataStore,
    private val baseUrl: String,
    val imageLoader: ImageLoader,
    @ApplicationContext private val ctx: Context
) : ViewModel() {
    fun rawUrl(id: String): String = "${baseUrl}api/files/$id/raw"
    fun thumbUrl(id: String): String = "${baseUrl}api/files/$id/thumb"
    suspend fun authToken(): String? = ds.tokenFlow.first()
    private val _state = MutableStateFlow<ViewerState>(ViewerState.Loading)
    val state = _state.asStateFlow()

    /** Galeri swipe (id gambar). */
    private val _gallery = MutableStateFlow<List<String>>(emptyList())
    val gallery = _gallery.asStateFlow()

    private val metaCache = mutableMapOf<String, Pair<String, String>>()
    private var currentId: String = ""

    /** File yang sedang tampil (untuk judul + retry). */
    private val _current = MutableStateFlow(Triple("", "", ""))
    val current = _current.asStateFlow()

    fun setGallery(ids: List<String>, start: Triple<String, String, String>) {
        _gallery.value = ids
        metaCache[start.first] = start.second to start.third
        _current.value = start
        currentId = start.first
    }

    fun openPage(id: String) {
        if (id.isBlank()) return
        currentId = id
        val cached = metaCache[id]
        if (cached != null) {
            _current.value = Triple(id, cached.first, cached.second)
            load(id, cached.first, cached.second)
            return
        }
        _state.value = ViewerState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = api.getFile(id)
                val f = res.data?.file ?: throw IllegalStateException(res.error ?: "File tidak ditemukan")
                val name = f.name
                val mime = f.mimeType ?: ""
                metaCache[id] = name to mime
                load(id, name, mime)
            } catch (e: Exception) {
                _state.value = ViewerState.Error(e.message ?: "Gagal memuat file")
            }
        }
    }

    fun retry() {
        val (id, name, mime) = _current.value
        if (id.isNotBlank()) load(id, name, mime)
    }

    fun openExternal(file: File, mime: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Buka dengan").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(chooser)
        } catch (e: Exception) {
            _state.value = ViewerState.Error("Tidak ada aplikasi untuk membuka file ini")
        }
    }

    /** Unduh ke cache (pakai ulang bila ada). Throw bila gagal; partial dibuang. */
    private suspend fun ensureFile(fileId: String, name: String, onProgress: (Int) -> Unit = {}): File {
        val dir = File(ctx.cacheDir, "preview").apply { mkdirs() }
        val safe = name.replace(Regex("[^a-zA-Z0-9._-]"), "_").takeLast(80)
        val dest = File(dir, "${fileId}_${safe}")
        if (dest.exists() && dest.length() > 0) return dest
        try {
            val body = api.raw(fileId)
            val total = body.contentLength().coerceAtLeast(1L)
            var done = 0L
            onProgress(0)
            body.byteStream().use { input ->
                dest.outputStream().use { output ->
                    val buf = ByteArray(256 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n == -1) break
                        output.write(buf, 0, n)
                        done += n
                        onProgress(((done * 100) / total).toInt().coerceIn(0, 100))
                    }
                }
            }
            return dest
        } catch (e: Exception) {
            try { dest.delete() } catch (_: Exception) { }
            throw e
        }
    }

    /** Prefetch tetangga galeri diam-diam (hangatkan cache). */
    fun prefetch(id: String) {
        if (id.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val (name, mime) = metaCache[id] ?: run {
                    val res = api.getFile(id)
                    val f = res.data?.file ?: return@launch
                    (f.name to (f.mimeType ?: "")).also { metaCache[id] = it }
                }
                if (viewerKind(name, mime) == ViewerKind.IMAGE) ensureFile(id, name)
            } catch (e: Exception) { /* prefetch best-effort, abaikan */ }
        }
    }

    fun load(fileId: String, name: String, mime: String) {
        _state.value = ViewerState.Loading
        _current.value = Triple(fileId, name, mime)
        currentId = fileId
        metaCache[fileId] = name to mime
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dest = ensureFile(fileId, name) { pct -> _state.value = ViewerState.Downloading(pct) }
                withContext(Dispatchers.Main) {
                    val kind = viewerKind(name, mime)
                    _state.value = if (kind == ViewerKind.EXTERNAL) ViewerState.External(dest)
                    else ViewerState.Ready(dest)
                }
            } catch (e: Exception) {
                _state.value = ViewerState.Error(e.message ?: "Gagal memuat file")
            }
        }
    }

    companion object {
        fun viewerKind(name: String, mime: String): ViewerKind {
            val n = name.lowercase()
            val m = mime.lowercase()
            if (m.startsWith("image/") || n.matches(Regex(".*\\.(png|jpe?g|gif|webp|bmp|heic|avif)$"))) return ViewerKind.IMAGE
            if (m.startsWith("video/") || n.matches(Regex(".*\\.(mp4|webm|mkv|avi|mov|m4v|3gp)$"))) return ViewerKind.VIDEO
            if (m == "application/pdf" || n.endsWith(".pdf")) return ViewerKind.PDF
            return ViewerKind.EXTERNAL
        }

        fun mimeFor(name: String, fallback: String): String {
            if (fallback.isNotBlank() && fallback.contains("/")) return fallback
            val n = name.lowercase()
            return when {
                n.endsWith(".pdf") -> "application/pdf"
                n.matches(Regex(".*\\.docx?$")) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                n.endsWith(".odt") -> "application/vnd.oasis.opendocument.text"
                n.matches(Regex(".*\\.xlsx?$")) -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                n.endsWith(".ods") -> "application/vnd.oasis.opendocument.spreadsheet"
                n.matches(Regex(".*\\.pptx?$")) -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                n.endsWith(".odp") -> "application/vnd.oasis.opendocument.presentation"
                n.endsWith(".txt") || n.endsWith(".md") -> "text/plain"
                else -> "*/*"
            }
        }
    }
}

enum class ViewerKind { IMAGE, VIDEO, PDF, EXTERNAL }
