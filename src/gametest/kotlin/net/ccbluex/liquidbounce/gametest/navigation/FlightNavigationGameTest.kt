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

package net.ccbluex.liquidbounce.gametest.navigation

import net.ccbluex.gametest.harness.Anticheat
import net.ccbluex.gametest.harness.GameTestScope
import net.ccbluex.gametest.harness.action.awaitServer
import net.ccbluex.gametest.harness.action.fill
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoWalk
import net.ccbluex.liquidbounce.gametest.command
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.harness.LiquidBounceGameTest
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.playerPosition
import net.ccbluex.liquidbounce.gametest.restoring
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

private const val GOTO_TICKS = 900
private const val ARRIVAL = 1.1
private const val LAND_TICKS = 20

/**
 * `.goto` flies where walking cannot go: a survival player with an elytra from the top of a tower down to the ground
 * far away, unhurt, a survival player with the Vanilla mode of Fly onto a pillar where only Paper watches, and a
 * creative player across a canyon into the void and over a wall wider than the searched area.
 */
class FlightNavigationGameTest : LiquidBounceGameTest({
    context.restoring(ModuleAutoWalk, ModuleFly) {
        val y = origin.y
        buildCourse(y)

        checkElytra(y)
        // The Vanilla mode of Fly is plain flying, which only Paper's own checks let through
        if (anticheats == setOf(Anticheat.VANILLA)) {
            checkFlyModule(y)
        }
        // Last, as a creative player who stops flying in mid-air falls in a way not every anticheat follows
        checkCreative(y)
    }
})

/** A canyon down to the void at x 10..19, a wall twelve blocks high at x 40, a pillar and a tower. */
private fun GameTestScope.buildCourse(y: Int) {
    fill(BlockPos(10, y - 4, -30), BlockPos(19, y - 1, 30), "minecraft:air")
    fill(BlockPos(40, y, -30), BlockPos(40, y + 11, 30), "minecraft:stone")
    fill(BlockPos(60, y, 10), BlockPos(62, y + 8, 12), "minecraft:stone")
    fill(BlockPos(0, y, -52), BlockPos(2, y + 39, -50), "minecraft:stone")
    awaitServer("block 2 ${y + 39} -50 minecraft:stone")
}

private fun GameTestScope.checkCreative(y: Int) {
    run("gamemode creative $playerName")
    awaitClient("creative mode") { checkNotNull(it.player).abilities.mayfly }
    moveTo(Vec3(4.5, y.toDouble(), 0.5), yaw = -90f)

    check(goto(Vec3(26.5, y.toDouble(), 0.5), "across the canyon")) { "Crossed the canyon without flying" }
    check(goto(Vec3(52.5, y.toDouble(), 0.5), "over the wall")) { "Crossed the wall without flying" }
    check(context.fromClient { checkNotNull(it.player).y } > y - 1) { "Fell into the canyon" }
}

private fun GameTestScope.checkFlyModule(y: Int) {
    moveTo(Vec3(54.5, y.toDouble(), 11.5), yaw = -90f)

    unhurt("Flying onto the pillar") {
        context.onClient {
            ModuleFly.containedValues.first { it.name == "Mode" }.setByString("Vanilla")
            ModuleFly.enabled = true
        }
        try {
            goto(Vec3(61.5, y + 9.0, 11.5), "onto the pillar")
        } finally {
            context.onClient { ModuleFly.enabled = false }
        }

        ticks(LAND_TICKS)
        val (position, onGround) = context.fromClient { mc ->
            val player = checkNotNull(mc.player)
            player.position() to player.onGround()
        }
        check(onGround && position.y >= y + 9.0) { "Did not land on the pillar, but at $position" }
    }
}

private fun GameTestScope.checkElytra(y: Int) {
    run("item replace entity $playerName armor.chest with minecraft:elytra")
    moveTo(Vec3(1.5, y + 40.0, -50.5), yaw = -90f)

    val destination = Vec3(71.5, y.toDouble(), -50.5)
    unhurt("Gliding down") {
        context.command("goto ${destination.x} ${destination.y} ${destination.z}")
        var glided = false
        await(context, GOTO_TICKS, "Did not glide to $destination") {
            val (arrived, gliding) = context.fromClient { mc ->
                !ModuleAutoWalk.enabled to checkNotNull(mc.player).isFallFlying
            }
            glided = glided || gliding
            arrived
        }
        val position = context.playerPosition()
        check(position.distanceTo(destination) <= ARRIVAL) { "AutoWalk stopped at $position" }
        check(glided) { "Got down from the tower without gliding" }
    }
    run("item replace entity $playerName armor.chest with minecraft:air")
}

private fun GameTestScope.goto(destination: Vec3, what: String): Boolean =
    goto(context, destination, what, GOTO_TICKS, ARRIVAL) { it.abilities.flying || !it.onGround() }
