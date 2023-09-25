package com.abedelazizshe.lightcompressorlibrary.video

data class Result(
    val success: Boolean,
    val failureMessage: String?,
    val size: Long = 0,
    val path: String? = null,
)
