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

package net.ccbluex.liquidbounce.features.module.modules.movement.noweb.modes

import net.ccbluex.liquidbounce.features.module.modules.movement.noweb.NoWebMode
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.world.level.block.Blocks

/**
 * No collision with cobwebs and breaks them to bypass check
 *
 * @anticheat Grim
 * @version 2.3.65
 */
object NoWebGrimBreak : NoWebMode("Grim2365") {

    // Needed to bypass BadPacketsX
    private val breakOnWorld by boolean("BreakOnWorld", true)

    override fun handleEntityCollision(pos: BlockPos): Boolean {
        if (breakOnWorld) world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState())

        val start = ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, Direction.DOWN
        )
        val abort = ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, pos, Direction.DOWN
        )
        val finish = ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.DOWN
        )

        network.send(start)
        network.send(abort)
        network.send(finish)

        return true
    }
}
