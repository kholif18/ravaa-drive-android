package com.ravaa.drive.data.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Notes API langsung ke Ravaa-Drive (port 2713). Auth: Bearer ravaa_... */
interface NotesApi {
    @GET("api/notes")
    suspend fun notes(
        @Query("notebookId") notebookId: String? = null,
        @Query("q") q: String? = null,
        @Query("tag") tag: String? = null,
        @Query("pinned") pinned: Boolean? = null,
        @Query("trash") trash: Boolean? = null,
        @Query("limit") limit: Int = 50,
        @Query("cursor") cursor: String? = null
    ): ApiResponse<NotesData>

    @POST("api/notes")
    suspend fun createNote(@Body body: Map<String, @JvmSuppressWildcards Any?>): ApiResponse<Note>

    @PATCH("api/notes/{id}")
    suspend fun updateNote(
        @Path("id") id: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): ApiResponse<Note>

    @DELETE("api/notes/{id}")
    suspend fun deleteNote(@Path("id") id: String): ApiResponse<Map<String, String>>

    @POST("api/notes/{id}/restore")
    suspend fun restoreNote(@Path("id") id: String): ApiResponse<Note>

    @POST("api/notes/{id}/permanent")
    suspend fun permanentDeleteNote(@Path("id") id: String): ApiResponse<Map<String, String>>

    @GET("api/notebooks")
    suspend fun notebooks(): ApiResponse<Map<String, List<Notebook>>>

    @POST("api/notebooks")
    suspend fun createNotebook(@Body body: Map<String, String?>): ApiResponse<Notebook>
}
