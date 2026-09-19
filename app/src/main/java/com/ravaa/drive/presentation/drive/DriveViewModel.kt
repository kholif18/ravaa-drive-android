package com.ravaa.drive.presentation.drive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravaa.drive.data.api.DriveApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DriveViewModel @Inject constructor(private val api: DriveApi) : ViewModel() {
    private val _files = MutableStateFlow<List<Any>>(emptyList())
    val files = _files.asStateFlow()
    fun load(folderId: String? = null) {
        viewModelScope.launch {
            try { val res = api.listFiles(folderId, null); _files.value = (res.data?.files ?: emptyList()) + (res.data?.folders ?: emptyList()) } catch(e: Exception){ }
        }
    }
}
