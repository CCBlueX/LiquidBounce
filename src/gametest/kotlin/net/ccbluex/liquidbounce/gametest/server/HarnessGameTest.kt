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

import net.ccbluex.gametest.harness.Anticheat
import net.ccbluex.gametest.harness.action.awaitServer
import net.ccbluex.gametest.harness.action.query
import net.ccbluex.gametest.harness.action.setBlock
import net.ccbluex.liquidbounce.gametest.harness.LiquidBounceGameTest
import net.minecraft.client.gui.screens.DisconnectedScreen
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket

private val POSITION = Regex("""\[(-?[\d.]+)d, (-?[\d.]+)d, (-?[\d.]+)d]""")
private val SCORE = Regex("""has (\d+) """)

/**
 * The harness itself: the session has its world, refuses commands beyond it and waits for a player who reconnects,
 * and a client claiming to be where it cannot be is caught, without which no clean verdict means anything.
 */
class HarnessGameTest : LiquidBounceGameTest({
    awaitServer("block 0 -61 0 minecraft:grass_block")
    val (x, y, z) = query("data get entity $playerName Pos", POSITION).destructured
    check(x == "0.5" && y == "-60.0" && z == "0.5") { "the server has the player at $x $y $z" }

    val objective = unique("harness")
    run("scoreboard objectives add $objective dummy")
    val set = run("scoreboard players set $playerName $objective 7")
    val score = query("scoreboard players get $playerName $objective", SCORE).groupValues[1]
    check(score == "7" && server.result(set) == 7) { "the score is $score, set returned ${server.result(set)}" }
    run("scoreboard objectives remove $objective")

    expectViolation("the server to refuse stopping it") { run("stop") }

    // The session and what was built in it outlast a player who leaves and one the server drops
    val mark = origin.north(3)
    setBlock(mark, "minecraft:gold_block")
    reconnect()
    awaitServer("block ${mark.x} ${mark.y} ${mark.z} minecraft:gold_block")
    drop()
    check(client { it.gui.screen() is DisconnectedScreen }) { "the drop did not end on the disconnect screen" }
    join()
    awaitServer("block ${mark.x} ${mark.y} ${mark.z} minecraft:gold_block")

    // An anticheat catches a jump five blocks up; Paper alone only a move too fast for anything but a teleport. Some
    // anticheats check only a player that moves, and one standing still sends no positions for a while.
    val height = if (anticheat == Anticheat.VANILLA) 20 else 5
    input.holdKey { options -> options.keyUp }
    ticks(10)
    expectViolation("the server to catch a move $height blocks up") {
        client { minecraft ->
            val player = checkNotNull(minecraft.player)
            val up = ServerboundMovePlayerPacket.Pos(player.x, player.y + height, player.z, true, false)
            checkNotNull(minecraft.connection).send(up)
        }
    }
    input.releaseKey { options -> options.keyUp }
})
