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
import net.ccbluex.liquidbounce.gametest.onClient
import net.minecraft.util.Mth
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.hypot

private const val FLIGHT_TIMEOUT_TICKS = 200
private const val THROW_TIMEOUT_TICKS = 30
private const val LANDING_TOLERANCE = 2.0

/**
 * AutoPearl: the simulated pitch lands next to the enemy's pearl, and a blocked low arc falls back to a high one.
 */
fun CombatScene.checkAutoPearlSimulatedPitch() {
    val free = flightAfterPearlFrom(at(0.0, 1.5, 3.0))
    check(free.isLowArc) { "AutoPearl: took a high arc with a free low one" }

    // A wall in front blocks the low arc
    val x = Mth.floor(origin.x)
    val y = Mth.floor(origin.y)
    val z = Mth.floor(origin.z)
    val wall = "${x - 3} $y ${z + 4} ${x + 3} ${y + 5} ${z + 4}"
    world.server.runCommand("fill $wall minecraft:stone")
    val blocked = try {
        flightAfterPearlFrom(at(0.0, 1.5, 8.0))
    } finally {
        world.server.runCommand("fill $wall minecraft:air")
    }
    check(!blocked.isLowArc) { "AutoPearl: no high arc over the wall" }

    context.onClient { ModuleAutoPearl.enabled = false }
}

/**
 * Enables AutoPearl, launches an enemy pearl from [from] and returns our flight, which has to land next to theirs.
 */
private fun CombatScene.flightAfterPearlFrom(from: Vec3): Flight {
    startAutoPearl {}
    // Let the pearls in the hotbar arrive before the enemy pearl is launched
    context.waitTicks(PEARL_TICKS)

    val (ours, theirs) = followPearl(from)
    check(ours != null && theirs != null && ours.landing.distanceTo(theirs.landing) <= LANDING_TOLERANCE) {
        "AutoPearl: landed at ${ours?.landing}, the enemy pearl at ${theirs?.landing}"
    }
    return ours
}

private class Flight(val start: Vec3, val landing: Vec3) {
    /**
     * Rises less steeply than 45 degrees.
     */
    val isLowArc: Boolean
        get() = abs(start.y) < hypot(start.x, start.z)
}

/**
 * Launches an ownerless pearl from [from] and follows it and the pearl we throw after it until both land.
 * Returns our flight (start velocity and landing) and theirs.
 */
private fun CombatScene.followPearl(from: Vec3): Pair<Flight?, Flight?> {
    val enemyId = onServer { server ->
        val level = server.overworld()
        val pearl = ThrownEnderpearl(EntityTypes.ENDER_PEARL, level)
        pearl.setPos(from)
        pearl.deltaMovement = Vec3(0.0, 0.4, 1.2)
        level.addFreshEntity(pearl)
        pearl.id
    }

    var oursId: Int? = null
    var oursStart: Vec3? = null
    val last = HashMap<Int, Vec3>()
    val starts = HashMap<Int, Vec3>()
    for (tick in 1..FLIGHT_TIMEOUT_TICKS) {
        context.waitTick()
        val pearls = onServer { server ->
            val player = serverPlayer(server)
            server.overworld().getEntities(EntityTypes.ENDER_PEARL) { true }
                .map { Triple(it.id, it.owner === player, it.position() to it.deltaMovement) }
        }
        for ((id, isOurs, state) in pearls) {
            starts.putIfAbsent(id, state.second)
            last[id] = state.first
            if (isOurs && oursId == null) {
                oursId = id
                oursStart = state.second
            }
        }
        val alive = pearls.map { it.first }.toSet()
        if (oursId == null && tick > THROW_TIMEOUT_TICKS) {
            break
        }
        if (oursId != null && oursId !in alive && enemyId !in alive) {
            break
        }
    }
    world.server.runCommand("kill @e[type=minecraft:ender_pearl]")
    world.server.runCommand("tp @a ${origin.x} ${origin.y} ${origin.z} 0 0")
    context.waitTicks(2)

    val ours = oursId?.let { id -> Flight(checkNotNull(oursStart), checkNotNull(last[id])) }
    val theirs = last[enemyId]?.let { Flight(checkNotNull(starts[enemyId]), it) }
    return ours to theirs
}
