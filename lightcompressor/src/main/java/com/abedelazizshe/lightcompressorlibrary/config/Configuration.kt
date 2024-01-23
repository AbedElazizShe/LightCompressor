package com.abedelazizshe.lightcompressorlibrary.config

import com.abedelazizshe.lightcompressorlibrary.VideoQuality

data class Configuration(
    var quality: VideoQuality = VideoQuality.MEDIUM,
    var isMinBitrateCheckEnabled: Boolean = true,
    var videoBitrateInMbps: Int? = null,
    var disableAudio: Boolean = false,
    val resizer: VideoResizer? = VideoResizer.auto,
    var videoNames: List<String>
) {
    @Deprecated("Use VideoResizer to override the output video dimensions.", ReplaceWith("Configuration(quality, isMinBitrateCheckEnabled, videoBitrateInMbps, disableAudio, resizer = if (keepOriginalResolution) null else VideoResizer.auto, videoNames)"))
    constructor(
        quality: VideoQuality = VideoQuality.MEDIUM,
        isMinBitrateCheckEnabled: Boolean = true,
        videoBitrateInMbps: Int? = null,
        disableAudio: Boolean = false,
        keepOriginalResolution: Boolean,
        videoNames: List<String>) : this(quality, isMinBitrateCheckEnabled, videoBitrateInMbps, disableAudio, getVideoResizer(keepOriginalResolution, null, null), videoNames)

    @Deprecated("Use VideoResizer to override the output video dimensions.", ReplaceWith("Configuration(quality, isMinBitrateCheckEnabled, videoBitrateInMbps, disableAudio, resizer = VideoResizer.matchSize(videoWidth, videoHeight), videoNames)"))
    constructor(
        quality: VideoQuality = VideoQuality.MEDIUM,
        isMinBitrateCheckEnabled: Boolean = true,
        videoBitrateInMbps: Int? = null,
        disableAudio: Boolean = false,
        keepOriginalResolution: Boolean = false,
        videoHeight: Double? = null,
        videoWidth: Double? = null,
        videoNames: List<String>) : this(quality, isMinBitrateCheckEnabled, videoBitrateInMbps, disableAudio, getVideoResizer(keepOriginalResolution, videoWidth, videoHeight), videoNames)
}

private fun getVideoResizer(keepOriginalResolution: Boolean, videoHeight: Double?, videoWidth: Double?): VideoResizer? =
    if (keepOriginalResolution) {
        null
    } else if (videoWidth != null && videoHeight != null) {
        VideoResizer.matchSize(videoWidth, videoHeight, true)
    } else {
        VideoResizer.auto
    }

data class AppSpecificStorageConfiguration(
    var subFolderName: String? = null,
)

data class SharedStorageConfiguration(
    var saveAt: SaveLocation? = null,
    var subFolderName: String? = null,
)

enum class SaveLocation {
    pictures,
    downloads,
    movies,
}