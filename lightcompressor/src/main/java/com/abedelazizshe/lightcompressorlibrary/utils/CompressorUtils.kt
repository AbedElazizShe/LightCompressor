package com.abedelazizshe.lightcompressorlibrary.utils

import android.media.*
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.video.Mp4Movie
import java.io.File
import kotlin.math.roundToInt

object CompressorUtils {

    private const val MIN_HEIGHT = 640.0
    private const val MIN_WIDTH = 368.0

    // 1 second between I-frames
    private const val I_FRAME_INTERVAL = 1

    fun prepareVideoWidth(
        mediaMetadataRetriever: MediaMetadataRetriever,
    ): Double {
        val widthData =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        return if (widthData.isNullOrEmpty()) {
            MIN_WIDTH
        } else {
            widthData.toDouble()
        }
    }

    fun prepareVideoHeight(
        mediaMetadataRetriever: MediaMetadataRetriever,
    ): Double {
        val heightData =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        return if (heightData.isNullOrEmpty()) {
            MIN_HEIGHT
        } else {
            heightData.toDouble()
        }
    }

    /**
     * Setup movie with the height, width, and rotation values
     * @param rotation video rotation
     *
     * @return set movie with new values
     */
    fun setUpMP4Movie(
        rotation: Int,
        cacheFile: File,
    ): Mp4Movie {
        val movie = Mp4Movie()
        movie.apply {
            setCacheFile(cacheFile)
            setRotation(rotation)
        }

        return movie
    }

    /**
     * Set output parameters like bitrate and frame rate
     */
    fun setOutputFileParameters(
        inputFormat: MediaFormat,
        outputFormat: MediaFormat,
        newBitrate: Int,
    ) {
        val newFrameRate = getFrameRate(inputFormat)
        val iFrameInterval = getIFrameIntervalRate(inputFormat)
        outputFormat.apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )

            setInteger(MediaFormat.KEY_FRAME_RATE, newFrameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)
            // expected bps
            setInteger(MediaFormat.KEY_BIT_RATE, newBitrate)

            if (Build.VERSION.SDK_INT > 23) {

                getColorStandard(inputFormat)?.let {
                    setInteger(MediaFormat.KEY_COLOR_STANDARD, it)
                }

                getColorTransfer(inputFormat)?.let {
                    setInteger(MediaFormat.KEY_COLOR_TRANSFER, it)
                }

                getColorRange(inputFormat)?.let {
                    setInteger(MediaFormat.KEY_COLOR_RANGE, it)
                }
            }

