package com.ravaa.drive.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravaa.drive.data.api.AuthApi
import com.ravaa.drive.data.api.LoginRequest
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
    fun login(id: String, pass: String) {
        viewModelScope.launch {
            _state.value = LoginState.Loading
            try {
                val res = authApi.login(LoginRequest(id, pass))
                if (res.accessToken != null) {
                    ds.saveTokens(res.accessToken, res.refreshToken)
                    _state.value = LoginState.Success
                } else _state.value = LoginState.Error("Login gagal")
            } catch (e: Exception) { _state.value = LoginState.Error(e.message ?: "Error") }
        }
    }
}
sealed class LoginState { object Idle: LoginState(); object Loading: LoginState(); object Success: LoginState(); data class Error(val msg: String): LoginState() }
