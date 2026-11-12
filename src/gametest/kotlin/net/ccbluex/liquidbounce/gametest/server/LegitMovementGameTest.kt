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
import net.ccbluex.gametest.harness.action.query
import net.ccbluex.liquidbounce.gametest.harness.LiquidBounceGameTest

private val SOUTH = Regex("""(-?[\d.]+)d$""")

// Two seconds of walking cover a good 8 blocks, three of sprint-jumping more than 20
private const val WALKED = 7.0
private const val SPRINT_JUMPED = 17.0

/**
 * Plain vanilla movement has to pass, or no other verdict means anything: the player walks and then
 * sprint-jumps south, the server sees it get there and the anticheat reports nothing.
 */
class LegitMovementGameTest : LiquidBounceGameTest({
    val start = south()

    input.holdKey { it.keyUp }
    ticks(40)
    input.releaseKey { it.keyUp }
    ticks(10)
    val walked = south() - start
    check(walked >= WALKED) { "walking got the player $walked blocks far" }

    input.holdKey { it.keyUp }
    input.holdKey { it.keySprint }
    input.holdKey { it.keyJump }
    ticks(60)
    input.releaseKey { it.keyJump }
    input.releaseKey { it.keySprint }
    input.releaseKey { it.keyUp }
    ticks(20)
    val sprintJumped = south() - start - walked
    check(sprintJumped >= SPRINT_JUMPED) { "sprint-jumping got the player $sprintJumped blocks far" }
})

/** How far south the server has the player. */
private fun GameTestScope.south() = query("data get entity $playerName Pos[2]", SOUTH).groupValues[1].toDouble()