            Log.i(
                "Output file parameters",
                "videoFormat: $this"
            )
        }
    }

    private fun getFrameRate(format: MediaFormat): Int {
        return if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) format.getInteger(MediaFormat.KEY_FRAME_RATE)
        else 30
    }

    private fun getIFrameIntervalRate(format: MediaFormat): Int {
        return if (format.containsKey(MediaFormat.KEY_I_FRAME_INTERVAL)) format.getInteger(
            MediaFormat.KEY_I_FRAME_INTERVAL
        )
        else I_FRAME_INTERVAL
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun getColorStandard(format: MediaFormat): Int? {
        return if (format.containsKey(MediaFormat.KEY_COLOR_STANDARD)) format.getInteger(
            MediaFormat.KEY_COLOR_STANDARD
        )
        else null
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun getColorTransfer(format: MediaFormat): Int? {
        return if (format.containsKey(MediaFormat.KEY_COLOR_TRANSFER)) format.getInteger(
            MediaFormat.KEY_COLOR_TRANSFER
        )
        else null
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun getColorRange(format: MediaFormat): Int? {
        return if (format.containsKey(MediaFormat.KEY_COLOR_RANGE)) format.getInteger(
            MediaFormat.KEY_COLOR_RANGE
        )
        else null
    }

    /**
     * Counts the number of tracks (video, audio) found in the file source provided
     * @param extractor what is used to extract the encoded data
     * @param isVideo to determine whether we are processing video or audio at time of call
     * @return index of the requested track
     */
    fun findTrack(
        extractor: MediaExtractor,
        isVideo: Boolean,
    ): Int {
        val numTracks = extractor.trackCount
        for (i in 0 until numTracks) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (isVideo) {
                if (mime?.startsWith("video/")!!) return i
            } else {
                if (mime?.startsWith("audio/")!!) return i
            }
        }
        return -5
    }

    fun printException(exception: Exception) {
        var message = "An error has occurred!"
        exception.localizedMessage?.let {
            message = it
        }
        Log.e("Compressor", message, exception)
    }

    /**
     * Get fixed bitrate value based on the file's current bitrate
     * @param bitrate file's current bitrate
     * @return new smaller bitrate value
     */
    fun getBitrate(
        bitrate: Int,
        quality: VideoQuality,
    ): Int {
        return when (quality) {
            VideoQuality.VERY_LOW -> (bitrate * 0.1).roundToInt()
            VideoQuality.LOW -> (bitrate * 0.2).roundToInt()
            VideoQuality.MEDIUM -> (bitrate * 0.3).roundToInt()
            VideoQuality.HIGH -> (bitrate * 0.4).roundToInt()
            VideoQuality.VERY_HIGH -> (bitrate * 0.6).roundToInt()
        }
    }

    /**
     * Generate new width and height for source file
     * @param width file's original width
     * @param height file's original height
     * @return new width and height pair
     */
    fun generateWidthAndHeight(
        width: Double,
        height: Double,
        keepOriginalResolution: Boolean,
    ): Pair<Int, Int> {

        if (keepOriginalResolution) {
            return Pair(width.roundToInt(), height.roundToInt())
        }

        val newWidth: Int
        val newHeight: Int

        when {
            width >= 1920 || height >= 1920 -> {
                newWidth = generateWidthHeightValue(width, 0.5)
                newHeight = generateWidthHeightValue(height, 0.5)
            }
            width >= 1280 || height >= 1280 -> {
                newWidth = generateWidthHeightValue(width, 0.75)
                newHeight = generateWidthHeightValue(height, 0.75)
            }
            width >= 960 || height >= 960 -> {
                newWidth = generateWidthHeightValue(width, 0.95)
                newHeight = generateWidthHeightValue(height, 0.95)
            }
            else -> {
                newWidth = generateWidthHeightValue(width, 0.9)
                newHeight = generateWidthHeightValue(height, 0.9)
            }
        }

        return Pair(newWidth, newHeight)
    }

    fun hasQTI(): Boolean {
        val list = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
        for (codec in list) {
            Log.i("CODECS: ", codec.name)
            if (codec.name.contains("qti.avc")) {
                return true
            }
        }
        return false
    }
}

/*
            if (codec.name == "c2.qti.avc.encoder") {
                val capabilities = codec.getCapabilitiesForType("video/avc")


                for (c in capabilities.colorFormats) {
                    Log.wtf("color format", c.toString())
                }

                for (c in capabilities.profileLevels) {
                    Log.wtf(" level", c.level.toString())
                    Log.wtf("profile ", c.profile.toString())
                }

                Log.wtf(
                    "complexity range",
                    capabilities.encoderCapabilities.complexityRange.upper.toString()
                )

                Log.wtf(
                    "quality range", " ${ capabilities.encoderCapabilities.qualityRange}"
                )

                Log.wtf(
                    "frame rates range", " ${ capabilities.videoCapabilities.supportedFrameRates}"
                )

                Log.wtf(
                    "bitrate rates range", " ${ capabilities.videoCapabilities.bitrateRange}"
                )

                Log.wtf(
                    "mode supported", " ${ capabilities.encoderCapabilities.isBitrateModeSupported(1)}"
                )

                Log.wtf(
                    "height alignment", " ${ capabilities.videoCapabilities.heightAlignment}"
                )

                Log.wtf(
                    "supported heights", " ${ capabilities.videoCapabilities.supportedHeights}"
                )

                Log.wtf(
                    "supported points", " ${ capabilities.videoCapabilities.supportedPerformancePoints}"
                )

                Log.wtf(
                    "supported width", " ${ capabilities.videoCapabilities.supportedWidths}"
                )

                Log.wtf(
                    "width alignment", " ${ capabilities.videoCapabilities.widthAlignment}"
                )

                Log.wtf(
                    "default format", " ${ capabilities.defaultFormat}"
                )

                Log.wtf(
                    "mime", " ${ capabilities.mimeType}"
                )

            }
 */