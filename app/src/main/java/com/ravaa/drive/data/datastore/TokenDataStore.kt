package com.ravaa.drive.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.ds by preferencesDataStore("ravaa_prefs")

/**
 * Sesi perangkat: token ApiToken `ravaa_...` hasil POST /api/auth/mobile-login.
 * Tidak ada refresh token — 401 berarti sesi habis/dicabut → login ulang.
 */
@Singleton
class TokenDataStore @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val K_TOKEN = stringPreferencesKey("api_token")
    private val K_PREFIX = stringPreferencesKey("token_prefix")
    private val K_EMAIL = stringPreferencesKey("user_email")
    private val K_SERVER = stringPreferencesKey("server_url")

    val tokenFlow: Flow<String?> = ctx.ds.data.map { it[K_TOKEN] }
    val prefixFlow: Flow<String?> = ctx.ds.data.map { it[K_PREFIX] }
    val emailFlow: Flow<String?> = ctx.ds.data.map { it[K_EMAIL] }
    val serverFlow: Flow<String?> = ctx.ds.data.map { it[K_SERVER] }

    suspend fun saveSession(token: String, prefix: String?, email: String?) {
        ctx.ds.edit {
            it[K_TOKEN] = token
            if (prefix != null) it[K_PREFIX] = prefix
            if (email != null) it[K_EMAIL] = email
        }
    }

    suspend fun saveServer(url: String) {
        ctx.ds.edit { it[K_SERVER] = url.trim().trimEnd('/') + "/" }
    }

    /** Logout: hapus sesi, tapi server URL dipertahankan. */
    suspend fun clear() {
        ctx.ds.edit {
            it.remove(K_TOKEN)
            it.remove(K_PREFIX)
            it.remove(K_EMAIL)
        }
    }
}
