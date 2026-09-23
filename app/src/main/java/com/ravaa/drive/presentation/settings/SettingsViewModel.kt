package com.ravaa.drive.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.Coil
import com.ravaa.drive.data.api.DriveApi
import com.ravaa.drive.data.api.StorageStats
import com.ravaa.drive.data.datastore.TokenDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val api: DriveApi,
    private val ds: TokenDataStore,
    @ApplicationContext private val ctx: Context
) : ViewModel() {
    private val _server = MutableStateFlow("")
    val server = _server.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved = _saved.asStateFlow()

    private val _storage = MutableStateFlow<StorageStats?>(null)
    val storage = _storage.asStateFlow()

    private val _cacheSize = MutableStateFlow(0L)
    val cacheSize = _cacheSize.asStateFlow()

    init {
        viewModelScope.launch {
            _server.value = ds.serverFlow.first() ?: ""
            refreshStorage()
            refreshCacheSize()
        }
    }

    fun setServer(v: String) { _server.value = v; _saved.value = false }

    fun saveServer() {
        viewModelScope.launch {
            var url = _server.value.trim()
            if (url.isNotBlank() && !url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://$url"
            }
            ds.saveServer(url)
            _server.value = url
            _saved.value = true
        }
    }

    /** Simpan; bila URL berubah, token lama tak berlaku → panggil onChanged (logout ke login). */
    fun saveServerAndLogout(onChanged: () -> Unit) {
        viewModelScope.launch {
            val before = ds.serverFlow.first().orEmpty()
            var url = _server.value.trim()
            if (url.isNotBlank() && !url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://$url"
            }
            ds.saveServer(url)
            _server.value = url
            _saved.value = true
            if (normalizeCompare(before) != normalizeCompare(url)) onChanged()
        }
    }

    private fun normalizeCompare(u: String): String {
        var v = u.trim()
        if (v.isNotEmpty() && !v.startsWith("http://") && !v.startsWith("https://")) v = "http://$v"
        return v.trimEnd('/')
    }

    fun refreshStorage() {
        viewModelScope.launch {
            try {
                val res = api.storageStats()
                if (res.success) _storage.value = res.data
            } catch (e: Exception) { /* opsional */ }
        }
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            _cacheSize.value = withContext(Dispatchers.IO) { dirSize(ctx.cacheDir) }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try { Coil.imageLoader(ctx).diskCache?.clear() } catch (e: Exception) { }
                ctx.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            }
            refreshCacheSize()
        }
    }

    private fun dirSize(f: java.io.File): Long {
        if (!f.exists()) return 0L
        if (f.isFile) return f.length()
        return f.listFiles()?.sumOf { dirSize(it) } ?: 0L
    }
}
