/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.config.NamedChoice
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.item.ItemUsageContext
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult

fun clickBlockWithSlot(
    player: ClientPlayerEntity,
    rayTraceResult: BlockHitResult,
    slot: Int
) {
    val prevHotbarSlot = player.inventory.selectedSlot

    player.inventory.selectedSlot = slot

    if (slot != prevHotbarSlot) {
        player.networkHandler.sendPacket(UpdateSelectedSlotC2SPacket(slot))
    }

    interaction.sendSequencedPacket(world) { sequence ->
        PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, rayTraceResult, sequence)
    }

    val itemUsageContext = ItemUsageContext(player, Hand.MAIN_HAND, rayTraceResult)

    val itemStack = player.inventory.getStack(slot)

    val actionResult: ActionResult

    if (player.isCreative) {
        val i = itemStack.count
        actionResult = itemStack.useOnBlock(itemUsageContext)
        itemStack.count = i
    } else {
        actionResult = itemStack.useOnBlock(itemUsageContext)
    }

    if (actionResult.shouldSwingHand()) {
        player.swingHand(Hand.MAIN_HAND)
    }

    if (slot != prevHotbarSlot) {
        player.networkHandler.sendPacket(UpdateSelectedSlotC2SPacket(prevHotbarSlot))
    }

    player.inventory.selectedSlot = prevHotbarSlot
}

fun handlePacket(packet: Packet<*>) =
    runCatching { (packet as Packet<ClientPlayPacketListener>).apply(mc.networkHandler) }

fun sendPacketSilently(packet: Packet<*>) = mc.networkHandler?.connection?.send(packet, null)

enum class MovePacketType(override val choiceName: String, val generatePacket: () -> PlayerMoveC2SPacket)
    : NamedChoice {
    ON_GROUND_ONLY("OnGroundOnly", {
        PlayerMoveC2SPacket.OnGroundOnly(player.isOnGround)
    }),
    POSITION_AND_ON_GROUND("PositionAndOnGround", {
        PlayerMoveC2SPacket.PositionAndOnGround(player.x, player.y, player.z, player.isOnGround)
    }),
    LOOK_AND_ON_GROUND("LookAndOnGround", {
        PlayerMoveC2SPacket.LookAndOnGround(player.yaw, player.pitch, player.isOnGround)
    }),
    FULL("Full", {
        PlayerMoveC2SPacket.Full(player.x, player.y, player.z, player.yaw, player.pitch, player.isOnGround)
    });
}
