package com.ravaa.drive.data.api

import okhttp3.ResponseBody
import retrofit2.http.*

data class DriveListResponse(val success: Boolean, val data: DriveData?)
data class DriveData(val files: List<DriveFile>?, val folders: List<DriveFolder>?)
data class DriveFile(val id: String, val name: String, val fileSize: String, val mimeType: String, val folderId: String?)
data class DriveFolder(val id: String, val name: String, val parentId: String?)

interface DriveApi {
    @GET("api/v1/mobile/drive/files")
    suspend fun listFiles(@Query("folderId") folderId: String?, @Query("q") q: String?): DriveListResponse
    @GET("api/v1/mobile/drive/storage")
    suspend fun storage(): Map<String,Any>
    @GET("api/v1/mobile/drive/files/{id}/download")
    suspend fun download(@Path("id") id: String): ResponseBody
}
