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

@Singleton
class TokenDataStore @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val K_TOKEN = stringPreferencesKey("ravaa_token")
    private val K_REFRESH = stringPreferencesKey("refresh_token")
    val tokenFlow: Flow<String?> = ctx.ds.data.map { it[K_TOKEN] }
    val refreshFlow: Flow<String?> = ctx.ds.data.map { it[K_REFRESH] }
    suspend fun saveTokens(access: String, refresh: String?) {
        ctx.ds.edit { it[K_TOKEN]=access; if(refresh!=null) it[K_REFRESH]=refresh }
    }
    suspend fun clear() { ctx.ds.edit { it.clear() } }
}
