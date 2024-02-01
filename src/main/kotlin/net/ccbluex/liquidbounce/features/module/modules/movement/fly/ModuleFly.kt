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
package net.ccbluex.liquidbounce.features.module.modules.movement.fly

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.FlyAirWalk
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.FlyEnderpearl
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.FlyExplosion
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.FlyJetpack
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.FlyVanilla
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.sentinel.FlySentinel27thJan
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.spartan.FlySpartan524
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.verus.FlyVerusDamage
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.vulcan.FlyVulcan277Glide
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.negativity.FlyNegativity

/**
 * Fly module
 *
 * Allows you to fly.
 */

object ModuleFly : Module("Fly", Category.MOVEMENT) {

    init {
        enableLock()
    }

    internal val modes = choices(
        "Mode", FlyVanilla, arrayOf(
            // Generic fly modes
            FlyVanilla,
            FlyJetpack,
            FlyEnderpearl,
            FlyAirWalk,
            FlyExplosion,

            // Anti-cheat specific fly modes
            FlyVulcan277Glide,
            FlySpartan524,
            FlySentinel27thJan,
            FlyVerusDamage,
            FlyNegativity,
        )
    )

    private object Visuals : ToggleableConfigurable(this, "Visuals", true) {

        private val stride by boolean("Stride", true)

        val strideHandler = handler<PlayerStrideEvent> { event ->
            if (stride) {
                event.strideForce = 0.1.coerceAtMost(player.velocity.horizontalLength()).toFloat()
            }

        }

    }

    init {
        tree(Visuals)
    }

}
