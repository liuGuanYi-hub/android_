package com.example.homework

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CatDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("status") val status: String,
    @SerialName("description") val bio: String,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("likes") val likes: Int
)

fun CatDto.toDomain(): Cat {
    return Cat(
        id = id,
        name = name,
        status = status,
        bio = bio,
        imageRes = R.drawable.cat_orange, // 暂时使用默认图片
        likes = likes
    )
}
