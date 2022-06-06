package com.abedelazizshe.lightcompressorlibrary.video

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import com.coremedia.iso.boxes.SampleDescriptionBox
import com.coremedia.iso.boxes.sampleentry.AudioSampleEntry
import com.coremedia.iso.boxes.sampleentry.VisualSampleEntry
import com.googlecode.mp4parser.boxes.mp4.ESDescriptorBox
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.AudioSpecificConfig
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.DecoderConfigDescriptor
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.ESDescriptor
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.SLConfigDescriptor
import com.mp4parser.iso14496.part15.AvcConfigurationBox
import java.util.*

class Track(id: Int, format: MediaFormat, audio: Boolean) {

    private var trackId: Long = 0
    private val samples = ArrayList<Sample>()
    private var duration: Long = 0
    private var handler: String
    private var sampleDescriptionBox: SampleDescriptionBox
    private var syncSamples: LinkedList<Int>? = null
    private var timeScale = 0
    private val creationTime = Date()
    private var height = 0
    private var width = 0
    private var volume = 0f
    private val sampleDurations = ArrayList<Long>()
    private val isAudio = audio
    private var samplingFrequencyIndexMap: Map<Int, Int> = HashMap()
    private var lastPresentationTimeUs: Long = 0
    private var first = true

    init {
        samplingFrequencyIndexMap = mapOf(
            96000 to 0x0,
            88200 to 0x1,
            64000 to 0x2,
            48000 to 0x3,
            44100 to 0x4,
            32000 to 0x5,
            24000 to 0x6,
            22050 to 0x7,
            16000 to 0x8,
            12000 to 0x9,
            11025 to 0xa,
            8000 to 0xb,
        )

        trackId = id.toLong()
        if (!isAudio) {
            sampleDurations.add(3015.toLong())
            duration = 3015
            width = format.getInteger(MediaFormat.KEY_WIDTH)
            height = format.getInteger(MediaFormat.KEY_HEIGHT)
            timeScale = 90000
            syncSamples = LinkedList()
            handler = "vide"

            sampleDescriptionBox = SampleDescriptionBox()
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime == "video/avc") {
                val visualSampleEntry =
                    VisualSampleEntry(VisualSampleEntry.TYPE3).setup(width, height)

                val avcConfigurationBox = AvcConfigurationBox()
                if (format.getByteBuffer("csd-0") != null) {
                    val spsArray = ArrayList<ByteArray>()
                    val spsBuff = format.getByteBuffer("csd-0")
                    spsBuff!!.position(4)

                    val spsBytes = ByteArray(spsBuff.remaining())
                    spsBuff[spsBytes]
                    spsArray.add(spsBytes)

                    val ppsArray = ArrayList<ByteArray>()
                    val ppsBuff = format.getByteBuffer("csd-1")
                    ppsBuff?.let {
                        it.position(4)

                        val ppsBytes = ByteArray(it.remaining())
                        it[ppsBytes]

                        ppsArray.add(ppsBytes)
                        avcConfigurationBox.sequenceParameterSets = spsArray
                        avcConfigurationBox.pictureParameterSets = ppsArray
                    }
                }

                if (format.containsKey("level")) {
                    when (format.getInteger("level")) {
                        MediaCodecInfo.CodecProfileLevel.AVCLevel1 -> {
                            avcConfigurationBox.avcLevelIndication = 1
                        }
                        MediaCodecInfo.CodecProfileLevel.AVCLevel2 -> {
                            avcConfigurationBox.avcLevelIndication = 2
                        }
                        MediaCodecInfo.CodecProfileLevel.AVCLevel11 -> {
                            avcConfigurationBox.avcLevelIndication = 11
                        }
                        MediaCodecInfo.CodecProfileLevel.AVCLevel12 -> {
                            avcConfigurationBox.avcLevelIndication = 12
                        }
                        MediaCodecInfo.CodecProfileLevel.AVCLevel13 -> {
                            avcConfigurationBox.avcLevelIndication = 13
                        }
                        MediaCodecInfo.CodecProfileLevel.AVCLevel21 -> {
                            avcConfigurationBox.avcLevelIndication = 21
                        }
                        MediaCodecInfo.CodecProfileLevel.AVCLevel22 -> {
                            avcConfigurationBox.avcLevelIndication = 22
                        }
                        MediaCodecInfo.CodecProfileLevel.AVCLevel3 -> {
                            avcConfigurationBox.avcLevelIndication = 3
                        }
                        MediaCodecInfo.CodecProfileLevel.AVCLevel31 -> {
                            avcConfigurationBox.avcLevelIndication = 31
                        }
                        MediaCodecInfo.CodecProfileLevel.AVCLevel32 -> {
                            avcConfigurationBox.avcLevelIndication = 32
                        }
                        MediaCodecInfo.CodecProfileLevel.AVCLevel4 -> {
                            avcConfigurationBox.avcLevelIndication = 4
                        }
                        MediaCodecInfo.CodecProfileLevel.AVCLevel41 -> {
                            avcConfigurationBox.avcLevelIndication = 41
                        }
                        MediaCodecInfo.CodecProfileLevel.AVCLevel42 -> {
                            avcConfigurationBox.avcLevelIndication = 42
                        }
                        MediaCodecInfo.CodecProfileLevel.AVCLevel5 -> {
                            avcConfigurationBox.avcLevelIndication = 5
                        }
                        MediaCodecInfo.CodecProfileLevel.AVCLevel51 -> {
                            avcConfigurationBox.avcLevelIndication = 51
                        }
                        MediaCodecInfo.CodecProfileLevel.AVCLevel52 -> {
                            avcConfigurationBox.avcLevelIndication = 52
                        }
                        MediaCodecInfo.CodecProfileLevel.AVCLevel1b -> {
                            avcConfigurationBox.avcLevelIndication = 0x1b
                        }
                        else -> avcConfigurationBox.avcLevelIndication = 13
                    }
                } else {
                    avcConfigurationBox.avcLevelIndication = 13
                }

                if (format.containsKey("profile")) {
                    when (format.getInteger("profile")) {
                        MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline -> {
                            avcConfigurationBox.avcProfileIndication = 66
                        }
                        MediaCodecInfo.CodecProfileLevel.AVCProfileMain -> {
                            avcConfigurationBox.avcProfileIndication = 77
                        }
                        MediaCodecInfo.CodecProfileLevel.AVCProfileExtended -> {
                            avcConfigurationBox.avcProfileIndication = 88
                        }
                        MediaCodecInfo.CodecProfileLevel.AVCProfileHigh -> {
                            avcConfigurationBox.avcProfileIndication = 100
                        }
                        MediaCodecInfo.CodecProfileLevel.AVCProfileHigh10 -> {
                            avcConfigurationBox.avcProfileIndication = 110
                        }
                        MediaCodecInfo.CodecProfileLevel.AVCProfileHigh422 -> {
                            avcConfigurationBox.avcProfileIndication = 122
                        }
                        MediaCodecInfo.CodecProfileLevel.AVCProfileHigh444 -> {
                            avcConfigurationBox.avcProfileIndication = 244
                        }
                        MediaCodecInfo.CodecProfileLevel.AVCProfileConstrainedHigh -> {
                            avcConfigurationBox.avcProfileIndication = 488
                        }
                        MediaCodecInfo.CodecProfileLevel.AVCProfileConstrainedBaseline -> {
                            avcConfigurationBox.avcProfileIndication = 244
                        }
                        else -> avcConfigurationBox.avcProfileIndication = 100
                    }
                } else {
                    avcConfigurationBox.avcProfileIndication = 100
                }
                avcConfigurationBox.bitDepthLumaMinus8 = -1
                avcConfigurationBox.bitDepthChromaMinus8 = -1
                avcConfigurationBox.chromaFormat = -1
                avcConfigurationBox.configurationVersion = 1
                avcConfigurationBox.lengthSizeMinusOne = 3
                avcConfigurationBox.profileCompatibility = 0

                visualSampleEntry.addBox(avcConfigurationBox)
                sampleDescriptionBox.addBox(visualSampleEntry)

            } else if (mime == "video/mp4v") {
                val visualSampleEntry =
                    VisualSampleEntry(VisualSampleEntry.TYPE1).setup(width, height)
                sampleDescriptionBox.addBox(visualSampleEntry)
            }
        } else {
            sampleDurations.add(1024.toLong())
            duration = 1024
            volume = 1f
            timeScale = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            handler = "soun"
            sampleDescriptionBox = SampleDescriptionBox()

            val audioSampleEntry = AudioSampleEntry(AudioSampleEntry.TYPE3).setup(format)

            val esds = ESDescriptorBox()

            val descriptor = ESDescriptor()
            descriptor.esId = 0

            val slConfigDescriptor = SLConfigDescriptor()
            slConfigDescriptor.predefined = 2
            descriptor.slConfigDescriptor = slConfigDescriptor

            val decoderConfigDescriptor = DecoderConfigDescriptor().setup()

            val audioSpecificConfig = AudioSpecificConfig()
            audioSpecificConfig.setAudioObjectType(2)
            audioSpecificConfig.setSamplingFrequencyIndex(
                samplingFrequencyIndexMap[audioSampleEntry.sampleRate.toInt()]!!
            )
            audioSpecificConfig.setChannelConfiguration(audioSampleEntry.channelCount)
            decoderConfigDescriptor.audioSpecificInfo = audioSpecificConfig
            descriptor.decoderConfigDescriptor = decoderConfigDescriptor

            val data = descriptor.serialize()
            esds.esDescriptor = descriptor
            esds.data = data
            audioSampleEntry.addBox(esds)
            sampleDescriptionBox.addBox(audioSampleEntry)
        }
    }

    fun getTrackId(): Long = trackId

    fun addSample(offset: Long, bufferInfo: MediaCodec.BufferInfo) {
        val isSyncFrame = !isAudio && bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0

        samples.add(Sample(offset, bufferInfo.size.toLong()))

        if (syncSamples != null && isSyncFrame) {
            syncSamples?.add(samples.size)
        }
        var delta = bufferInfo.presentationTimeUs - lastPresentationTimeUs
        lastPresentationTimeUs = bufferInfo.presentationTimeUs
        delta = (delta * timeScale + 500000L) / 1000000L
        if (!first) {
            sampleDurations.add(sampleDurations.size - 1, delta)
            duration += delta
        }
        first = false
    }

    fun getSamples(): ArrayList<Sample> = samples

    fun getDuration(): Long = duration

    fun getHandler(): String = handler

    fun getSampleDescriptionBox(): SampleDescriptionBox = sampleDescriptionBox

    fun getSyncSamples(): LongArray? {
        if (syncSamples == null || syncSamples!!.isEmpty()) {
            return null
        }
        val returns = LongArray(syncSamples!!.size)
        for (i in syncSamples!!.indices) {
            returns[i] = syncSamples!![i].toLong()
        }
        return returns
    }

    fun getTimeScale(): Int = timeScale

    fun getCreationTime(): Date = creationTime

    fun getWidth(): Int = width

    fun getHeight(): Int = height

    fun getVolume(): Float = volume

    fun getSampleDurations(): ArrayList<Long> = sampleDurations

    fun isAudio(): Boolean = isAudio

    private fun DecoderConfigDescriptor.setup(): DecoderConfigDescriptor = apply {
        objectTypeIndication = 0x40
        streamType = 5
        bufferSizeDB = 1536
        maxBitRate = 96000
        avgBitRate = 96000
    }

    private fun VisualSampleEntry.setup(w: Int, h: Int): VisualSampleEntry = apply {
        dataReferenceIndex = 1
        depth = 24
        frameCount = 1
        horizresolution = 72.0
        vertresolution = 72.0
        width = w
        height = h
        compressorname = "AVC Coding"
    }

    private fun AudioSampleEntry.setup(format: MediaFormat): AudioSampleEntry = apply {
        channelCount =
            if (format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) == 1) 2 else format.getInteger(
                MediaFormat.KEY_CHANNEL_COUNT
            )
        sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE).toLong()
        dataReferenceIndex = 1
        sampleSize = 16
    }
}
