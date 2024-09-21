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
package net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.ModuleLongJump
import net.ccbluex.liquidbounce.utils.entity.upwards

/**
 * abuses an exemption on some simulation anticheats allowing you to fly for a bit
 * after dismounting a vehicle (e.g. boat)
 * works on grim and on intave with the correct config
 */

internal object VehicleLongJump : Choice("Vehicle") {
    override val parent: ChoiceConfigurable<*>
        get() = ModuleLongJump.mode

    private val verticalLaunch by float("VerticalLaunch", 0.6f, 0.0f..1f)
    private val horizontalSpeed by float("HorizontalSpeed", 10f, 0f..100f)
    private val ticksToBoost by int("TicksToBoost", 1, 1..20)
    var inVehicleTicks = 0

    val repeatable = repeatable {
        if (player.hasVehicle()) {
            inVehicleTicks++
        }

        if (inVehicleTicks == ticksToBoost) {
            player.dismountVehicle()
            player.upwards(verticalLaunch)
            player.forwardSpeed = horizontalSpeed
        }
    }
}
