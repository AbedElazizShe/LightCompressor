package com.abedelazizshe.lightcompressorlibrary

import android.content.Context
import android.net.Uri
import com.abedelazizshe.lightcompressorlibrary.Compressor.compressVideo
import com.abedelazizshe.lightcompressorlibrary.Compressor.isRunning
import com.abedelazizshe.lightcompressorlibrary.config.Configuration
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
     * @param [configureWith] to allow add video compression configuration that could be:
     * [Configuration.quality] to allow choosing a video quality that can be [VideoQuality.LOW],
     * [VideoQuality.MEDIUM], [VideoQuality.HIGH], and [VideoQuality.VERY_HIGH].
     * This defaults to [VideoQuality.MEDIUM]
     * [Configuration.isMinBitRateEnabled] to determine if the checking for a minimum bitrate threshold
     * before compression is enabled or not. This default to `true`
     * [Configuration.keepOriginalResolution] to keep the original video height and width when compressing.
     * This defaults to `false`
     * [Configuration.videoHeight] which is a custom height for the video. Must be specified with [Configuration.videoWidth]
     * [Configuration.videoWidth] which is a custom width for the video. Must be specified with [Configuration.videoHeight]
     * [Configuration.videoBitrate] which is a custom bitrate for the video. You might consider setting
     * [Configuration.isMinBitRateEnabled] to `false` if your bitrate is less than 2000000.
     */
    @JvmStatic
    @JvmOverloads
    fun start(
        context: Context? = null,
        srcUri: Uri? = null,
        srcPath: String? = null,
        destPath: String,
        listener: CompressionListener,
        configureWith: Configuration,
    ) {
        job = doVideoCompression(
            context,
            srcUri,
            srcPath,
            destPath,
            configureWith,
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
        configuration: Configuration,
        listener: CompressionListener,
    ) = launch {
        isRunning = true
        listener.onStart()
        val result = startCompression(
            context,
            srcUri,
            srcPath,
            destPath,
            configuration,
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
        configuration: Configuration,
        listener: CompressionListener,
    ): Result = withContext(Dispatchers.IO) {
        return@withContext compressVideo(
            context,
            srcUri,
            srcPath,
            destPath,
            configuration,
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
