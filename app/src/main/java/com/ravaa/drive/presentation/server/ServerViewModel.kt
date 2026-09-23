package com.ravaa.drive.presentation.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravaa.drive.data.datastore.TokenDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

sealed interface ServerState {
    data object Idle : ServerState
    data object Checking : ServerState
    data class Ok(val version: String) : ServerState
    data class Error(val msg: String) : ServerState
}

/** Layar server ala Nextcloud: isi URL → Connect (cek /api/health) → login. */
@HiltViewModel
class ServerViewModel @Inject constructor(
    private val ds: TokenDataStore
) : ViewModel() {
    private val _url = MutableStateFlow("")
    val url = _url.asStateFlow()

    private val _state = MutableStateFlow<ServerState>(ServerState.Idle)
    val state = _state.asStateFlow()

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    init {
        viewModelScope.launch {
            ds.serverFlow.first()?.let { if (it.isNotBlank()) _url.value = it.trimEnd('/') }
        }
    }

    fun setUrl(v: String) {
        _url.value = v
        _state.value = ServerState.Idle
    }

    private fun normalize(raw: String): String {
        // Buang SEMUA whitespace (keyboard/Gboard suka selipkan spasi) + lowercase host
        var u = raw.filterNot { it.isWhitespace() }
        if (u.isNotEmpty() && !u.startsWith("http://", ignoreCase = true) && !u.startsWith("https://", ignoreCase = true)) {
            u = "http://$u"
        }
        return u.trimEnd('/')
    }

    /** Blocking OkHttp — hanya dipanggil dari Dispatchers.IO. */
    private fun checkHealth(base: String): Triple<Boolean, String, Int> {
        val req = Request.Builder().url("$base/api/health").get().build()
        http.newCall(req).execute().use { res ->
            val body = res.body?.string().orEmpty()
            val ok = res.isSuccessful && JSONObject(body).optBoolean("success", false)
            val version = try {
                JSONObject(body).getJSONObject("data").optString("version", "")
            } catch (e: Exception) {
                ""
            }
            return Triple(ok, version, res.code)
        }
    }

    /** Cek server, simpan bila OK. onSaved dipanggil dengan URL final. */
    fun connect(onSaved: (String) -> Unit) {
        val base = normalize(_url.value)
        if (base.isBlank()) {
            _state.value = ServerState.Error("Isi Server URL dulu")
            return
        }
        _state.value = ServerState.Checking
        viewModelScope.launch {
            try {
                // Network blocking WAJIB di IO — di Main = NetworkOnMainThreadException
                val (ok, version, code) = withContext(Dispatchers.IO) { checkHealth(base) }
                if (!ok) throw IllegalStateException("Bukan server Ravaa Drive (HTTP $code)")
                ds.saveServer(base)
                _state.value = ServerState.Ok(version)
                onSaved(base)
            } catch (e: Exception) {
                android.util.Log.e("ServerCheck", "connect $base gagal", e)
                val detail = e.message?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName
                _state.value = ServerState.Error("$base → $detail")
            }
        }
    }
}
