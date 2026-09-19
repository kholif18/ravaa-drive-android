package com.ravaa.drive.data.api

import retrofit2.http.*

interface DriveApi {
    @GET("api/v1/mobile/drive/files")
    suspend fun listFiles(@Query("folderId") folderId: String?, @Query("q") q: String?): DriveListResponse

    @GET("api/v1/mobile/drive/files/{id}/download")
    suspend fun download(@Path("id") id: String): okhttp3.ResponseBody

    @POST("api/v1/mobile/drive/chunk")
    suspend fun uploadChunk(@Body body: okhttp3.MultipartBody): ChunkResponse

    @GET("api/v1/mobile/drive/chunk/status")
    suspend fun chunkStatus(@Query("fileId") fileId: String): ChunkStatus
}
data class DriveListResponse(val success: Boolean, val data: DriveData)
data class DriveData(val files: List<DriveFile>, val folders: List<DriveFolder>)
data class DriveFile(val id: String, val name: String, val fileSize: Long, val mimeType: String, val ownerId: String, val folderId: String?)
data class DriveFolder(val id: String, val name: String)
data class ChunkResponse(val success: Boolean, val data: Map<String, Any>?)
data class ChunkStatus(val success: Boolean, val data: Map<String, Long>)
