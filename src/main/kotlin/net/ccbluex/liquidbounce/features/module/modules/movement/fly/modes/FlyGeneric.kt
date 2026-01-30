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

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.events.BlockShapeEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.math.withLength
import net.minecraft.network.protocol.game.ClientboundExplodePacket
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.level.block.LiquidBlock
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import kotlin.jvm.optionals.getOrNull

internal object FlyVanilla : Mode("Vanilla") {

    private val glide by float("Glide", 0.0f, -1f..1f)

    private val bypassVanillaCheck by boolean("BypassVanillaCheck", true)

    object BaseSpeed : ValueGroup("BaseSpeed") {
        val horizontalSpeed by float("Horizontal", 0.44f, 0.1f..10f)
        val verticalSpeed by float("Vertical", 0.44f, 0.1f..10f)
    }

    object SprintSpeed : ToggleableValueGroup(this, "SprintSpeed", true) {
        val horizontalSpeed by float("Horizontal", 1f, 0.1f..10f)
        val verticalSpeed by float("Vertical", 1f, 0.1f..10f)
    }

    init {
        tree(BaseSpeed)
        tree(SprintSpeed)
    }

    override val parent: ModeValueGroup<*>
        get() = ModuleFly.modes

    @Suppress("unused")
    private val tickHandler = tickHandler {
        val useSprintSpeed = mc.options.keySprint.isDown && SprintSpeed.enabled
        val hSpeed =
            if (useSprintSpeed) SprintSpeed.horizontalSpeed else BaseSpeed.horizontalSpeed
        val vSpeed =
            if (useSprintSpeed) SprintSpeed.verticalSpeed else BaseSpeed.verticalSpeed

        player.deltaMovement = player.deltaMovement.withStrafe(speed = hSpeed.toDouble())
        player.deltaMovement.y = when {
            mc.options.keyJump.isDown -> vSpeed.toDouble()
            mc.options.keyShift.isDown -> (-vSpeed).toDouble()
            else -> glide.toDouble()
        }

        // Most basic bypass for vanilla fly check
        // This can also be done via packets, but this is easier.
        if (bypassVanillaCheck && player.tickCount % 40 == 0) {
            waitTicks(1)
            player.deltaMovement.y = -0.04
            waitTicks(1)
        }
    }

}

internal object FlyCreative : Mode("Creative") {

    override val parent: ModeValueGroup<*>
        get() = ModuleFly.modes

    private val speed by float("Speed", 0.1f, 0.1f..5f)

    private object SprintSpeed : ToggleableValueGroup(this, "SprintSpeed", true) {
        val speed by float("Speed", 0.1f, 0.1f..5f)
    }

    init {
        tree(SprintSpeed)
    }

    private val maxVelocity by float("MaxVelocity", 4f, 1f..20f)

    private val bypassVanillaCheck by boolean("BypassVanillaCheck", true)

    private val forceFlight by boolean("ForceFlight", true)

    override fun enable() {
        player.abilities.mayfly = true
    }

    private fun shouldFlyDown(): Boolean {
        if (!bypassVanillaCheck) return false
        if (player.tickCount % 40 != 0) return false

        // check if the player is above a block or in midair
        // if the player is right above a block, we don't need to fly down
        if (world.getBlockStates(player.boundingBox.move(0.0, -0.55, 0.0)).anyMatch { !it.isAir }) return false

        return true
    }

    val repeatable = tickHandler {
        player.abilities.flyingSpeed =
            if (mc.options.keySprint.isDown && SprintSpeed.enabled) SprintSpeed.speed else speed

        if (forceFlight) player.abilities.flying = true

        if (player.deltaMovement.lengthSqr() > maxVelocity.sq()) {
            player.deltaMovement = player.deltaMovement.withLength(maxVelocity.toDouble())
        }

        if (shouldFlyDown()) {
            network.send(MovePacketType.POSITION_AND_ON_GROUND.generatePacket())
        }

    }

    val packetHandler = handler<PacketEvent> { event ->
        if (shouldFlyDown() && event.packet is ServerboundMovePlayerPacket) {
            event.packet.y = player.yLast - 0.04
        }
    }

    override fun disable() {
        player.abilities.mayfly = false
        player.abilities.flying = false
    }

}

internal object FlyAirWalk : Mode("AirWalk") {

    override val parent: ModeValueGroup<*>
        get() = ModuleFly.modes

    val onGround by boolean("OnGround", true)

    val packetHandler = handler<PacketEvent> { event ->
        if (event.packet is ServerboundMovePlayerPacket) {
            event.packet.onGround = onGround
        }
    }

    @Suppress("unused")
    val shapeHandler = handler<BlockShapeEvent> { event ->
        if (event.state.block !is LiquidBlock && event.pos.y < player.y) {
            event.shape = Shapes.block()
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
internal object FlyExplosion : Mode("Explosion") {

    override val parent: ModeValueGroup<*>
        get() = ModuleFly.modes

    val vertical by float("Vertical", 4f, 0f..10f)
    val startStrafe by float("StartStrafe", 1f, 0.6f..4f)
    val strafeDecrease by float("StrafeDecrease", 0.005f, 0.001f..0.1f)

    private var strafeSince = 0.0f

    override fun enable() {
        chat("You need to be damaged by an explosion to fly.")
        super.enable()
    }

    val repeatable = tickHandler {
        if (strafeSince > 0) {
            if (!player.onGround()) {
                player.deltaMovement = player.deltaMovement.withStrafe(speed = strafeSince.toDouble())
                strafeSince -= strafeDecrease
            } else {
                strafeSince = 0f
            }
        }
    }

    val packetHandler = sequenceHandler<PacketEvent> { event ->
        val packet = event.packet

        // Check if this is a regular velocity update
        if (packet is ClientboundSetEntityMotionPacket && packet.id == player.id) {
            // Modify packet according to the specified values
            packet.movement.x = 0.0
            packet.movement.y = packet.movement.y * vertical
            packet.movement.z = 0.0

            waitTicks(1)
            strafeSince = startStrafe
        } else if (packet is ClientboundExplodePacket) { // Check if explosion affects velocity
            packet.playerKnockback.getOrNull()?.let { knockback ->
                knockback.x = 0.0
                knockback.y *= vertical
                knockback.z = 0.0

                waitTicks(1)
                strafeSince = startStrafe
            }
        }
    }

}

internal object FlyJetpack : Mode("Jetpack") {

    override val parent: ModeValueGroup<*>
        get() = ModuleFly.modes

    val repeatable = handler<GameTickEvent> {
        if (player.input.keyPresses.jump) {
            val deltaMovement = player.deltaMovement
            player.deltaMovement = Vec3(
                deltaMovement.x * 1.1,
                deltaMovement.y + 0.15,
                deltaMovement.z * 1.1,
            )
        }
    }

}
