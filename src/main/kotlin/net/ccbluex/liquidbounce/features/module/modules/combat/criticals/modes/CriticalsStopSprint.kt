/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.combat.criticals.modes


import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura


//1.9+  Critical

object CriticalsStopSprint : Mode("StopSprint") {

    override val parent: ModeValueGroup<*>
        get() = ModuleCriticals.modes

    private val controlSprintKey by boolean("ControlSprintKey", true)
    private val hurtTime by intRange("HurtTime", 0..10, 0..10)

    @Suppress("unused")
    private val sprintEventHandler = handler<SprintEvent> { event ->
        if (player.deltaMovement.y < 0.0
            && !player.onGround()
            && !player.isInWater
            && !player.isInLava
            && !player.onClimbable()
            && event.sprint
            && event.source == SprintEvent.Source.INPUT
            && ModuleKillAura.running
            && ModuleKillAura.targetTracker.target != null
            && ModuleKillAura.targetTracker.target!!.hurtTime in hurtTime
        ) {
            if (controlSprintKey) {
                mc.options.keySprint.isDown = false
            }
            else {
                event.sprint = false
            }
        }
    }

}
