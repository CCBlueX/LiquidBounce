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
import net.ccbluex.liquidbounce.event.events.PlayerStrideEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.*
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.fireball.FlyFireball
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.grim.FlyGrim2859V
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.sentinel.FlySentinel10thMar
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.sentinel.FlySentinel20thApr
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.sentinel.FlySentinel27thJan
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.spartan.FlySpartan524
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.specific.FlyClip
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.verus.FlyVerusDamage
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.vulcan.FlyVulcan277
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.vulcan.FlyVulcan286
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.vulcan.FlyVulcan286MC18
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.vulcan.FlyVulcan286Teleport

/**
 * Fly module
 *
 * Allows you to fly.
 */

object ModuleFly : Module("Fly", Category.MOVEMENT, aliases = arrayOf("Glide", "Jetpack")) {

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
            FlyFireball,

            // Anti-cheat specific fly modes
            FlyVulcan277,
            FlyVulcan286,
            FlyVulcan286MC18,
            FlyVulcan286Teleport,
            FlyGrim2859V,
            FlySpartan524,

            // Server specific fly modes
            FlySentinel20thApr,
            FlySentinel27thJan,
            FlySentinel10thMar,

            FlyVerusDamage,
            FlyClip,
        )
    )

    private object Visuals : ToggleableConfigurable(this, "Visuals", true) {

        private val stride by boolean("Stride", true)

        @Suppress("unused")
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
