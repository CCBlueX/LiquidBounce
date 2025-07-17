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
import net.minecraft.block.enums.NoteBlockInstrument
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

class NoteBlock(
    val blockPos: BlockPos,
    val instrument: NoteBlockInstrument,
    val noteValue: Int
) : MinecraftShortcuts {

    var tuned = false
    var deliveredCurrent = false
    var tested = false
    var verified = false
    var currentNote = 0

    fun test(): Boolean {
        if (!tested) {
            click()
            tested = true
        }

        return deliveredCurrent
    }

    fun tune(): Boolean {
        if (!verified) {
            return false
        }

        if (currentNote != noteValue) {
            interact()
            verified = false
            return false
        }

        tuned = true
        return true
    }

    // TODO switch to empty slot?
    private fun interact() {
        val blockState = blockPos.getState()!!
        val raytrace = raytraceBlockRotation(
            player.eyePos,
            blockPos,
            blockState,
            range = ModuleNotebot.range.toDouble(),
            wallsRange = ModuleNotebot.range.toDouble()
        ) ?: return

        val blockHitResult: BlockHitResult = raytraceBlock(
            ModuleNotebot.range.toDouble(),
            raytrace.rotation,
            blockPos,
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
    }

    fun click() {
        interaction.sendSequencedPacket(world) { sequence ->
            PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                blockPos,
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

    override fun equals(other: Any?) = other is NoteBlock && blockPos == other.blockPos

    override fun hashCode() = blockPos.hashCode()

}
