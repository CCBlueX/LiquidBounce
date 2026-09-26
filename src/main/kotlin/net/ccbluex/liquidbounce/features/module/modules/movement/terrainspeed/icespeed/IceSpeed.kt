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

package net.ccbluex.liquidbounce.features.module.modules.movement.terrainspeed.icespeed

import net.ccbluex.fastutil.referenceHashSetOf
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.BlockSlipperinessMultiplierEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.terrainspeed.ModuleTerrainSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.terrainspeed.icespeed.IceSpeed.Motion.horizontalMotion
import net.minecraft.world.level.block.Blocks

/**
 * Ice Speed allows you to manipulate slipperiness speed
 */
internal object IceSpeed : ToggleableValueGroup(ModuleTerrainSpeed, "IceSpeed", true) {

    private val slipperiness by float("Slipperiness", 0.6f, 0.3f..1f)

    private object Motion : ToggleableValueGroup(ModuleTerrainSpeed, "Motion", false) {
        val horizontalMotion by float("Motion", 0.5f, 0.2f..1.5f)
    }

    init {
        tree(Motion)
    }

    private val iceBlocks = referenceHashSetOf(Blocks.ICE, Blocks.BLUE_ICE, Blocks.FROSTED_ICE, Blocks.PACKED_ICE)

    @Suppress("unused")
    val blockSlipperinessMultiplierHandler = handler<BlockSlipperinessMultiplierEvent> { event ->
        if (event.block in iceBlocks) {
            if (Motion.enabled) {
                player.deltaMovement =
                    player.deltaMovement.multiply(horizontalMotion.toDouble(), 1.0, horizontalMotion.toDouble())
            }

            event.slipperiness = slipperiness
        }
    }
}
