package com.abedelazizshe.lightcompressorlibrary.config

import com.abedelazizshe.lightcompressorlibrary.VideoQuality

data class Configuration(
    var quality: VideoQuality = VideoQuality.MEDIUM,
    var isMinBitrateCheckEnabled: Boolean = true,
    var videoBitrate: Int? = null,
    var disableAudio: Boolean = false,
    val keepOriginalResolution: Boolean = false,
    var videoHeight: Double? = null,
    var videoWidth: Double? = null,
)

data class StorageConfiguration(
    var fileName: String? = null,
    var saveAt: String? = null,
    var isExternal: Boolean = true,
)