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
package net.ccbluex.liquidbounce.features.module.modules.player.antivoid.mode

import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.BlockShapeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.ModuleAntiVoid
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.ModuleAntiVoid.isLikelyFalling
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.ModuleAntiVoid.rescuePosition
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.mode.AntiVoidGhostBlockMode.handleBlockShape
import net.minecraft.util.shape.VoxelShapes
import kotlin.math.floor

object AntiVoidGhostBlockMode : AntiVoidMode("GhostBlock") {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleAntiVoid.mode

    @Suppress("unused")
    private val handleBlockShape = handler<BlockShapeEvent> { event ->
        if (!isLikelyFalling || isExempt) {
            return@handler
        }

        // We only want to place a fake-block collision below the player if the collision shape is empty.
        var safePosition = rescuePosition
        if (event.shape != VoxelShapes.empty() || safePosition == null || event.pos.y >= floor(safePosition.y)) {
            return@handler
        }

        event.shape = VoxelShapes.fullCube()
    }

    /**
     * We have [handleBlockShape] to fix our situation instead.
     */
    override fun rescue() = false

}
