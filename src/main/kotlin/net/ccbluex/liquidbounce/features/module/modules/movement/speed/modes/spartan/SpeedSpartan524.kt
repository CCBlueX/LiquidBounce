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
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.spartan

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.airTicks
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.copy
import net.ccbluex.liquidbounce.utils.movement.stopXZVelocity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.Items


/**
 * @anticheat Spartan
 * @anticheatVersion v4.0.4.3
 * @testedOn minecraft.vagdedes.com
 * @note it will flag randomly, that's just spartan for you
 */
class SpeedSpartanV4043(override val parent: ModeValueGroup<*>) : Mode("Spartan-4.0.4.3") {

    @Suppress("unused")
    private val moveHandler = handler<PlayerMoveEvent> { event ->
        if (!player.input.keyPresses.forward) {
            return@handler
        }

        val wearingLeatherBoots = player.getItemBySlot(EquipmentSlot.FEET).`is`(Items.LEATHER_BOOTS)
        val horizontalMove = if (wearingLeatherBoots) 1.8 else 1.3

        if (player.onGround()) {
            event.movement = event.movement.copy(
                x = player.deltaMovement.x * horizontalMove,
                z = player.deltaMovement.z * horizontalMove,
            )

            repeat(4) {
                player.jumpFromGround()
            }
            event.movement.y = player.jumpPower.toDouble()
        }
    }
}

/**
 * @anticheat Spartan
 * @anticheatVersion v4.0.4.3
 * @testedOn minecraft.vagdedes.com
 * @note it will flag randomly, that's just spartan for you. Could flag anywhere from 0-20vl if you do 180's with it on
 */
class SpeedSpartanV4043FastFall(override val parent: ModeValueGroup<*>) : Mode("Spartan-4.0.4.3-FastFall") {

    override fun disable() {
        player.stopXZVelocity()
    }

    @Suppress("unused")
    private val moveHandler = handler<PlayerMoveEvent> { event ->
        if (!player.input.keyPresses.forward) {
            return@handler
        }

        val wearingLeatherBoots = player.getItemBySlot(EquipmentSlot.FEET).`is`(Items.LEATHER_BOOTS)
        val horizontalMove = if (wearingLeatherBoots) 1.2 else 1.05
        val jumps = if (wearingLeatherBoots) 7 else 3

        if (player.onGround()) {
            event.movement = event.movement.copy(
                x = player.deltaMovement.x * horizontalMove,
                z = player.deltaMovement.z * horizontalMove,
            )

            repeat(jumps) {
                player.jumpFromGround()
            }

            event.movement.y = 0.42
        } else if (player.airTicks == 1) {
            Timer.requestTimerSpeed(0.5f, Priority.NORMAL, ModuleSpeed, 0)

            network.send(MovePacketType.FULL.generatePacket().apply { // for some reason full works best
                onGround = true
            })

            event.movement.y = -0.0784
        }
    }
}

