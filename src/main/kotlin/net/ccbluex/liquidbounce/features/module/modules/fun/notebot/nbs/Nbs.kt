/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.nbs

import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets

/**
 * https://opennbs.org/nbs
 */
class Nbs(sourceStream: InputStream, val name: String) {

    var header = NbsHeader()
    var noteBlocks = mutableListOf<NbsNoteBlock>()

    init {
        DataInputStream(sourceStream).use { dis ->
            // Determine the format by reading the first short
            val firstShort = readLittleEndianShort(dis)
            val version: Byte
            val vanillaInstrumentCount: Byte
            val songLength: Short

            if (firstShort.toInt() == 0) {
                // New format
                version = dis.readByte()
                vanillaInstrumentCount = dis.readByte()
                songLength = if (version >= 3) {
                    readLittleEndianShort(dis)
                } else {
                    0
                }
            } else {
                // Old format
                songLength = firstShort
                vanillaInstrumentCount = 10 // Default for old format (instruments 0-9)
                version = 0
            }

            // Read common header fields
            val layerCount = readLittleEndianShort(dis) // Song height in old format
            val songName = readString(dis)
            val songAuthor = readString(dis)
            val songOriginalAuthor = readString(dis)
            val songDescription = readString(dis)
            val tempo = readLittleEndianShort(dis)
            val autoSaving = dis.readByte()
            val autoSavingDuration = dis.readByte()
            val timeSignature = dis.readByte()
            val minutesSpent = readLittleEndianInt(dis)
            val leftClicks = readLittleEndianInt(dis)
            val rightClicks = readLittleEndianInt(dis)
            val noteBlocksAdded = readLittleEndianInt(dis) // Blocks added in old format
            val noteBlocksRemoved = readLittleEndianInt(dis) // Blocks removed in old format
            val midiFileName = readString(dis)

            // New format-specific fields (loop settings)
            var loopOnOff: Byte = 0
            var maxLoopCount: Byte = 0
            var loopStartTick: Short = 0
            if (version >= 4) {
                loopOnOff = dis.readByte()
                maxLoopCount = dis.readByte()
                loopStartTick = readLittleEndianShort(dis)
            }

            // Parse note blocks
            var tick = -1
            while (true) {
                val jumps = readLittleEndianShort(dis)
                if (jumps.toInt() == 0) break
                tick += jumps.toInt()
                var layer = -1
                while (true) {
                    val jumpsLayer = readLittleEndianShort(dis)
                    if (jumpsLayer.toInt() == 0) {
                        break
                    }

                    layer += jumpsLayer.toInt()
                    val instrument = dis.readByte()
                    val key = dis.readByte()
                    var velocity: Byte = 100 // Default for old format
                    var panning = 100 // Default for old format
                    var pitch: Short = 2 // Default for old format
                    if (version >= 4) {
                        velocity = dis.readByte()
                        panning = dis.readUnsignedByte()
                        pitch = readLittleEndianShort(dis)
                    }
                    noteBlocks.add(NbsNoteBlock(tick, layer, instrument, key, velocity, panning, pitch))
                }
            }

            header = NbsHeader()
            header.version = version
            header.vanillaInstrumentCount = vanillaInstrumentCount
            header.songLength = songLength
            header.layerCount = layerCount
            header.songName = songName
            header.songAuthor = songAuthor
            header.songOriginalAuthor = songOriginalAuthor
            header.songDescription = songDescription
            header.tempo = tempo
            header.autoSaving = autoSaving
            header.autoSavingDuration = autoSavingDuration
            header.timeSignature = timeSignature
            header.minutesSpent = minutesSpent
            header.leftClicks = leftClicks
            header.rightClicks = rightClicks
            header.noteBlocksAdded = noteBlocksAdded
            header.noteBlocksRemoved = noteBlocksRemoved
            header.midiFileName = midiFileName
            header.loopOnOff = loopOnOff
            header.maxLoopCount = maxLoopCount
            header.loopStartTick = loopStartTick
        }
    }

    @Throws(IOException::class)
    private fun readLittleEndianShort(dis: DataInputStream): Short {
        val byte1 = dis.readUnsignedByte()
        val byte2 = dis.readUnsignedByte()
        return ((byte2 shl 8) or byte1).toShort()
    }

    @Throws(IOException::class)
    private fun readLittleEndianInt(dis: DataInputStream): Int {
        val byte1 = dis.readUnsignedByte()
        val byte2 = dis.readUnsignedByte()
        val byte3 = dis.readUnsignedByte()
        val byte4 = dis.readUnsignedByte()
        return (byte4 shl 24) or (byte3 shl 16) or (byte2 shl 8) or byte1
    }

    @Throws(IOException::class)
    private fun readString(dis: DataInputStream): String {
        val length = readLittleEndianInt(dis)
        val bytes = ByteArray(length)
        dis.readFully(bytes)
        return String(bytes, StandardCharsets.UTF_8)
    }

}
