package com.example.homework

data class Cat(
    val id: Int,
    val name: String,
    val status: String,
    val bio: String,
    val imageRes: Int,
    var isLiked: Boolean = false,
    var isFavorite: Boolean = false,
    var likes: Int = 0
)
