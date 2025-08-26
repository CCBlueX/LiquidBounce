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
 *
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.grim

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly.modes
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.util.math.Vec3d

/**
 * @anticheat Grim
 * @anticheatVersion 2.3.59 (works on latest)
 * @testedOn eu.loyisa.cn
 * @note Slow on high ping
 */
internal object FlyGrim2859V : Choice("Grim2859-V") {

    private val toggle by int("Toggle", 0, 0..100)
    private val timer by float("Timer", 0.446f, 0.1f..1f)

    override val parent: ChoiceConfigurable<*>
        get() = modes


    var ticks = 0
    var pos: Vec3d? = null

    override fun enable() {
        ticks = 0
        pos = null
    }

    val tickHandler = handler<PlayerTickEvent> {
        when {
            ticks == 0 -> player.jump()
            // For some reason, low timer makes the timer jump (2 tick start)
            // A lot more stable.
            ticks <= 5 -> Timer.requestTimerSpeed(timer, Priority.IMPORTANT_FOR_USAGE_2, ModuleFly, 1)
            // If ticks goes over toggle limit and toggle isnt 0, disable.
            ticks >= toggle && toggle != 0 -> ModuleFly.enabled = false
        }

        ticks++
    }

    @Suppress("unused")
    val movementPacketsPre = handler<PlayerNetworkMovementTickEvent> { event ->
        // After 2 ticks of jumping start setting positions.
        if (ticks >= 2) {
            if (event.state == EventState.PRE) {

                /**
                 * Main logic, offsets to unloaded chunks so grim wont flag
                 * for simulation.
                 *
                 * This is done in NetworkMovementTick so packets wont be edited.
                 * If this would be a packet event, grim would flag for BadPacketsN
                 * since we are setting setback packet positions to be in unloaded.
                 * By setting position far away, grim sets us back (relative to motion).
                 * Before, this was used for a damage fly, but it was patched.
                 * For some reason this still exists.
                 *
                 * Tested versions: 2.3.59
                 */

                pos = player.pos
                player.setPosition(player.pos.x + 1152, player.pos.y, player.pos.z + 1152)
            } else if (pos != null) {
                player.setPosition(pos)
            }
        }
    }

}
