/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.item.ItemUsageContext
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
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

    mc.interactionManager!!.sendSequencedPacket(mc.world!!) { sequence ->
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

