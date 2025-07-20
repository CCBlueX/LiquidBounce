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
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerAfterJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldNormalTechnique
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.entity.airTicks
import net.ccbluex.liquidbounce.utils.entity.moving

/**
 * Telly feature
 *
 * This is based on the telly technique and means that the player will jump when moving.
 * That allows for a faster scaffold.
 * Depending on the SameY setting, we might scaffold upwards.
 *
 * @see ModuleScaffold
 */
object ScaffoldTellyFeature : ToggleableConfigurable(ScaffoldNormalTechnique, "Telly", false) {

    val doNotAim: Boolean
        get() = player.airTicks <= straightTicks && 
                ticksUntilJump >= jumpTicks &&
                !(ModuleScaffold.isTowering && aimOnTower)
                

    // New val to determine if the player is telly bridging
    val isTellyBridging: Boolean
        get() = ticksUntilJump >= jumpTicks && player.moving

    private var ticksUntilJump = 0

    val resetMode by enumChoice("ResetMode", Mode.RESET)
    private val straightTicks by int("Straight", 0, 0..5, "ticks")
    private val jumpTicksOpt by intRange("Jump", 0..0, 0..10, "ticks")
    private val aimOnTower by boolean("AimOnTower", true)
    private var jumpTicks = jumpTicksOpt.random()

    @Suppress("unused")
    private val gameHandler = handler<GameTickEvent> {
        if (player.isOnGround) {
            ticksUntilJump++
        }
    }

    @Suppress("unused")
    private val movementInputHandler = handler<MovementInputEvent> { event ->
        if (!player.moving || ModuleScaffold.blockCount <= 0 || !player.isOnGround) {
            return@handler
        }

        val isStraight = RotationManager.currentRotation == null || straightTicks == 0

        when (resetMode) {
            Mode.REVERSE -> event.jump = true
            Mode.RESET -> if (isStraight && ticksUntilJump >= jumpTicks) event.jump = true
        }
    }

    @Suppress("unused")
    private val afterJumpHandler = handler<PlayerAfterJumpEvent> {
        ticksUntilJump = 0
        jumpTicks = jumpTicksOpt.random()
    }

    enum class Mode(override val choiceName: String) : NamedChoice {
        REVERSE("Reverse"),
        RESET("Reset")
    }

}
