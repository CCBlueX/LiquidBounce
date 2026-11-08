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

import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleAutoPearl
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityTypes

private const val MEMORY_MILLIS = 5000L
// Game ticks can run faster than real time in game tests
private const val MEMORY_TIMEOUT_TICKS = 2000
private const val UPDATE_TICKS = 40

/**
 * The enemy is remembered after a direct hit and after a hit on an end crystal next to it, and forgotten when it
 * dies, moves away or after [MEMORY_MILLIS].
 */
fun CombatScene.checkLastAttackedMemory() {
    reset()
    val first = spawnHusk(at(4.0, 0.0, 0.0))
    val second = spawnHusk(at(-4.0, 0.0, 0.0))
    fun remembered() = context.fromClient { CombatManager.lastAttackedEnemy?.id }

    attack(first)
    check(remembered() == first) { "The attacked enemy is not remembered" }

    // Invulnerable, so the hit does not blow it up
    val crystal = onServer { server ->
        val level = server.overworld()
        val endCrystal = checkNotNull(EntityTypes.END_CRYSTAL.create(level, EntitySpawnReason.COMMAND))
        endCrystal.setPos(at(-4.0, 0.0, 1.5))
        endCrystal.isPermanentlyInvulnerable = true
        level.addFreshEntity(endCrystal)
        endCrystal.id
    }
    context.waitFor({ client -> client.level?.getEntity(crystal) != null }, CombatScene.ENTITY_TIMEOUT_TICKS)
    attack(crystal)
    check(remembered() == second) { "The enemy next to the attacked crystal is not remembered" }
    onServer { server -> serverEntity(server, second).kill(server.overworld()) }
    context.waitFor({ CombatManager.lastAttackedEnemy == null }, UPDATE_TICKS)

    attack(first)
    check(remembered() == first) { "The attacked enemy is not remembered" }
    onServer { server -> serverEntity(server, first).teleportTo(origin.x + 20, origin.y, origin.z) }
    context.waitFor({ CombatManager.lastAttackedEnemy == null }, UPDATE_TICKS)
    onServer { server -> serverEntity(server, first).teleportTo(origin.x + 4, origin.y, origin.z) }
    context.waitFor({ client -> clientEntity(client, first).x < origin.x + 5 }, UPDATE_TICKS)

    attack(first)
    val attackedAt = System.currentTimeMillis()
    context.waitFor({ CombatManager.lastAttackedEnemy == null }, MEMORY_TIMEOUT_TICKS)
    val forgotAfter = System.currentTimeMillis() - attackedAt
    check(forgotAfter >= MEMORY_MILLIS) { "Forgotten after $forgotAfter ms" }
}

/**
 * LastAttacked follows only the pearls of the enemy we hit last.
 */
fun CombatScene.checkAutoPearlLastAttacked() {
    startAutoPearl { ModuleAutoPearl.set("Mode", "LastAttacked") }
    val enemy = spawnHusk(at(4.0, 0.0, 0.0))
    val other = spawnHusk(at(-4.0, 0.0, 0.0))

    attack(enemy)
    check(pearlThrownAfter(other, at(0.0, 1.5, 3.0)) == null) { "Followed a pearl of another enemy" }
    attack(enemy)
    check(pearlThrownAfter(enemy, at(0.0, 1.5, 3.0)) != null) { "Did not follow the last attacked enemy" }
}
