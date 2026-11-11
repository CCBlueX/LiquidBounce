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
package net.ccbluex.liquidbounce.gametest.inventory

import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item

/**
 * Shared world state for the inventory option checks: one survival player whose inventory and effects are reset
 * between checks, and helpers that read the server's view of it.
 */
class InventoryBench(val context: ClientGameTestContext, val server: TestServerContext) {

    /** A key no vanilla binding uses, for the module keys under test. */
    val testKey: InputConstants.Key = InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_K)

    lateinit var origin: BlockPos
        private set

    fun setUp() {
        command("gamemode survival @a")
        command("time set noon")
        context.waitFor({ client -> client.player?.onGround() == true && !client.player!!.isCreative }, SETTLE_TICKS)
        origin = context.fromClient { client -> checkNotNull(client.player).blockPosition() }
        reset()
    }

    /**
     * Puts the player back on its spot with an empty inventory, full health and no effects.
     */
    fun reset() {
        command("clear @a")
        command("effect clear @a")
        command("kill @e[type=!player]")
        command("tp @a ${origin.x + 0.5} ${origin.y} ${origin.z + 0.5} 0 0")
        onServer { player ->
            player.health = player.maxHealth
            player.foodData.foodLevel = FULL_FOOD
            player.inventory.selectedSlot = 0
        }
        context.onClient { client ->
            val player = checkNotNull(client.player)
            player.inventory.selectedSlot = 0
            player.xRot = 0f
            player.yRot = 0f
            client.gui.setScreen(null)
        }
        context.waitFor({ client -> client.player?.onGround() == true }, SETTLE_TICKS)
        context.waitTicks(2)
    }

    fun command(command: String) = server.runCommand(command)

    fun <T> onServer(block: (ServerPlayer) -> T): T =
        server.computeOnServer<T, RuntimeException> { server -> block(server.playerList.players.single()) }

    fun count(item: Item): Int = onServer { it.inventory.countItem(item) }

    fun stackAt(slot: Int) = onServer { it.inventory.getItem(slot).copy() }

    val serverSlot: Int
        get() = onServer { it.inventory.selectedSlot }

    /**
     * Waits until [condition] holds, failing with [message] after [ticks] ticks.
     */
    fun waitUntil(ticks: Int, message: () -> String, condition: () -> Boolean): Int {
        repeat(ticks) { tick ->
            if (condition()) {
                return tick
            }
            context.waitTick()
        }
        check(condition(), message)
        return ticks
    }

    /**
     * Configures [module] on the client, enables it for [block] and restores its configuration afterwards.
     */
    fun withModule(module: ClientModule, configure: () -> Unit, block: () -> Unit) {
        val saved = context.fromClient { ConfigSystem.serializeValueGroup(module) }
        try {
            context.onClient {
                configure()
                module.enabled = true
            }
            block()
        } finally {
            context.onClient {
                module.enabled = false
                ConfigSystem.deserializeValueGroup(module, saved)
            }
            reset()
        }
    }

    private companion object {
        const val SETTLE_TICKS = 200
        const val FULL_FOOD = 20
    }

}

/**
 * The value at [path] below this group, stepping into modes by their name.
 */
@Suppress("UNCHECKED_CAST")
fun <T : Any> ValueGroup.setting(vararg path: String): Value<T> {
    var group: ValueGroup = this
    var index = 0
    while (true) {
        val name = path[index]
        val value = group.containedValues.firstOrNull { it.name == name }
            ?: error("No value $name in ${group.name}")

        if (index == path.lastIndex) {
            return value as Value<T>
        }

        index++
        group = if (value is ModeValueGroup<*>) {
            val modeName = path[index++]
            value.modes.first { it.name == modeName }
        } else {
            value as ValueGroup
        }
    }
}

fun ValueGroup.set(vararg path: String, value: String) = setting<Any>(*path).setByString(value)
