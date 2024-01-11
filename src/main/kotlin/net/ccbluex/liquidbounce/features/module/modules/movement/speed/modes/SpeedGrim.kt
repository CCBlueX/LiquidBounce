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
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.events.TickJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.directionYaw
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.entity.MovementType

/**
 * @anticheat Grim
 * @anticheatVersion 2.3.54
 * @testedOn localhost:25565
 */
object SpeedGrim : Choice("Grim") {

    override val parent: ChoiceConfigurable
        get() = ModuleSpeed.modes

    private val speed by float("Speed", 1.116f, 1.001f..1.4f)
    private val dist by float("Distance", 2f, 0.001f..3.001f)

    val moveHandler = handler<PlayerMoveEvent> { event ->
        // get target
        val target = ModuleKillAura.targetTracker.lockedOnTarget
        // check if null
        if (target !== null) {
            // check if we are moving && target is near the dist
            if (event.type == MovementType.SELF && target.boxedDistanceTo(player) < dist) {
                // get velocity * by 8000 * speed / 8000 (minecraft velocity rule idfk)
                player.velocity.x = (player.velocity.x*8000*speed/8000)
                player.velocity.z = (player.velocity.z*8000*speed/8000)
            }
        }
    }
}
