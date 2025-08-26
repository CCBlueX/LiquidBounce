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
 *
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.modes

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.BlockShapeEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.ModuleLiquidWalk
import net.ccbluex.liquidbounce.utils.block.isBlockAtPosition
import net.ccbluex.liquidbounce.utils.entity.box
import net.minecraft.block.FluidBlock
import net.minecraft.util.shape.VoxelShapes

internal object LiquidWalkVanilla : Choice("Vanilla") {

    override val parent: ChoiceConfigurable<Choice>
        get() = ModuleLiquidWalk.modes

    @Suppress("unused")
    val inputHandler = handler<MovementInputEvent> { event ->
        if (event.sneak || !player.box.isBlockAtPosition { it is FluidBlock }) {
            return@handler
        }

        // Swims up
        event.jump = true
    }

    @Suppress("unused")
    val shapeHandler = handler<BlockShapeEvent> { event ->
        if (mc.options.sneakKey.isPressed || player.fallDistance > 3.0f || player.isOnFire) {
            return@handler
        }

        val block = event.state.block

        if (block is FluidBlock && !player.box.isBlockAtPosition { it is FluidBlock }) {
            event.shape = VoxelShapes.fullCube()
        }
    }

}
