package com.ravaa.drive.data.api

import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(val identifier: String, val password: String)
data class LoginResponse(val user: Map<String,Any>?, val accessToken: String?, val refreshToken: String?)

interface AuthApi {
    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse
}
