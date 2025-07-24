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
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule

/**
 * NoFOV module
 *
 * Changes FOV value.
 */
object ModuleNoFov : ClientModule("NoFOV", Category.RENDER) {

    val mode = choices("Mode", ConstantFov, arrayOf(ConstantFov, Custom))

    fun getFovMultiplier(original: Float) = mode.activeChoice.getFovMultiplier(original)

    fun getFov(original: Int) = mode.activeChoice.getFov(original)

    object ConstantFov : FovMode("Constant") {

        private val fov by int("FOV", 90, 1..179)

        override fun getFovMultiplier(original: Float) = 1f

        override fun getFov(original: Int): Int {
            return fov
        }

    }

    object Custom : FovMode("Custom") {

        private val baseFov by float("BaseFOV", 1f, 0f..1.5f)
        private val limit by floatRange("Limit", 0f..1.5f, 0f..1.5f)
        private val multiplier by float("Multiplier", 1f, 0.1f..1.5f)

        override fun getFovMultiplier(original: Float): Float {
            val newFov = (original - 1) * multiplier + baseFov
            return newFov.coerceIn(limit)
        }

    }

    abstract class FovMode(name: String) : Choice(name) {

        override val parent: ChoiceConfigurable<*>
            get() = mode

        abstract fun getFovMultiplier(original: Float): Float

        open fun getFov(original: Int) = original

    }

}
