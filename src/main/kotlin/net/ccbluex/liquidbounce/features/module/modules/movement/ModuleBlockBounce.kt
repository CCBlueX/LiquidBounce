/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.isBlockAtPosition
import net.ccbluex.liquidbounce.utils.entity.box
import net.minecraft.block.BedBlock
import net.minecraft.block.HoneyBlock
import net.minecraft.block.SlimeBlock

/**
 * BlockBounce module
 *
 * Allows you to bounce higher on bouncy blocks.
 */

object ModuleBlockBounce : Module("BlockBounce", Category.MOVEMENT) {

    private val motion by float("Motion", 0.42f, 0.2f..2f)

    val jumpHandler = handler<PlayerJumpEvent> { event ->
        if (standingOnBouncyBlock()) {
            event.motion += motion
        }
    }

    fun standingOnBouncyBlock(): Boolean {
        val boundingBox = player.box
        val detectionBox = boundingBox.withMinY(boundingBox.minY - 0.01)

        return isBlockAtPosition(detectionBox) { it is SlimeBlock || it is BedBlock || it is HoneyBlock }
    }
}
