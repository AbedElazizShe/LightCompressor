package com.abedelazizshe.lightcompressorlibrary.config

import com.abedelazizshe.lightcompressorlibrary.VideoQuality

data class Configuration(
    var quality: VideoQuality = VideoQuality.MEDIUM,
    var isMinBitrateCheckEnabled: Boolean = true,
    var videoBitrateInMbps: Int? = null,
    var disableAudio: Boolean = false,
    val keepOriginalResolution: Boolean = false,
    var videoHeight: Double? = null,
    var videoWidth: Double? = null,
)

data class AppSpecificStorageConfiguration(
    var videoName: String,
    var subFolderName: String? = null,
)

data class SharedStorageConfiguration(
    var videoName: String,
    var saveAt: SaveLocation? = null,
)

enum class SaveLocation {
    pictures,
    downloads,
    movies,
}