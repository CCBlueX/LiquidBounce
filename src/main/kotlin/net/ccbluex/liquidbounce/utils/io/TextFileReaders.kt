/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.utils.io

import it.unimi.dsi.fastutil.ints.IntIterator
import it.unimi.dsi.fastutil.longs.LongArrayList
import okio.Buffer
import java.io.Closeable
import java.io.EOFException
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.Spliterator
import java.util.Spliterators
import java.util.stream.IntStream
import java.util.stream.StreamSupport

private const val NEWLINE: Byte = '\n'.code.toByte()

private val READ = setOf(StandardOpenOption.READ)

/**
 * Reads the UTF-8 code points of [path] chunk by chunk, the file is never loaded into memory.
 *
 * A new chunk is only read from the [FileChannel] once the buffered data was consumed, so the
 * memory usage stays at [DEFAULT_BUFFER_SIZE]. With [cyclic] the reader wraps around to the
 * beginning of the file instead of ending.
 */
internal class CodePointReader(
    private val path: Path,
    private val cyclic: Boolean,
) : IntIterator, Closeable {

    private val channel = FileChannel.open(path, READ)
    private val buffer = Buffer()
    private var offset = 0L
    private var lookahead = 0
    private var hasLookahead = false

    /** @return the next code point, or `-1` if the data is exhausted. */
    private fun advance(): Int {
        var rewinds = 0
        while (true) {
            if (buffer.size > 0L) {
                try {
                    return buffer.readUtf8CodePoint()
                } catch (_: EOFException) {
                    // The chunk ends inside of a multi-byte code point, read more data first.
                }
            }
            if (refill()) {
                continue
            }
            if (!cyclic || rewinds++ > 0) {
                return -1
            }
            rewind()
        }
    }

    /** @return whether more data was read, `false` at the end of the file. */
    private fun refill(): Boolean {
        val read = channel.transferTo(offset, DEFAULT_BUFFER_SIZE.toLong(), buffer)
        if (read <= 0L) {
            return false
        }
        offset += read
        return true
    }

    /** Continues at the beginning of the file, the next [refill] picks up the current content. */
    private fun rewind() {
        offset = 0L
        buffer.clear()
    }

    override fun hasNext(): Boolean {
        if (hasLookahead) {
            return true
        }

        val codePoint = advance()
        if (codePoint < 0) {
            return false
        }

        lookahead = codePoint
        hasLookahead = true
        return true
    }

    override fun nextInt(): Int {
        check(hasNext()) { "No more code points to read from $path" }
        hasLookahead = false
        return lookahead
    }

    override fun remove() = throw UnsupportedOperationException("remove")

    override fun close() = channel.close()

    fun stream(): IntStream = StreamSupport
        .intStream(Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED), false)
        .onClose(this::close)

}

/**
 * Reads the line starting at the byte [offset] of [path], UTF-8 decoded and without its terminator.
 */
internal fun readUtf8LineAt(path: Path, offset: Long): String {
    FileChannel.open(path, READ).use { channel ->
        val buffer = Buffer()
        var appended = 0L
        while (true) {
            val newline = buffer.indexOf(NEWLINE)
            if (newline >= 0L) {
                return buffer.readUtf8(newline).trimEnd { it == '\r' }
            }

            val read = channel.transferTo(offset + appended, DEFAULT_BUFFER_SIZE.toLong(), buffer)
            if (read <= 0L) {
                return buffer.readUtf8()
            }
            appended += read
        }
    }
}

/**
 * Scans the byte offset of every line start in [file], chunk by chunk.
 *
 * An offset without any content behind it is not reported, so neither an empty file nor a trailing
 * newline yields an empty last line.
 */
internal fun scanLineStarts(file: Path): LongArrayList {
    val starts = LongArrayList()
    var size = 0L

    FileChannel.open(file, READ).use { channel ->
        val buffer = Buffer()
        while (true) {
            val read = channel.transferTo(size, DEFAULT_BUFFER_SIZE.toLong(), buffer)
            if (read <= 0L) {
                break
            }

            var newline = buffer.indexOf(NEWLINE)
            while (newline >= 0L) {
                starts.add(size + newline + 1)
                newline = buffer.indexOf(NEWLINE, newline + 1)
            }

            size += read
            buffer.clear()
        }
    }

    if (starts.isNotEmpty() && starts.getLong(starts.size - 1) >= size) {
        starts.removeLong(starts.size - 1)
    }
    if (size > 0L) {
        starts.add(0, 0L)
    }

    return starts
}
