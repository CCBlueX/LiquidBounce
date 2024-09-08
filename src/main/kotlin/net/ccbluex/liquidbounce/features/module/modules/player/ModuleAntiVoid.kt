/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015-2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.fakelag.FakeLag
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.register.IncludeModule
import net.ccbluex.liquidbounce.utils.block.canStandOn
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket

/**
 * AntiVoid module protects the player from falling into the void by simulating
 * future movements and taking action if necessary.
 */
@IncludeModule
object ModuleAntiVoid : Module("AntiVoid", Category.PLAYER) {

    // The height at which the void is deemed to begin.
    val voidThreshold by int("VoidLevel", 0, -256..0)
    val velocityTimeout by boolean("VelocityTimeout", true)

    // Flags indicating if an action has been already taken or needs to be taken.
    private var actionAlreadyTaken = false
    private var needsAction = false

    private var velocityTimed = false

    // Cases in which the AntiVoid protection should not be active.
    private val isExempt
        get() = player.isDead || ModuleFly.enabled || ModuleScaffold.enabled

    // Whether artificial lag is needed to prevent falling into the void.
    val needsArtificialLag
        get() = enabled && needsAction && !actionAlreadyTaken && !isExempt

    // How many future ticks to simulate to ensure safety.
    private const val SAFE_TICKS_THRESHOLD = 10

    override fun disable() {
        actionAlreadyTaken = false
        needsAction = false
        velocityTimed = false
        super.disable()
    }

    /**
     * Handles movement input by simulating future movements of a player to detect potential falling into the void.
     */
    @Suppress("unused")
    val movementInputHandler = handler<MovementInputEvent> {
        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(
            SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(it.directionalInput)
        )

        // Analyzes if the player might be falling into the void soon.
        needsAction = isLikelyFalling(simulatedPlayer)
    }

    val packetHandler = sequenceHandler<PacketEvent> {
        val packet = it.packet

        if (packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id || packet is ExplosionS2CPacket) {
            if (velocityTimed || !velocityTimeout) {
                return@sequenceHandler
            }

            velocityTimed = true
            waitTicks(2)
            waitUntil { player.isOnGround }
            velocityTimed = false
        }
    }

    /**
     * Simulates a player's future movement to determine if falling into the void is likely.
     * @param simulatedPlayer The simulated player instance.
     * @return True if a simulated fall into the void is likely.
     */
    private fun isLikelyFalling(simulatedPlayer: SimulatedPlayer): Boolean {
        var ticksPassed = 0
        repeat(SAFE_TICKS_THRESHOLD) {
            simulatedPlayer.tick()
            ticksPassed++

            if (simulatedPlayer.fallDistance > 0 && !simulatedPlayer.pos.toBlockPos().down().canStandOn()) {
                val distanceToVoid = simulatedPlayer.pos.y - voidThreshold
                ModuleDebug.debugParameter(this, "DistanceToVoid", distanceToVoid)
                val ticksToVoid = (distanceToVoid * 1.4 / 0.98).toInt()
                ModuleDebug.debugParameter(this, "TicksToVoid", ticksToVoid)
                // Simulate additional ticks to project further movement.
                // TODO: Fix considering the player's velocity horizontally
                //   because FallingPlayer did not work as expected and was not very
                //   consistent, since even slight rotation changes would cause
                //   the collision check to fail and return impossible results.
                repeat(ticksToVoid) {
                    // 1 s is enough to stop touching keyboard
                    if (ticksPassed >= 20) {
                        simulatedPlayer.input = SimulatedPlayer.SimulatedPlayerInput(
                            DirectionalInput.NONE,
                            jumping = false,
                            sprinting = false,
                            sneaking = false
                        )
                    }
                    simulatedPlayer.tick()
                    ticksPassed++
                }

                return simulatedPlayer.pos.y < voidThreshold
            }
        }

        return false
    }

    /**
     * Executes periodically to check if an anti-void action is required, and triggers it if necessary.
     */
    @Suppress("unused")
    val antiVoidListener = repeatable {
        if (isExempt) {
            return@repeatable
        }

        if (player.fallDistance > 0.5 && needsAction) {
            if (actionAlreadyTaken) {
                return@repeatable
            }

            val simulatedFallingPlayer = FallingPlayer.fromPlayer(player)

            // If no collision is detected within a threshold beyond which falling
            // into void is likely, take the necessary action.
            if (simulatedFallingPlayer.findCollision(500) == null) {
                FakeLag.cancel()
                notification(
                    "AntiVoid", "Action taken to prevent void fall", NotificationEvent.Severity.INFO
                )
                actionAlreadyTaken = true
            }
        } else {
            actionAlreadyTaken = false
        }
    }
}
