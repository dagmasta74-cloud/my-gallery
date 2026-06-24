package com.example.mygallery

enum class MediaType {
    IMAGE, VIDEO
}

data class MediaItem(
    val id: Long,
    val path: String,
    val dateTaken: Long,
    val type: MediaType,
    val displayName: String = ""
)
