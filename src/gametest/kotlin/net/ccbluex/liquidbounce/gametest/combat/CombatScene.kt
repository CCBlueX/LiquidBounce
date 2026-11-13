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

import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3

/**
 * A flat test world with the player standing at [origin], looking along +Z.
 */
class CombatScene(val context: ClientGameTestContext, val world: TestSingleplayerContext) {

    val origin: Vec3

    init {
        world.server.runCommand("gamemode survival @a")
        world.server.runCommand("time set day")
        context.waitFor({ client -> client.player?.onGround() == true }, SETTLE_TIMEOUT_TICKS)
        val feet = context.fromClient { client -> checkNotNull(client.player).position() }
        origin = Vec3(Math.floor(feet.x) + 0.5, feet.y, Math.floor(feet.z) + 0.5)
    }

    fun at(dx: Double, dy: Double, dz: Double): Vec3 = origin.add(dx, dy, dz)

    /**
     * Removes all other entities, empties the inventory and puts the player back at [origin], healed.
     */
    fun reset() {
        // A failed check may have left keys down
        context.input.releaseKey { options -> options.keyUp }
        context.input.releaseKey { options -> options.keySprint }
        context.input.releaseKey(InputConstants.KEY_K)
        world.server.runCommand("kill @e[type=!player]")
        world.server.runCommand("clear @a")
        world.server.runCommand("effect clear @a")
        world.server.runCommand("tp @a ${origin.x} ${origin.y} ${origin.z} 0 0")
        onServer { server -> serverPlayer(server).health = serverPlayer(server).maxHealth }
        context.onClient { client -> checkNotNull(client.player).inventory.selectedSlot = 0 }
        context.waitTicks(RESET_TICKS)
    }

    fun <T> onServer(block: (MinecraftServer) -> T): T = world.server.computeOnServer<T, RuntimeException> { block(it) }

    fun serverPlayer(server: MinecraftServer): ServerPlayer = server.playerList.players.first()

    fun serverEntity(server: MinecraftServer, id: Int): Entity = checkNotNull(server.overworld().getEntity(id))

    companion object {
        const val SETTLE_TIMEOUT_TICKS = 200
        const val RESET_TICKS = 4
    }

}

/**
 * Runs [checks] in one shared world, each from a reset scene so one failure does not hide the others,
 * and restores the settings of [modules] afterwards.
 */
fun ClientGameTestContext.runCombatChecks(modules: List<ClientModule>, vararg checks: CombatScene.() -> Unit) {
    fun disableAll() = modules.forEach { it.enabled = false }

    val saved = fromClient { modules.map(ConfigSystem::serializeValueGroup) }

    try {
        worldBuilder().create().use { world ->
            val scene = CombatScene(this, world)
            val failures = checks.mapNotNull { check ->
                runCatching { scene.check() }.exceptionOrNull()?.also { onClient { disableAll() } }
            }
            failures.firstOrNull()?.let { first ->
                failures.drop(1).forEach(first::addSuppressed)
                throw first
            }
        }
    } finally {
        onClient {
            disableAll()
            modules.zip(saved).forEach { (module, json) -> ConfigSystem.deserializeValueGroup(module, json) }
        }
    }
}
