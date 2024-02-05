package net.ccbluex.liquidbounce.web.socket.protocol.rest.game

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.features.container.inventoryAsCompound
import net.ccbluex.liquidbounce.features.itemgroup.ClientItemGroups
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.web.socket.netty.httpForbidden
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket

internal fun RestNode.setupContainerRestApi() {
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

                    val itemStack = ItemStack.fromNbt(chestItemNbt)
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
