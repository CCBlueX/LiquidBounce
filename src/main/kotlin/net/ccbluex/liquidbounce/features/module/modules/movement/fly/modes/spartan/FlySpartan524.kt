/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.spartan

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly.modes
import kotlin.math.cos
import kotlin.math.sin

/**
 * @anticheat Spartan
 * @anticheatVersion phase 524
 * @testedOn minecraft.vagdedes.com
 * @note spartan flags less if your motion is stable, that's why we use PlayerMoveEvent
 */
internal object FlySpartan524 : Choice("Spartan524") {

    override val parent: ChoiceConfigurable<*>
        get() = modes

    val moveHandler = handler<PlayerMoveEvent> { event ->
        val yaw = Math.toRadians(player.yaw.toDouble())
        event.movement.x = -sin(yaw) * 0.28
        event.movement.y = 0.0
        event.movement.z = cos(yaw) * 0.28
    }

}
