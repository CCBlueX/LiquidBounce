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

package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.`fun`.ModuleDerp.Spinny.increment
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable

/**
 * Derp module
 *
 * Makes it look as if you were derping around.
 */

object ModuleDerp : Module("Derp", Category.FUN) {

    private object Spinny : ToggleableConfigurable(this, "Spinny", true) {
        val increment by float("Increment", 10f, -180f..180f)
    }

    val headLess by boolean("Headless", false)

    val rotations = RotationsConfigurable()
    var currentSpin = 0f

    val repeatable = repeatable {
        RotationManager.aimAt(
            Rotation(
                if (Spinny.enabled) incrementSpin else player.yaw + (Math.random() * 360 - 180).toFloat(),
                if (headLess) 180f else (Math.random() * 180 - 90).toFloat()
            ), configurable = rotations
        )
    }

    val incrementSpin: Float
        get() {
            val incrementYaw = currentSpin + increment
            currentSpin = incrementYaw
            return incrementYaw
        }

    init {
        tree(rotations)
        tree(Spinny)
    }
}
