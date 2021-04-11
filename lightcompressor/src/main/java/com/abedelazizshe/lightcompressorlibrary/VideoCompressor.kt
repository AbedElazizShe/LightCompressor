package com.abedelazizshe.lightcompressorlibrary

import android.content.Context
import android.net.Uri
import com.abedelazizshe.lightcompressorlibrary.Compressor.compressVideo
import com.abedelazizshe.lightcompressorlibrary.Compressor.isRunning
import kotlinx.coroutines.*

enum class VideoQuality {
    VERY_HIGH, HIGH, MEDIUM, LOW, VERY_LOW
}

object VideoCompressor : CoroutineScope by MainScope() {

    private var job: Job? = null

    /**
     * This function compresses a given [srcPath] or [srcUri] video file and writes the compressed
     * video file at [destPath]
     *
     * The source video can be provided as a string path or a content uri. If both [srcPath] and
     * [srcUri] are provided, [srcUri] will be ignored.
     *
     * Passing [srcUri] requires [context].
     *
     * @param [context] the application context.
     * @param [srcUri] the content Uri of the video file.
     * @param [srcPath] the path of the provided video file to be compressed
     * @param [destPath] the path where the output compressed video file should be saved
     * @param [listener] a compression listener that listens to compression [CompressionListener.onStart],
     * [CompressionListener.onProgress], [CompressionListener.onFailure], [CompressionListener.onSuccess]
     * and if the compression was [CompressionListener.onCancelled]
     * @param [quality] to allow choosing a video quality that can be [VideoQuality.LOW],
     * [VideoQuality.MEDIUM], [VideoQuality.HIGH], and [VideoQuality.VERY_HIGH].
     * This defaults to [VideoQuality.MEDIUM]
     * @param [isMinBitRateEnabled] to determine if the checking for a minimum bitrate threshold
     * before compression is enabled or not. This default to `true`
     * @param [keepOriginalResolution] to keep the original video height and width when compressing.
     * This defaults to `false`
     */
    @JvmStatic
    @JvmOverloads
    fun start(
        context: Context? = null,
        srcUri: Uri? = null,
        srcPath: String? = null,
        destPath: String,
        listener: CompressionListener,
        quality: VideoQuality = VideoQuality.MEDIUM,
        isMinBitRateEnabled: Boolean = true,
        keepOriginalResolution: Boolean = false,
    ) {
        job = doVideoCompression(
            context,
            srcUri,
            srcPath,
            destPath,
            quality,
            isMinBitRateEnabled,
            keepOriginalResolution,
            listener,
        )
    }

    /**
     * Call this function to cancel video compression process which will call [CompressionListener.onCancelled]
     */
    @JvmStatic
    fun cancel() {
        job?.cancel()
        isRunning = false
    }

    private fun doVideoCompression(
        context: Context?,
        srcUri: Uri?,
        srcPath: String?,
        destPath: String,
        quality: VideoQuality,
        isMinBitRateEnabled: Boolean,
        keepOriginalResolution: Boolean,
        listener: CompressionListener,
    ) = launch {
        isRunning = true
        listener.onStart()
        val result = startCompression(
            context,
            srcUri,
            srcPath,
            destPath,
            quality,
            isMinBitRateEnabled,
            keepOriginalResolution,
            listener,
        )

        // Runs in Main(UI) Thread
        if (result.success) {
            listener.onSuccess()
        } else {
            listener.onFailure(result.failureMessage ?: "An error has occurred!")
        }

    }

    private suspend fun startCompression(
        context: Context?,
        srcUri: Uri?,
        srcPath: String?,
        destPath: String,
        quality: VideoQuality,
        isMinBitRateEnabled: Boolean,
        keepOriginalResolution: Boolean,
        listener: CompressionListener,
    ): Result = withContext(Dispatchers.IO) {
        return@withContext compressVideo(
            context,
            srcUri,
            srcPath,
            destPath,
            quality,
            isMinBitRateEnabled,
            keepOriginalResolution,
            object : CompressionProgressListener {
                override fun onProgressChanged(percent: Float) {
                    listener.onProgress(percent)
                }

                override fun onProgressCancelled() {
                    listener.onCancelled()
                }
            },
        )
    }
}
