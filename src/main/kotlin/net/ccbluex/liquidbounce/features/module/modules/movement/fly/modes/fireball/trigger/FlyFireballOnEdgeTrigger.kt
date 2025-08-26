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
 */

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.fireball.trigger

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.fireball.FlyFireball
import net.ccbluex.liquidbounce.utils.entity.isCloseToEdge
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention

object FlyFireballOnEdgeTrigger : Choice("OnEdge") {

    override val parent: ChoiceConfigurable<Choice>
        get() = FlyFireball.trigger

    private val edgeDistance by float("EdgeDistance", 0.01f, 0.01f..0.5f)

    @Suppress("unused")
    val inputHandler = handler<MovementInputEvent>(
        priority = EventPriorityConvention.OBJECTION_AGAINST_EVERYTHING
    ) { event ->
        val shouldBeActive = player.isOnGround && !player.isSneaking

        if (shouldBeActive && player.isCloseToEdge(event.directionalInput, edgeDistance.toDouble())) {
            FlyFireball.wasTriggered = true
        }
    }

}
