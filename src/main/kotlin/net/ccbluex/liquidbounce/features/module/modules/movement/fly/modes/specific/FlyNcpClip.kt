/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
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

    override val parent: ChoiceConfigurable
        get() = ModuleFly.modes

    private var needsClipping = true

    val repeatable = repeatable {
        if (needsClipping) {
            needsClipping = false

            // Wait until there is a vertical collision
            waitUntil { collidesVertical() }

            network.sendPacket(
                PlayerMoveC2SPacket.PositionAndOnGround(player.x, player.y - 0.05, player.z,
                    false))
            network.sendPacket(
                PlayerMoveC2SPacket.PositionAndOnGround(player.x, player.y, player.z,
                    false))

            // Wait until there is no vertical collision
            waitUntil { !collidesVertical() }

            // Proceed to jump (just like speeding up) and boost strafe entry
            player.jump()
            player.strafe(speed = (speed + additionalEntrySpeed).toDouble())

            // Wait until the player is not on ground
            waitUntil { !player.isOnGround }

            // Proceed to strafe with the normal speed
            player.strafe(speed = speed.toDouble())
        } else if (player.isOnGround) {
            // Disable the module if the player is on ground again
            ModuleFly.enabled = false
            return@repeatable
        }

        // Strafe the player to improve control
        player.strafe()
    }

    override fun disable() {
        needsClipping = true
        super.disable()
    }

    /**
     * Check if there is a vertical collision possible above the player
     */
    private fun collidesVertical() =
        world.getBlockCollisions(player, player.boundingBox.offset(0.0, 0.5, 0.0)).any { shape ->
            shape != VoxelShapes.empty()
        }

}
