package com.abedelazizshe.lightcompressorlibrary

import com.abedelazizshe.lightcompressorlibrary.Compressor.compressVideo
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

object VideoCompressor : CoroutineScope {

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job // to run code in Main(UI) Thread

    fun doVideoCompression(srcPath: String, destPath: String, listener: CompressionListener) = launch {
        listener.onStart()
        val result = startCompression(srcPath, destPath, listener)

        // Runs in Main(UI) Thread
        if (result) {
            listener.onSuccess()
        } else {
            listener.onFailure()
        }

    }

    // To run code in Background Thread
    private suspend fun startCompression(srcPath: String, destPath: String, listener: CompressionListener) : Boolean = withContext(Dispatchers.IO){

        return@withContext compressVideo(srcPath, destPath, object : CompressionProgressListener {
            override fun onProgressChanged(percent: Float) {
                listener.onProgress(percent)
            }
        })
    }


}