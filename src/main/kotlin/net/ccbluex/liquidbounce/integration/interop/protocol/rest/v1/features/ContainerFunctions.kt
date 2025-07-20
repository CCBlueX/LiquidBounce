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

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.features

import com.google.gson.JsonObject
import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.liquidbounce.config.gson.util.emptyJsonObject
import net.ccbluex.liquidbounce.features.item.inventoryAsCompound
import net.ccbluex.liquidbounce.features.itemgroup.ClientItemGroups
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpForbidden
import net.ccbluex.netty.http.util.httpOk
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket
import kotlin.jvm.optionals.getOrNull

// GET /api/v1/container
@Suppress("UNUSED_PARAMETER")
fun getContainerInfo(requestObject: RequestObject) = httpOk(JsonObject().apply {
    val screenHandler = mc.currentScreen

    if (screenHandler is GenericContainerScreen) {
        val inventory = screenHandler.screenHandler.inventory

        if (inventory !is SimpleInventory) {
            return httpForbidden("Not a simple inventory")
        }

        addProperty("syncId", screenHandler.screenHandler.syncId)
        addProperty("title", screenHandler.title.convertToString())
        addProperty("slots", screenHandler.screenHandler.rows * 9)
        addProperty("emptySlots", inventory.heldStacks.count { it.isEmpty })
        addProperty("rows", screenHandler.screenHandler.rows)
    } else {
        return httpForbidden("Not a container")
    }
})

// POST /api/v1/container/give
@Suppress("UNUSED_PARAMETER")
fun postGiveItem(requestObject: RequestObject): FullHttpResponse {
    if (!interaction.hasCreativeInventory()) {
        return httpForbidden("Must be in creative mode")
    }

    val screenHandler = mc.currentScreen

    if (screenHandler !is GenericContainerScreen) {
        return httpForbidden("Not a container")
    }

    val inventory = screenHandler.screenHandler.inventory

    if (inventory !is SimpleInventory) {
        return httpForbidden("Not a simple inventory")
    }

    val compoundList = inventory.inventoryAsCompound(screenHandler.title)

    for (compound in compoundList) {
        val errResponse = giveItem(compound)

        if (errResponse != null) {
            return errResponse
        }
    }

    return httpOk(emptyJsonObject())
}

private fun giveItem(compound: NbtCompound): FullHttpResponse? {
    val chestItemNbt = NbtCompound().apply {
        putString("id", "minecraft:chest")
        putByte("Count", 1)
        put("tag", compound)
    }

    val itemStack = ItemStack.fromNbt(mc.world!!.registryManager, chestItemNbt).getOrNull()
        ?: return httpForbidden("Invalid item")

    val emptySlot = player.inventory.emptySlot

    if (emptySlot == -1) {
        return httpForbidden("No empty slot")
    }

    player.inventory.setStack(emptySlot, itemStack)
    network.sendPacket(
        CreativeInventoryActionC2SPacket(if (emptySlot < 9) emptySlot + 36 else emptySlot, itemStack)
    )

    return null
}

// POST /api/v1/container/store
@Suppress("UNUSED_PARAMETER")
fun postStoreItem(requestObject: RequestObject): FullHttpResponse {
    val screenHandler = mc.currentScreen

    return if (screenHandler is GenericContainerScreen) {
        val inventory = screenHandler.screenHandler.inventory

        if (inventory !is SimpleInventory) {
            return httpForbidden("Not a simple inventory")
        }

        val compoundList = inventory.inventoryAsCompound(screenHandler.title)
        compoundList.forEach(ClientItemGroups::storeAsContainerItem)

        httpOk(emptyJsonObject())
    } else {
        httpForbidden("Not a container")
    }
}
