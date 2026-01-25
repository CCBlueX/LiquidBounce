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
package net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot

import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import net.ccbluex.fastutil.enumMapOf
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.nbs.InstrumentNote
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.nbs.SongData
import net.ccbluex.liquidbounce.utils.block.getSortedSphere
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.minecraft.ChatFormatting
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument

object NotebotScanner : MinecraftShortcuts {
    fun scanBlocksAndCheckRequirements(songData: SongData): BlocksAndRequirements {
        return BlocksAndRequirements(
            availableBlocks = scanSurroundingNoteBlocks(songData),
            requirements = calculateRequirements(songData)
        )
    }

    private fun scanSurroundingNoteBlocks(songData: SongData): Map<NoteBlockInstrument, MutableList<NoteBlockTracker>> {
        val result = enumMapOf<NoteBlockInstrument, ArrayDeque<NoteBlockTracker>>()

        val surroundings = player.eyePosition.toBlockPos().getSortedSphere(ModuleNotebot.range)
        val noteBlocks = surroundings.filter { pos ->
            pos.getState()?.block == Blocks.NOTE_BLOCK && pos.above().getState()!!.isAir
        }

        val requiredInstruments = ModuleNotebot.getRequiredInstruments(songData)
        noteBlocks.forEach { pos ->
            val instrument = pos.below().getState()!!.instrument()
            if (instrument in requiredInstruments) {
                result.getOrPut(instrument) { ArrayDeque() }.add(NoteBlockTracker(pos))
            }
        }

        return result
    }

    // technically we'd need even more blocks than returned by this function
    // since a song tick != a game tick thus this is technically incorrect but works well enough
    // it has the advantage that we don't get super huge requirements for very fast songs -
    // and well playing the same sound multiple times a tick due to minecraft's limitations
    // would sound weird anyway
    private fun calculateRequirements(songData: SongData): Object2IntMap<InstrumentNote> {
        val maxConcurrentCounts = Object2IntOpenHashMap<InstrumentNote>()
        val countsInTick = Object2IntOpenHashMap<InstrumentNote>()
        for (notes in songData.notesByTick.values) {
            countsInTick.clear()
            for (note in notes) {
                val instrumentNote = ModuleNotebot.getPlayedNote(note)

                if (ModuleNotebot.reuseBlocks) {
                    maxConcurrentCounts.put(instrumentNote, 1)
                } else {
                    countsInTick.addTo(instrumentNote, 1)
                }
            }

            if (ModuleNotebot.reuseBlocks) {
                continue
            }

            for ((instrumentNote, count) in countsInTick) {
                maxConcurrentCounts.mergeInt(instrumentNote, count, ::maxOf)
            }
        }

        return maxConcurrentCounts
    }

    class BlocksAndRequirements(
        val availableBlocks: Map<NoteBlockInstrument, List<NoteBlockTracker>>,
        val requirements: Object2IntMap<InstrumentNote>,
    ) {
        fun validateRequirements(): Boolean {
            val totalRequired = requirements.values.sum()
            val totalAvailable = this.availableBlocks.values.sumOf { it.size }
            if (totalAvailable < totalRequired) {
                return false
            }

            val requirementByInstrument = enumMapOf<NoteBlockInstrument, Int>()

            requirements.forEach { (key, value) ->
                requirementByInstrument.merge(key.instrumentEnum, value, Int::plus)
            }

            return requirementByInstrument.all { (instrument, required) ->
                this.availableBlocks[instrument].let { it != null && it.size >= required }
            }
        }

        fun printRequirements() {
            val aggregatedRequirements = enumMapOf<NoteBlockInstrument, Int>()
            for ((key1, count) in requirements) {
                aggregatedRequirements.merge(key1.instrumentEnum, count, Int::plus)
            }

            val text = ModuleNotebot.message("notEnoughNoteBlocks").withStyle(ChatFormatting.RED)
            aggregatedRequirements.entries.sortedBy { -it.value }.forEach { (instrument, requiredCount) ->
                val availableCount = this.availableBlocks[instrument]?.size ?: 0

                val messageLine = "\n - ${instrument.name} ($availableCount/$requiredCount)"

                if (availableCount >= requiredCount) {
                    text.append(messageLine.asPlainText(ChatFormatting.GREEN))
                } else if (availableCount == 0) {
                    text.append(messageLine.asPlainText(ChatFormatting.RED))
                } else {
                    text.append(messageLine.asPlainText(ChatFormatting.YELLOW))
                }
            }

            chat(text, ModuleNotebot)
        }
    }
}
