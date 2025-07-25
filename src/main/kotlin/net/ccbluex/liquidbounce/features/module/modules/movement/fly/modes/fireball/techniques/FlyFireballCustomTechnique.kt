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
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.fireball.FlyFireball
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.entity.MovementType

object FlyFireballCustomTechnique : Choice("Custom") {

    override val parent: ChoiceConfigurable<Choice>
        get() = FlyFireball.technique

    private val disableDelay by int("DisableDelay", 10, 0..20)
    private val throwDelay by int("ThrowDelay", 2, 0..20)

    object Jump : ToggleableConfigurable(this, "Jump", true) {
        val delay by int("JumpDelay", 1, 0..20, "ticks")
    }

    object YVelocity : ToggleableConfigurable(this, "YVelocity", true) {
        val velocity by float("Velocity", 0f, -5f..5f)
        val delay by int("Delay", 0, 0..20, "ticks")
    }

    val sprint by boolean("Sprint", true)
    //  Stop moving when module is active to avoid falling off, for example a bridge
    val stopMove by boolean("StopMove", true)

    object Rotations : RotationsConfigurable(this) {
        val pitch by float("Pitch", 90f, 0f..90f)
    }

    var canMove = true

    init {
        tree(Jump)
        tree(YVelocity)
        tree(Rotations)
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        RotationManager.setRotationTarget(
            Rotation(player.yaw, Rotations.pitch),
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
    val playerMoveHandler = sequenceHandler<PlayerMoveEvent> {
        if (it.type != MovementType.SELF) return@sequenceHandler

        if (player.isOnGround) {
            if (Jump.enabled) {
                waitTicks(Jump.delay)
                player.jump()
            }

            waitTicks(throwDelay)

            FlyFireball.throwFireball()

            if (sprint) {
                player.isSprinting = true
            }
        }

        if (YVelocity.enabled) {
            waitTicks(YVelocity.delay)
            player.velocity.y = YVelocity.velocity.toDouble()
        }

        waitTicks(disableDelay)

        ModuleFly.enabled = false // Disable after the fireball was thrown
        canMove = true
        FlyFireball.wasTriggered = false
    }

}
