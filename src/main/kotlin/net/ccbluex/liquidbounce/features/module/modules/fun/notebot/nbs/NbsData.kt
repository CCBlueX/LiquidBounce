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

import net.minecraft.block.enums.NoteBlockInstrument

data class NbsHeader(
    val version: Byte = 0,
    val vanillaInstrumentCount: Byte = 0,
    val songLength: Short = 0,
    val layerCount: Short = 0,
    val songName: String? = null,
    val songAuthor: String? = null,
    val songOriginalAuthor: String? = null,
    val songDescription: String? = null,
    val tempo: Short = 0,
    val autoSaving: Byte = 0,
    val autoSavingDuration: Byte = 0,
    val timeSignature: Byte = 0,
    val minutesSpent: Int = 0,
    val leftClicks: Int = 0,
    val rightClicks: Int = 0,
    val noteBlocksAdded: Int = 0,
    val noteBlocksRemoved: Int = 0,
    val midiFileName: String? = null,
    val loopOnOff: Byte = 0,
    val maxLoopCount: Byte = 0,
    val loopStartTick: Short = 0,
)

data class NbsNoteBlock(
    val tick: Int,
    val layer: Int,
    val instrument: Byte,
    val key: Byte,
    val velocity: Byte,
    val panning: Int,
    val pitch: Short,
)

data class InstrumentNote(val instrument: Int, val noteValue: Int) {

    companion object {
        fun getInstrumentEnumFromId(id: Int): NoteBlockInstrument {
            return when (id) {
                1 -> NoteBlockInstrument.BASS
                2 -> NoteBlockInstrument.BASEDRUM
                3 -> NoteBlockInstrument.SNARE
                4 -> NoteBlockInstrument.HAT
                5 -> NoteBlockInstrument.GUITAR
                6 -> NoteBlockInstrument.FLUTE
                7 -> NoteBlockInstrument.BELL
                8 -> NoteBlockInstrument.CHIME
                9 -> NoteBlockInstrument.XYLOPHONE
                10 -> NoteBlockInstrument.IRON_XYLOPHONE
                11 -> NoteBlockInstrument.COW_BELL
                12 -> NoteBlockInstrument.DIDGERIDOO
                13 -> NoteBlockInstrument.BIT
                14 -> NoteBlockInstrument.BANJO
                15 -> NoteBlockInstrument.PLING
                else -> NoteBlockInstrument.HARP // 0
            }
        }
    }

    val instrumentEnum = getInstrumentEnumFromId(instrument)

}

data class SongData(
    /** The original name of file. */
    val name: String,
    val nbs: NbsData,
    val notesByTick: Map<Int, List<NbsNoteBlock>>,
    val songTickLength: Int,
    val songTicksPerGameTick: Float
)

data class NbsData(
    val header: NbsHeader,
    val noteBlocks: List<NbsNoteBlock>,
)
