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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.getState
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult

/**
 * AutoBreak module
 *
 * Automatically breaks blocks.
 */
object ModuleAutoBreak : Module("AutoBreak", Category.PLAYER) {

    private var wasBreaking = false

    val repeatable = repeatable {
        val crosshairTarget = mc.crosshairTarget

        if (crosshairTarget is BlockHitResult && crosshairTarget.type == HitResult.Type.BLOCK) {
            val blockState = crosshairTarget.blockPos.getState() ?: return@repeatable
            if (blockState.isAir) {
                return@repeatable
            }

            // Start breaking
            mc.options.attackKey.isPressed = true
            wasBreaking = true
        } else if (wasBreaking) {
            // Stop breaking
            wasBreaking = false
            mc.options.attackKey.isPressed = false
        }
    }

    override fun enable() {
        // Just in case something goes wrong. o.O
        wasBreaking = false
    }

    override fun disable() {
        // Check if auto break was breaking a block
        if (wasBreaking) {
            mc.options.attackKey.isPressed = false
            wasBreaking = false
        }
    }

}
