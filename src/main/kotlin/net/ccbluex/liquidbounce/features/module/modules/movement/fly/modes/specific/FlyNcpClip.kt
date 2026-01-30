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

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.specific

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.BlinkPacketEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.features.blink.BlinkManager
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes

/**
 * NCP Clip Fly
 * Allows you to fly on BlocksMC.
 *
 * In order to bypass the second anti-cheat, it is required to enable PingSpoof,
 * which disables certain anti-cheat checks.
 *
 * The concept behind this fly is taken from CrossSine, made by shxp3, which is a fork of LiquidBounce Legacy
 * The code however is not copied as it follows a different approach.
 *
 * @author 1zuna <marco@ccbluex.net>
 */
object FlyNcpClip : Mode("NcpClip") {

    private val speed by float("Speed", 7.5f, 2f..10f)
    private val additionalEntrySpeed by float("AdditionalEntry", 2f, 0f..2f)
    private val timer by float("Timer", 0.4f, 0.1f..1f)
    private val strafe by boolean("Strafe", true)

    private val clipping by float("Clipping", -0.5f, -1.0f..1.0f)
    private val blink by boolean("Blink", false)
    private val fallDamage by boolean("FallDamage", false)

    private val maximumDistance by float("MaximumDistance", 200f, 0.1f..500f)

    override val parent: ModeValueGroup<*>
        get() = ModuleFly.modes

    private var startPosition: Vec3? = null
    private var damage = false

    private var shouldLag = false

    @Suppress("unused")
    val tickHandler = tickHandler {
        val startPos = startPosition

        // If fall damage is required, wait for damage
        if (fallDamage) {
            tickUntil { damage }
        }

        if (startPos == null) {
            startPosition = player.position()

            // Wait until there is a vertical collision
            tickUntil { collidesVertical() }

            if (clipping != 0f) {
                network.send(
                    ServerboundMovePlayerPacket.Pos(
                        player.x, player.y + clipping, player.z,
                        false, player.horizontalCollision
                    )
                )
                network.send(
                    ServerboundMovePlayerPacket.Pos(
                        player.x, player.y, player.z,
                        false, player.horizontalCollision
                    )
                )
            }

            if (blink) {
                shouldLag = true
            }

            // Wait until there is no vertical collision
            tickUntil { !collidesVertical() }

            // Proceed to jump (just like speeding up) and boost strafe entry
            player.jumpFromGround()
            player.deltaMovement = player.deltaMovement.withStrafe(speed = (speed + additionalEntrySpeed).toDouble())

            // Wait until the player is in air
            tickUntil { !player.onGround() }

            // Proceed to strafe with the normal speed
            player.deltaMovement = player.deltaMovement.withStrafe(speed = speed.toDouble())
        } else if (collidesBottomVertical()) {
            shouldLag = false

            // Disable the module when the player touches ground
            ModuleFly.enabled = false
            return@tickHandler
        } else if (startPos.distanceTo(player.position()) > maximumDistance) {
            if (shouldLag) {
                // If we are lagging, we can abuse this to get us back to safety
                BlinkManager.cancel()
                shouldLag = false
            }

            // Disable the module
            ModuleFly.enabled = false

            notification("Fly", "You have exceeded the maximum distance.",
                NotificationEvent.Severity.ERROR)
            return@tickHandler
        }

        // Strafe the player to improve control
        if (strafe) {
            player.deltaMovement = player.deltaMovement.withStrafe()
        }

        // Set timer speed
        Timer.requestTimerSpeed(timer, Priority.IMPORTANT_FOR_USAGE_1, ModuleFly)
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> {
        val packet = it.packet
        // 3.5 is technically the minimum, 5 is consistent and doesn't flag for nofall
        // Falling from 5 blocks deals 3hp damage.
        if (packet is ServerboundMovePlayerPacket && player.fallDistance > 5) {
            if (!damage && fallDamage) {
                /**
                 * Alright, we are able to take fall damage.
                 * NCP calculates fall damage differently,
                 * this seems as the only proper way to
                 * take damage out of nowhere.
                 *
                 * It's called ncp setbacks!
                 */

                // Adding 1 to y because it flags consistently
                packet.y += 1

                // Reset fallDistance so this same logic
                // doesn't get called multiple times
                player.fallDistance = 0.0
            }

        }

        if (packet is ClientboundDamageEventPacket && packet.entityId == player.id) {
            damage = true
        }
    }

    @Suppress("unused")
    private val fakeLagHandler = handler<BlinkPacketEvent> { event ->
        if (blink && shouldLag && event.origin == TransferOrigin.OUTGOING) {
            event.action = BlinkManager.Action.QUEUE
        }
    }

    override fun disable() {
        startPosition = null
        damage = false
        shouldLag = false

        // Cancel the motion
        player.setDeltaMovement(0.0, player.deltaMovement.y, 0.0)
        super.disable()
    }

    /**
     * Check if there is a vertical collision possible above the player
     */
    private fun collidesVertical() =
        world.getBlockCollisions(player, player.boundingBox.move(0.0, 0.5, 0.0)).any { shape ->
            shape != Shapes.empty()
        }

    private fun collidesBottomVertical() =
        world.getBlockCollisions(player, player.boundingBox.move(0.0, -0.4, 0.0)).any { shape ->
            shape != Shapes.empty()
        }

}
