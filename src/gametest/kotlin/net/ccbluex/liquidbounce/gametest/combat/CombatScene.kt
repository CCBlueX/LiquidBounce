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
import net.ccbluex.liquidbounce.gametest.onClient
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3

/**
 * The world of [combatTest] with the player standing at [origin], looking along +Z.
 */
class CombatScene(val context: ClientGameTestContext, val server: TestServerContext, y: Int) {

    val origin = Vec3(0.5, y.toDouble(), 0.5)

    init {
        server.runCommand("time set day")
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
        server.runCommand("kill @e[type=!player]")
        server.runCommand("clear @a")
        server.runCommand("effect clear @a")
        server.runCommand("tp @a ${origin.x} ${origin.y} ${origin.z} 0 0")
        onServer { server -> serverPlayer(server).health = serverPlayer(server).maxHealth }
        context.onClient { client -> checkNotNull(client.player).inventory.selectedSlot = 0 }
        context.waitTicks(4)
    }

    fun <T> onServer(block: (MinecraftServer) -> T): T = server.computeOnServer<T, RuntimeException> { block(it) }

    fun serverPlayer(server: MinecraftServer): ServerPlayer = server.playerList.players.first()
}
