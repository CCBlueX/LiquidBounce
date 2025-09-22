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

import okio.BufferedSource
import java.io.IOException

/**
 * https://opennbs.org/nbs
 */
@Throws(IOException::class)
fun BufferedSource.readNbsData(): NbsData {
    val header = readNbsHeader()
    val noteBlocks = mutableListOf<NbsNoteBlock>()

    // Parse note blocks
    var tick = -1
    while (true) {
        val jumps = this.readShortLe()
        if (jumps.toInt() == 0) break
        tick += jumps.toInt()
        var layer = -1
        while (true) {
            val jumpsLayer = this.readShortLe()
            if (jumpsLayer.toInt() == 0) {
                break
            }

            layer += jumpsLayer.toInt()
            val instrument = this.readByte()
            val key = this.readByte()
            var velocity: Byte = 100 // Default for old format
            var panning = 100 // Default for old format
            var pitch: Short = 2 // Default for old format
            if (header.version >= 4) {
                velocity = this.readByte()
                panning = this.readUByte()
                pitch = this.readShortLe()
            }
            noteBlocks.add(NbsNoteBlock(tick, layer, instrument, key, velocity, panning, pitch))
        }
    }

    return NbsData(header = header, noteBlocks = noteBlocks)
}

@Suppress("detekt:LongMethod")
@Throws(IOException::class)
private fun BufferedSource.readNbsHeader(): NbsHeader {
    // Determine the format by reading the first short
    val firstShort = this.readShortLe()
    val version: Byte
    val vanillaInstrumentCount: Byte
    val songLength: Short

    if (firstShort.toInt() == 0) {
        // New format
        version = this.readByte()
        vanillaInstrumentCount = this.readByte()
        songLength = if (version >= 3) this.readShortLe() else 0
    } else {
        // Old format
        songLength = firstShort
        vanillaInstrumentCount = 10 // Default for old format (instruments 0-9)
        version = 0
    }

    // Read common header fields
    val layerCount = this.readShortLe() // Song height in old format
    val songName = this.readString()
    val songAuthor = this.readString()
    val songOriginalAuthor = this.readString()
    val songDescription = this.readString()
    val tempo = this.readShortLe()
    val autoSaving = this.readByte()
    val autoSavingDuration = this.readByte()
    val timeSignature = this.readByte()
    val minutesSpent = this.readIntLe()
    val leftClicks = this.readIntLe()
    val rightClicks = this.readIntLe()
    val noteBlocksAdded = this.readIntLe() // Blocks added in old format
    val noteBlocksRemoved = this.readIntLe() // Blocks removed in old format
    val midiFileName = this.readString()

    // New format-specific fields (loop settings)
    var loopOnOff: Byte = 0
    var maxLoopCount: Byte = 0
    var loopStartTick: Short = 0
    if (version >= 4) {
        loopOnOff = this.readByte()
        maxLoopCount = this.readByte()
        loopStartTick = this.readShortLe()
    }

    return NbsHeader(
        version = version,
        vanillaInstrumentCount = vanillaInstrumentCount,
        songLength = songLength,
        layerCount = layerCount,
        songName = songName,
        songAuthor = songAuthor,
        songOriginalAuthor = songOriginalAuthor,
        songDescription = songDescription,
        tempo = tempo,
        autoSaving = autoSaving,
        autoSavingDuration = autoSavingDuration,
        timeSignature = timeSignature,
        minutesSpent = minutesSpent,
        leftClicks = leftClicks,
        rightClicks = rightClicks,
        noteBlocksAdded = noteBlocksAdded,
        noteBlocksRemoved = noteBlocksRemoved,
        midiFileName = midiFileName,
        loopOnOff = loopOnOff,
        maxLoopCount = maxLoopCount,
        loopStartTick = loopStartTick,
    )
}

@Throws(IOException::class)
private fun BufferedSource.readString(): String {
    return readUtf8(readIntLe().toLong())
}

@Throws(IOException::class)
private fun BufferedSource.readUByte(): Int = readByte().toUByte().toInt()
