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
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBlock
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBlockRotation
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

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
            player.eyePos,
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

        network.sendPacket(
            PlayerMoveC2SPacket.LookAndOnGround(
                raytrace.rotation.yaw,
                raytrace.rotation.pitch,
                player.lastOnGround,
                player.horizontalCollision
            )
        )

        interaction.sendSequencedPacket(world) { sequence ->
            PlayerInteractBlockC2SPacket(
                Hand.MAIN_HAND,
                blockHitResult,
                sequence
            )
        }

        // We don't know what the current note is after interacting
        this.currentNote = null
    }

    fun click() {
        interaction.sendSequencedPacket(world) { sequence ->
            PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                this.pos,
                Direction.UP,
                sequence
            )
        }

        network.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))

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
