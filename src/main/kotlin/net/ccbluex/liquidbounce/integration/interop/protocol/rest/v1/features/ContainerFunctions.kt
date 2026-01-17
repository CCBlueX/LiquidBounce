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

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.features

import com.google.gson.JsonObject
import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.liquidbounce.features.item.inventoryAsComponents
import net.ccbluex.liquidbounce.features.itemgroup.ClientItemGroups
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.network
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.netty.http.util.httpForbidden
import net.ccbluex.netty.http.util.httpNoContent
import net.ccbluex.netty.http.util.httpOk
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

// GET /api/v1/container
fun getContainerInfo() = httpOk(JsonObject().apply {
    val screenHandler = mc.screen

    if (screenHandler is ContainerScreen) {
        val inventory = screenHandler.menu.container

        if (inventory !is SimpleContainer) {
            return httpForbidden("Not a simple inventory")
        }

        addProperty("syncId", screenHandler.menu.containerId)
        addProperty("title", screenHandler.title.string)
        addProperty("slots", screenHandler.menu.rowCount * 9)
        addProperty("emptySlots", inventory.items.count { it.isEmpty })
        addProperty("rows", screenHandler.menu.rowCount)
    } else {
        return httpForbidden("Not a container")
    }
})

// POST /api/v1/container/give
fun postGiveItem(): FullHttpResponse {
    if (!player.isCreative) {
        return httpForbidden("Must be in creative mode")
    }

    val screenHandler = mc.screen

    if (screenHandler !is ContainerScreen) {
        return httpForbidden("Not a container")
    }

    val inventory = screenHandler.menu.container

    if (inventory !is SimpleContainer) {
        return httpForbidden("Not a simple inventory")
    }

    val componentChangesList = inventory.inventoryAsComponents(screenHandler.title)

    for (components in componentChangesList) {
        val errResponse = giveItem(components)

        if (errResponse != null) {
            return errResponse
        }
    }

    return httpNoContent()
}

private fun giveItem(components: DataComponentPatch): FullHttpResponse? {
    val itemStack = ItemStack(BuiltInRegistries.ITEM.wrapAsHolder(Items.CHEST), 1, components)

    val emptySlot = player.inventory.freeSlot

    if (emptySlot == -1) {
        return httpForbidden("No empty slot")
    }

    player.inventory.setItem(emptySlot, itemStack)
    network.send(
        ServerboundSetCreativeModeSlotPacket(if (emptySlot < 9) emptySlot + 36 else emptySlot, itemStack)
    )

    return null
}

// POST /api/v1/container/store
fun postStoreItem(): FullHttpResponse {
    val screenHandler = mc.screen

    return if (screenHandler is ContainerScreen) {
        val inventory = screenHandler.menu.container

        if (inventory !is SimpleContainer) {
            return httpForbidden("Not a simple inventory")
        }

        val components = inventory.inventoryAsComponents(screenHandler.title)
        components.forEach(ClientItemGroups::storeAsContainerItem)

        httpNoContent()
    } else {
        httpForbidden("Not a container")
    }
}
