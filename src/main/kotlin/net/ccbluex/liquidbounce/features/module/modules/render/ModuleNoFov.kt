/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

/**
 * NoFOV module
 *
 * Changes FOV value.
 */

object ModuleNoFov : Module("NoFOV", Category.RENDER) {
    val mode = choices("Mode", ConstantFov, arrayOf(ConstantFov, Custom))

    fun getFov(orig: Float) = mode.activeChoice.getFov(orig)


    object ConstantFov : FovMode("Constant") {
        private val fov by float("FOV", 1f, 0f..1.5f)
        override fun getFov(orig: Float) = fov
    }

    object Custom : FovMode("Custom") {
        private val baseFov by float("BaseFOV", 1f, 0f..1.5f)
        private val limit by floatRange("Limit", 0f..1.5f, 0f..1.5f)
        private val multiplier by float("Multiplier", 1f, 0.1f..1.5f)
        override fun getFov(orig: Float): Float {
            val newFov = (orig - 1) * multiplier + baseFov
            return newFov.coerceIn(limit)
        }
    }

    abstract class FovMode(name: String) : Choice(name) {
        override val parent: ChoiceConfigurable<*>
            get() = mode
        open fun getFov(orig: Float) = orig
    }
}
