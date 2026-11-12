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
package net.ccbluex.liquidbounce.gametest

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ModuleInventoryCleaner
import net.ccbluex.liquidbounce.gametest.harness.LocalGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.AABB
import java.util.concurrent.atomic.AtomicInteger

private const val JUNK = 8

// Long enough for several clicks of the cleaner, whose click delay is a few ticks
private const val IDLE_TICKS = 60
private const val TIMEOUT = 200

// Hotbar slot 9, as the player inventory menu numbers it
private const val LOBBY_SLOT = 8
private const val LOBBY_MENU_SLOT = 36 + LOBBY_SLOT
private const val ITEM_RANGE = 32.0

/**
 * [ModuleInventoryCleaner] keeps items it has no category for, unless ThrowUnknownItems is on. A lobby item the
 * server puts back after every throw has to be left alone after a few tries, while other junk still goes.
 */
class InventoryCleanerGameTest : LocalGameTest() {

    override fun test(context: ClientGameTestContext) = context.restoring(ModuleInventoryCleaner) {
        context.worldBuilder().create().use { world ->
            val server = world.server
            server.runCommand("gamemode survival @a")
            context.waitFor({ it.player?.hasInfiniteMaterials() == false }, TIMEOUT)
            server.runCommand("give @a minecraft:diamond_sword")
            server.runCommand("give @a minecraft:bone $JUNK")
            context.waitFor({ it.count(Items.BONE) == JUNK }, TIMEOUT)

            context.onClient { ModuleInventoryCleaner.enabled = true }
            context.waitTicks(IDLE_TICKS)
            check(context.fromClient { it.count(Items.BONE) } == JUNK) {
                "the bones were thrown without ThrowUnknownItems"
            }

            context.onClient { ModuleInventoryCleaner.settings.getValue("ThrowUnknownItems").setByString("true") }
            context.waitFor({ it.count(Items.BONE) == 0 }, TIMEOUT)
            check(context.fromClient { it.count(Items.DIAMOND_SWORD) } == 1) { "the sword was thrown with the bones" }
            server.runCommand("kill @e[type=item]")

            val lobbyClicks = AtomicInteger()
            val listener = object : EventListener {}
            listener.on(PacketEvent::class.java) {
                val packet = it.packet
                if (packet is ServerboundContainerClickPacket && packet.slotNum.toInt() == LOBBY_MENU_SLOT) {
                    lobbyClicks.incrementAndGet()
                }
            }.use {
                server.runCommand("give @a minecraft:bone $JUNK")
                repeat(TIMEOUT) {
                    server.keepLobbyItem()
                    context.waitTick()
                }
                check(lobbyClicks.get() > 0) { "the cleaner never tried to throw the lobby item" }
                check(context.fromClient { it.count(Items.BONE) } == 0) { "the lobby item kept the bones in" }

                val clicks = lobbyClicks.get()
                repeat(IDLE_TICKS) {
                    server.keepLobbyItem()
                    context.waitTick()
                }
                check(lobbyClicks.get() == clicks) {
                    "the cleaner still clicks the lobby item after ${lobbyClicks.get()} tries"
                }
            }
        }
    }

}

private fun Minecraft.count(item: Item) = checkNotNull(player).inventory.countItem(item)

/** Puts the compass back after every throw and resends the inventory, like a lobby plugin does. */
private fun TestServerContext.keepLobbyItem() = runOnServer<RuntimeException> { server ->
    val player = server.playerList.players.single()
    if (player.inventory.getItem(LOBBY_SLOT).`is`(Items.COMPASS)) {
        return@runOnServer
    }

    player.inventory.setItem(LOBBY_SLOT, ItemStack(Items.COMPASS))
    val around = AABB.ofSize(player.position(), ITEM_RANGE, ITEM_RANGE, ITEM_RANGE)
    player.level().getEntitiesOfClass(ItemEntity::class.java, around) { it.item.`is`(Items.COMPASS) }
        .forEach { it.discard() }
    player.inventoryMenu.sendAllDataToRemote()
}
