package com.abedelazizshe.lightcompressorlibrary

import com.abedelazizshe.lightcompressorlibrary.Compressor.compressVideo
import com.abedelazizshe.lightcompressorlibrary.Compressor.isRunning
import kotlinx.coroutines.*

enum class VideoQuality {
    HIGH, MEDIUM, LOW
}

object VideoCompressor : CoroutineScope by MainScope() {

    private var job: Job = Job()

    private fun doVideoCompression(
        srcPath: String, destPath: String, quality: VideoQuality,
        isMinBitRateEnabled: Boolean, keepOriginalResolution: Boolean, listener: CompressionListener
    ) = launch {
        isRunning = true
        listener.onStart()
        val result = startCompression(
            srcPath,
            destPath,
            quality,
            isMinBitRateEnabled,
            keepOriginalResolution,
            listener
        )

        // Runs in Main(UI) Thread
        if (result.success) {
            listener.onSuccess()
        } else {
            listener.onFailure(result.failureMessage ?: "An error has occurred!")
        }

    }

    fun start(
        srcPath: String,
        destPath: String,
        listener: CompressionListener,
        quality: VideoQuality = VideoQuality.MEDIUM,
        isMinBitRateEnabled: Boolean = true,
        keepOriginalResolution: Boolean = false
    ) {
        job = doVideoCompression(
            srcPath,
            destPath,
            quality,
            isMinBitRateEnabled,
            keepOriginalResolution,
            listener
        )
    }

    fun cancel() {
        job.cancel()
        isRunning = false
    }

    // To run code in Background Thread
    private suspend fun startCompression(
        srcPath: String, destPath: String, quality: VideoQuality, isMinBitRateEnabled: Boolean,
        keepOriginalResolution: Boolean, listener: CompressionListener
    ): Result = withContext(Dispatchers.IO) {

        return@withContext compressVideo(
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
            })
    }


}