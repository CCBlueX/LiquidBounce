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

import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.ModuleNotebot.NotebotStage
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.ModuleNotebot.NotebotStageHandler
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.ModuleNotebot.renderer
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.nbs.SongData
import net.ccbluex.liquidbounce.features.module.modules.`fun`.notebot.stages.NotebotTestStageHandler
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.sound.SoundCategory
import net.minecraft.util.math.BlockPos
import kotlin.math.log2
import kotlin.math.roundToInt

class NotebotEngine(
    val songData: SongData,
    val blocksAndRequirements: NotebotScanner.BlocksAndRequirements
) {

    private var currentStageHandler: NotebotStageHandler = NotebotTestStageHandler(this)
    private var ticksToWait: Int? = null

    private val notebotTrackerMap: Map<BlockPos, NoteBlockTracker> = blocksAndRequirements.availableBlocks
        .flatMap { it.value }
        .associateBy { it.pos }

    fun handleSoundPacket(packet: PlaySoundS2CPacket) {
        if (currentStageHandler.handledStage == NotebotStage.PLAY) {
            return
        }

        val soundKey = packet.sound.key.get()

        if (packet.category != SoundCategory.RECORDS || !soundKey.value.path.contains("note_block")) {
            return
        }

        val pos = BlockPos((packet.x - 0.5).toInt(), (packet.y - 0.5).toInt(), (packet.z - 0.5).toInt())
        val causingNoteBlock = this.notebotTrackerMap[pos] ?: return

        causingNoteBlock.setObservedNote((12f + 12f * log2(packet.pitch)).roundToInt())
    }

    suspend fun onTick(sequence: Sequence) {
        val ticks = ticksToWait

        if (ticks != null) {
            sequence.waitTicks(ticks)

            ticksToWait = null
        }

        currentStageHandler.onTick(this)
    }

    fun changeStage(handler: NotebotStageHandler) {
        ticksToWait = handler.handledStage.stageStartDelay()

        renderer.onStateChange(handler.handledStage)

        this.currentStageHandler = handler
    }

}
