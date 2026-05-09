package com.example.homework

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton
import android.util.Log

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
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                Log.d("RetrofitRequest", "Sending request to: ${request.url}")
                val response = chain.proceed(request)
                Log.d("RetrofitResponse", "Response code: ${response.code} for ${request.url}")
                response
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(json: Json, okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/") 
            .client(okHttpClient)
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
                    val remoteCats = remoteDataSource.fetchCats()
                    val filteredCats = if (category.isEmpty() || category == "全部") {
                        remoteCats
                    } else {
                        // 模糊匹配名称，比如“大橘”匹配包含“橘”的猫
                        remoteCats.filter { it.name.contains(category) || it.status.contains(category) }
                    }
                    emit(filteredCats)
                } catch (e: Exception) {
                    Log.e("CatRepository", "Error fetching cats", e)
                    throw e // 抛出异常让 ViewModel 的 .catch 捕获
                }
            }
        }
    }
}
