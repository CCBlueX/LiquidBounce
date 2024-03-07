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

package net.ccbluex.liquidbounce.web.socket.protocol.rest.features

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.features.container.inventoryAsCompound
import net.ccbluex.liquidbounce.features.itemgroup.ClientItemGroups
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.ccbluex.liquidbounce.web.socket.netty.httpBadRequest
import net.ccbluex.liquidbounce.web.socket.netty.httpForbidden
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket
import kotlin.jvm.optionals.getOrNull

fun RestNode.containerRest() {
    get("/container") {
        val screenHandler = mc.currentScreen

        if (screenHandler is GenericContainerScreen) {
            val inventory = screenHandler.screenHandler.inventory

            if (inventory !is SimpleInventory) {
                return@get httpForbidden("Not a simple inventory")
            }

            val jsonObject = JsonObject()
            jsonObject.addProperty("syncId", screenHandler.screenHandler.syncId)
            jsonObject.addProperty("title", screenHandler.title.convertToString())
            jsonObject.addProperty("slots", screenHandler.screenHandler.rows * 9)
            jsonObject.addProperty("emptySlots", inventory.heldStacks.count { it.isEmpty })
            jsonObject.addProperty("rows", screenHandler.screenHandler.rows)
            httpOk(jsonObject)
        } else {
            httpForbidden("Not a container")
        }
    }.apply {
        post("/give") {
            if (!interaction.hasCreativeInventory()) {
                return@post httpForbidden("Must be in creative mode")
            }

            val screenHandler = mc.currentScreen

            if (screenHandler is GenericContainerScreen) {
                val inventory = screenHandler.screenHandler.inventory

                if (inventory !is SimpleInventory) {
                    return@post httpForbidden("Not a simple inventory")
                }

                val compoundList = inventory.inventoryAsCompound(screenHandler.title)

                for (compound in compoundList) {
                    val chestItemNbt = NbtCompound()
                    chestItemNbt.putString("id", "minecraft:chest")
                    chestItemNbt.putByte("Count", 1)
                    chestItemNbt.put("tag", compound)

                    val itemStack = ItemStack.fromNbt(mc.world!!.registryManager, chestItemNbt).getOrNull()
                        ?: return@post httpForbidden("Invalid item")

                    val emptySlot = player.inventory.emptySlot

                    if (emptySlot == -1) {
                        return@post httpForbidden("No empty slot")
                    }

                    player.inventory.setStack(emptySlot, itemStack)
                    network.sendPacket(
                        CreativeInventoryActionC2SPacket(if (emptySlot < 9) emptySlot + 36 else emptySlot,
                            itemStack)
                    )
                }

                httpOk(JsonObject())
            } else {
                httpForbidden("Not a container")
            }
        }

        post("/store") {
            val screenHandler = mc.currentScreen

            if (screenHandler is GenericContainerScreen) {
                val inventory = screenHandler.screenHandler.inventory

                if (inventory !is SimpleInventory) {
                    return@post httpForbidden("Not a simple inventory")
                }

                val compoundList = inventory.inventoryAsCompound(screenHandler.title)
                compoundList.forEach(ClientItemGroups::storeAsContainerItem)

                httpOk(JsonObject())
            } else {
                httpForbidden("Not a container")
            }
        }
    }
}
