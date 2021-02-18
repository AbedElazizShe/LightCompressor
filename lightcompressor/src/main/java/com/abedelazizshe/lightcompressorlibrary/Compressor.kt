package com.abedelazizshe.lightcompressorlibrary

import android.media.*
import android.os.Build
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/**
 * Created by AbedElaziz Shehadeh on 27 Jan, 2020
 * elaziz.shehadeh@gmail.com
 */

object Compressor {

    private const val MIN_BITRATE = 2000000
    private const val MIN_HEIGHT = 640.0
    private const val MIN_WIDTH = 360.0
    private const val FRAME_RATE = 30
    private const val I_FRAME_INTERVAL = 2
    private const val MIME_TYPE = "video/avc"
    private const val MEDIACODEC_TIMEOUT_DEFAULT = 100L

    private const val INVALID_BITRATE =
        "The provided bitrate is smaller than what is needed for compression " +
                "try to set isMinBitRateEnabled to false"

    var isRunning = true

    @Suppress("DEPRECATION")
    fun compressVideo(
        source: String,
        destination: String,
        quality: VideoQuality,
        isMinBitRateEnabled: Boolean,
        keepOriginalResolution: Boolean,
        listener: CompressionProgressListener,
    ): Result {

        //Retrieve the source's metadata to be used as input to generate new values for compression
        val mediaMetadataRetriever = MediaMetadataRetriever()
        try {
            mediaMetadataRetriever.setDataSource(source)
        } catch (exception: IllegalArgumentException) {
            return Result(
                success = false,
                failureMessage = "Source path: $source can be invalid! or you don't have READ_EXTERNAL_STORAGE permission"
            )
        }

        val heightData =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)

