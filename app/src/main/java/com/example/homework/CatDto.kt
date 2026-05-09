package com.example.homework

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CatDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("breed") val status: String = "Online", // 映射 breed 到 status，并提供默认值
    @SerialName("description") val bio: String = "",
    @SerialName("image") val imageUrl: String = "",    // 匹配 db.json 中的 image 字段
    @SerialName("likes") val likes: Int = 0            // 提供默认值，防止 JSON 缺少该字段时报错
)

fun CatDto.toDomain(): Cat {
    return Cat(
        id = id,
        name = name,
        status = status,
        bio = bio,
        imageRes = when (name) {
            "蓝猫" -> R.drawable.cat_blue
            "暹罗" -> R.drawable.cat_siamese
            else -> R.drawable.cat_orange
        },
        likes = likes
    )
}
