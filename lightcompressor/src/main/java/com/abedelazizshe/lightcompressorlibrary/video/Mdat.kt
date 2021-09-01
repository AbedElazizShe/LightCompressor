package com.abedelazizshe.lightcompressorlibrary.video

import com.coremedia.iso.BoxParser
import com.coremedia.iso.IsoFile
import com.coremedia.iso.IsoTypeWriter
import com.coremedia.iso.boxes.Box
import com.coremedia.iso.boxes.Container
import com.googlecode.mp4parser.DataSource
import java.nio.ByteBuffer
import java.nio.channels.WritableByteChannel

class Mdat : Box {

    private lateinit var parent: Container
    private var contentSize = (1024 * 1024 * 1024).toLong()
    private var dataOffset: Long = 0

    override fun getParent(): Container = parent

    override fun setParent(parent: Container) {
        this.parent = parent
    }

    override fun getSize(): Long = 16 + contentSize

    override fun getOffset(): Long = dataOffset

    fun setDataOffset(offset: Long) {
        dataOffset = offset
    }

    fun setContentSize(contentSize: Long) {
        this.contentSize = contentSize
    }

    fun getContentSize(): Long {
        return contentSize
    }

    override fun getType(): String = "mdat"

    private fun isSmallBox(contentSize: Long): Boolean = contentSize + 8 < 4294967296L

    override fun getBox(writableByteChannel: WritableByteChannel) {
        val bb = ByteBuffer.allocate(16)
        val size = size
        if (isSmallBox(size)) {
            if (size >= 0 && size <= 1L shl 32) {
                IsoTypeWriter.writeUInt32(bb, size)
            } else {
                // TODO(ABED): Investigate when this could happen.
                IsoTypeWriter.writeUInt32(bb, 1)
            }
        } else {
            IsoTypeWriter.writeUInt32(bb, 1)
        }
        bb.put(IsoFile.fourCCtoBytes("mdat"))
        if (isSmallBox(size)) {
            bb.put(ByteArray(8))
        } else {
            IsoTypeWriter.writeUInt64(bb, if (size >= 0) size else 1)
        }
        bb.rewind()
        writableByteChannel.write(bb)
    }

    override fun parse(
        dataSource: DataSource?,
        header: ByteBuffer?,
        contentSize: Long,
        boxParser: BoxParser?
    ) {
    }
}