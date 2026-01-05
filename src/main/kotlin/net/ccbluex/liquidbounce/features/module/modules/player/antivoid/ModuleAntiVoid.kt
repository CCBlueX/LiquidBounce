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
 *
 */
package net.ccbluex.liquidbounce.features.module.modules.player.antivoid

import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.mode.AntiVoidBlinkMode
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.mode.AntiVoidFlagMode
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.mode.AntiVoidGhostBlockMode
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer.SimulatedPlayerInput
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.movement.getDirectionalInputForDegrees
import kotlin.math.atan2

/**
 * AntiVoid module protects the player from falling into the void by simulating
 * future movements and taking action if necessary.
 */
object ModuleAntiVoid : ClientModule("AntiVoid", Category.PLAYER) {

    val mode = choices(
        "Mode", AntiVoidGhostBlockMode, arrayOf(
            AntiVoidGhostBlockMode,
            AntiVoidFlagMode,
            AntiVoidBlinkMode
        )
    )

    private val maxUnsafeTime by int("MaxUnsafeTime", 500, 0..10_000)

    val unsafeTime = Chronometer()

    @Volatile
    var canSaveYourself = false
        private set

    // How many future ticks to simulate to ensure safety.
    private const val SAFE_TICKS_THRESHOLD = 40

    override fun onEnabled() {
        canSaveYourself = false
        unsafeTime.reset()
        super.onDisabled()
    }

    /**
     * Executes periodically to check if an anti-void action is required, and triggers it if necessary.
     */
    @Suppress("unused")
    private val voidHandler = tickHandler(priority = EventPriorityConvention.FINAL_DECISION) {
        val canSaveYourself = (inputToSavePlayer() != null).also {
            this@ModuleAntiVoid.canSaveYourself = it
        }

        if (canSaveYourself) {
            unsafeTime.reset()
            return@tickHandler
        }

        if (unsafeTime.hasElapsed(maxUnsafeTime.toLong())) {
            if (mode.activeChoice.rescue()) {
                notification(
                    "AntiVoid", "Action taken to prevent void fall",
                    NotificationEvent.Severity.INFO
                )
                unsafeTime.reset()
            }
        }
    }

    fun inputToSavePlayer(): SimulatedPlayerInput? {
        val zeroMoveInput = SimulatedPlayerInput(
            directionalInput = DirectionalInput.NONE,
            jumping = false,
            sprinting = false,
            sneaking = true,
        )
        if (zeroMoveInput.isSafeInput()) {
            return zeroMoveInput
        }

        val continueMovement = SimulatedPlayerInput(
            directionalInput = DirectionalInput(player.input),
            jumping = player.jumping,
            sprinting = player.isSprinting,
            sneaking = player.crouching,
        )
        if (continueMovement.isSafeInput()) {
            return continueMovement
        }

        // we try to move in a way so that we reset our velocity
        val movementYawToCancelVelocity =
            atan2(-player.deltaMovement.x, player.deltaMovement.z).toFloat()
        val directionToCancelVelocity =
            getDirectionalInputForDegrees(DirectionalInput.NONE, movementYawToCancelVelocity)
        val cancelDeltaMovement = SimulatedPlayerInput(
            directionalInput = directionToCancelVelocity,
            jumping = true,
            sprinting = true,
            sneaking = false,
        )
        if (cancelDeltaMovement.isSafeInput()) {
            return cancelDeltaMovement
        }

        return null
    }

    private fun SimulatedPlayerInput.isSafeInput(): Boolean {
        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(
            pos = player.position(),
            fallDistance = player.fallDistance,
            velocity = player.deltaMovement,
            onGround = player.onGround(),
            input = this,
        )

        repeat(SAFE_TICKS_THRESHOLD) {
            simulatedPlayer.tick()
            // TODO: better way to detect if player is safe, for example we don't consider ladders safe
            if (simulatedPlayer.onGround) {
                return true
            }
        }
        return false
    }
}