        val widthData =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)

        val rotationData =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)

        val bitrateData =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)

        val durationData =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

        val height: Double
        val width: Double

        // If height or width data failed to be extracted, reset to the min video size values
        if (heightData.isNullOrEmpty() || widthData.isNullOrEmpty()) {
            height = MIN_HEIGHT
            width = MIN_WIDTH
        } else {
            height = heightData.toDouble()
            width = widthData.toDouble()
        }

        if (rotationData.isNullOrEmpty() || bitrateData.isNullOrEmpty() || durationData.isNullOrEmpty()) {
            // Exit execution
            return Result(
                success = false,
                failureMessage = "Failed to extract video meta-data, please try again"
            )
        }

        var rotation = rotationData.toInt()
        val bitrate = bitrateData.toInt()
        val duration = durationData.toLong() * 1000

        // Check for a min video bitrate before compression
        // Note: this is an experimental value
        if (isMinBitRateEnabled && bitrate <= MIN_BITRATE)
            return Result(success = false, failureMessage = INVALID_BITRATE)

        //Handle new bitrate value
        val newBitrate = getBitrate(bitrate, quality)

        //Handle new width and height values
        var (newWidth, newHeight) = generateWidthAndHeight(
            width,
            height,
            keepOriginalResolution
        )

        //Handle rotation values and swapping height and width if needed
        rotation = when (rotation) {
            90, 270 -> {
                val tempHeight = newHeight
                newHeight = newWidth
                newWidth = tempHeight
                0
            }
            180 -> 0
            else -> rotation
        }

        val file = File(source)
        if (!file.canRead()) return Result(
            success = false,
            failureMessage = "The source file cannot be accessed!"
        )

        var noExceptions = true

        if (newWidth != 0 && newHeight != 0) {

            val cacheFile = File(destination)

            try {
                // MediaCodec accesses encoder and decoder components and processes the new video
                //input to generate a compressed/smaller size video
                val bufferInfo = MediaCodec.BufferInfo()

                // Setup mp4 movie
                val movie = setUpMP4Movie(rotation, cacheFile)

                // MediaMuxer outputs MP4 in this app
                val mediaMuxer = MP4Builder().createMovie(movie)
                // MediaExtractor extracts encoded media data from the source
                val extractor = MediaExtractor()
                extractor.setDataSource(file.toString())

                // Start with video track
                val videoIndex = selectTrack(extractor, isVideo = true)
                extractor.selectTrack(videoIndex)
                extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                val inputFormat = extractor.getTrackFormat(videoIndex)

                val frameRate = getFrameRate(inputFormat)
                val iFrameInterval = getIFrameIntervalRate(inputFormat)

                val outputFormat: MediaFormat =
                    MediaFormat.createVideoFormat(MIME_TYPE, newWidth, newHeight)
                //set output format
                setOutputFileParameters(
                    outputFormat,
                    newBitrate,
                    frameRate,
                    iFrameInterval,
                    newWidth,
                    newHeight,
                )

                var decoder: MediaCodec? = null

                // This seems to cause an issue with certain phones
                //val encoderName = MediaCodecList(REGULAR_CODECS).findEncoderForFormat(outputFormat)
                //  val encoder: MediaCodec = MediaCodec.createByCodecName(encoderName)
                //Log.i("encoderName", encoder.name)
                val encoder: MediaCodec = MediaCodec.createEncoderByType(MIME_TYPE)

                var inputSurface: InputSurface? = null
                var outputSurface: OutputSurface? = null

                try {

                    var inputDone = false
                    var outputDone = false

                    var videoTrackIndex = -5

                    encoder.configure(
                        outputFormat, null, null,
                        MediaCodec.CONFIGURE_FLAG_ENCODE
                    )
                    inputSurface = InputSurface(encoder.createInputSurface())
                    inputSurface.makeCurrent()
                    //Move to executing state
                    encoder.start()

                    outputSurface = OutputSurface()

                    // This seems to cause an issue with certain phones
                    // val decoderName =
                    //     MediaCodecList(REGULAR_CODECS).findDecoderForFormat(inputFormat)
                    // decoder = MediaCodec.createByCodecName(decoderName)
                    // Log.i("decoderName", decoder.name)

                    decoder =
                        MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME)!!)

                    decoder.configure(inputFormat, outputSurface.surface, null, 0)
                    //Move to executing state
                    decoder.start()

                    while (!outputDone) {
                        if (!inputDone) {

                            val index = extractor.sampleTrackIndex

                            if (index == videoIndex) {
                                val inputBufferIndex =
                                    decoder.dequeueInputBuffer(MEDIACODEC_TIMEOUT_DEFAULT)
                                if (inputBufferIndex >= 0) {
                                    val inputBuffer = decoder.getInputBuffer(inputBufferIndex)
                                    val chunkSize = extractor.readSampleData(inputBuffer!!, 0)
                                    when {
                                        chunkSize < 0 -> {
                                            decoder.queueInputBuffer(
                                                inputBufferIndex,
                                                0,
                                                0,
                                                0L,
                                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                            )
                                            inputDone = true
                                        }
                                        else -> {
                                            decoder.queueInputBuffer(
                                                inputBufferIndex,
                                                0,
                                                chunkSize,
                                                extractor.sampleTime,
                                                0
                                            )
                                            extractor.advance()
                                        }
                                    }
                                }

                            } else if (index == -1) { //end of file
                                val inputBufferIndex =
                                    decoder.dequeueInputBuffer(MEDIACODEC_TIMEOUT_DEFAULT)
                                if (inputBufferIndex >= 0) {
                                    decoder.queueInputBuffer(
                                        inputBufferIndex,
                                        0,
                                        0,
                                        0L,
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                    )
                                    inputDone = true
                                }
                            }
                        }


                        var decoderOutputAvailable = true
                        var encoderOutputAvailable = true

                        loop@ while (decoderOutputAvailable || encoderOutputAvailable) {

                            if (!isRunning) {
                                listener.onProgressCancelled()
                                return Result(
                                    success = false,
                                    failureMessage = "The compression has been stopped!"
                                )
                            }

                            //Encoder
                            val encoderStatus =
                                encoder.dequeueOutputBuffer(bufferInfo, MEDIACODEC_TIMEOUT_DEFAULT)

                            when {
                                encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER -> encoderOutputAvailable =
                                    false
                                encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                                    val newFormat = encoder.outputFormat
                                    if (videoTrackIndex == -5)
                                        videoTrackIndex = mediaMuxer.addTrack(newFormat, false)
                                }
                                encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                                    // ignore this status
                                }
                                encoderStatus < 0 -> throw RuntimeException("unexpected result from encoder.dequeueOutputBuffer: $encoderStatus")
                                else -> {
                                    val encodedData = encoder.getOutputBuffer(encoderStatus)
                                        ?: throw RuntimeException("encoderOutputBuffer $encoderStatus was null")

                                    if (bufferInfo.size > 1) {
                                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                            mediaMuxer.writeSampleData(
                                                videoTrackIndex,
                                                encodedData, bufferInfo, false
                                            )

                                        } else if (videoTrackIndex == -5) {
                                            val csd = ByteArray(bufferInfo.size)
                                            encodedData.apply {
                                                limit(bufferInfo.offset + bufferInfo.size)
                                                position(bufferInfo.offset)
                                                get(csd)
                                            }
                                            var sps: ByteBuffer? = null
                                            var pps: ByteBuffer? = null
                                            for (a in bufferInfo.size - 1 downTo 0) {
                                                if (a > 3) {
                                                    if (csd[a].toInt() == 1 && csd[a - 1].toInt() == 0 && csd[a - 2].toInt() == 0 && csd[a - 3].toInt() == 0) {
                                                        sps = ByteBuffer.allocate(a - 3)
                                                        pps =
                                                            ByteBuffer.allocate(bufferInfo.size - (a - 3))
                                                        sps!!.put(csd, 0, a - 3).position(0)
                                                        pps!!.put(
                                                            csd,
                                                            a - 3,
                                                            bufferInfo.size - (a - 3)
                                                        )
                                                            .position(0)
                                                        break
                                                    }
                                                } else {
                                                    break
                                                }
                                            }

                                            val newFormat = MediaFormat.createVideoFormat(
                                                MIME_TYPE,
                                                newWidth,
                                                newHeight
                                            )
                                            if (sps != null && pps != null) {
                                                newFormat.setByteBuffer("csd-0", sps)
                                                newFormat.setByteBuffer("csd-1", pps)
                                            }
                                            videoTrackIndex =
                                                mediaMuxer.addTrack(newFormat, false)
                                        }
                                    }

                                    outputDone =
                                        bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                                    encoder.releaseOutputBuffer(encoderStatus, false)
                                }
                            }
                            if (encoderStatus != MediaCodec.INFO_TRY_AGAIN_LATER) continue@loop


                            //Decoder
                            val decoderStatus =
                                decoder.dequeueOutputBuffer(bufferInfo, MEDIACODEC_TIMEOUT_DEFAULT)
                            when {
                                decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER -> decoderOutputAvailable =
                                    false
                                decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                                    // ignore this status
                                }
                                decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                                    // ignore this status
                                }
                                decoderStatus < 0 -> throw RuntimeException("unexpected result from decoder.dequeueOutputBuffer: $decoderStatus")
                                else -> {
                                    val doRender = bufferInfo.size != 0

                                    decoder.releaseOutputBuffer(decoderStatus, doRender)
                                    if (doRender) {
                                        try {
                                            outputSurface.awaitNewImage()

                                            outputSurface.drawImage()
                                            inputSurface.setPresentationTime(bufferInfo.presentationTimeUs * 1000)

                                            listener.onProgressChanged(bufferInfo.presentationTimeUs.toFloat() / duration.toFloat() * 100)

                                            inputSurface.swapBuffers()
                                        } catch (e: Exception) {
                                            Log.e(
                                                "Compressor",
                                                e.message ?: "Compression failed at swapping buffer"
                                            )
                                        }
                                    }
                                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                        decoderOutputAvailable = false
                                        encoder.signalEndOfInputStream()
                                    }
                                }
                            }
                        }
                    }
                } catch (exception: Exception) {
                    printException(exception)
                    noExceptions = false

                } finally {
                    extractor.unselectTrack(videoIndex)

                    decoder?.stop()
                    decoder?.release()

                    encoder.stop()
                    encoder.release()

                    inputSurface?.release()
                    outputSurface?.release()


                    if (noExceptions) {
                        processAudio(
                            extractor = extractor,
                            mediaMuxer = mediaMuxer,
                            bufferInfo = bufferInfo
                        )
                    }
                }
                extractor.release()
                try {
                    mediaMuxer.finishMovie()
                } catch (e: Exception) {
                    printException(e)
                }

            } catch (exception: Exception) {
                printException(exception)
            }

            return Result(success = true, failureMessage = null)
        }

        return Result(success = false, failureMessage = "Something went wrong, please try again")

    }

    private fun printException(exception: Exception) {
        var message = "An error has occurred!"
        exception.message?.let {
            message = it
        }
        Log.e("Compressor", message)
    }

    /**
     * Get fixed bitrate value based on the file's current bitrate
     * @param bitrate file's current bitrate
     * @return new smaller bitrate value
     */
    private fun getBitrate(
        bitrate: Int,
        quality: VideoQuality,
    ): Int {

        return when (quality) {
            VideoQuality.VERY_LOW -> (bitrate * 0.08).roundToInt()
            VideoQuality.LOW -> (bitrate * 0.1).roundToInt()
            VideoQuality.MEDIUM -> (bitrate * 0.2).roundToInt()
            VideoQuality.HIGH -> (bitrate * 0.3).roundToInt()
            VideoQuality.VERY_HIGH -> (bitrate * 0.5).roundToInt()
        }
    }

    /**
     * Generate new width and height for source file
     * @param width file's original width
     * @param height file's original height
     * @return new width and height pair
     */
    private fun generateWidthAndHeight(
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
                newWidth = (((width * 0.5) / 16).roundToInt() * 16)
                newHeight = (((height * 0.5) / 16f).roundToInt() * 16)
            }
            width >= 1280 || height >= 1280 -> {
                newWidth = (((width * 0.75) / 16).roundToInt() * 16)
                newHeight = (((height * 0.75) / 16).roundToInt() * 16)
            }
            width >= 960 || height >= 960 -> {
                if (width > height) {
                    newWidth = (((MIN_HEIGHT * 0.95) / 16).roundToInt() * 16)
                    newHeight = (((MIN_WIDTH * 0.95) / 16).roundToInt() * 16)
                } else {
                    newWidth = (((MIN_WIDTH * 0.95) / 16).roundToInt() * 16)
                    newHeight = (((MIN_HEIGHT * 0.95) / 16).roundToInt() * 16)
                }
            }
            else -> {
                newWidth = (((width * 0.9) / 16).roundToInt() * 16)
                newHeight = (((height * 0.9) / 16).roundToInt() * 16)
            }
        }

        return Pair(newWidth, newHeight)
    }

    /**
     * Setup movie with the height, width, and rotation values
     * @param rotation video rotation
     *
     * @return set movie with new values
     */
    private fun setUpMP4Movie(
        rotation: Int,
        cacheFile: File,
    ): Mp4Movie {
        val movie = Mp4Movie()
        movie.apply {
            this.cacheFile = cacheFile
            setRotation(rotation)
        }

        return movie
    }

    /**
     * Set output parameters like bitrate and frame rate
     */
    @Suppress("SameParameterValue")
    private fun setOutputFileParameters(
        outputFormat: MediaFormat,
        newBitrate: Int,
        frameRate: Int,
        iFrameInterval: Int,
        resultHeight: Int,
        resultWidth: Int,
    ) {
        outputFormat.apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )

            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)
            setInteger(MediaFormat.KEY_BIT_RATE, newBitrate)

            if (Build.VERSION.SDK_INT >= 23) {
                var profile: Int
                val level: Int
                when {
                    resultHeight.coerceAtMost(resultWidth) >= 1080 -> {
                        profile = MediaCodecInfo.CodecProfileLevel.AVCProfileHigh
                        level = MediaCodecInfo.CodecProfileLevel.AVCLevel41
                    }
                    resultHeight.coerceAtMost(resultWidth) >= 720 -> {
                        profile = MediaCodecInfo.CodecProfileLevel.AVCProfileHigh
                        level = MediaCodecInfo.CodecProfileLevel.AVCLevel4
                    }
                    resultHeight.coerceAtMost(resultWidth) >= 480 -> {
                        profile = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline
                        level = MediaCodecInfo.CodecProfileLevel.AVCLevel31
                    }
                    else -> {
                        profile = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline
                        level = MediaCodecInfo.CodecProfileLevel.AVCLevel3
                    }
                }
                var capabilities: MediaCodecInfo.CodecCapabilities? =
                    MediaCodecInfo.CodecCapabilities.createFromProfileLevel(
                        MIME_TYPE,
                        profile,
                        level
                    )
                if (capabilities == null && profile == MediaCodecInfo.CodecProfileLevel.AVCProfileHigh) {
                    profile = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline
                    capabilities = MediaCodecInfo.CodecCapabilities.createFromProfileLevel(
                        MIME_TYPE,
                        profile,
                        level
                    )
                }
                if (capabilities?.encoderCapabilities != null) {
                    setInteger(MediaFormat.KEY_PROFILE, profile)
                    setInteger(MediaFormat.KEY_LEVEL, level)
                    val maxBitrate: Int =
                        capabilities.videoCapabilities.bitrateRange.upper
                    if (newBitrate > maxBitrate) {
                        setInteger(MediaFormat.KEY_BIT_RATE, maxBitrate)
                    }
                    val maxFrameRate: Int =
                        capabilities.videoCapabilities.supportedFrameRates.upper
                    if (frameRate > maxFrameRate) {
                        setInteger(MediaFormat.KEY_FRAME_RATE, maxFrameRate)
                    }
                }
            } else {
                if (resultHeight.coerceAtMost(resultWidth) <= 480) {
                    if (newBitrate > 921600)
                        setInteger(MediaFormat.KEY_BIT_RATE, 921600)
                }
            }
        }
    }

    /**
     * Counts the number of tracks (video, audio) found in the file source provided
     * @param extractor what is used to extract the encoded data
     * @param isVideo to determine whether we are processing video or audio at time of call
     * @return index of the requested track
     */
    private fun selectTrack(
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

    private fun processAudio(
        extractor: MediaExtractor,
        mediaMuxer: MP4Builder,
        bufferInfo: MediaCodec.BufferInfo,
    ) {
        val audioIndex = selectTrack(extractor, isVideo = false)
        if (audioIndex >= 0) {
            extractor.selectTrack(audioIndex)
            val audioFormat = extractor.getTrackFormat(audioIndex)
            val muxerTrackIndex = mediaMuxer.addTrack(audioFormat, true)
            val maxBufferSize = audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)

            var inputDone = false
            extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            val buffer = ByteBuffer.allocateDirect(maxBufferSize)

            while (!inputDone) {
                val index = extractor.sampleTrackIndex
                if (index == audioIndex) {
                    bufferInfo.size = extractor.readSampleData(buffer, 0)

                    if (bufferInfo.size >= 0) {
                        bufferInfo.apply {
                            presentationTimeUs = extractor.sampleTime
                            offset = 0
                            flags = extractor.sampleFlags
                        }
                        mediaMuxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo, true)
                        extractor.advance()

                    }
                } else if (index == -1) {
                    inputDone = true
                }
            }

            extractor.unselectTrack(audioIndex)

        }
    }

    private fun getFrameRate(format: MediaFormat): Int {
        return if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) format.getInteger(MediaFormat.KEY_FRAME_RATE)
        else FRAME_RATE
    }

    private fun getIFrameIntervalRate(format: MediaFormat): Int {
        return if (format.containsKey(MediaFormat.KEY_I_FRAME_INTERVAL)) format.getInteger(
            MediaFormat.KEY_I_FRAME_INTERVAL
        )
        else I_FRAME_INTERVAL
    }

    /* Calculate and get video bitrate based on original and new sizes of the video
    * Will keep it for reference in case it is needed later
    fun makeVideoBitrate(
        originalHeight: Int,
        originalWidth: Int,
        originalBitrate: Int,
        height: Int,
        width: Int,
    ): Int {
        val compressFactor: Float
        val minCompressFactor: Float
        val maxBitrate: Int
        when {
            min(height, width) >= 1080 -> {
                maxBitrate = 6800000
                compressFactor = 1f
                minCompressFactor = 1f
            }
            min(height, width) >= 720 -> {
                maxBitrate = 2621440
                compressFactor = 1f
                minCompressFactor = 1f
            }
            min(height, width) >= 480 -> {
                maxBitrate = 1000000
                compressFactor = 0.8f
                minCompressFactor = 0.9f
            }
            else -> {
                maxBitrate = 750000
                compressFactor = 0.6f
                minCompressFactor = 0.7f
            }
        }
        var remeasuredBitrate =
            (originalBitrate / min(
                originalHeight / height.toFloat(),
                originalWidth / width.toFloat()
            )).toInt()
        remeasuredBitrate *= compressFactor.toInt()
        val minBitrate =
            (getVideoBitrateWithFactor(minCompressFactor) / (1280f * 720f / (width * height))).toInt()
        if (originalBitrate < minBitrate) {
            return remeasuredBitrate
        }
        return if (remeasuredBitrate > maxBitrate) {
            maxBitrate
        } else max(remeasuredBitrate, minBitrate)
    }

    private fun getVideoBitrateWithFactor(f: Float): Int {
        return (f * 2000f * 1000f * 1.13f).toInt()
    }

     */
}