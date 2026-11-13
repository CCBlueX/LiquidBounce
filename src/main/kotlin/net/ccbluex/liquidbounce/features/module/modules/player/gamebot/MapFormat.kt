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
package net.ccbluex.liquidbounce.features.module.modules.player.gamebot

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.Deflater
import java.util.zip.GZIPOutputStream
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Layouts of the files [MapRecorder] writes, read by `scripts/bot/map/round.py`. All numbers are little-endian.
 *
 * - Columns: 256 records of [COLUMN_BYTES] in `lz * 16 + lx` order: `i16 topY, i16 surfaceY, i16 standY,
 *   u16 surfaceId, u16 standId, u8 flags`, heights [NONE] when the column has no such block.
 * - World: `i16 base`, then per column `u16 runCount` and `runCount * (u8 length, u16 id)` from `base` up to the
 *   highest block of the column; an empty column has no runs.
 */
@Suppress("CognitiveComplexMethod")
internal object MapFormat {

    const val VERSION = 1
    const val NONE = Short.MIN_VALUE.toInt()
    const val COLUMNS = 256
    const val COLUMN_BYTES = 11
    const val MAX_RUN = 255

    const val FLAG_WATER = 1
    const val FLAG_BLOCK_ENTITY = 2

    /** The highest block has no map color, such as glass, so the surface lies below it. */
    const val FLAG_TRANSLUCENT = 4

    /** A palette entry; [argb] is the flat map color. */
    class Block(
        val name: String,
        val argb: Int,
        val colored: Boolean,
        val solid: Boolean,
        val fluid: Boolean,
        val leaves: Boolean,
        val blockEntity: Boolean,
    )

    /** Blocks by name, id 0 is air. */
    class Palette {
        val blocks = arrayListOf(Block("minecraft:air", 0, false, false, false, false, false))
        private val ids = HashMap<String, Int>().apply { put("minecraft:air", 0) }

        fun id(name: String, block: () -> Block): Int = ids.getOrPut(name) {
            blocks += block()
            blocks.lastIndex
        }
    }

    /** Palette ids of a chunk from [base] up, at `(y - base) * 256 + lz * 16 + lx`. */
    class ChunkBlocks(val base: Int, val height: Int, val ids: CharArray) {
        init {
            require(ids.size == height * COLUMNS)
        }
    }

