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

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.fireball.techniques

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.fireball.FlyFireball
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.util.math.MathHelper

object FlyFireballLegitTechnique : Choice("Legit") {

    override val parent: ChoiceConfigurable<Choice>
        get() = FlyFireball.technique

    object Jump : ToggleableConfigurable(this, "Jump", true) {
        val delay by int("Delay", 3, 0..20, "ticks")
    }

    val sprint by boolean("Sprint", true)

    // Stop moving when module is active to avoid falling off, for example a bridge.
    val stopMove by boolean("StopMove", true)

    var canMove = true

    object Rotations : RotationsConfigurable(this) {
        val pitch by float("Pitch", 90f, 0f..90f)
        val backwards by boolean("Backwards", true)
    }

    init {
        tree(Jump)
        tree(Rotations)
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        RotationManager.setRotationTarget(
            Rotation(if (Rotations.backwards) this.invertYaw(player.yaw) else player.yaw, Rotations.pitch),
            configurable = Rotations,
            priority = Priority.IMPORTANT_FOR_PLAYER_LIFE,
            provider = ModuleFly
        )
    }

    @Suppress("unused")
    private val movementInputHandler = sequenceHandler<MovementInputEvent> { event ->
        if (stopMove && !canMove) {
            event.directionalInput = DirectionalInput.BACKWARDS // Cancel out movement.
        }
    }

    @Suppress("unused")
    private val repeatable = tickHandler {
        if (FlyFireball.wasTriggered) {
            canMove = !stopMove

            if (Jump.enabled && player.isOnGround) {
                player.jump()
            }


            if (Jump.enabled) {
                waitTicks(Jump.delay)
            }

            FlyFireball.throwFireball()

            if (sprint) {
                player.isSprinting = true
            }

            ModuleFly.enabled = false // Disable after the fireball was thrown
            canMove = true
            FlyFireball.wasTriggered = false
        }
    }

    /**
     * Inverts yaw (-180 to 180)
     */
    private fun invertYaw(yaw: Float): Float {
        return MathHelper.wrapDegrees(yaw + 180)
    }

}
