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

data class NbsHeader(
    var version: Byte = 0,
    var vanillaInstrumentCount: Byte = 0,
    var songLength: Short = 0 ,
    var layerCount: Short = 0,
    var songName: String? = null,
    var songAuthor: String? = null,
    var songOriginalAuthor: String? = null,
    var songDescription: String? = null,
    var tempo: Short = 0,
    var autoSaving: Byte = 0,
    var autoSavingDuration: Byte = 0,
    var timeSignature: Byte = 0,
    var minutesSpent: Int = 0,
    var leftClicks: Int = 0,
    var rightClicks: Int = 0,
    var noteBlocksAdded: Int = 0,
    var noteBlocksRemoved: Int = 0,
    var midiFileName: String? = null,
    var loopOnOff: Byte = 0,
    var maxLoopCount: Byte = 0,
    var loopStartTick: Short = 0
)

data class NbsNoteBlock(
    var tick: Int,
    var layer: Int,
    var instrument: Byte,
    var key: Byte,
    var velocity: Byte,
    var panning: Int,
    var pitch: Short
)

data class InstrumentNote(val instrument: Int, val noteValue: Int)

data class SongData(
    val nbs: Nbs,
    val notesByTick: Map<Int, List<NbsNoteBlock>>,
    val songTickLength: Int,
    val songTicksPerGameTick: Float
)
