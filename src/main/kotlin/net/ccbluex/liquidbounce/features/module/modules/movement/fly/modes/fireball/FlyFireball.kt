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
import net.ccbluex.liquidbounce.utils.entity.directionYaw
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.util.Hand

internal object FlyFireball : Choice("Fireball") {

    override val parent: ChoiceConfigurable
        get() = ModuleFly.modes

    private val disableDelay by int("DisableDelay", 10, 0..20)

    object Movement : ToggleableConfigurable(this, "Strafe", true) {
        val yVelocity by float("YVelocity", 0f, -1f..1f)
        val delay by int("StrafeDelay", 0, 0..20)
        val strength by float("Strength", 0f, 0f..1f)
    }

    object Rotations : RotationsConfigurable(80f..120f) {
        val pitch by float("Pitch", 80f, 0f..90f)
    }

    init {
        tree(Movement)
        tree(Rotations)
    }

    val rotationUpdateHandler = handler<SimulatedTickEvent> {
        RotationManager.aimAt(
            Rotations.toAimPlan(Rotation(mc.player?.yaw!!, Rotations.pitch)),
            priority = Priority.IMPORTANT_FOR_PLAYER_LIFE,
            provider = ModuleFly
        )
    }

    val playerMoveHandler = sequenceHandler<PlayerMoveEvent> {
        mc.interactionManager?.interactItem(mc.player, Hand.MAIN_HAND)
        waitTicks(Movement.delay)
        if (Movement.enabled) {
            if (Movement.yVelocity != 0f) {
                mc.player?.velocity?.y = Movement.yVelocity.toDouble()
            }
            it.movement.strafe(mc.player?.directionYaw!!, strength = Movement.strength.toDouble())
        }
        waitTicks(disableDelay)
        ModuleFly.enabled = false
    }

}
