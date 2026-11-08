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

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.module.modules.player.offhand.ModuleOffhand
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * Runs [block] in a new flat world with only the Offhand totem mode enabled and an empty inventory. The Offhand
 * options are restored afterwards. Inventory state is read on the server, so a swap only counts once the server
 * applied it.
 */
fun ClientGameTestContext.inOffhandWorld(block: OffhandFixture.() -> Unit) {
    waitForClient()
    val saved = fromClient { ConfigSystem.serializeValueGroup(ModuleOffhand) }

    try {
        worldBuilder().create().use { world ->
            OffhandFixture(this, world.server).apply {
                setUp()
                block()
            }
        }
    } finally {
        onClient {
            ModuleOffhand.enabled = false
            ConfigSystem.deserializeValueGroup(ModuleOffhand, saved)
        }
    }
}

class OffhandFixture(val context: ClientGameTestContext, val server: TestServerContext) {

    private var lastRefill = 0L

    fun cmd(command: String) = server.runCommand(command)

    fun setUp() {
        cmd("gamemode survival @a")
        cmd("effect give @a resistance infinite 4 true")
        cmd("clear @a")
        context.waitFor({ client -> client.player?.inventory?.isEmpty == true }, TIMEOUT)
        configure(
            "Totem/Enabled" to "true",
            "Totem/Health/Enabled" to "false",
            "Gapple/Enabled" to "false",
            "Crystal/Enabled" to "false",
            "Constraints/StartDelay" to "0..0",
            "Constraints/ClickDelay" to "0..0",
            "Constraints/CloseDelay" to "0..0"
        )
    }

    fun configure(vararg options: Pair<String, String>) = context.onClient {
        for ((path, value) in options) {
            ModuleOffhand.set(path, value)
        }
    }

    /**
     * Adds [count] totems to the main inventory, keeping the hotbar free.
     */
    fun giveTotems(count: Int) {
        val (before, free) = server.query { server ->
            val player = server.playerList.players[0]
            player.inventoryTotems() to (Inventory.SELECTION_SIZE until Inventory.INVENTORY_SIZE)
                .filter { player.inventory.getItem(it).isEmpty }
        }
        free.take(count).forEach {
            cmd("item replace entity @a inventory.${it - Inventory.SELECTION_SIZE} with totem_of_undying")
        }
        waitServer("$count more totems in the inventory") { it.inventoryTotems() >= before + count }
    }

    fun removeOffhand() {
        cmd("item replace entity @a weapon.offhand with air")
        waitServer("an empty offhand") { !it.offhandItem.isTotem() }
    }

    /**
     * Empties the offhand, waits for a totem in it again and returns the milliseconds since the last refill.
     */
    fun refill(): Long {
        removeOffhand()
        waitServer("a refilled offhand") { it.offhandItem.isTotem() }
        val now = System.currentTimeMillis()
        return (now - lastRefill).also { lastRefill = now }
    }

    fun waitServer(what: String, predicate: (ServerPlayer) -> Boolean) {
        for (tick in 0 until LONG_TIMEOUT) {
            if (server.query { predicate(it.playerList.players[0]) }) {
                return
            }
            context.waitTick()
        }

        error("Timed out waiting for $what")
    }

    fun checkServer(message: String, predicate: (ServerPlayer) -> Boolean) {
        check(server.query { predicate(it.playerList.players[0]) }) { message }
    }

    companion object {
        const val TIMEOUT = 60
        const val LONG_TIMEOUT = 120
    }

}

fun ItemStack.isTotem() = `is`(Items.TOTEM_OF_UNDYING)

fun Player.inventoryTotems() =
    (Inventory.SELECTION_SIZE until Inventory.INVENTORY_SIZE).count { inventory.getItem(it).isTotem() }
