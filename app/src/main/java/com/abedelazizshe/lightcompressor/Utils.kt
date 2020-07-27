package com.abedelazizshe.lightcompressor

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow


fun getMediaPath(context: Context, uri: Uri?): String {
    val projection = arrayOf(MediaStore.Video.Media.DATA)
    var cursor: Cursor? = null
    try {
        cursor = context.contentResolver.query(uri!!, projection, null, null, null)
        return if (cursor != null) {
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            cursor.moveToFirst()
            cursor.getString(columnIndex)

        } else ""
    }finally {
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