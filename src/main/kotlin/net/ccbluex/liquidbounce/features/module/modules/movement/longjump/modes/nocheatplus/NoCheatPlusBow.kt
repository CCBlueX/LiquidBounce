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

package net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes.nocheatplus

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.KeybindIsPressedEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.ModuleLongJump
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket

/**
 * @anticheat NoCheatPlus
 * @anticheatVersion 3.16.1-SNAPSHOT-sMD5NET-b115s
 * @testedOn eu.loyisa.cn
 */

internal object NoCheatPlusBow : Choice("NoCheatPlusBow") {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleLongJump.mode

    private var arrowBoost = 0f
    private var shotArrows = 0f

    val rotations = tree(RotationsConfigurable(this))
    private val charged by int("Charged", 4, 3..20)
    val speed by float("Speed", 2.5f, 0f..20f)
    private val arrowsToShoot by int("ArrowsToShoot", 8, 0..20)
    val fallDistance by float("FallDistanceToJump", 0.42f, 0f..2f)

    private var stopMovement = false
    private var forceUseKey = false

    val movementInputHandler = handler<MovementInputEvent> {
        if (stopMovement) {
            it.directionalInput = DirectionalInput.NONE
            stopMovement = false
        }
    }

    @Suppress("unused")
    private val keyBindIsPressedHandler = handler<KeybindIsPressedEvent> { event ->
        if (event.keyBinding == mc.options.useKey && forceUseKey) {
            event.isPressed = true
        }
    }

    @Suppress("unused")
    private val tickJumpHandler = tickHandler {
        if (arrowBoost <= arrowsToShoot) {
            forceUseKey = true
            RotationManager.setRotationTarget(
                Rotation(player.yaw, -90f),
                configurable = rotations,
                priority = Priority.IMPORTANT_FOR_USAGE_2,
                provider = ModuleLongJump
            )

            // Stops moving
            stopMovement = true

            // Shoots arrow
            if (player.itemUseTime >= charged) {
                interaction.stopUsingItem(player)
                shotArrows++
            }
        } else {
            forceUseKey = false
            if (player.isUsingItem) {
                interaction.stopUsingItem(player)
            }

            shotArrows = 0f
            waitTicks(5)
            player.jump()
            player.velocity = player.velocity.withStrafe(speed = speed.toDouble())
            waitTicks(5)
            arrowBoost = 0f
        }
    }

    // what, why two events here?
    @Suppress("unused")
    private val handleMovementInput = handler<MovementInputEvent> {
        if (arrowBoost <= arrowsToShoot) {
            return@handler
        }

        if (player.fallDistance >= fallDistance) {
            it.jump = true
            player.fallDistance = 0f
        }
    }

    @Suppress("unused")
    private val velocityHandler = handler<PacketEvent> {
        val packet = it.packet

        if (packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id && shotArrows > 0.0) {
            shotArrows--
            arrowBoost++
        }
    }

    override fun disable() {
        shotArrows = 0.0f
        arrowBoost = 0.0f
    }

}
