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

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleFastUse
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.block.isBlockAtPosition
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.minecraft.block.Block
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.util.Hand

internal object FlyEnderpearl : Choice("Enderpearl") {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleFly.modes

    val speed by float("Speed", 1f, 0.5f..2f)

    var threwPearl = false
    var canFly = false

    val rotations = tree(RotationsConfigurable(this))

    override fun enable() {
        threwPearl = false
        canFly = false
    }

    val repeatable = tickHandler {
        val slot = Slots.Hotbar.findSlot(Items.ENDER_PEARL)?.hotbarSlot

        if (player.isDead || player.isSpectator || player.abilities.creativeMode) {
            return@tickHandler
        }

        if (!threwPearl && !canFly) {
            if (slot != null) {
                if (slot != player.inventory.selectedSlot) {
                    network.sendPacket(UpdateSelectedSlotC2SPacket(slot))
                }

                if (player.pitch <= 80) {
                    RotationManager.setRotationTarget(
                        Rotation(player.yaw, (80f..90f).random()),
                        configurable = rotations,
                        provider = ModuleFastUse,
                        priority = Priority.IMPORTANT_FOR_USAGE_2
                    )
                }

                waitTicks(2)
                interaction.sendSequencedPacket(world) { sequence ->
                    PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, player.yaw, player.pitch)
                }

                if (slot != player.inventory.selectedSlot) {
                    network.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot))
                }

                threwPearl = true
            }
        } else if (!threwPearl && canFly) {
            player.velocity = player.velocity.withStrafe(speed = speed.toDouble())
            player.velocity.y = when {
                mc.options.jumpKey.isPressed -> speed.toDouble()
                mc.options.sneakKey.isPressed -> -speed.toDouble()
                else -> 0.0
            }
            return@tickHandler
        }
    }

    val packetHandler = handler<PacketEvent> { event ->
        if (event.origin == TransferOrigin.OUTGOING && event.packet is TeleportConfirmC2SPacket
            && isABitAboveGround() && threwPearl) {
            threwPearl = false
            canFly = true
        }
    }

    fun isABitAboveGround(): Boolean {
        for (y in 0..5) {
            val boundingBox = player.box
            val detectionBox = boundingBox.withMinY(boundingBox.minY - y)

            return detectionBox.isBlockAtPosition { it is Block }
        }
        return false
    }
}
