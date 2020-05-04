package com.abedelazizshe.lightcompressorlibrary

import com.abedelazizshe.lightcompressorlibrary.Compressor.compressVideo
import com.abedelazizshe.lightcompressorlibrary.Compressor.isRunning
import kotlinx.coroutines.*

object VideoCompressor : CoroutineScope by MainScope() {

    private var job: Job = Job()

   private fun doVideoCompression(srcPath: String, destPath: String, listener: CompressionListener) = launch {
        isRunning = true
        listener.onStart()
        val result = startCompression(srcPath, destPath, listener)

        // Runs in Main(UI) Thread
        if (result) {
            listener.onSuccess()
        } else {
            listener.onFailure()
        }

    }

    fun start(srcPath: String, destPath: String, listener: CompressionListener){
       job =  doVideoCompression(srcPath, destPath, listener)
    }

    fun cancel(){
        job.cancel()
        isRunning = false
    }

    // To run code in Background Thread
    private suspend fun startCompression(srcPath: String, destPath: String, listener: CompressionListener) : Boolean = withContext(Dispatchers.IO){

        return@withContext compressVideo(srcPath, destPath, object : CompressionProgressListener {
            override fun onProgressChanged(percent: Float) {
                listener.onProgress(percent)
            }

            override fun onProgressCancelled() {
                listener.onCancelled()
            }
        })
    }


}