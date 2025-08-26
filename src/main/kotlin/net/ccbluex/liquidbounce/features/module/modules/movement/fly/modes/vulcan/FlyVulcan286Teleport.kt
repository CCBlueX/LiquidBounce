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

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.vulcan

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly.modes
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.math.Vec3d

/**
 * @anticheat Vulcan
 * @anticheat Version 2.8.6
 * @testedOn eu.loyisa.cn, anticheat-test.com
 * @note Few seconds cooldown to not flag. DAMAGE METHOD ONLY 1.8
 * @author Nullable
 */
internal object FlyVulcan286Teleport : Choice("Vulcan286-Teleport-18") {

    override val parent: ChoiceConfigurable<*>
        get() = modes

    private var jumping = false
    private var flagged = false

    override fun disable() {
        jumping = false
        flagged = false
    }

    /**
     * Alright, lets fly...
     * Fall damage is based on fall distance and the fact that
     * you aren't on ground.
     *
     * By spoofing onground false and jumping 3 times,
     * you gain a falldistance of 3 which is enough to take damage.
     *
     * After damage, vulcan gives leniency to all sorts of stuff like
     * motion, and teleporting.
     */
    val repeatable = tickHandler {
        jumping = true

        repeat(3) {
            player.jump()
            // Ugly code, yes I know
            // If this wasn't like this, it would trigger at the same tick...
            waitUntil { !player.isOnGround }
            waitUntil { player.isOnGround }
        }

        jumping = false
        waitUntil { player.hurtTime > 0 }

        // Flag to disable some checks...
        network.sendPacket(PositionAndOnGround(
            player.x,
            player.y - 0.1,
            player.z,
            player.isOnGround,
            player.horizontalCollision
        ))

        waitUntil { flagged }

        // Cool, we took damage so lets fly
        val vector = Vec3d.fromPolar(0F, player.yaw).normalize()
        // After 3 times vulcan flags us. 3 is the max
        repeat(3) {
            // 10 Blocks per teleport...
            // Used 9 because stable...
            // Otherwise, last teleport would flag since player also moves a bit
            player.setPosition(player.x + vector.x * 9, player.y, player.z + vector.z * 9)
            network.sendPacket(PositionAndOnGround(
                player.x,
                player.y,
                player.z,
                player.isOnGround,
                player.horizontalCollision
            ))
        }

        ModuleFly.enabled = false
    }


    // Let's not move around while jumping, that would make it harder.
    val moveHandler = handler<PlayerMoveEvent> { event ->
        if (jumping) {
            event.movement.x = 0.0
            event.movement.z = 0.0
        }
    }

    val packetHandler = handler<PacketEvent> {
        val packet = it.packet
        if (packet is PlayerMoveC2SPacket) {
            if (jumping) {
                // This allows us to do the jump "exploit"
                packet.onGround = false
            }
        }
        if (packet is PlayerPositionLookS2CPacket) {
            flagged = true
        }
    }

}

