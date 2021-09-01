package com.abedelazizshe.lightcompressorlibrary.utils

import android.util.Log
import com.abedelazizshe.lightcompressorlibrary.data.*
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

object StreamableVideo {

    private const val tag = "StreamableVideo"
    private const val ATOM_PREAMBLE_SIZE = 8

    /**
     * @param in  Input file.
     * @param out Output file.
     * @return false if input file is already fast start.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun start(`in`: File?, out: File): Boolean {
        var ret = false
        var inStream: FileInputStream? = null
        var outStream: FileOutputStream? = null
        return try {
            inStream = FileInputStream(`in`)
            val infile = inStream.channel
            outStream = FileOutputStream(out)
            val outfile = outStream.channel
            convert(infile, outfile).also { ret = it }
        } finally {
            safeClose(inStream)
            safeClose(outStream)
            if (!ret) {
                out.delete()
            }
        }
    }

    @Throws(IOException::class)
    private fun convert(infile: FileChannel, outfile: FileChannel): Boolean {
        val atomBytes = ByteBuffer.allocate(ATOM_PREAMBLE_SIZE).order(ByteOrder.BIG_ENDIAN)
        var atomType = 0
        var atomSize: Long = 0
        val lastOffset: Long
        val moovAtom: ByteBuffer
        var ftypAtom: ByteBuffer? = null
        var startOffset: Long = 0

        // traverse through the atoms in the file to make sure that 'moov' is at the end
        while (readAndFill(infile, atomBytes)) {
            atomSize = uInt32ToLong(atomBytes.int)
            atomType = atomBytes.int

            // keep ftyp atom
            if (atomType == FTYP_ATOM) {
                val ftypAtomSize = uInt32ToInt(atomSize)
                ftypAtom = ByteBuffer.allocate(ftypAtomSize).order(ByteOrder.BIG_ENDIAN)
                atomBytes.rewind()
                ftypAtom.put(atomBytes)
                if (infile.read(ftypAtom) < ftypAtomSize - ATOM_PREAMBLE_SIZE) break
                ftypAtom.flip()
                startOffset = infile.position() // after ftyp atom
            } else {
                if (atomSize == 1L) {
                    /* 64-bit special case */
                    atomBytes.clear()
                    if (!readAndFill(infile, atomBytes)) break
                    atomSize = uInt64ToLong(atomBytes.long)
                    infile.position(infile.position() + atomSize - ATOM_PREAMBLE_SIZE * 2) // seek
                } else {
                    infile.position(infile.position() + atomSize - ATOM_PREAMBLE_SIZE) // seek
                }
            }
            if (atomType != FREE_ATOM
                && atomType != JUNK_ATOM
                && atomType != MDAT_ATOM
                && atomType != MOOV_ATOM
                && atomType != PNOT_ATOM
                && atomType != SKIP_ATOM
                && atomType != WIDE_ATOM
                && atomType != PICT_ATOM
                && atomType != UUID_ATOM
                && atomType != FTYP_ATOM
            ) {
                Log.wtf(tag, "encountered non-QT top-level atom (is this a QuickTime file?)")
                break
            }

            /* The atom header is 8 (or 16 bytes), if the atom size (which
         * includes these 8 or 16 bytes) is less than that, we won't be
         * able to continue scanning sensibly after this atom, so break. */
            if (atomSize < 8) break
        }
        if (atomType != MOOV_ATOM) {
            Log.wtf(tag, "last atom in file was not a moov atom")
            return false
        }

        // atomSize is uint64, but for moov uint32 should be stored.
        val moovAtomSize: Int = uInt32ToInt(atomSize)
        lastOffset =
            infile.size() - moovAtomSize
        moovAtom = ByteBuffer.allocate(moovAtomSize).order(ByteOrder.BIG_ENDIAN)
        if (!readAndFill(infile, moovAtom, lastOffset)) {
            throw Exception("failed to read moov atom")
        }

        if (moovAtom.getInt(12) == CMOV_ATOM) {
            throw Exception("this utility does not support compressed moov atoms yet")
        }

        // crawl through the moov chunk in search of stco or co64 atoms
        while (moovAtom.remaining() >= 8) {
            val atomHead = moovAtom.position()
            atomType = moovAtom.getInt(atomHead + 4)
            if (!(atomType == STCO_ATOM || atomType == CO64_ATOM)) {
                moovAtom.position(moovAtom.position() + 1)
                continue
            }
            atomSize = uInt32ToLong(moovAtom.getInt(atomHead)) // uint32
            if (atomSize > moovAtom.remaining()) {
                throw Exception("bad atom size")
            }
            // skip size (4 bytes), type (4 bytes), version (1 byte) and flags (3 bytes)
            moovAtom.position(atomHead + 12)
            if (moovAtom.remaining() < 4) {
                throw Exception("malformed atom")
            }
            // uint32_t, but assuming moovAtomSize is in int32 range, so this will be in int32 range
            val offsetCount = uInt32ToInt(moovAtom.int)
            if (atomType == STCO_ATOM) {
                Log.i(tag, "patching stco atom...")
                if (moovAtom.remaining() < offsetCount * 4) {
                    throw Exception("bad atom size/element count")
                }
                for (i in 0 until offsetCount) {
                    val currentOffset = moovAtom.getInt(moovAtom.position())
                    val newOffset =
                        currentOffset + moovAtomSize // calculate uint32 in int, bitwise addition

                    if (currentOffset < 0 && newOffset >= 0) {
                        throw Exception(
                            "This is bug in original qt-faststart.c: "
                                    + "stco atom should be extended to co64 atom as new offset value overflows uint32, "
                                    + "but is not implemented."
                        )
                    }
                    moovAtom.putInt(newOffset)
                }
            } else if (atomType == CO64_ATOM) {
                Log.wtf(tag, "patching co64 atom...")
                if (moovAtom.remaining() < offsetCount * 8) {
                    throw Exception("bad atom size/element count")
                }
                for (i in 0 until offsetCount) {
                    val currentOffset = moovAtom.getLong(moovAtom.position())
                    moovAtom.putLong(currentOffset + moovAtomSize) // calculate uint64 in long, bitwise addition
                }
            }
        }
        infile.position(startOffset) // seek after ftyp atom
        if (ftypAtom != null) {
            // dump the same ftyp atom
            Log.i(tag, "writing ftyp atom...")
            ftypAtom.rewind()
            outfile.write(ftypAtom)
        }

        // dump the new moov atom
        Log.i(tag, "writing moov atom...")
        moovAtom.rewind()
        outfile.write(moovAtom)

        // copy the remainder of the infile, from offset 0 -> (lastOffset - startOffset) - 1
        Log.i(tag, "copying rest of file...")
        infile.transferTo(startOffset, lastOffset - startOffset, outfile)
        return true
    }

    private fun safeClose(closeable: Closeable?) {
        if (closeable != null) {
            try {
                closeable.close()
            } catch (e: IOException) {
                Log.wtf(tag, "Failed to close file: ")
            }
        }
    }

    @Throws(IOException::class)
    private fun readAndFill(infile: FileChannel, buffer: ByteBuffer): Boolean {
        buffer.clear()
        val size = infile.read(buffer)
        buffer.flip()
        return size == buffer.capacity()
    }

    @Throws(IOException::class)
    private fun readAndFill(infile: FileChannel, buffer: ByteBuffer, position: Long): Boolean {
        buffer.clear()
        val size = infile.read(buffer, position)
        buffer.flip()
        return size == buffer.capacity()
    }
}
