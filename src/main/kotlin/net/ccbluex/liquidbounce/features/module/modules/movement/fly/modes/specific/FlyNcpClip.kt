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

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.specific

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.QueuePacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket
import net.minecraft.util.math.Vec3d
import net.minecraft.util.shape.VoxelShapes

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
object FlyNcpClip : Choice("NcpClip") {

    private val speed by float("Speed", 7.5f, 2f..10f)
    private val additionalEntrySpeed by float("AdditionalEntry", 2f, 0f..2f)
    private val timer by float("Timer", 0.4f, 0.1f..1f)
    private val strafe by boolean("Strafe", true)

    private val clipping by float("Clipping", -0.5f, -1.0f..1.0f)
    private val blink by boolean("Blink", false)
    private val fallDamage by boolean("FallDamage", false)

    private val maximumDistance by float("MaximumDistance", 200f, 0.1f..500f)

    override val parent: ChoiceConfigurable<*>
        get() = ModuleFly.modes

    private var startPosition: Vec3d? = null
    private var damage = false

    private var shouldLag = false

    @Suppress("unused")
    val tickHandler = tickHandler {
        val startPos = startPosition

        // If fall damage is required, wait for damage to be true
        if (fallDamage) {
            waitUntil { damage }
        }

        if (startPos == null) {
            startPosition = player.pos

            // Wait until there is a vertical collision
            waitUntil { collidesVertical() }

            if (clipping != 0f) {
                network.sendPacket(
                    PlayerMoveC2SPacket.PositionAndOnGround(
                        player.x, player.y + clipping, player.z,
                        false, player.horizontalCollision
                    )
                )
                network.sendPacket(
                    PlayerMoveC2SPacket.PositionAndOnGround(
                        player.x, player.y, player.z,
                        false, player.horizontalCollision
                    )
                )
            }

            if (blink) {
                shouldLag = true
            }

            // Wait until there is no vertical collision
            waitUntil { !collidesVertical() }

            // Proceed to jump (just like speeding up) and boost strafe entry
            player.jump()
            player.velocity = player.velocity.withStrafe(speed = (speed + additionalEntrySpeed).toDouble())

            // Wait until the player is not on ground
            waitUntil { !player.isOnGround }

            // Proceed to strafe with the normal speed
            player.velocity = player.velocity.withStrafe(speed = speed.toDouble())
        } else if (collidesBottomVertical()) {
            shouldLag = false

            // Disable the module if the player is on ground again
            ModuleFly.enabled = false
            return@tickHandler
        } else if (startPos.distanceTo(player.pos) > maximumDistance) {
            if (shouldLag) {
                // If we are lagging, we might abuse this to get us back to safety
                PacketQueueManager.cancel()
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
            player.velocity = player.velocity.withStrafe()
        }

        // Set timer speed
        Timer.requestTimerSpeed(timer, Priority.IMPORTANT_FOR_USAGE_1, ModuleFly)
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> {
        val packet = it.packet
        // 3.5 is the minimum, 5 doesn't flag for nofall
        // Should be a float setting but no easy way to
        // make settings hidden with booleans
        //
        // Falling from 5 blocks deals 3hp damage.
        if (packet is PlayerMoveC2SPacket && player.fallDistance > 5) {
            if (!damage && fallDamage) {
                /**
                 * Alright, we are able to take fall damge.
                 * NCP calculates fall damage differently,
                 * this seems as the only proper way to
                 * take damage out of nowhere.
                 *
                 * It's called ncp setbacks!
                 */

                // Adding 1 to y because it's consistent and easy.
                packet.y += 1

                // Requires falldistance = 0 otherwise
                // we would try to float..
                player.fallDistance = 0.0f
            }

        }

        if (packet is EntityDamageS2CPacket && packet.entityId == player.id) {
            damage = true
        }
    }

    @Suppress("unused")
    private val fakeLagHandler = handler<QueuePacketEvent> { event ->
        if (blink && shouldLag && event.origin == TransferOrigin.OUTGOING) {
            event.action = PacketQueueManager.Action.QUEUE
        }
    }

    override fun disable() {
        startPosition = null
        damage = false
        shouldLag = false

        // Cancel the motion
        player.setVelocity(0.0, player.velocity.y, 0.0)
        super.disable()
    }

    /**
     * Check if there is a vertical collision possible above the player
     */
    private fun collidesVertical() =
        world.getBlockCollisions(player, player.boundingBox.offset(0.0, 0.5, 0.0)).any { shape ->
            shape != VoxelShapes.empty()
        }

    private fun collidesBottomVertical() =
        world.getBlockCollisions(player, player.boundingBox.offset(0.0, -0.4, 0.0)).any { shape ->
            shape != VoxelShapes.empty()
        }

}
