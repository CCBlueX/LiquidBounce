/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.getBlock
import net.ccbluex.liquidbounce.utils.block.toBlockPos
import net.minecraft.block.BedBlock
import net.minecraft.block.Block
import net.minecraft.block.Blocks

object ModuleBlockBounce : Module("BlockBounce", Category.MOVEMENT) {

    private val motion by float("Motion", 0.42f, 0.2f..1f)

    val repeatable = repeatable {
        val block = player.pos.toBlockPos().down().getBlock() ?: return@repeatable

        if (isBouncingBlock(block) && mc.options.keyJump.isPressed) {
            player.velocity.y += motion
        }
    }

    private fun isBouncingBlock(block: Block) = block == Blocks.SLIME_BLOCK || block is BedBlock

}
