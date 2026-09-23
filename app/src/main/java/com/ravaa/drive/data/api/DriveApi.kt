package com.ravaa.drive.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * Drive API langsung ke Ravaa-Drive (port 2713).
 * Auth: header `Authorization: Bearer ravaa_...` (disisipkan interceptor).
 * Semua respons list memakai envelope { success, data: { files, folders, nextCursor? } }.
 */
interface DriveApi {
    // ---------- List & filter ----------

    /** Isi folder. folderId null/"root" = root. Pagination via limit + cursor. */
    @GET("api/files")
    suspend fun listFiles(
        @Query("folderId") folderId: String?,
        @Query("limit") limit: Int = 100,
        @Query("cursor") cursor: String? = null
    ): DriveListResponse

    @GET("api/files/photos")
    suspend fun photos(): DriveListResponse

    @GET("api/files/recent")
    suspend fun recent(): DriveListResponse

    @GET("api/files/starred")
    suspend fun starred(): DriveListResponse

    @GET("api/files/shared")
    suspend fun shared(): DriveListResponse

    @GET("api/files/trash")
    suspend fun trash(): DriveListResponse

    /** { totalFiles, totalSize, storageLimit, storageUsed, categories, percent }. */
    @GET("api/files/storage-stats")
    suspend fun storageStats(): ApiResponse<StorageStats>

    /** Cari file+folder milik user (q min 2 huruf) → { files, folders }. */
    @GET("api/search")
    suspend fun search(@Query("q") q: String): DriveListResponse

    // ---------- File bytes ----------

    /** Detail satu file: { file, canEdit }. */
    @GET("api/files/{id}")
    suspend fun getFile(@Path("id") id: String): ApiResponse<FileDetail>

    @Streaming
    @GET("api/files/{id}/download")
    suspend fun download(@Path("id") id: String): ResponseBody

    @Streaming
    @GET("api/files/{id}/raw")
    suspend fun raw(@Path("id") id: String): ResponseBody

    @GET("api/files/{id}/thumb")
    suspend fun thumb(@Path("id") id: String): ResponseBody

    // ---------- Tulis ----------

    /** Upload kecil (multipart: file + folderId opsional). File besar: pakai chunk. */
    @Multipart
    @POST("api/files/upload")
    suspend fun upload(
        @Part file: MultipartBody.Part,
        @Part("folderId") folderId: RequestBody? = null
    ): ApiResponse<UploadResult>

    /** Offset byte yang sudah diterima server (untuk resume). */
    @GET("api/files/chunk")
    suspend fun chunkStatus(@Query("fileId") fileId: String): ApiResponse<ChunkStatus>

    /** Satu chunk 2MB (multipart: chunk + fileId + chunkIndex + totalChunks + fileName + folderId?). */
    @Multipart
    @POST("api/files/chunk")
    suspend fun uploadChunk(
        @Part chunk: MultipartBody.Part,
        @Part("fileId") fileId: RequestBody,
        @Part("chunkIndex") chunkIndex: RequestBody,
        @Part("totalChunks") totalChunks: RequestBody,
        @Part("fileName") fileName: RequestBody,
        @Part("folderId") folderId: RequestBody? = null
    ): ApiResponse<ChunkReceipt>

    /** Rename / star / trash: { "name"?, "isStarred"?, "isTrashed"? }. */
    @PATCH("api/files/{id}")
    suspend fun updateFile(
        @Path("id") id: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): ApiResponse<DriveFile>

    /** Tanpa trash: hapus permanen. Untuk trash dulu, PATCH { "isTrashed": true }. */
    @DELETE("api/files/{id}")
    suspend fun deleteFile(@Path("id") id: String): ApiResponse<Map<String, String>>

    /** Pindah file/folder: { ids: [id], folderId (null = root) }. */
    @POST("api/files/bulk-move")
    suspend fun bulkMove(@Body body: Map<String, @JvmSuppressWildcards Any?>): ApiResponse<Map<String, String>>

    /** Duplikat di sebelah aslinya (" - Copy"): { ids: [{ id, type }] }. */
    @POST("api/files/copy")
    suspend fun copyItems(@Body body: Map<String, @JvmSuppressWildcards Any>): ApiResponse<Map<String, String>>

    /** Riwayat versi file: { versions }. */
    @GET("api/files/{id}/versions")
    suspend fun versions(@Path("id") id: String): ApiResponse<VersionList>

    /** Kembalikan versi jadi aktif. */
    @POST("api/files/{id}/versions/{versionId}")
    suspend fun restoreVersion(@Path("id") id: String, @Path("versionId") versionId: String): ApiResponse<Map<String, String>>

    /** Buat link publik LINK (view): { fileId/folderId, permission, visibility } → { share }. */
    @POST("api/share")
    suspend fun shareLink(@Body body: Map<String, String>): ApiResponse<ShareResult>

    /** Buat folder: { "name", "parentId"? }. */
    @POST("api/folders")
    suspend fun createFolder(@Body body: Map<String, String?>): ApiResponse<DriveFolder>

    @PATCH("api/folders/{id}")
    suspend fun renameFolder(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): ApiResponse<DriveFolder>

    @DELETE("api/folders/{id}")
    suspend fun deleteFolder(@Path("id") id: String): ApiResponse<Map<String, String>>
}
