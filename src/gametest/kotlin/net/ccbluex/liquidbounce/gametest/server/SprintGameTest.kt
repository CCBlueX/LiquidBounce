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
package net.ccbluex.liquidbounce.gametest.server

import net.ccbluex.gametest.harness.GameTestScope
import net.ccbluex.gametest.harness.action.awaitServer
import net.ccbluex.gametest.harness.action.query
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleSprint
import net.ccbluex.liquidbounce.gametest.harness.LiquidBounceGameTest
import net.ccbluex.liquidbounce.gametest.harness.disable
import net.ccbluex.liquidbounce.gametest.harness.enable

private val SCORE = Regex("""has (\d+) """)

// Centimetres: a second of sprinting covers more than five blocks, one of walking more than four. In a
// sprint-jump only the landing is on the ground, where the statistic counts
private const val SPRINTED = 300
private const val LANDED = 60
private const val WALKED = 150

/**
 * Sprint's default Legit mode starts only the sprint the key would: the server counts sprinting forward, on the
 * ground, between jumps and while strafing, walking backwards, and the anticheat reports nothing.
 */
class SprintGameTest : LiquidBounceGameTest({
    val sprinted = statistic("sprint_one_cm")
    val walked = statistic("walk_one_cm")

    input.holdKey { it.keyUp }
    ticks(20)
    check(score(sprinted) == 0) { "the player sprinted without the module" }

    enable(ModuleSprint)
    awaitSprint(sprinted, SPRINTED)
    val walkedBefore = score(walked)

    input.holdKey { it.keyJump }
    awaitSprint(sprinted, LANDED)
    input.releaseKey { it.keyJump }

    input.holdKey { it.keyLeft }
    awaitSprint(sprinted, SPRINTED)
    check(score(walked) == walkedBefore) { "the player walked forward with the module on" }
    input.releaseKey { it.keyLeft }
    input.releaseKey { it.keyUp }
    ticks(10)

    val sprintedForward = score(sprinted)
    input.holdKey { it.keyDown }
    awaitServer("score $playerName $walked matches ${score(walked) + WALKED}..")
    check(score(sprinted) == sprintedForward) { "the player sprinted backwards" }
    input.releaseKey { it.keyDown }
    disable(ModuleSprint)
})

/** Has the server count a movement statistic of the player, from zero so that "none yet" can be asked for. */
private fun GameTestScope.statistic(name: String): String {
    val objective = unique(name)
    run("scoreboard objectives add $objective minecraft.custom:minecraft.$name")
    run("scoreboard players set $playerName $objective 0")
    return objective
}

private fun GameTestScope.score(objective: String) =
    query("scoreboard players get $playerName $objective", SCORE).groupValues[1].toInt()

/** Waits until the server has seen the player sprint another [distance] centimetres. */
private fun GameTestScope.awaitSprint(objective: String, distance: Int) =
    awaitServer("score $playerName $objective matches ${score(objective) + distance}..")
