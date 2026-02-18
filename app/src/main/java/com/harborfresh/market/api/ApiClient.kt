package com.harborfresh.market.api

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    // Emulator -> local XAMPP backend (10.0.2.2 maps to host machine)
    // Point to project root so we can hit both /user and /seller APIs.
    const val BASE_URL = "http://192.168.1.10/harborfresh_backend/"

    val apiService: ApiService by lazy {
        val gson = GsonBuilder()
            .setLenient()
            .create()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}

