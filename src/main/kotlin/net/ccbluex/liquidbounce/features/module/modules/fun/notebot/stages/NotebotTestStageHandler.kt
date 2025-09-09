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
package net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.stages

import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.ModuleNotebot
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.NoteBlockTracker
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.NotebotEngine
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.util.Formatting

// we could also read the note property from blocks, but I've found this unreliable
// this design also seems more future-proof if minecraft should stop sending this data to the client in future versions
class NotebotTestStageHandler(engine: NotebotEngine): ModuleNotebot.NotebotStageHandler {

    private val progressName = ModuleNotebot.message("progressTest")
    private val allBlocks = engine.blocksAndRequirements.availableBlocks.flatMap { it.value }
    private val remainingNoteBlocks = ArrayDeque(allBlocks)

    override val handledStage: ModuleNotebot.NotebotStage
        get() = ModuleNotebot.NotebotStage.TEST

    override fun onTick(engine: NotebotEngine) {
        val untestedBlock = getNextBlockToTest()

        if (untestedBlock == null) {
            chat(ModuleNotebot.message("startTuning").formatted(Formatting.GREEN), ModuleNotebot)
            engine.changeStage(NotebotTuneStageHandler(engine))

            return
        }

        if (!untestedBlock.canTestRightNow()) {
            return
        }

        untestedBlock.testOnce()

        // Requeue for checking at another point in time...
        remainingNoteBlocks.add(untestedBlock)

        val notTestedBlocks = remainingNoteBlocks.count { it.currentNote == null }

        val total = this.allBlocks.size

        ModuleNotebot.sendNewProgressMessage(progressName, total - notTestedBlocks, total)
    }

    private fun getNextBlockToTest(): NoteBlockTracker? {
        while (remainingNoteBlocks.isNotEmpty()) {
            val currentSubject = remainingNoteBlocks.removeFirst()

            // Check if the note is already known...
            if (currentSubject.currentNote == null) {
                return currentSubject
            }
        }

        return null
    }

}
