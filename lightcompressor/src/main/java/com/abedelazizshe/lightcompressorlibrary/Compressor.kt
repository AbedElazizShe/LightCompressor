@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "DEPRECATION")

package com.abedelazizshe.lightcompressorlibrary

import android.media.*
import android.util.Log
import java.io.File
import java.lang.Exception
import java.nio.ByteBuffer

/**
 * Created by AbedElaziz Shehadeh on 27 Jan, 2020
 * elaziz.shehadeh@gmail.com
 */

object Compressor {

    private const val MIN_BITRATE = 2000000
    private const val MIN_HEIGHT = 640
    private const val MIN_WIDTH = 360
    private const val MIME_TYPE = "video/avc"


    fun compressVideo(
        source: String,
        destination: String,
        listener: CompressionProgressListener
    ): Boolean {

        //Retrieve the source's metadata to be used as input to generate new values for compression
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(source)

        val height =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                .toInt()
        val width =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                .toInt()
        var rotation =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                .toInt()
        val bitrate =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                .toInt()
        val duration =
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                .toLong() * 1000

        //There are min values set to determine if the file needs to be compressed or not
        if (bitrate <= MIN_BITRATE || height <= MIN_HEIGHT || width <= MIN_WIDTH) return false

        //Handle new bitrate value
        val newBitrate = getBitrate(bitrate)

        //Handle new width and height values
        var (newWidth, newHeight) = generateWidthAndHeight(width, height)

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
        if (!file.canRead()) return false

        var noExceptions = true

        if (newWidth != 0 && newHeight != 0) {

            val cacheFile = File(destination)

            try {
                // MediaCodec accesses encoder and decoder components and processes the new video
                //input to generate a compressed/smaller size video
                val bufferInfo = MediaCodec.BufferInfo()

                // Setup mp4 movie
                val movie = setUpMP4Movie(rotation, newWidth, newHeight, cacheFile)

                // MediaMuxer outputs MP4 in this app
                val mediaMuxer = MP4Builder().createMovie(movie)
                // MediaExtractor extracts encoded media data from the source
                val extractor = MediaExtractor()
                extractor.setDataSource(file.toString())

                if (newWidth != width || newHeight != height) {

                    // Start with video track
                    val videoIndex = selectTrack(extractor, isVideo = true)
                    extractor.selectTrack(videoIndex)
                    extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                    val inputFormat = extractor.getTrackFormat(videoIndex)
                    val outputFormat: MediaFormat =
                        MediaFormat.createVideoFormat(MIME_TYPE, newWidth, newHeight)

                    var decoder: MediaCodec? = null
                    val encoder: MediaCodec = MediaCodec.createEncoderByType(MIME_TYPE)

                    var inputSurface: InputSurface? = null
                    var outputSurface: OutputSurface? = null

                    try {

                        var inputDone = false
                        var outputDone = false

                        var videoTrackIndex = -5

                        // MediaCodecInfo provides information about a media codec
                        val colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface

                        //set output format
                        setOutputFileParameters(outputFormat, colorFormat, newBitrate)

                        encoder.configure(
                            outputFormat, null, null,
                            MediaCodec.CONFIGURE_FLAG_ENCODE
                        )
                        inputSurface = InputSurface(encoder.createInputSurface())
                        inputSurface.makeCurrent()
                        //Move to executing state
                        encoder.start()

                        outputSurface = OutputSurface()
                        decoder =
                            MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME))
                        decoder.configure(inputFormat, outputSurface.surface, null, 0)
                        //Move to executing state
                        decoder.start()

                        while (!outputDone) {
                            if (!inputDone) {

                                val index = extractor.sampleTrackIndex

                                if (index == videoIndex) {
                                    val inputBufferIndex = decoder.dequeueInputBuffer(0)
                                    if (inputBufferIndex >= 0) {
                                        val inputBuffer = decoder.getInputBuffer(inputBufferIndex)
                                        val chunkSize = extractor.readSampleData(inputBuffer, 0)
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
                                    val inputBufferIndex = decoder.dequeueInputBuffer(0)
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
                                //Encoder
                                val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, 0)

                                when {
                                    encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER -> encoderOutputAvailable =
                                        false
                                    encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                                        val newFormat = encoder.outputFormat
                                        if (videoTrackIndex == -5) videoTrackIndex =
                                            mediaMuxer.addTrack(newFormat, false)
                                    }
                                    encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                                    }
                                    encoderStatus < 0 -> throw RuntimeException("unexpected result from encoder.dequeueOutputBuffer: $encoderStatus")
                                    else -> {
                                        val encodedData = encoder.getOutputBuffer(encoderStatus)
                                            ?: throw RuntimeException("encoderOutputBuffer $encoderStatus was null")

                                        if (bufferInfo.size > 1) {
                                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
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
                                val decoderStatus = decoder.dequeueOutputBuffer(bufferInfo, 0)
                                when {
                                    decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER -> decoderOutputAvailable =
                                        false
                                    decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                                    }
                                    decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
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
                                                Log.e("Compressor", e.message)
                                            }

                                        }

                                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                            decoderOutputAvailable = false
                                            encoder.signalEndOfInputStream()
                                        }
                                    }
                                }
                            }
                        }
                    } catch (exception: Exception) {
                        Log.e("Compressor", exception.message)
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

                }else{
                    return false
                }
                extractor.release()
                try {
                    mediaMuxer.finishMovie()
                } catch (e: Exception) {
                    Log.e("Compressor", e.message)
                }

            } catch (exception: Exception) {
                Log.e("Compressor", exception.message)
            }

            return true
        }

        return false

    }

    /**
     * Get fixed bitrate value based on the file's current bitrate
     * @param bitrate file's current bitrate
     * @return new smaller bitrate value
     */
    private fun getBitrate(bitrate: Int): Int {
        return when {
            bitrate >= 15000000 -> 2000000 // > 15MB becomes 2MB
            bitrate >= 8000000 -> 1500000 // > 8MB becomes 1.5MB
            bitrate >= 4000000 -> 1000000 // > 4MB becomes 1MB
            else -> 750000 // other values become 750KB
        }
    }

    /**
     * Generate new width and height for source file
     * @param width file's original width
     * @param height file's original height
     * @return new width and height pair
     */
    private fun generateWidthAndHeight(width: Int, height: Int): Pair<Int, Int> {

        val newWidth: Int
        val newHeight: Int

        when {
            width >= 1920 || height >= 1920 -> {
                newWidth = (width * 0.5).toInt()
                newHeight = (height * 0.5).toInt()
            }
            width >= 1280 || height >= 1280 -> {
                newWidth = (width * 0.75).toInt()
                newHeight = (height * 0.75).toInt()
            }
            width >= 960 || height >= 960 -> {
                newWidth = MIN_HEIGHT
                newHeight = MIN_WIDTH
            }
            else -> {
                newWidth = width
                newHeight = height
            }
        }

        return Pair(newWidth, newHeight)
    }

    /**
     * Setup movie with the height, width, and rotation values
     * @param rotation video rotation
     * @param newWidth video width value
     * @param newHeight video height value
     *
     * @return set movie with new values
     */
    private fun setUpMP4Movie(rotation: Int, newWidth: Int, newHeight: Int, cacheFile: File) : Mp4Movie{
        val movie = Mp4Movie()
        movie.apply {
            this.cacheFile = cacheFile
            setRotation(rotation)
            setSize(newWidth, newHeight)
        }

        return movie
    }

      /**
     * Set output parameters like bitrate and frame rate
     */
    @Suppress("SameParameterValue")
    private fun setOutputFileParameters(
        outputFormat: MediaFormat,
        colorFormat: Int,
        newBitrate: Int
    ) {
        outputFormat.apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
            setInteger(MediaFormat.KEY_BIT_RATE, newBitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 15)
        }
    }

    /**
     * Counts the number of tracks (video, audio) found in the file source provided
     * @param extractor what is used to extract the encoded data
     * @param isVideo to determine whether we are processing video or audio at time of call
     * @return index of the requested track
     */
    private fun selectTrack(extractor: MediaExtractor, isVideo: Boolean): Int {
        val numTracks = extractor.trackCount
        for (i in 0 until numTracks) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (isVideo) {
                if (mime.startsWith("video/")) return i
            } else {
                if (mime.startsWith("audio/")) return i
            }
        }
        return -5
    }

    private fun processAudio(
        extractor: MediaExtractor, mediaMuxer: MP4Builder,
        bufferInfo: MediaCodec.BufferInfo
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

}