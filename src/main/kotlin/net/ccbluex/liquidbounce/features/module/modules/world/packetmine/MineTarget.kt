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
package net.ccbluex.liquidbounce.features.module.modules.world.packetmine

import net.ccbluex.liquidbounce.render.EMPTY_BOX
import net.ccbluex.liquidbounce.utils.block.getCenterDistanceSquaredEyes
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.network
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

class MineTarget(val targetPos: BlockPos) {

    var finished = false
    var progress = 0f
    var started = false
    var direction: Direction? = null
    var blockState = targetPos.getState()!!

    fun init() {
        with(ModulePacketMine) {
            targetRenderer.addBlock(targetPos, box = EMPTY_BOX.expand(0.01e-5, 0.0, 0.0))
            targetRenderer.updateAll()
        }
    }

    fun cleanUp() {
        with(ModulePacketMine) {
            targetRenderer.removeBlock(targetPos)
            if (!finished && mode.activeChoice.canAbort) {
                abort(true)
            }
        }
    }

    fun updateBlockState() {
        blockState = targetPos.getState()!!
    }

    fun isInvalidOrOutOfRange(): Boolean {
        val state = targetPos.getState()!!
        val invalid = ModulePacketMine.mode.activeChoice.isInvalid(this, state)
        return invalid || targetPos.getCenterDistanceSquaredEyes() > ModulePacketMine.keepRange.sq()
    }

    fun abort(force: Boolean = false) {
        val notPossible = !started || finished || !ModulePacketMine.mode.activeChoice.canAbort
        if (notPossible || !force && targetPos.getCenterDistanceSquaredEyes() <= ModulePacketMine.keepRange.sq()) {
            return
        }

        val dir = if (ModulePacketMine.abortAlwaysDown) {
            Direction.DOWN
        } else {
            direction ?: Direction.DOWN
        }

        network.sendPacket(PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, targetPos, dir))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (javaClass != other?.javaClass) {
            return false
        }

        return targetPos == (other as MineTarget).targetPos
    }

    override fun hashCode(): Int {
        return targetPos.hashCode()
    }

    fun copy() = MineTarget(targetPos)

}