    fun columns(chunk: ChunkBlocks, palette: List<Block>): ByteArray {
        val buffer = ByteBuffer.allocate(COLUMNS * COLUMN_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        val ids = chunk.ids
        for (column in 0 until COLUMNS) {
            var top = NONE
            var surface = NONE
            var stand = NONE
            var surfaceId = 0
            var standId = 0
            var flags = 0
            for (dy in chunk.height - 1 downTo 0) {
                val id = ids[dy * COLUMNS + column].code
                if (id == 0) {
                    continue
                }

                val block = palette[id]
                val y = chunk.base + dy
                if (top == NONE) {
                    top = y
                    if (block.fluid) flags = flags or FLAG_WATER
                    if (!block.colored) flags = flags or FLAG_TRANSLUCENT
                }
                if (surface == NONE && block.colored) {
                    surface = y
                    surfaceId = id
                }
                if (stand == NONE && block.solid) {
                    stand = y
                    standId = id
                }
                if (block.blockEntity) {
                    flags = flags or FLAG_BLOCK_ENTITY
                }
            }
            buffer.putShort(top.toShort()).putShort(surface.toShort()).putShort(stand.toShort())
                .putChar(surfaceId.toChar()).putChar(standId.toChar()).put(flags.toByte())
        }
        return buffer.array()
    }

    fun rle(chunk: ChunkBlocks): ByteArray {
        val ids = chunk.ids
        val tops = IntArray(COLUMNS) { -1 }
        var lowest = Int.MAX_VALUE
        for (dy in 0 until chunk.height) {
            for (column in 0 until COLUMNS) {
                if (ids[dy * COLUMNS + column].code != 0) {
                    tops[column] = dy
                    if (dy < lowest) lowest = dy
                }
            }
        }

        val maxRuns = if (lowest == Int.MAX_VALUE) 0 else chunk.height - lowest
        val buffer = ByteBuffer.allocate(2 + COLUMNS * (2 + 3 * maxRuns)).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort((if (lowest == Int.MAX_VALUE) NONE else chunk.base + lowest).toShort())
        for (column in 0 until COLUMNS) {
            val countAt = buffer.position()
            buffer.putChar(0.toChar())
            val top = tops[column]
            if (top < 0) {
                continue
            }

            var runs = 0
            var dy = lowest
            while (dy <= top) {
                val id = ids[dy * COLUMNS + column]
                var length = 1
                while (dy + length <= top && length < MAX_RUN && ids[(dy + length) * COLUMNS + column] == id) {
                    length++
                }
                buffer.put(length.toByte()).putChar(id)
                runs++
                dy += length
            }
            buffer.putChar(countAt, runs.toChar())
        }
        return buffer.array().copyOf(buffer.position())
    }

    fun gzip(bytes: ByteArray, level: Int = Deflater.DEFAULT_COMPRESSION): ByteArray {
        val out = ByteArrayOutputStream(bytes.size / 4 + 64)
        object : GZIPOutputStream(out) {
            init {
                def.setLevel(level)
            }
        }.use { it.write(bytes) }
        return out.toByteArray()
    }

    /** [value] with exactly [decimals] digits after the point; nothing for NaN or infinity. */
    fun appendFixed(out: StringBuilder, value: Double, decimals: Int) {
        if (!value.isFinite()) {
            return
        }

        var scale = 1L
        repeat(decimals) { scale *= 10 }
        val scaled = (abs(value) * scale).roundToLong()
        if (value < 0 && scaled != 0L) {
            out.append('-')
        }
        out.append(scaled / scale)
        if (decimals > 0) {
            out.append('.')
            val fraction = (scaled % scale).toString()
            repeat(decimals - fraction.length) { out.append('0') }
            out.append(fraction)
        }
    }

    /**
     * JSON of strings, numbers, booleans, null, [IntArray], [DoubleArray], lists and string-keyed maps. Doubles are
     * rounded to 0.01; NaN and infinity become null.
     */
    fun appendJson(out: StringBuilder, value: Any?) {
        when (value) {
            null -> out.append("null")
            is String -> appendString(out, value)
            is Boolean, is Int, is Long, is Short, is Byte -> out.append(value)
            is Double -> appendDouble(out, value)
            is Float -> appendDouble(out, value.toDouble())
            is IntArray -> appendList(out, value.size) { out.append(value[it]) }
            is DoubleArray -> appendList(out, value.size) { appendDouble(out, value[it]) }
            is List<*> -> appendList(out, value.size) { appendJson(out, value[it]) }
            is Map<*, *> -> {
                out.append('{')
                var first = true
                for ((key, item) in value) {
                    if (!first) out.append(',')
                    first = false
                    appendString(out, key.toString())
                    out.append(':')
                    appendJson(out, item)
                }
                out.append('}')
            }

            else -> appendString(out, value.toString())
        }
    }

    private inline fun appendList(out: StringBuilder, size: Int, item: (Int) -> Unit) {
        out.append('[')
        for (index in 0 until size) {
            if (index > 0) out.append(',')
            item(index)
        }
        out.append(']')
    }

    private fun appendDouble(out: StringBuilder, value: Double) {
        if (!value.isFinite()) {
            out.append("null")
            return
        }
        val rounded = (value * 100.0).roundToLong()
        if (rounded % 100 == 0L) {
            out.append(rounded / 100)
        } else {
            appendFixed(out, rounded / 100.0, if (rounded % 10 == 0L) 1 else 2)
        }
    }

    private fun appendString(out: StringBuilder, value: String) {
        out.append('"')
        for (char in value) {
            when {
                char == '"' -> out.append("\\\"")
                char == '\\' -> out.append("\\\\")
                char == '\n' -> out.append("\\n")
                char == '\r' -> out.append("\\r")
                char == '\t' -> out.append("\\t")
                char < ' ' -> out.append("\\u").append(char.code.toString(16).padStart(4, '0'))
                else -> out.append(char)
            }
        }
        out.append('"')
    }

}
