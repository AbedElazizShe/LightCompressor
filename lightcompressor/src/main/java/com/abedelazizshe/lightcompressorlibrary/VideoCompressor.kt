package com.abedelazizshe.lightcompressorlibrary

import com.abedelazizshe.lightcompressorlibrary.Compressor.compressVideo
import com.abedelazizshe.lightcompressorlibrary.Compressor.isRunning
import kotlinx.coroutines.*

enum class VideoQuality {
    HIGH, MEDIUM, LOW
}

object VideoCompressor : CoroutineScope by MainScope() {

    private var job: Job = Job()

    private fun doVideoCompression(srcPath: String, destPath: String, quality: VideoQuality, isMinBitRateEnabled: Boolean, listener: CompressionListener) = launch {
        isRunning = true
        listener.onStart()
        val result = startCompression(srcPath, destPath, quality, isMinBitRateEnabled, listener)

        // Runs in Main(UI) Thread
        if (result) {
            listener.onSuccess()
        } else {
            listener.onFailure()
        }

    }

    fun start(srcPath: String, destPath: String, listener: CompressionListener, quality: VideoQuality = VideoQuality.MEDIUM, isMinBitRateEnabled: Boolean = true) {
        job = doVideoCompression(srcPath, destPath, quality, isMinBitRateEnabled, listener)
    }

    fun cancel() {
        job.cancel()
        isRunning = false
    }

    // To run code in Background Thread
    private suspend fun startCompression(srcPath: String, destPath: String, quality: VideoQuality, isMinBitRateEnabled: Boolean,
                                         listener: CompressionListener): Boolean = withContext(Dispatchers.IO) {

        return@withContext compressVideo(srcPath, destPath, quality, isMinBitRateEnabled, object : CompressionProgressListener {
            override fun onProgressChanged(percent: Float) {
                listener.onProgress(percent)
            }

            override fun onProgressCancelled() {
                listener.onCancelled()
            }
        })
    }


}