package com.abedelazizshe.lightcompressorlibrary.config

import com.abedelazizshe.lightcompressorlibrary.VideoQuality

data class Configuration(
    var quality: VideoQuality = VideoQuality.MEDIUM,
    val isMinBitRateEnabled: Boolean = true,
    val keepOriginalResolution: Boolean = false,
    var videoHeight: Double? = null,
    var videoWidth: Double? = null,
    var videoBitrate: Int? = null,
)
