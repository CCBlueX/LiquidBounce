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

import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBlockRotation
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.raytracing.raytraceBlock
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundSwingPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult

class NoteBlockTracker(val pos: BlockPos): MinecraftShortcuts {
    var currentNote: Int? = null
        private set

    private val tuneTimeout = Chronometer()
    private val testTimeout = Chronometer()

    fun canTuneRightNow(): Boolean {
        return currentNote != null || tuneTimeout.hasElapsed(2000)
    }
    fun canTestRightNow(): Boolean {
        return testTimeout.hasElapsed(2000)
    }

    fun tuneOnce() {
        this.interact()

        this.tuneTimeout.reset()
    }

    fun testOnce() {
        this.click()

        this.testTimeout.reset()
    }

    // TODO switch to empty slot?
    private fun interact() {
        val blockState = this.pos.getState()!!
        val raytrace = raytraceBlockRotation(
            player.eyePosition,
            this.pos,
            blockState,
            range = ModuleNotebot.range.toDouble(),
            wallsRange = ModuleNotebot.range.toDouble()
        ) ?: return

        val blockHitResult: BlockHitResult = raytraceBlock(
            ModuleNotebot.range.toDouble(),
            raytrace.rotation,
            this.pos,
            blockState,
        ) ?: return

        network.send(
            ServerboundMovePlayerPacket.Rot(
                raytrace.rotation.yaw,
                raytrace.rotation.pitch,
                player.lastOnGround,
                player.horizontalCollision
            )
        )

        interaction.startPrediction(world) { sequence ->
            ServerboundUseItemOnPacket(
                InteractionHand.MAIN_HAND,
                blockHitResult,
                sequence
            )
        }

        // We don't know what the current note is after interacting
        this.currentNote = null
    }

    fun click() {
        interaction.startPrediction(world) { sequence ->
            ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                this.pos,
                Direction.UP,
                sequence
            )
        }

        network.send(ServerboundSwingPacket(InteractionHand.MAIN_HAND))

//        interaction.sendSequencedPacket(world) { sequence ->
//            PlayerActionC2SPacket(
//                PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,
//                blockPos,
//                Direction.UP,
//                sequence
//            )
//        }
    }

    override fun equals(other: Any?) = other is NoteBlockTracker && pos == other.pos
    override fun hashCode() = pos.hashCode()

    fun setObservedNote(note: Int) {
        this.currentNote = note
    }

}
