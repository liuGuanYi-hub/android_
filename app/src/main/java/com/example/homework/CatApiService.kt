package com.example.homework

import retrofit2.http.GET
import retrofit2.http.Query

interface CatApiService {
    @GET("cats")
    suspend fun getCats(@Query("category") category: String): List<Cat>
}
