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

import net.ccbluex.gametest.harness.GameTestScope
import net.ccbluex.gametest.harness.action.awaitAnswer
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleElytraRecast
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoWalk
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.harness.LiquidBounceGameTest
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.restoring
import net.ccbluex.liquidbounce.utils.client.logger
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

private const val PLATFORM_A = 300
private const val PLATFORM_B = 650
private const val PILLAR = 475
private const val LOW = 100
private const val RISE = 10
private const val ROOF = 128
private const val BOUNCE_DISTANCE = 400.0
private const val MIN_BOUNCE_SPEED = 12.0
private const val TICKS_PER_SECOND = 20.0
private const val CRUISE_TICKS = 1600
private const val BOUNCE_TICKS = 1600
private const val ARRIVAL = 1.5

/**
 * `.goto` with an elytra over long distances, unhurt: with rockets between platforms over the void of the End, a
 * pillar in the way, and bouncing along the Nether roof with ElytraRecast, faster than sprinting.
 */
class ElytraTravelGameTest : LiquidBounceGameTest({
    context.restoring(ModuleAutoWalk, ModuleElytraRecast) {
        run("item replace entity $playerName armor.chest with minecraft:elytra")

        checkEndCruise()
        // ElytraRecast is made for vanilla servers
        if (paperOnly) {
            checkRoofBounce()
        }
    }
})

/** Platforms at x 300 and x 650, the second ten blocks higher, with a pillar reaching above both halfway. */
private fun GameTestScope.checkEndCruise() {
    run("item replace entity $playerName hotbar.0 with minecraft:firework_rocket 64")
    for (x in listOf(PLATFORM_A, PLATFORM_B, PILLAR)) {
        inEnd("forceload add ${x - 4} 296 ${x + 4} 304")
        awaitAnswer("execute in minecraft:the_end if loaded $x $LOW 300", "Test passed")
    }
    inEnd("fill ${PLATFORM_A - 4} $LOW 296 ${PLATFORM_A + 4} $LOW 304 minecraft:end_stone")
    inEnd("fill ${PLATFORM_B - 4} ${LOW + RISE} 296 ${PLATFORM_B + 4} ${LOW + RISE} 304 minecraft:end_stone")
    inEnd("fill $PILLAR ${LOW - 30} 298 ${PILLAR + 2} ${LOW + 40} 302 minecraft:obsidian")
    inEnd("kill @e[type=minecraft:ender_dragon]")
    awaitAnswer("execute in minecraft:the_end if block $PILLAR ${LOW + 40} 300 minecraft:obsidian", "Test passed")

    travel(BlockPos(PLATFORM_A, LOW + 1, 300), yaw = -90f, dimension = Level.END)
    val goal = Vec3(PLATFORM_B + 0.5, LOW + RISE + 1.0, 300.5)
    val glided = elytraGoto(goal, "across the void", CRUISE_TICKS)
    check(glided) { "Crossed the void without flying" }
}

/** The natural roof of the Nether, flat bedrock with its top at y 127. */
private fun GameTestScope.checkRoofBounce() {
    run("clear $playerName minecraft:firework_rocket")
    travel(BlockPos(0, ROOF, 0), yaw = -90f, dimension = Level.NETHER)
    context.onClient { ModuleElytraRecast.enabled = true }

    val goal = Vec3(BOUNCE_DISTANCE + 0.5, ROOF.toDouble(), 0.5)
    val began = context.fromClient { checkNotNull(it.level).gameTime }
    val bounced = elytraGoto(goal, "along the roof", BOUNCE_TICKS)
    val ticks = context.fromClient { checkNotNull(it.level).gameTime } - began
    check(bounced) { "Crossed the roof without the elytra" }

    val speed = BOUNCE_DISTANCE / (ticks / TICKS_PER_SECOND)
    logger.info("Bounced $BOUNCE_DISTANCE blocks in $ticks ticks, $speed blocks per second")
    check(speed > MIN_BOUNCE_SPEED) { "Bounced at only $speed blocks per second" }
}

private fun GameTestScope.inEnd(command: String) = run("execute in minecraft:the_end run $command")

private fun GameTestScope.elytraGoto(goal: Vec3, what: String, ticks: Int): Boolean {
    var flew = false
    unhurt("Going $what") {
        flew = goto(context, goal, what, ticks, ARRIVAL) { it.isFallFlying }
    }
    return flew
}
