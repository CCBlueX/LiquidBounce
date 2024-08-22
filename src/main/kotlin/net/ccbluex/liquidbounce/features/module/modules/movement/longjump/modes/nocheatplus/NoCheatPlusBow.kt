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

package net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes.nocheatplus

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.ModuleLongJump
import net.ccbluex.liquidbounce.utils.aiming.data.Orientation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationEngine
import net.ccbluex.liquidbounce.utils.aiming.tracking.RotationTracker
import net.ccbluex.liquidbounce.utils.entity.strafe
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

    var arrowBoost = 0f
    var shotArrows = 0f

    val rotationEngine = tree(RotationEngine(this))
    val charged by int("Charged", 4, 3..20)
    val speed by float("Speed", 2.5f, 0f..20f)
    val arrowsToShoot by int("ArrowsToShoot", 8, 0..20)
    val fallDistance by float("FallDistanceToJump", 0.42f, 0f..2f)

    var stopMovement = false

    val movementInputHandler = handler<MovementInputEvent> {
        if (stopMovement) {
            it.directionalInput = DirectionalInput.NONE
            stopMovement = false
        }
    }

    val tickJumpHandler = repeatable {
        if (arrowBoost <= arrowsToShoot) {
            mc.options.useKey.isPressed = true
            RotationManager.aimAt(
                // todo: implement unhooking player yaw and only spoof pitch
                RotationTracker.withFixedAngle(rotationEngine, Orientation(player.yaw, 90f)),
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
            mc.options.useKey.isPressed = false
            if (player.isUsingItem) {
                interaction.stopUsingItem(player)
            }

            shotArrows = 0f
            waitTicks(5)
            player.jump()
            player.strafe(speed = speed.toDouble())
            waitTicks(5)
            arrowBoost = 0f
        }
    }

    // what, why two events here?
    val handleMovementInput = handler<MovementInputEvent> {
        if (arrowBoost <= arrowsToShoot) {
            return@handler
        }

        if (player.fallDistance >= fallDistance) {
            it.jumping = true
            player.fallDistance = 0f
        }
    }

    val velocityHandler = handler<PacketEvent> {
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
