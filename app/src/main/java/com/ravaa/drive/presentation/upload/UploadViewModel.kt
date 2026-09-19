package com.ravaa.drive.presentation.upload

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravaa.drive.data.api.DriveApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UploadItem(val id: String, val name: String, val size: Long, val progress: Int = 0, val status: String = "pending")

@HiltViewModel
class UploadViewModel @Inject constructor(private val api: DriveApi) : ViewModel() {
    private val _uploads = MutableStateFlow<List<UploadItem>>(emptyList())
    val uploads = _uploads.asStateFlow()
    fun addFiles(files: List<UploadItem>) { _uploads.value = _uploads.value + files }
    fun pause(id: String) { _uploads.value = _uploads.value.map { if(it.id==id) it.copy(status="paused") else it } }
    fun resume(id: String) {
        _uploads.value = _uploads.value.map { if(it.id==id) it.copy(status="pending", progress=0) else it }
        viewModelScope.launch { /* trigger chunk upload */ }
    }
    fun cancelAll() { _uploads.value = _uploads.value.filter { it.status=="success" || it.status=="error" } }
}
