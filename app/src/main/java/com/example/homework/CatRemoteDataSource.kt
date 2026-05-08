package com.example.homework

import retrofit2.http.GET
import javax.inject.Inject

interface CatService {
    @GET("cats_api") // 示例端点
    suspend fun getCats(): List<CatDto>
}

class CatRemoteDataSource @Inject constructor(
    private val catService: CatService
) {
    suspend fun fetchCats(): List<Cat> {
        return catService.getCats().map { it.toDomain() }
    }
}
