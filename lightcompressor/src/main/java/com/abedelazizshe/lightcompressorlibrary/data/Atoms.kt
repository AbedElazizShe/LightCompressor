package com.abedelazizshe.lightcompressorlibrary.data

import java.nio.ByteBuffer
import java.nio.ByteOrder

/*
FOURCC is short for "four character code" - an identifier for a video codec, compression format,
color or pixel format used in media files.

A character in this context is a 1 byte/8 bit value, thus a FOURCC always takes up exactly 32 bits/4
bytes in a file.
*/
fun fourCcToInt(byteArray: ByteArray): Int {
    // The bytes of a byteArray value are ordered from most significant to least significant.
    return ByteBuffer.wrap(byteArray).order(ByteOrder.BIG_ENDIAN).int
}

// Unused space available in file.
val FREE_ATOM =
    fourCcToInt(
        byteArrayOf(
            'f'.code.toByte(),
            'r'.code.toByte(),
            'e'.code.toByte(),
            'e'.code.toByte()
        )
    )

val JUNK_ATOM =
    fourCcToInt(
        byteArrayOf(
            'j'.code.toByte(),
            'u'.code.toByte(),
            'n'.code.toByte(),
            'k'.code.toByte()
        )
    )

// Movie sample data— media samples such as video frames and groups of audio samples. Usually this
// data can be interpreted only by using the movie resource.
val MDAT_ATOM =
    fourCcToInt(
        byteArrayOf(
            'm'.code.toByte(),
            'd'.code.toByte(),
            'a'.code.toByte(),
            't'.code.toByte()
        )
    )

// Movie resource metadata about the movie (number and type of tracks, location of sample data,
// and so on). Describes where the movie data can be found and how to interpret it.
val MOOV_ATOM =
    fourCcToInt(
        byteArrayOf(
            'm'.code.toByte(),
            'o'.code.toByte(),
            'o'.code.toByte(),
            'v'.code.toByte()
        )
    )

// Reference to movie preview data.
val PNOT_ATOM =
    fourCcToInt(
        byteArrayOf(
            'p'.code.toByte(),
            'n'.code.toByte(),
            'o'.code.toByte(),
            't'.code.toByte()
        )
    )

// Unused space available in file.
val SKIP_ATOM =
    fourCcToInt(
        byteArrayOf(
            's'.code.toByte(),
            'k'.code.toByte(),
            'i'.code.toByte(),
            'p'.code.toByte()
        )
    )

// Reserved space—can be overwritten by an extended size field if the following atom exceeds 2^32
// bytes, without displacing the contents of the following atom.
val WIDE_ATOM =
    fourCcToInt(
        byteArrayOf(
            'w'.code.toByte(),
            'i'.code.toByte(),
            'd'.code.toByte(),
            'e'.code.toByte()
        )
    )

val PICT_ATOM =
    fourCcToInt(
        byteArrayOf(
            'P'.code.toByte(),
            'I'.code.toByte(),
            'C'.code.toByte(),
            'T'.code.toByte()
        )
    )

// File type compatibility— identifies the file type and differentiates it from similar file
// types, such as MPEG-4 files and JPEG-2000 files.
val FTYP_ATOM =
    fourCcToInt(
        byteArrayOf(
            'f'.code.toByte(),
            't'.code.toByte(),
            'y'.code.toByte(),
            'p'.code.toByte()
        )
    )

val UUID_ATOM =
    fourCcToInt(
        byteArrayOf(
            'u'.code.toByte(),
            'u'.code.toByte(),
            'i'.code.toByte(),
            'd'.code.toByte()
        )
    )

val CMOV_ATOM =
    fourCcToInt(
        byteArrayOf(
            'c'.code.toByte(),
            'm'.code.toByte(),
            'o'.code.toByte(),
            'v'.code.toByte()
        )
    )

val STCO_ATOM =
    fourCcToInt(
        byteArrayOf(
            's'.code.toByte(),
            't'.code.toByte(),
            'c'.code.toByte(),
            'o'.code.toByte()
        )
    )

val CO64_ATOM =
    fourCcToInt(
        byteArrayOf(
            'c'.code.toByte(),
            'o'.code.toByte(),
            '6'.code.toByte(),
            '4'.code.toByte()
        )
    )
