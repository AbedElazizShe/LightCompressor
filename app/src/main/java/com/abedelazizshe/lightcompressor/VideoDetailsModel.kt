package com.abedelazizshe.lightcompressor

import android.net.Uri

data class VideoDetailsModel(
    val playableVideoPath: String?,
    val uri: Uri,
    val newSize: String,
    val progress: Float = 0F
)
