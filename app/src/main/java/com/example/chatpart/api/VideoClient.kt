package com.example.chatpart.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object VideoClient {
    private const val BASE_URL = "https://uu919299-8af2-5ff2838e.westc.seetacloud.com:8443/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(10, TimeUnit.MINUTES)
        .build()

    val api: VideoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VideoApiService::class.java)
    }

    fun fullMediaUrl(path: String): String {
        return if (path.startsWith("http")) path else BASE_URL.removeSuffix("/") + path
    }
}