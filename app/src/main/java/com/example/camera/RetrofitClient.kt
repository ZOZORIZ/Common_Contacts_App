package com.example.camera

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://api.remove.bg/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val retryInterceptor = Interceptor { chain ->
        var request = chain.request()
        var response = chain.proceed(request)
        var tryCount = 0
        val maxLimit = 3

        while (!response.isSuccessful && tryCount < maxLimit) {
            tryCount++
            response = chain.proceed(request)
        }

        response
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS) // Increased connection timeout
            .readTimeout(120, TimeUnit.SECONDS)    // Increased read timeout
            .writeTimeout(120, TimeUnit.SECONDS)   // Increased write timeout
            .addInterceptor(retryInterceptor)      // Retry interceptor
            .addInterceptor(loggingInterceptor)    // Logging interceptor (optional but useful for debugging)
            .build()
    }

    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
