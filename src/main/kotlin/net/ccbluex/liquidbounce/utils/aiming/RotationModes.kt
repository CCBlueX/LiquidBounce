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
package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.waitMatches
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.data.Surface
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket

sealed class RotationMode(
    name: String,
    private val modeValueGroup: ModeValueGroup<RotationMode>,
    val eventListener: EventListener,
) : Mode(name), MinecraftShortcuts {

    /**
     * Already sends the packet on post-move.
     * This might get us a little advantage because the packets are added a little bit earlier to the server tick queue.
     *
     * The downside is that it is not legit and will flag post-rotation checks on some anti-cheats.
     */
    val postMove by boolean("PostMove", false)

    /**
     * Instantly sends the action if possible.
     * This does not account for packet order and might flag on some anti-cheats.
     *
     * PostMove might be irrelevant if this is enabled.
     */
    val instant by boolean("Instant", false)

    /**
     * Turns to the rotation [plan] gives on each tick, aimed at [surface], and suspends until the action may go out.
     * Returns what [hit] makes of the rotation it goes out with, or null when this mode acts without one that hits.
     */
    abstract suspend fun <T : Any> look(plan: () -> Rotation, surface: Surface?, hit: (Rotation) -> T?): T?

    /**
     * Waits for the movement packet when [postMove] is set and nothing else rotates, else for the next tick.
     */
    protected suspend fun afterMovementPacket() {
        if (postMove) {
            eventListener.waitMatches<PlayerNetworkMovementTickEvent>(FIRST_PRIORITY) { it.state == EventState.POST }
            if (RotationManager.currentRotation == null) {
                return
            }
        }

        waitTicks(1)
    }

    override val parent: ModeValueGroup<*>
        get() = modeValueGroup

}

class NormalRotationMode(
    modeValueGroup: ModeValueGroup<RotationMode>,
    eventListener: EventListener,
    val priority: Priority = Priority.IMPORTANT_FOR_USAGE_2,

    // some modules might want to aim even tho it was instantly executed because the player's rotation should not
    // snap back as the same rotation might be needed for the next action
    private val aimAfterInstantAction: Boolean = false
) : RotationMode("Normal", modeValueGroup, eventListener) {

    val rotations = tree(RotationsValueGroup(this))
    val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)

    override suspend fun <T : Any> look(plan: () -> Rotation, surface: Surface?, hit: (Rotation) -> T?): T? {
        fun target() = rotations.toRotationTarget(plan(), considerInventory = !ignoreOpenInventory, surface = surface)

        // Sent at once, so the server judges it with the rotation it already has
        if (instant) {
            hit(RotationManager.serverRotation)?.let { found ->
                if (aimAfterInstantAction) {
                    RotationManager.setRotationTarget(target(), priority, eventListener)
                }
                return found
            }
        }

        val found = eventListener.lookAt(priority, ::target) { going -> hit(going) }
        if (postMove) {
            eventListener.waitMatches<PlayerNetworkMovementTickEvent>(FIRST_PRIORITY) { it.state == EventState.POST }
        }
        return found
    }

}

class NoRotationMode(modeValueGroup: ModeValueGroup<RotationMode>, eventListener: EventListener)
    : RotationMode("None", modeValueGroup, eventListener) {

    val send by boolean("SendRotationPacket", false)

    override suspend fun <T : Any> look(plan: () -> Rotation, surface: Surface?, hit: (Rotation) -> T?): T? {
        if (!instant) {
            afterMovementPacket()
        }

        val rotation = plan()
        if (send) {
            val fixedRotation = rotation.normalize()
            network.send(
                ServerboundMovePlayerPacket.Rot(
                    fixedRotation.yaw, fixedRotation.pitch, player.onGround(),
                    player.horizontalCollision
                )
            )
        }

        return hit(rotation)
    }

}

/**
 * Never rotates. The action only goes out when [look]'s hit holds for the rotation the player already has, so the
 * player's own crosshair decides what gets hit.
 */
class CrosshairRotationMode(modeValueGroup: ModeValueGroup<RotationMode>, eventListener: EventListener)
    : RotationMode("Crosshair", modeValueGroup, eventListener) {

    override suspend fun <T : Any> look(plan: () -> Rotation, surface: Surface?, hit: (Rotation) -> T?): T? {
        if (!instant) {
            afterMovementPacket()
        }

        return hit(player.rotation)
    }

}
