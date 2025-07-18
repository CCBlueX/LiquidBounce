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
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.minecraft.block.Blocks
import net.minecraft.block.enums.NoteBlockInstrument
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import java.util.*
import kotlin.collections.ArrayDeque

object NotebotScanner : MinecraftShortcuts {

    fun scanAndAssignNotes(songData: SongData): Boolean {
        val noteBlocks = scanSurroundingNoteBlocks()
        val requirements = calculateRequirements(songData)

        if (!validateRequirements(requirements, noteBlocks)) {
            printRequirements(requirements, noteBlocks)
            return false
        }

        assignNoteBlocks(requirements, noteBlocks)
        ModuleNotebot.renderer.updateAll()

        return true
    }

    private fun scanSurroundingNoteBlocks(): Map<NoteBlockInstrument, MutableList<BlockPos>> {
        val result = EnumMap<_, ArrayDeque<BlockPos>>(NoteBlockInstrument::class.java)

        player.eyePos.toBlockPos().getSortedSphere(ModuleNotebot.range).filter { pos ->
            pos.getState()?.block == Blocks.NOTE_BLOCK && pos.up().getState()!!.isAir
        }.forEach { pos ->
            result.getOrPut(pos.down().getState()!!.instrument) { ArrayDeque() }.add(pos)
        }

        return result
    }

    private fun calculateRequirements(songData: SongData): Map<InstrumentNote, Int> {
        val maxConcurrentCounts = hashMapOf<InstrumentNote, Int>()
        val countsInTick = hashMapOf<InstrumentNote, Int>()
        for (notes in songData.notesByTick.values) {
            countsInTick.clear()
            for (note in notes) {
                val noteValue = MathHelper.clamp(note.key - 33, 0, 24)
                val instrumentNote = InstrumentNote(
                    instrument = note.instrument.toInt(),
                    noteValue = noteValue
                )

                countsInTick.inlineMerge(instrumentNote, 1, Int::plus)
            }

            for ((instrumentNote, count) in countsInTick) {
                maxConcurrentCounts.inlineMerge(instrumentNote, count, ::maxOf)
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

        val requirementByInstrument = EnumMap<_, Int>(NoteBlockInstrument::class.java)

        requirements.forEach { (key, value) ->
            requirementByInstrument.inlineMerge(key.instrumentEnum, value, Int::plus)
        }

        return requirementByInstrument.all { (instrument, required) ->
            available[instrument].let { it != null && it.size >= required }
        }
    }

    private fun assignNoteBlocks(
        requirements: Map<InstrumentNote, Int>,
        available: Map<NoteBlockInstrument, MutableList<BlockPos>>
    ) {
        requirements.forEach { (instrumentNote, count) ->
            val instrument = ModuleNotebot.instrumentFromNbs(instrumentNote.instrument)
            val blockPosList = available[instrument]!!
            repeat(count) {
                val pos = blockPosList.removeFirst()
                ModuleNotebot.noteBlocks.add(NoteBlock(pos, instrument, instrumentNote.noteValue))
                ModuleNotebot.renderer.addBlock(pos, false)
            }
        }
    }

    private fun printRequirements(
        requirements: Map<InstrumentNote, Int>,
        available: Map<NoteBlockInstrument, List<BlockPos>>
    ) {
        val aggregatedRequirements = EnumMap<_, Int>(NoteBlockInstrument::class.java)
        for ((key1, count) in requirements) {
            val instrument = ModuleNotebot.instrumentFromNbs(key1.instrument)
            aggregatedRequirements.inlineMerge(instrument, count, Int::plus)
        }

        val text = "Not enough note blocks in range, required are:".asText().formatted(Formatting.RED)
        aggregatedRequirements.entries.sortedBy { -it.value }.forEach { (instrument, requiredCount) ->
            val availableCount = if (available.containsKey(instrument)) {
                minOf(available[instrument]!!.size, requiredCount)
            } else {
                0
            }

            val messageLine = "\n - ${instrument.name} ($availableCount/$requiredCount)"
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

    private inline fun <K> MutableMap<K, Int>.inlineMerge(key: K, value: Int, remappingFunction: (Int, Int) -> Int) {
        get(key)?.let {
            put(key, remappingFunction(it, value))
        } ?: put(key, value)
    }

}
