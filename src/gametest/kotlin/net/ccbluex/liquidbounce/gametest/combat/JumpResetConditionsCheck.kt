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
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.ModuleVelocity
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.ModuleInventoryMove
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.ModuleNoSlow
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.InventoryScreen

private const val SPRINT_TIMEOUT_TICKS = 40
private const val JUMP_WATCH_TICKS = 3
private const val LAND_TIMEOUT_TICKS = 60

/**
 * Velocity JumpReset: every condition holds the jump back in its state, which on its own does not.
 * NoSlow and InventoryMove keep the player sprinting while using an item and in a screen.
 */
fun CombatScene.checkJumpResetConditions() {
    reset()
    context.onClient {
        ModuleVelocity.modes.setByString("JumpReset")
        jumpReset().set("JumpByDelay/Enabled", "false")
        ModuleVelocity.enabled = true
        ModuleNoSlow.enabled = true
        ModuleInventoryMove.enabled = true
    }
    context.input.holdKey { options -> options.keyUp }
    context.input.holdKey { options -> options.keySprint }
    try {
        check(jumpsAfter("generic")) { "JumpReset: no jump after damage" }
        requireConditions("OnlyPlayerAttacker")
        check(!jumpsAfter("generic")) { "JumpReset: jumped without a player attacker" }
        check(jumpsAfter("player_attack by @p")) { "JumpReset: no jump after a player hit" }
        requireConditions()

        onServer { server -> serverPlayer(server).igniteForSeconds(10f) }
        context.waitFor({ client -> checkNotNull(client.player).isOnFire }, SPRINT_TIMEOUT_TICKS)
        check(jumpsAfter("generic")) { "JumpReset: no jump while burning" }
        requireConditions("NotBurning")
        check(!jumpsAfter("generic")) { "JumpReset: jumped while burning" }
        requireConditions()
        onServer { server -> serverPlayer(server).clearFire() }
        context.waitFor({ client -> !checkNotNull(client.player).isOnFire }, SPRINT_TIMEOUT_TICKS)

        world.server.runCommand("item replace entity @a weapon.mainhand with minecraft:bow")
        world.server.runCommand("give @a minecraft:arrow")
        context.waitTicks(PICK_TICKS)
        context.input.holdKey { options -> options.keyUse }
        context.waitFor({ client -> checkNotNull(client.player).isUsingItem }, SPRINT_TIMEOUT_TICKS)
        check(jumpsAfter("generic")) { "JumpReset: no jump while using an item" }
        requireConditions("NotUsingItem")
        check(!jumpsAfter("generic")) { "JumpReset: jumped while using an item" }
        requireConditions()
        context.input.releaseKey { options -> options.keyUse }

        context.setScreen { InventoryScreen(checkNotNull(Minecraft.getInstance().player)) }
        check(jumpsAfter("generic")) { "JumpReset: no jump in a screen" }
        requireConditions("NotInScreen")
        check(!jumpsAfter("generic")) { "JumpReset: jumped in a screen" }
    } finally {
        context.input.releaseKey { options -> options.keyUse }
        context.input.releaseKey { options -> options.keyUp }
        context.input.releaseKey { options -> options.keySprint }
        context.setScreen { null }
        context.onClient { ModuleVelocity.enabled = false }
    }
}

private fun jumpReset() = ModuleVelocity.modes.modes.first { it.name == "JumpReset" }

private fun CombatScene.requireConditions(vararg names: String) = context.onClient {
    jumpReset().value("Conditions").deserializeFrom(GSON_DEFAULT, JsonArray().apply { names.forEach { add(it) } })
}

/**
 * Deals damage of [type] while sprinting on the ground and reports whether a jump was pressed right after.
 */
private fun CombatScene.jumpsAfter(type: String): Boolean {
    context.waitFor({ client -> checkNotNull(client.player).let { it.onGround() && it.hurtTime == 0 } },
        LAND_TIMEOUT_TICKS)
    world.server.runCommand("tp @a ${origin.x} ${origin.y} ${origin.z} 0 0")
    world.server.waitFor({ server -> serverPlayer(server).isSprinting }, SPRINT_TIMEOUT_TICKS)
    onServer { server ->
        val player = serverPlayer(server)
        player.invulnerableTime = 0
        player.health = player.maxHealth
    }
    world.server.runCommand("damage @p 1 minecraft:$type")
    var jumped = false
    repeat(JUMP_WATCH_TICKS) {
        context.waitTick()
        jumped = jumped || context.fromClient { checkNotNull(it.player).input.keyPresses.jump() }
    }
    return jumped
}
