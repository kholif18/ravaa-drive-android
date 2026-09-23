package com.ravaa.drive.presentation.auth

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravaa.drive.data.api.AuthApi
import com.ravaa.drive.data.api.MobileLoginRequest
import com.ravaa.drive.data.datastore.TokenDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val ds: TokenDataStore
) : ViewModel() {
    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state = _state.asStateFlow()

    /** Login via POST /api/auth/mobile-login → simpan token ravaa_... */
    fun login(id: String, pass: String) {
        viewModelScope.launch {
            _state.value = LoginState.Loading
            try {
                val email = id.trim().lowercase()
                if (email.isBlank() || pass.isEmpty()) {
                    _state.value = LoginState.Error("Email dan password diperlukan")
                    return@launch
                }
                val deviceName = "Android ${Build.MODEL ?: "Device"}".take(100)
                val res = authApi.mobileLogin(MobileLoginRequest(email, pass, deviceName))
                val token = res.data?.token.orEmpty()
                if (res.success && token.isNotBlank()) {
                    ds.saveSession(token, res.data?.prefix, res.data?.user?.email)
                    _state.value = LoginState.Success
                } else {
                    _state.value = LoginState.Error(res.error ?: "Login gagal")
                }
            } catch (e: Exception) {
                _state.value = LoginState.Error(e.message ?: "Error")
            }
        }
    }
}
sealed class LoginState { object Idle: LoginState(); object Loading: LoginState(); object Success: LoginState(); data class Error(val msg: String): LoginState() }
