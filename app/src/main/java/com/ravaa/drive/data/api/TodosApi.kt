package com.ravaa.drive.data.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Tasks API langsung ke Ravaa-Drive (port 2713). Auth: Bearer ravaa_... */
interface TodosApi {
    @GET("api/todo-lists")
    suspend fun lists(): ApiResponse<Map<String, List<TodoList>>>

    @POST("api/todo-lists")
    suspend fun createList(@Body body: Map<String, String>): ApiResponse<TodoList>

    @PATCH("api/todo-lists/{id}")
    suspend fun renameList(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): ApiResponse<TodoList>

    @DELETE("api/todo-lists/{id}")
    suspend fun deleteList(@Path("id") id: String): ApiResponse<Map<String, String>>

    @GET("api/todos")
    suspend fun todos(@Query("listId") listId: String? = null): ApiResponse<Map<String, List<Todo>>>

    @POST("api/todos")
    suspend fun createTodo(@Body body: Map<String, @JvmSuppressWildcards Any?>): ApiResponse<Todo>

    @PATCH("api/todos/{id}")
    suspend fun updateTodo(
        @Path("id") id: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): ApiResponse<Todo>

    @DELETE("api/todos/{id}")
    suspend fun deleteTodo(@Path("id") id: String): ApiResponse<Map<String, String>>
}
