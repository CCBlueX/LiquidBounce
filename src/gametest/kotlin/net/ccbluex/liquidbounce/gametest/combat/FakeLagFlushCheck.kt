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
package net.ccbluex.liquidbounce.gametest.combat

import com.google.gson.JsonArray
import net.ccbluex.liquidbounce.config.gson.util.GSON_DEFAULT
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleFakeLag
import net.ccbluex.liquidbounce.gametest.onClient
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.level.Level

private const val STALE_DELAY_TICKS = 20
private const val SPRINT_TICKS = 6
private const val SETTLE_TICKS = 3
private const val HELD = 0.6
private const val EMPTY_INVENTORY_SLOT = 9
private const val EXPLOSION_DISTANCE = 1.5

/**
 * FakeLag: using an item, an inventory click and an explosion that reaches the player send the held packets,
 * each only when selected in FlushOn.
 */
fun CombatScene.checkFakeLagFlushes() {
    reset()
    world.server.runCommand("effect give @a minecraft:resistance 60 4 true")
    world.server.runCommand("give @a minecraft:stick")
    context.onClient {
        ModuleFakeLag.set("Delay", "1000..1000")
        ModuleFakeLag.set("RecoilTime", "0")
        ModuleFakeLag.set("Mode", "Constant")
        ModuleFakeLag.enabled = true
    }
    // The delay FakeLag picked before Delay was set stays in use until it has run out once
    context.waitTicks(STALE_DELAY_TICKS)

    val triggers = mapOf<String, () -> Unit>(
        "ItemInteract" to { context.input.holdKeyFor({ options -> options.keyUse }, 1) },
        "InventoryClick" to this::clickInventory,
        "Explosion" to this::explodeAtServerPosition,
    )

    flushOn(emptySet())
    triggers.forEach { (name, trigger) -> checkFlush(name, expectFlush = false, trigger) }
    flushOn(triggers.keys)
    triggers.forEach { (name, trigger) -> checkFlush(name, expectFlush = true, trigger) }

    context.onClient { ModuleFakeLag.enabled = false }
}

private fun CombatScene.flushOn(triggers: Set<String>) = context.onClient {
    ModuleFakeLag.value("FlushOn").deserializeFrom(GSON_DEFAULT, JsonArray().apply { triggers.forEach { add(it) } })
}

/**
 * A click without an open inventory, as modules send it.
 */
private fun CombatScene.clickInventory() = context.onClient { client ->
    val player = checkNotNull(client.player)
    checkNotNull(client.gameMode).handleContainerInput(
        player.inventoryMenu.containerId, EMPTY_INVENTORY_SLOT, 0, ContainerInput.PICKUP, player
    )
}

/**
 * The server still has the player at the origin while FakeLag holds its movement.
 */
private fun CombatScene.explodeAtServerPosition() {
    val center = at(0.0, 0.0, -EXPLOSION_DISTANCE)
    onServer { server ->
        server.overworld().explode(null, center.x, center.y, center.z, 1f, Level.ExplosionInteraction.NONE)
    }
}

/**
 * Sprints away from the origin with FakeLag holding the movement, runs [trigger] and checks whether the server
 * got to see the movement.
 */
private fun CombatScene.checkFlush(name: String, expectFlush: Boolean, trigger: () -> Unit) {
    context.onClient { ModuleFakeLag.enabled = false }
    world.server.runCommand("tp @a ${origin.x} ${origin.y} ${origin.z} 0 0")
    context.waitTicks(CombatScene.RESET_TICKS)
    context.onClient { ModuleFakeLag.enabled = true }

    context.input.holdKey { options -> options.keyUp }
    context.input.holdKey { options -> options.keySprint }
    context.waitTicks(SPRINT_TICKS)
    val held = heldDistance()
    trigger()
    context.waitTicks(SETTLE_TICKS)
    context.input.releaseKey { options -> options.keyUp }
    context.input.releaseKey { options -> options.keySprint }
    val seen = onServer { server -> serverPlayer(server).position().distanceTo(origin) }

    check(held > HELD) { "FakeLag: held only $held blocks before $name" }
    check((seen > HELD) == expectFlush) {
        "FakeLag: $name with flush expected $expectFlush, the server saw $seen of $held held blocks"
    }
}
