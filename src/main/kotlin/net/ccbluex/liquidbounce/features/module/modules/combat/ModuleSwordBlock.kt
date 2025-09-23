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
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraAutoBlock
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_8
import net.ccbluex.liquidbounce.utils.item.isSword
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.ShieldItem
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.util.Hand

/**
 * This module allows the user to block with swords. This makes sense to be used on servers with ViaVersion.
 */
object ModuleSwordBlock : ClientModule("SwordBlock", Category.COMBAT, aliases = listOf("OldBlocking")) {

    val onlyVisual by boolean("OnlyVisual", false)
    val hideShieldSlot by boolean("HideShieldSlot", false).doNotIncludeAlways()
    private val alwaysHideShield by boolean("AlwaysHideShield", false).doNotIncludeAlways()

    @JvmOverloads
    fun shouldHideOffhand(
        player: PlayerEntity = this.player,
        offHandStack: ItemStack = player.offHandStack,
        mainHandStack: ItemStack = player.mainHandStack,
    ) = (running || KillAuraAutoBlock.blockVisual) && offHandStack.item is ShieldItem
        && (mainHandStack.isSword || player === this.player && running && alwaysHideShield)

    @Suppress("UNUSED")
    private val packetHandler = sequenceHandler<PacketEvent> { event ->
        if (onlyVisual) {
            return@sequenceHandler
        }

        // If we are already on the old combat protocol, we don't need to do anything
        if (isOlderThanOrEqual1_8) {
            return@sequenceHandler
        }

        val packet = event.packet

        if (packet is PlayerInteractItemC2SPacket) {
            val hand = packet.hand
            val itemInHand = player.getStackInHand(hand) // or activeItem

            if (hand == Hand.MAIN_HAND && itemInHand.isSword) {
                val offHandItem = player.getStackInHand(Hand.OFF_HAND)
                if (offHandItem?.item !is ShieldItem) {
                    // Until "now" we should get a shield from the server
                    waitTicks(1)
                    interaction.sendSequencedPacket(world) { sequence ->
                        // This time we use a new sequence
                        PlayerInteractItemC2SPacket(Hand.OFF_HAND, sequence,
                            player.yaw, player.pitch)
                    }
                } else {
                    event.cancelEvent()
                    // We use the old sequence
                    network.sendPacket(PlayerInteractItemC2SPacket(Hand.OFF_HAND, packet.sequence,
                        player.yaw, player.pitch))
                }
            }
        }
    }
}
