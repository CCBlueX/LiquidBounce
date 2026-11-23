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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.bridging

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerAfterJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.entity.airTicks
import net.ccbluex.liquidbounce.utils.entity.moving

/**
 * Jumps while moving and only aims at the face of the next block (the way [ScaffoldBridgingNormal] does by default)
 * once the jump carried the player off the row. Depending on the SameY setting, it scaffolds upwards.
 */
object ScaffoldBridgingTelly : ScaffoldBridging("Telly", "Sneak") {

    val doNotAim: Boolean
        get() = player.airTicks <= straightTicks &&
                ticksUntilJump >= jumpTicks &&
                !(ModuleScaffold.isTowering && aimOnTower)

    val isTellyBridging: Boolean
        get() = ticksUntilJump >= jumpTicks && player.moving && running

    private var ticksUntilJump = 0

    val resetMode by enumChoice("ResetMode", ResetMode.RESET)
    private val straightTicks by int("Straight", 0, 0..5, "ticks")
    private val jumpTicksOpt by intRange("Jump", 0..0, 0..10, "ticks")
    private val aimOnTower by boolean("AimOnTower", true)
    private var jumpTicks = jumpTicksOpt.random()

    @Suppress("unused")
    private val gameHandler = handler<GameTickEvent> {
        if (player.onGround()) {
            ticksUntilJump++
        }
    }

    @Suppress("unused")
    private val movementInputHandler = handler<MovementInputEvent> { event ->
        if (!player.moving || ModuleScaffold.blockCount <= 0 || !player.onGround()) {
            return@handler
        }

        val isStraight = RotationManager.currentRotation == null || straightTicks == 0

        when (resetMode) {
            ResetMode.REVERSE -> event.jump = true
            ResetMode.RESET -> if (isStraight && ticksUntilJump >= jumpTicks) event.jump = true
        }
    }

    @Suppress("unused")
    private val afterJumpHandler = handler<PlayerAfterJumpEvent> {
        ticksUntilJump = 0
        jumpTicks = jumpTicksOpt.random()
    }

    enum class ResetMode(override val tag: String) : Tagged {
        REVERSE("Reverse"),
        RESET("Reset")
    }

}
