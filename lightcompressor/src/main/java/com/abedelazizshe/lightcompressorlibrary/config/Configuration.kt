package com.abedelazizshe.lightcompressorlibrary.config

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.utils.saveVideoInExternal
import java.io.File
import java.io.FileInputStream
import java.io.IOException

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
        videoNames: List<String>) : this(quality, isMinBitrateCheckEnabled, videoBitrateInMbps, disableAudio, getVideoResizer(keepOriginalResolution, videoHeight, videoWidth), videoNames)
}

private fun getVideoResizer(keepOriginalResolution: Boolean, videoHeight: Double?, videoWidth: Double?): VideoResizer? =
    if (keepOriginalResolution) {
        null
    } else if (videoWidth != null && videoHeight != null) {
        VideoResizer.matchSize(videoWidth, videoHeight, true)
    } else {
        VideoResizer.auto
    }

interface StorageConfiguration {
    fun createFileToSave(
        context: Context,
        videoFile: File,
        fileName: String,
        shouldSave: Boolean
    ): File
}

class AppSpecificStorageConfiguration(
    private val subFolderName: String? = null,
) : StorageConfiguration {

    override fun createFileToSave(
        context: Context,
        videoFile: File,
        fileName: String,
        shouldSave: Boolean
    ): File {
        val fullPath =
            if (subFolderName != null) "${subFolderName}/$fileName"
            else fileName

        if (!File("${context.filesDir}/$fullPath").exists()) {
            File("${context.filesDir}/$fullPath").parentFile?.mkdirs()
        }
        return File(context.filesDir, fullPath)
    }
}


enum class SaveLocation {
    pictures,
    downloads,
    movies,
}

class SharedStorageConfiguration(
    private val saveAt: SaveLocation? = null,
    private val subFolderName: String? = null,
) : StorageConfiguration {

    override fun createFileToSave(
        context: Context,
        videoFile: File,
        fileName: String,
        shouldSave: Boolean
    ): File {
        val saveLocation =
            when (saveAt) {
                SaveLocation.downloads -> {
                    Environment.DIRECTORY_DOWNLOADS
                }

                SaveLocation.pictures -> {
                    Environment.DIRECTORY_PICTURES
                }

                else -> {
                    Environment.DIRECTORY_MOVIES
                }
            }

        if (Build.VERSION.SDK_INT >= 29) {
            val fullPath =
                if (subFolderName != null) "$saveLocation/${subFolderName}"
                else saveLocation
            if (shouldSave) {
                saveVideoInExternal(context, fileName, fullPath, videoFile)
                File(context.filesDir, fileName).delete()
                return File("/storage/emulated/0/${fullPath}", fileName)
            }
            return File(context.filesDir, fileName)
        } else {
            val savePath =
                Environment.getExternalStoragePublicDirectory(saveLocation)

            val fullPath =
                if (subFolderName != null) "$savePath/${subFolderName}"
                else savePath.path

            val desFile = File(fullPath, fileName)

            if (!desFile.exists()) {
                try {
                    desFile.parentFile?.mkdirs()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            if (shouldSave) {
                context.openFileOutput(fileName, Context.MODE_PRIVATE)
                    .use { outputStream ->
                        FileInputStream(videoFile).use { inputStream ->
                            val buf = ByteArray(4096)
                            while (true) {
                                val sz = inputStream.read(buf)
                                if (sz <= 0) break
                                outputStream.write(buf, 0, sz)
                            }

                        }
                    }

            }
            return desFile
        }
    }
}

class CacheStorageConfiguration(
) : StorageConfiguration {
    override fun createFileToSave(
        context: Context,
        videoFile: File,
        fileName: String,
        shouldSave: Boolean
    ): File =
        File.createTempFile(videoFile.nameWithoutExtension,videoFile.extension)
}
