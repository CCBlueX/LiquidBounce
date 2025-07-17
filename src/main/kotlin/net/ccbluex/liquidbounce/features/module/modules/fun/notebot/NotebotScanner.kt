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
package net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot

import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.nbs.InstrumentNote
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.nbs.SongData
import net.ccbluex.liquidbounce.utils.block.getSortedSphere
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.block.Blocks
import net.minecraft.block.enums.NoteBlockInstrument
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.math.min

object NotebotScanner : MinecraftShortcuts {

    fun scanAndAssignNotes(): Boolean {
        val songData = ModuleNotebot.songData ?: return true
        val noteBlocks = scanSurroundingNoteBlocks()
        val requirements = calculateRequirements(songData)

        if (!validateRequirements(requirements, noteBlocks)) {
            printRequirements(requirements, noteBlocks)
            return true
        }

        assignNoteBlocks(requirements, noteBlocks)
        ModuleNotebot.renderer.updateAll()

        return false
    }

    private fun scanSurroundingNoteBlocks(): Map<NoteBlockInstrument, MutableList<BlockPos>> {
        val middle = player.blockPos
        return BlockPos.ORIGIN.getSortedSphere(ModuleNotebot.range)
            .map { it.toImmutable().add(middle) }
            .filter { pos ->
                pos.getState()!!.block == Blocks.NOTE_BLOCK && pos.up().getState()!!.isAir
            }
            .groupBy { pos -> pos.down().getState()!!.instrument }
            .mapValues { ArrayDeque(it.value) }
    }

    private fun calculateRequirements(songData: SongData): Map<InstrumentNote, Int> {
        val maxConcurrentCounts = mutableMapOf<InstrumentNote, Int>()
        for ((_, notes) in songData.notesByTick) {
            val countsInTick = mutableMapOf<InstrumentNote, Int>()

            for (note in notes) {
                val noteValue = MathHelper.clamp(note.key - 33, 0, 24)
                val instrumentNote = InstrumentNote(
                    instrument = note.instrument.toInt(),
                    noteValue = noteValue
                )

                countsInTick[instrumentNote] = countsInTick.getOrDefault(instrumentNote, 0) + 1
            }

            for ((instrumentNote, count) in countsInTick) {
                val currentMax = maxConcurrentCounts.getOrDefault(instrumentNote, 0)
                if (count > currentMax) {
                    maxConcurrentCounts[instrumentNote] = count
                }
            }
        }

        return maxConcurrentCounts
    }

    private fun validateRequirements(
        requirements: Map<InstrumentNote, Int>,
        available: Map<NoteBlockInstrument, List<BlockPos>>
    ): Boolean {
        val totalRequired = requirements.values.sum()
        val totalAvailable = available.values.sumOf { it.size }
        if (totalAvailable < totalRequired) {
            return false
        }

        return requirements.entries
            .groupBy({it.key.instrument}, {it.value})
            .mapValues { it.value.sum() }
            .all { (instrumentId, required) ->
                val instrument = ModuleNotebot.instrumentFromNbs(instrumentId)
                (available[instrument]?.size ?: 0) >= required
            }
    }

    @Suppress("ThrowingExceptionsWithoutMessageOrCause", "SwallowedException")
    private fun assignNoteBlocks(
        requirements: Map<InstrumentNote, Int>,
        available: Map<NoteBlockInstrument, MutableList<BlockPos>>
    ) {
        try {
            requirements.forEach { (instrumentNote, count) ->
                val instrument = ModuleNotebot.instrumentFromNbs(instrumentNote.instrument)
                val blockPosList = available[instrument]!!
                for (i in 0..<count) {
                    val pos = blockPosList.removeFirst()
                    ModuleNotebot.noteBlocks.add(NoteBlock(pos, instrument, instrumentNote.noteValue))
                    ModuleNotebot.renderer.addBlock(pos, false)
                }
            }
        } catch (e: Exception) {
            // TODO why tf does this happen?????? why is the block pos list empty?
            printRequirements(requirements, available)
            ModuleNotebot.noteBlocks.clear()
            ModuleNotebot.renderer.disable()
            throw IllegalStateException()
        }
    }

    private fun printRequirements(
        requirements: Map<InstrumentNote, Int>,
        available: Map<NoteBlockInstrument, List<BlockPos>>
    ) {
        val aggregatedRequirements = EnumMap<NoteBlockInstrument, Int>(NoteBlockInstrument::class.java)
        for ((key1, count) in requirements) {
            val instrument = ModuleNotebot.instrumentFromNbs(key1.instrument)
            aggregatedRequirements[instrument] = aggregatedRequirements.getOrDefault(instrument, 0) + count
        }

        val sortedRequirements = ArrayList<Map.Entry<NoteBlockInstrument, Int>>(aggregatedRequirements.entries)
        sortedRequirements.sortedWith { entry1, entry2 -> entry2.value.compareTo(entry1.value) }

        val text = "Not enough note blocks in range, required are:".asText().formatted(Formatting.RED)
        for ((instrument, requiredCount) in sortedRequirements) {
            val availableCount = if (available.containsKey(instrument)) {
                min(available[instrument]!!.size.toDouble(), requiredCount.toDouble()).toInt()
            } else {
                0
            }

            val messageLine = "\n - " + instrument.name + " (" + availableCount + "/" + requiredCount + ")"
            if (availableCount >= requiredCount) {
                text.append(messageLine.asText().formatted(Formatting.GREEN))
            } else if (availableCount == 0) {
                text.append(messageLine.asText().formatted(Formatting.RED))
            } else {
                text.append(messageLine.asText().formatted(Formatting.YELLOW))
            }
        }

        chat(text, ModuleNotebot)
    }

}
