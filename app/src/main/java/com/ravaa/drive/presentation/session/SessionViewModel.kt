package com.ravaa.drive.presentation.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravaa.drive.data.datastore.TokenDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Sesi global: email user + logout (hapus token). */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val ds: TokenDataStore
) : ViewModel() {
    val email = ds.emailFlow.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    suspend fun getServer(): String? = ds.serverFlow.first()

    suspend fun getToken(): String? = ds.tokenFlow.first()

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            ds.clear()
            onDone()
        }
    }
}
