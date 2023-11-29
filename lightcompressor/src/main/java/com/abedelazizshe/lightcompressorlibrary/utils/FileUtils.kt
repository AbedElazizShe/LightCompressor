package com.abedelazizshe.lightcompressorlibrary.utils

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

fun saveVideoInExternal(
    context: Context,
    videoFileName: String,
    saveLocation: String,
    videoFile: File
) {
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
    }
}

