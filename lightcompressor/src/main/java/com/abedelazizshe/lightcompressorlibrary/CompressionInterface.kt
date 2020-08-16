package com.abedelazizshe.lightcompressorlibrary

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread

/**
 * Created by AbedElaziz Shehadeh on 27 Jan, 2020
 * elaziz.shehadeh@gmail.com
 */
interface CompressionListener {
    @MainThread
    fun onStart()

    @MainThread
    fun onSuccess()

    @MainThread
    fun onFailure(failureMessage: String)

    @WorkerThread
    fun onProgress(percent: Float)

    @WorkerThread
    fun onCancelled()
}

interface CompressionProgressListener {
    fun onProgressChanged(percent: Float)
    fun onProgressCancelled()
}