package com.empresa.erpapp.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "https://dummyjson.com/"

    // El token vive solo en memoria: se obtiene tras el login, nunca se escribe en el código.
    @Volatile
    var token: String? = null

    private val authInterceptor = Interceptor { chain ->
        val builder = chain.request().newBuilder().header("Accept", "application/json")
        token?.let { builder.header("Authorization", "Bearer $it") }
        chain.proceed(builder.build())
    }

    // Nivel BASIC: no imprime cuerpos; además se oculta el token en los logs.
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
        redactHeader("Authorization")
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    val erpService: ErpService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ErpService::class.java)
    }
}