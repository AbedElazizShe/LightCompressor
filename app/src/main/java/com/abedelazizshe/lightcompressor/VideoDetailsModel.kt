package com.abedelazizshe.lightcompressor

import android.net.Uri

data class VideoDetailsModel(
    val playableVideoPath: String?,
    val uri: Uri,
    val originalSize: String,
    val newSize: String,
    val timeTaken: String
)
