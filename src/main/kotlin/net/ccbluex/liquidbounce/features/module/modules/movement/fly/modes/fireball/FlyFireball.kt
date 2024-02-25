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

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.fireball

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.util.Hand

internal object FlyFireball : Choice("Fireball") {

    override val parent: ChoiceConfigurable
        get() = ModuleFly.modes

    private val disableDelay by int("DisableDelay", 10, 0..20)

    object YMovement : ToggleableConfigurable(this, "YMovement", true) {
        val yVelocity by float("YVelocity", 0f, -5f..5f)
        val yVelocityDelay by int("YVelocityDelay", 0, 0..20)
    }

    object Rotations : RotationsConfigurable(80f..120f) {
        val pitch by float("Pitch", 80f, 0f..90f)
    }

    init {
        tree(YMovement)
        tree(Rotations)
    }

    val rotationUpdateHandler = handler<SimulatedTickEvent> {
        RotationManager.aimAt(
            Rotations.toAimPlan(Rotation(player.yaw, Rotations.pitch)),
            priority = Priority.IMPORTANT_FOR_PLAYER_LIFE,
            provider = ModuleFly
        )
    }

    val playerMoveHandler = sequenceHandler<PlayerMoveEvent> {
        if (player.isOnGround) {
            interaction.interactItem(player, Hand.MAIN_HAND)
            player.isSprinting = true
        }

        if (YMovement.enabled) {
            waitTicks(YMovement.yVelocityDelay)
            player.velocity.y = YMovement.yVelocity.toDouble()
        }

        waitTicks(disableDelay)
        ModuleFly.enabled = false
    }

}
