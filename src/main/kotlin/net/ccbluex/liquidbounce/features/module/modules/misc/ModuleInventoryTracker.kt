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
package net.ccbluex.liquidbounce.features.module.modules.misc

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import net.ccbluex.liquidbounce.event.events.ItemLoreQueryEvent
import net.ccbluex.liquidbounce.event.events.PlayerEquipmentChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.command.commands.module.CommandInvsee
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.inventory.ViewedInventoryScreen
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.EquipmentSlot.MAINHAND
import net.minecraft.entity.EquipmentSlot.OFFHAND
import net.minecraft.entity.EquipmentSlot.Type.*
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.Formatting
import java.util.Locale
import java.util.UUID
import kotlin.collections.ArrayDeque
import kotlin.collections.associateBy
import kotlin.collections.forEach
import kotlin.collections.get
import kotlin.collections.getOrNull
import kotlin.collections.getOrPut
import kotlin.collections.hashMapOf
import kotlin.collections.set

/**
 * Module InventoryTracker
 *
 * Tracks the inventories of other players.
 *
 * Command: [CommandInvsee]
 */
object ModuleInventoryTracker : ClientModule("InventoryTracker", Category.WORLD) {

    /** Saves the non-persistent player object associated with the uuid.
     * This makes it possible to look up inventories of players which aren't in
     * the render distance. */
    private val savePlayers by boolean("SavePlayers", false).onChanged { playerMap.clear() }

    private val inventoryMap = hashMapOf<UUID, TrackedInventory>()
    val playerMap = hashMapOf<UUID, PlayerEntity>()

    @Suppress("unused")
    val playerEquipmentChangeHandler = handler<PlayerEquipmentChangeEvent> { event ->
        val player = event.player
        if (player !is OtherClientPlayerEntity || ModuleAntiBot.isBot(player)) return@handler

        val updatedSlot = event.equipmentSlot
        if (updatedSlot.type === ANIMAL_ARMOR) return@handler

        val newItemStack = event.itemStack

        val mainHandStack = if (updatedSlot === MAINHAND) newItemStack else player.mainHandStack
        val offHandStack = if (updatedSlot === OFFHAND) newItemStack else player.offHandStack

        val trackedInventory = inventoryMap.getOrPut(player.uuid) { TrackedInventory() }
        if (savePlayers) {
            playerMap[player.uuid] = player
        }

        when (updatedSlot.type) {
            HAND -> {
                trackedInventory.update(offHandStack, OFFHAND)
                trackedInventory.update(mainHandStack, MAINHAND)
            }
            HUMANOID_ARMOR -> trackedInventory.update(newItemStack, updatedSlot)
            else -> {}
        }

        val inventory = player.inventory
        val items = trackedInventory.items

        val mainHandEmpty = mainHandStack.isEmpty
        val range = if (mainHandEmpty) 0..34 else 1..35
        val offset = if (mainHandEmpty) 1 else 0

        for (i in range) {
            inventory.main[i + offset] = items.getOrNull(i) ?: ItemStack.EMPTY
        }
    }

    override fun onDisabled() = reset()

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> { reset() }

    private fun reset() {
        val players = world.players.associateBy { it.uuid }
        inventoryMap.keys.forEach { uuid ->
            val player = players[uuid] ?: return@forEach
            for (i in 1 until player.inventory.main.size) {
                player.inventory.main[i] = ItemStack.EMPTY
            }
        }
        inventoryMap.clear()
        playerMap.clear()
    }

    @Suppress("unused")
    private val itemLoreQueryHandler = handler<ItemLoreQueryEvent> { event ->
        if (mc.currentScreen !is ViewedInventoryScreen) return@handler
        val player = CommandInvsee.viewedPlayer
        val timeStamp = inventoryMap[player]?.timeMap?.getLong(event.itemStack)?.takeIf { it != 0L } ?: return@handler
        val lastSeen = System.currentTimeMillis() - timeStamp
        event.lore.add(
            "Last Seen: ${toMinutesSeconds(lastSeen)}".asText().formatted(Formatting.GRAY)
        )
    }

    private fun toMinutesSeconds(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(Locale.US, minutes, seconds)
    }
}

private class TrackedInventory {

    val items = ArrayDeque<ItemStack>()
    val timeMap = Object2LongOpenHashMap<ItemStack>()

    /**
     * if slot type is armor then we check if the item is already in the tracked items
     * and if yes we remove it because it has been equipped
     */
    fun update(newItemStack: ItemStack, updatedSlot: EquipmentSlot) {
        items.removeIf { it.count == 0 }
        if (newItemStack.isEmpty) return

        items.removeIf { newItemStack.item == it.item && newItemStack.enchantments == it.enchantments }
        if (updatedSlot.type === HAND) {
            items.addFirst(newItemStack)
            timeMap.put(newItemStack, System.currentTimeMillis())

            if (items.size > 36) {
                items.removeLast()
            }
        }
    }
}
