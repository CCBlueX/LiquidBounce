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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.repeatable
import net.ccbluex.liquidbounce.utils.extensions.getState
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult

/**
 * FastBreak module
 *
 * Allows you to break blocks faster.
 */
object ModuleAutoBreak : Module("AutoBreak", Category.WORLD) {

    private var wasBreaking: Boolean = false

    val repeatable = repeatable {
        if (wasBreaking) {
            wasBreaking = false
            mc.options.keyAttack.isPressed = false
        }

        val crosshairTarget = mc.crosshairTarget

        if (crosshairTarget == null || crosshairTarget.type != HitResult.Type.BLOCK)
            return@repeatable

        val blockHitResult = crosshairTarget as BlockHitResult

        val blockState = blockHitResult.blockPos.getState() ?: return@repeatable

        if (blockState.isAir) {
            return@repeatable
        }

        mc.options.keyAttack.isPressed = true // Default back to off
        wasBreaking = true
    }

    override fun enable() {
        wasBreaking = false
    }

    override fun disable() {
        mc.options.keyAttack.isPressed = false // Default back to off

        super.disable()
    }

}
