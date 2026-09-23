package com.ravaa.drive.di

import android.content.Context
import coil.ImageLoader
import com.ravaa.drive.BuildConfig
import com.ravaa.drive.data.api.AuthApi
import com.ravaa.drive.data.api.DriveApi
import com.ravaa.drive.data.api.NotesApi
import com.ravaa.drive.data.api.TodosApi
import com.ravaa.drive.data.datastore.TokenDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    /**
     * Base URL Ravaa-Drive: override dari Settings (DataStore) bila ada,
     * fallback ke BuildConfig per buildType. Ganti URL = restart aplikasi.
     */
    @Provides @Singleton
    fun provideBaseUrl(ds: TokenDataStore): String =
        runBlocking { ds.serverFlow.first() }?.takeIf { it.isNotBlank() }
            ?: BuildConfig.DRIVE_BASE_URL

    /**
     * Tulis ulang host setiap request ke server aktif (Settings → Server URL).
     * Retrofit baseUrl hanya placeholder — ganti server berlaku instan tanpa restart.
     */
    @Provides @Singleton @javax.inject.Named("host")
    fun provideHostInterceptor(ds: TokenDataStore): Interceptor = Interceptor { chain ->
        val saved = runBlocking { ds.serverFlow.first() }?.takeIf { it.isNotBlank() }
        val request = if (saved != null) {
            try {
                val override = saved.toHttpUrl()
                val url = chain.request().url
                chain.request().newBuilder().url(
                    url.newBuilder()
                        .scheme(override.scheme)
                        .host(override.host)
                        .port(override.port)
                        .build()
                ).build()
            } catch (e: Exception) {
                chain.request()
            }
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    /** Sisipkan `Authorization: Bearer ravaa_...` dari DataStore ke semua request API. */
    @Provides @Singleton @javax.inject.Named("auth")
    fun provideAuthInterceptor(ds: TokenDataStore): Interceptor = Interceptor { chain ->
        val token = runBlocking { ds.tokenFlow.first() }
        val request = if (!token.isNullOrBlank() && chain.request().header("Authorization") == null) {
            chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    @Provides @Singleton
    fun provideOkHttp(
        @javax.inject.Named("host") hostRewrite: Interceptor,
        @javax.inject.Named("auth") auth: Interceptor
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // Jangan log BODY di release — token bisa bocor ke logcat.
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(hostRewrite)
            .addInterceptor(auth)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    @Provides @Singleton
    fun provideRetrofit(baseUrl: String, client: OkHttpClient): Retrofit =
        Retrofit.Builder().baseUrl(baseUrl).client(client).addConverterFactory(GsonConverterFactory.create()).build()

    @Provides @Singleton fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)
    @Provides @Singleton fun provideDriveApi(retrofit: Retrofit): DriveApi = retrofit.create(DriveApi::class.java)
    @Provides @Singleton fun provideNotesApi(retrofit: Retrofit): NotesApi = retrofit.create(NotesApi::class.java)
    @Provides @Singleton fun provideTodosApi(retrofit: Retrofit): TodosApi = retrofit.create(TodosApi::class.java)

    /** Coil dengan OkHttp ber-auth (Bearer otomatis) untuk thumbnail api/files/{id}/thumb. */
    @Provides @Singleton
    fun provideImageLoader(@ApplicationContext ctx: Context, client: OkHttpClient): ImageLoader =
        ImageLoader.Builder(ctx).okHttpClient(client).crossfade(true).build()
}
