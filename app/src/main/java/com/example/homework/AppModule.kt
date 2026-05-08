package com.example.homework

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideRetrofit(json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.example.com/") // 替换为实际的基础 URL，此处仅为示例
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideCatService(retrofit: Retrofit): CatService {
        return retrofit.create(CatService::class.java)
    }

    @Provides
    @Singleton
    fun provideCatRepository(
        remoteDataSource: CatRemoteDataSource
    ): CatRepository {
        return object : CatRepository {
            override fun getCats(category: String): Flow<List<Cat>> = flow {
                try {
                    // 这里模拟了从网络获取数据并转换为 Domain Model 的过程
                    val remoteCats = remoteDataSource.fetchCats()
                    emit(remoteCats)
                } catch (e: Exception) {
                    // 在演示中，如果网络请求失败（比如 URL 不存在），我们打印错误并返回空列表
                    e.printStackTrace()
                    emit(emptyList())
                }
            }
        }
    }
}
