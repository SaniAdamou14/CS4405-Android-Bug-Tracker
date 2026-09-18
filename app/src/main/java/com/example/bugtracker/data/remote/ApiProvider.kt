package com.example.bugtracker.data.remote

import com.example.bugtracker.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

object ApiProvider {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    val issueApi: IssueApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(IssueApi::class.java)
    }
}
