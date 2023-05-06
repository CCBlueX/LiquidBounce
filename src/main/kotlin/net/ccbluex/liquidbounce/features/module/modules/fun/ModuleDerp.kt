/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

/**
 * Derp module
 *
 * Makes it look as if you were derping around.
 */
object ModuleDerp : Module("Derp", Category.FUN) {

    private val headLess by boolean("Headless", false)

    private object NoRandomSpin : ToggleableConfigurable(this, "NoRandomSpin", false) {
        val increment by float("Increment", 0f, 0f..50f)
    }

    init {
        tree(NoRandomSpin)
    }

    private var currentSpin = 0f

    val rotation: FloatArray
        get() {
            val derpRotations =
                floatArrayOf(player.yaw + (Math.random() * 360 - 180).toFloat(), (Math.random() * 180 - 90).toFloat())

            if (headLess) {
                derpRotations[1] = 180f
            }

            if (NoRandomSpin.enabled) {
                derpRotations[0] = currentSpin + NoRandomSpin.increment
                currentSpin = derpRotations[0]
            }

            return derpRotations
        }
}
