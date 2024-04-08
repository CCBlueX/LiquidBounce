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

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.BlockShapeEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.minecraft.block.FluidBlock
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket
import net.minecraft.util.shape.VoxelShapes

internal object FlyVanilla : Choice("Vanilla") {

    val horizontalSpeed by float("Horizontal", 0.44f, 0.1f..5f)
    val verticalSpeed by float("Vertical", 0.44f, 0.1f..5f)

    val glide by float("Glide", 0.0f, -1f..1f)

    val bypassVanillaCheck by boolean("BypassVanillaCheck", true)

    override val parent: ChoiceConfigurable<*>
        get() = ModuleFly.modes

    val repeatable = repeatable {
        player.strafe(speed = horizontalSpeed.toDouble())
        player.velocity.y = when {
            player.input.jumping -> verticalSpeed.toDouble()
            player.input.sneaking -> (-verticalSpeed).toDouble()
            else -> glide.toDouble()
        }

        // Most basic bypass for vanilla fly check
        // This can also be done via packets, but this is easier.
        if (bypassVanillaCheck && player.age % 40 == 0) {
            waitTicks(1)
            player.velocity.y = -0.04
            waitTicks(1)
        }
    }

}

internal object FlyAirWalk : Choice("AirWalk") {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleFly.modes

    val onGround by boolean("OnGround", true)

    val packetHandler = handler<PacketEvent> { event ->
        if (event.packet is PlayerMoveC2SPacket) {
            event.packet.onGround = onGround
        }
    }

    @Suppress("unused")
    val shapeHandler = handler<BlockShapeEvent> { event ->
        if (event.state.block !is FluidBlock && event.pos.y < player.y) {
            event.shape = VoxelShapes.fullCube()
        }
    }

    @Suppress("unused")
    val jumpEvent = handler<PlayerJumpEvent> { event ->
        event.cancelEvent()
    }
}

/**
 * Explode yourself to fly
 * Takes any kind of damage, preferably explosion damage.
 * Might bypass some anti-cheats.
 */
internal object FlyExplosion : Choice("Explosion") {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleFly.modes

    val vertical by float("Vertical", 4f, 0f..10f)
    val startStrafe by float("StartStrafe", 1f, 0.6f..4f)
    val strafeDecrease by float("StrafeDecrease", 0.005f, 0.001f..0.1f)

    private var strafeSince = 0.0f

    override fun enable() {
        chat("You need to be damaged by an explosion to fly.")
        super.enable()
    }

    val repeatable = repeatable {
        if (strafeSince > 0) {
            if (!player.isOnGround) {
                player.strafe(speed = strafeSince.toDouble())
                strafeSince -= strafeDecrease
            } else {
                strafeSince = 0f
            }
        }
    }

    val packetHandler = sequenceHandler<PacketEvent> { event ->
        val packet = event.packet

        // Check if this is a regular velocity update
        if (packet is EntityVelocityUpdateS2CPacket && packet.id == player.id) {
            // Modify packet according to the specified values
            packet.velocityX = 0
            packet.velocityY = (packet.velocityY * vertical).toInt()
            packet.velocityZ = 0

            waitTicks(1)
            strafeSince = startStrafe
        } else if (packet is ExplosionS2CPacket) { // Check if explosion affects velocity
            packet.playerVelocityX = 0f
            packet.playerVelocityY *= vertical
            packet.playerVelocityZ = 0f

            waitTicks(1)
            strafeSince = startStrafe
        }
    }

}

internal object FlyJetpack : Choice("Jetpack") {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleFly.modes

    val repeatable = repeatable {
        if (player.input.jumping) {
            player.velocity.x *= 1.1
            player.velocity.y += 0.15
            player.velocity.z *= 1.1
        }
    }

}
