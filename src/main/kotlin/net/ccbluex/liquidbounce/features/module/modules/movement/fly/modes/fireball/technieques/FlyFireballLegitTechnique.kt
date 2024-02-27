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
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.fireball.technieques

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.fireball.FlyFireball
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.fireball.trigger.FlyFireballInstantTrigger
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.fireball.trigger.FlyFireballOnEdgeTrigger
import net.ccbluex.liquidbounce.utils.entity.isCloseToEdge
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import kotlin.coroutines.suspendCoroutine

object FlyFireballLegitTechnique : Choice("Legit") {

    override val parent: ChoiceConfigurable
        get() = FlyFireball.technique

    object Jump: ToggleableConfigurable(this, "Jump", true) {
        val delay by int("Delay", 3, 0..20, "ticks")
    }

    val sprint by boolean("Sprint", true)
    val stopMove by boolean("StopMove", true) /* Stop moving when module is active to avoid falling off, for example a bridge. */

    var canMove = true

    init {
        tree(Jump)
    }

    private val movementInputHandler = sequenceHandler<MovementInputEvent> { event ->
        if (stopMove && !canMove)
            event.directionalInput = DirectionalInput.BACKWARDS // Cancel out movement.
    }

    private val repeatable = repeatable {
        if (FlyFireball.wasTriggered) {
            canMove = !stopMove

            if (Jump.enabled && player.isOnGround)
                player.jump()


            if (Jump.enabled)
                waitTicks(Jump.delay)

            FlyFireball.throwFireball()

            if (sprint)
                player.isSprinting = true

            ModuleFly.enabled = false // Disable after the fireball was thrown
            canMove = true
            FlyFireball.wasTriggered = false
        }
    }

}
