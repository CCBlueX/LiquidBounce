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

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.BlockShapeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.block.CobwebBlock
import net.minecraft.util.shape.VoxelShapes

/**
 * NoWeb module
 *
 * Prevents you from walking into webs in different ways.
 */
object ModuleNoWeb : Module("NoWeb", Category.MOVEMENT) {

    val modes = choices("Mode", Air) {
        arrayOf(
            Air,
            Block
        )
    }

    object Air : Choice("Air") {
        override val parent: ChoiceConfigurable
            get() = modes

        // Mixins take care of this mode
    }

    object Block : Choice("Block") {
        override val parent: ChoiceConfigurable
            get() = modes

        val shapeHandler = handler<BlockShapeEvent> { event ->
            if (event.state.block is CobwebBlock) {
                event.shape = VoxelShapes.fullCube()
            }
        }
    }
}

