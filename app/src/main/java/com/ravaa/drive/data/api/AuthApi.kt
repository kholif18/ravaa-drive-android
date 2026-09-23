package com.ravaa.drive.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Auth langsung ke Ravaa-Drive (port 2713).
 * Login HP memakai token perangkat `ravaa_...` (ApiToken), BUKAN cookie/JWT web.
 * Token dikirim via header `Authorization: Bearer ravaa_...` (otomatis oleh interceptor).
 */
interface AuthApi {
    /** Login perangkat → { token, user, scopes, prefix }. Email selalu lowercase. */
    @POST("api/auth/mobile-login")
    suspend fun mobileLogin(@Body body: MobileLoginRequest): ApiResponse<MobileLoginData>

    /** Validasi token perangkat → { user, scopes, prefix }. */
    @POST("api/auth/token")
    suspend fun validateToken(@Body body: Map<String, String>): ApiResponse<TokenInfo>

    /** Profil user login (butuh Bearer). */
    @GET("api/auth/me")
    suspend fun me(): ApiResponse<ApiUser>
}
