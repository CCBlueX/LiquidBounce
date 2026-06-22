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

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.vulcan

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly.modes
import net.ccbluex.liquidbounce.utils.math.copy
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos
import net.minecraft.world.phys.Vec3

/**
 * @anticheat Vulcan
 * @anticheat Version 2.8.6
 * @testedOn eu.loyisa.cn, anticheat-test.com
 * @note Few seconds cooldown to not flag. Requires 1.8 serverside
 * @author Nullable
 */
internal object FlyVulcan286Teleport : Mode("Vulcan286-Teleport-18") {

    override val parent: ModeValueGroup<*>
        get() = modes

    private var jumping = false
    private var flagged = false

    override fun disable() {
        jumping = false
        flagged = false
    }

    /**
     * Alright, lets fly...
     * Fall damage is based on fall distance. By spoofing the ground
     * state to be false and jumping 3 times, you gain a fall distance
     * of 3 which is enough to take damage.
     *
     * After taking damage, vulcan gives leniency to all sorts of stuff like
     * motion, and teleporting.
     */
    val repeatable = tickHandler {
        jumping = true

        // TODO: remove assumption of getting a falldistance of 3
        // Only works if there isnt a block above head...
        repeat(3) {
            player.jumpFromGround()
            // Ugly code, yes I know
            // If this wasn't like this, it would trigger at the same tick...
            tickUntil { !player.onGround() }
            tickUntil { player.onGround() }
        }

        jumping = false
        tickUntil { player.hurtTime > 0 }

        // Flag to disable more checks...
        network.send(
            Pos(
            player.x,
            player.y - 0.1,
            player.z,
            player.onGround(),
            player.horizontalCollision
        ))

        tickUntil { flagged }

        // Cool, we took damage so lets fly
        val vector = Vec3.directionFromRotation(0F, player.yRot).normalize()
        // After 3 times vulcan flags us. 3 is the max
        repeat(3) {
            // 10 Blocks per teleport...
            // Used 9 because stable...
            // Otherwise, last teleport would flag since player also moves a bit
            player.setPos(player.x + vector.x * 9, player.y, player.z + vector.z * 9)
            network.send(
                Pos(
                player.x,
                player.y,
                player.z,
                player.onGround(),
                player.horizontalCollision
            ))
        }

        ModuleFly.enabled = false
    }


    // Let's not move around while jumping, that would make it harder.
    val moveHandler = handler<PlayerMoveEvent> { event ->
        if (jumping) {
            event.movement = event.movement.copy(x = 0.0, z = 0.0)
        }
    }

    val packetHandler = handler<PacketEvent> {
        val packet = it.packet
        if (packet is ServerboundMovePlayerPacket) {
            if (jumping) {
                // This allows us to do the jump "exploit"
                packet.onGround = false
            }
        }
        if (packet is ClientboundPlayerPositionPacket) {
            flagged = true
        }
    }

}

