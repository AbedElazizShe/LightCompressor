package com.abedelazizshe.lightcompressorlibrary

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.abedelazizshe.lightcompressorlibrary.compressor.Compressor.compressVideo
import com.abedelazizshe.lightcompressorlibrary.compressor.Compressor.isRunning
import com.abedelazizshe.lightcompressorlibrary.config.*
import com.abedelazizshe.lightcompressorlibrary.video.Result
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

enum class VideoQuality {
    VERY_HIGH, HIGH, MEDIUM, LOW, VERY_LOW
}

object VideoCompressor : CoroutineScope by MainScope() {

    private var job: Job? = null

    /**
     * This function compresses a given list of [uris] of video files and writes the compressed
     * video file at [SharedStorageConfiguration.saveAt] directory, or at [AppSpecificStorageConfiguration.subFolderName]
     *
     * The source videos should be provided content uris.
     *
     * Only [sharedStorageConfiguration] or [appSpecificStorageConfiguration] must be specified at a
     * time. Passing both will throw an Exception.
     *
     * @param [context] the application context.
     * @param [uris] the list of content Uris of the video files.
     * @param [isStreamable] determines if the output video should be prepared for streaming.
     * @param [sharedStorageConfiguration] configuration for the path directory where the compressed
     * videos will be saved, and the name of the file
     * @param [appSpecificStorageConfiguration] configuration for the path directory where the compressed
     * videos will be saved, the name of the file, and any sub-folders name. The library won't create the subfolder
     * and will throw an exception if the subfolder does not exist.
     * @param [listener] a compression listener that listens to compression [CompressionListener.onStart],
     * [CompressionListener.onProgress], [CompressionListener.onFailure], [CompressionListener.onSuccess]
     * and if the compression was [CompressionListener.onCancelled]
     * @param [configureWith] to allow add video compression configuration that could be:
     * [Configuration.quality] to allow choosing a video quality that can be [VideoQuality.LOW],
     * [VideoQuality.MEDIUM], [VideoQuality.HIGH], and [VideoQuality.VERY_HIGH].
     * This defaults to [VideoQuality.MEDIUM]
     * [Configuration.isMinBitrateCheckEnabled] to determine if the checking for a minimum bitrate threshold
     * before compression is enabled or not. This default to `true`
     * [Configuration.videoBitrateInMbps] which is a custom bitrate for the video. You might consider setting
     * [Configuration.isMinBitrateCheckEnabled] to `false` if your bitrate is less than 2000000.
     *  * [Configuration.keepOriginalResolution] to keep the original video height and width when compressing.
     * This defaults to `false`
     * [Configuration.videoHeight] which is a custom height for the video. Must be specified with [Configuration.videoWidth]
     * [Configuration.videoWidth] which is a custom width for the video. Must be specified with [Configuration.videoHeight]
     */
    @JvmStatic
    @JvmOverloads
    fun start(
        context: Context,
        uris: List<Uri>,
        isStreamable: Boolean = false,
        sharedStorageConfiguration: SharedStorageConfiguration? = null,
        appSpecificStorageConfiguration: AppSpecificStorageConfiguration? = null,
        configureWith: Configuration,
        listener: CompressionListener,
    ) {
        // Only one is allowed
        assert(sharedStorageConfiguration == null || appSpecificStorageConfiguration == null)

        doVideoCompression(
            context,
            uris,
            isStreamable,
            sharedStorageConfiguration,
            appSpecificStorageConfiguration,
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
        context: Context,
        uris: List<Uri>,
        isStreamable: Boolean,
        sharedStorageConfiguration: SharedStorageConfiguration?,
        appSpecificStorageConfiguration: AppSpecificStorageConfiguration?,
        configuration: Configuration,
        listener: CompressionListener,
    ) {
        var streamableFile: File? = null
        for (i in uris.indices) {

            job = launch {

                val job = async { getMediaPath(context, uris[i]) }
                val path = job.await()

                val desFile = saveVideoFile(
                    context, path, sharedStorageConfiguration,
                    appSpecificStorageConfiguration, isStreamable
                )

                if (isStreamable)
                    streamableFile = saveVideoFile(
                        context, path, sharedStorageConfiguration,
                        appSpecificStorageConfiguration, null
                    )

                desFile?.let {
                    isRunning = true
                    listener.onStart(i)
                    val result = startCompression(
                        i,
                        context,
                        uris[i],
                        desFile.path,
                        streamableFile?.path,
                        configuration,
                        listener,
                    )

                    // Runs in Main(UI) Thread
                    if (result.success) {
                        listener.onSuccess(i, result.size, result.path)
                    } else {
                        listener.onFailure(i, result.failureMessage ?: "An error has occurred!")
                    }
                }
            }
        }
    }

    private suspend fun startCompression(
        index: Int,
        context: Context,
        srcUri: Uri,
        destPath: String,
        streamableFile: String? = null,
        configuration: Configuration,
        listener: CompressionListener,
    ): Result = withContext(Dispatchers.Default) {
        return@withContext compressVideo(
            index,
            context,
            srcUri,
            destPath,
            streamableFile,
            configuration,
            object : CompressionProgressListener {
                override fun onProgressChanged(index: Int, percent: Float) {
                    listener.onProgress(index, percent)
                }

                override fun onProgressCancelled(index: Int) {
                    listener.onCancelled(index)
                }
            },
        )
    }


    private fun saveVideoFile(
        context: Context,
        filePath: String?,
        sharedStorageConfiguration: SharedStorageConfiguration?,
        appSpecificStorageConfiguration: AppSpecificStorageConfiguration?,
        isStreamable: Boolean?
    ): File? {
        filePath?.let {
            val videoFile = File(filePath)

            if (sharedStorageConfiguration != null) {
                val videoFileName = validatedFileName(
                    sharedStorageConfiguration.videoName,
                    isStreamable
                )

                val saveLocation =
                    when (sharedStorageConfiguration.saveAt) {
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

                if (Build.VERSION.SDK_INT >= 30) {
                    return saveVideoInExternal(context, videoFileName, saveLocation, videoFile)
                } else {
                    val savePath =
                        Environment.getExternalStoragePublicDirectory(saveLocation)

                    val desFile = File(savePath, videoFileName)

                    if (desFile.exists())
                        desFile.delete()

                    try {
                        desFile.createNewFile()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    return desFile
                }
            } else {
                val videoFileName = validatedFileName(
                    appSpecificStorageConfiguration!!.videoName,
                    isStreamable
                )


                return saveVideoInInternal(
                    context,
                    videoFileName,
                    appSpecificStorageConfiguration.subFolderName,
                    videoFile
                )
            }
        }
        return null
    }

    private fun saveVideoInInternal(
        context: Context,
        videoFileName: String,
        subFolderName: String?,
        videoFile: File
    ): File {
        context.openFileOutput(videoFileName, Context.MODE_PRIVATE)
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
        val fullPath = if (subFolderName != null) "$subFolderName/$videoFileName"
        else videoFileName

        return File(context.filesDir, fullPath)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveVideoInExternal(
        context: Context,
        videoFileName: String,
        saveLocation: String,
        videoFile: File
    ): File? {
        val values = ContentValues().apply {

            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                videoFileName
            )
            put(MediaStore.Images.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Images.Media.RELATIVE_PATH, saveLocation)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        var collection =
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        if (saveLocation == Environment.DIRECTORY_DOWNLOADS) {
            collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        }

        val fileUri = context.contentResolver.insert(collection, values)

        fileUri?.let {
            context.contentResolver.openFileDescriptor(fileUri, "rw")
                .use { descriptor ->
                    descriptor?.let {
                        FileOutputStream(descriptor.fileDescriptor).use { out ->
                            FileInputStream(videoFile).use { inputStream ->
                                val buf = ByteArray(4096)
                                while (true) {
                                    val sz = inputStream.read(buf)
                                    if (sz <= 0) break
                                    out.write(buf, 0, sz)
                                }
                            }
                        }
                    }
                }

            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            context.contentResolver.update(fileUri, values, null, null)

            return File(getMediaPath(context, fileUri))
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun getMediaPath(context: Context, uri: Uri): String {

        val resolver = context.contentResolver
        val projection = arrayOf(MediaStore.Video.Media.DATA)
        var cursor: Cursor? = null
        try {
            cursor = resolver.query(uri, projection, null, null, null)
            return if (cursor != null) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                cursor.moveToFirst()
                cursor.getString(columnIndex)

            } else throw Exception()

        } catch (e: Exception) {
            resolver.let {
                val filePath = (context.applicationInfo.dataDir + File.separator
                        + System.currentTimeMillis())
                val file = File(filePath)

                resolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        val buf = ByteArray(4096)
                        var len: Int
                        while (inputStream.read(buf).also { len = it } > 0) outputStream.write(
                            buf,
                            0,
                            len
                        )
                    }
                }
                return file.absolutePath
            }
        } finally {
            cursor?.close()
        }
    }

    private fun validatedFileName(name: String, isStreamable: Boolean?): String {
        val videoName = if (isStreamable == null || !isStreamable) name
        else "${name}_temp"

        if (!videoName.contains("mp4")) return "${videoName}.mp4"
        return videoName
    }
}
