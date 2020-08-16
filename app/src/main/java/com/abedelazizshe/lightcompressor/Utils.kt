package com.abedelazizshe.lightcompressor

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import java.io.*
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

fun getMediaPath(context: Context, uri: Uri): String {

    val resolver = context.contentResolver
    val projection = arrayOf(MediaStore.Video.Media.DATA)
    var cursor: Cursor? = null
    try {
        cursor = resolver.query(uri, projection, null, null, null)
        return if (cursor != null) {
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            cursor.moveToFirst()
            cursor.getString(columnIndex)

        } else ""

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

fun getFileSize(size: Long): String {
    if (size <= 0)
        return "0"

    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()

    return DecimalFormat("#,##0.#").format(
        size / 1024.0.pow(digitGroups.toDouble())
    ) + " " + units[digitGroups]
}

//The following methods can be alternative to [getMediaPath].
// todo(abed): remove [getPathFromUri], [getVideoExtension], and [copy]
fun getPathFromUri(context: Context, uri: Uri): String {
    var file: File? = null
    var inputStream: InputStream? = null
    var outputStream: OutputStream? = null
    var success = false
    try {
        val extension: String = getVideoExtension(uri)
        inputStream = context.contentResolver.openInputStream(uri)
        file = File.createTempFile("compressor", extension, context.cacheDir)
        file.deleteOnExit()
        outputStream = FileOutputStream(file)
        if (inputStream != null) {
            copy(inputStream, outputStream)
            success = true
        }
    } catch (ignored: IOException) {
    } finally {
        try {
            inputStream?.close()
        } catch (ignored: IOException) {
        }
        try {
            outputStream?.close()
        } catch (ignored: IOException) {
            // If closing the output stream fails, we cannot be sure that the
            // target file was written in full. Flushing the stream merely moves
            // the bytes into the OS, not necessarily to the file.
            success = false
        }
    }
    return if (success) file!!.path else ""
}

/** @return extension of video with dot, or default .mp4 if it none.
 */
private fun getVideoExtension(uriVideo: Uri): String {
    var extension: String? = null
    try {
        val imagePath = uriVideo.path
        if (imagePath != null && imagePath.lastIndexOf(".") != -1) {
            extension = imagePath.substring(imagePath.lastIndexOf(".") + 1)
        }
    } catch (e: Exception) {
        extension = null
    }
    if (extension == null || extension.isEmpty()) {
        //default extension for matches the previous behavior of the plugin
        extension = "mp4"
    }
    return ".$extension"
}

private fun copy(`in`: InputStream, out: OutputStream) {
    val buffer = ByteArray(4 * 1024)
    var bytesRead: Int
    while (`in`.read(buffer).also { bytesRead = it } != -1) {
        out.write(buffer, 0, bytesRead)
    }
    out.flush()
}
