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

import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.groundY
import net.ccbluex.liquidbounce.gametest.harness.LocalGameTest
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer.SimulatedPlayerInput
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.navigation.terrain.Terrain
import net.ccbluex.liquidbounce.utils.navigation.walk.MotionTraits
import net.ccbluex.liquidbounce.utils.navigation.walk.PlayerMotion
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.phys.Vec3

/**
 * [PlayerMotion], which route searches plan jumps with, has to move like the game moves the player on land. It is
 * held against [SimulatedPlayer], which is the game's own movement, with the same keys from the same spot.
 */
class PlayerMotionGameTest : LocalGameTest() {

    override fun test(context: ClientGameTestContext) {
        context.worldBuilder().create().use { world ->
            val server = world.server
            val y = server.groundY()
            server.runCommand("gamemode survival @a")

            val failures = courses.mapNotNull { course ->
                server.runCommand("fill 0 $y -4 40 ${y + 9} 4 air")
                server.runCommand("fill 0 ${y - 1} -4 40 ${y - 1} 4 stone")
                course.build(y).forEach(server::runCommand)
                val start = Vec3(2.5, (y + course.height).toDouble(), 0.5)
                server.runCommand("tp @a ${start.x} ${start.y} ${start.z} -90 0")
                context.waitFor({ it.player?.position()?.near(start) == true }, ARRIVAL_TICKS)
                context.waitTicks(SETTLE_TICKS)

                context.fromClient { client -> course.difference(checkNotNull(client.player)) }
            }

            check(failures.isEmpty()) { "PlayerMotion moves unlike the game:\n" + failures.joinToString("\n") }
        }
    }

    /**
     * The player starts [height] above the ground at x 2.5, looking down the x axis, with the keys held from then on.
     */
    private class Course(
        val name: String,
        val forward: Int = 1,
        val strafe: Int = 0,
        val jump: Boolean = false,
        val sprint: Boolean = false,
        val sneak: Boolean = false,
        val ticks: Int = 40,
        val height: Int = 0,
        val build: (Int) -> List<String> = { emptyList() },
    ) {
        /** The first tick the two are apart at, if any. */
        fun difference(player: LocalPlayer): String? {
            val motion = PlayerMotion(Terrain.live(), MotionTraits.capture(player))
            motion.reset(player)
            val game = SimulatedPlayer.fromClientPlayer(
                SimulatedPlayerInput(DirectionalInput(forward.toFloat(), strafe.toFloat()), jump, sprint, sneak)
            )

            repeat(ticks) { tick ->
                motion.tick(player.yRot, forward, strafe, jump, sprint, sneak)
                game.tick()
                if (motion.position.distanceTo(game.position()) > TOLERANCE || motion.onGround != game.onGround()) {
                    return "$name: tick $tick is ${motion.position} on ground ${motion.onGround}, " +
                        "the game moves to ${game.position()} on ground ${game.onGround()}"
                }
            }
            return null
        }
    }

    private companion object {
        /** The motion works out its turns with a sine table. */
        const val TOLERANCE = 1.0E-4

        const val ARRIVAL_TICKS = 200
        const val SETTLE_TICKS = 10

        fun platform(y: Int) = listOf("fill 0 $y -1 5 ${y + 2} 1 stone")

        fun floor(block: String) = { y: Int -> listOf("fill 4 ${y - 1} -1 30 ${y - 1} 1 $block") }

        val courses = listOf(
            Course("walking"),
            Course("sprinting", sprint = true),
            Course("sprint jumping", jump = true, sprint = true, ticks = 60),
            Course("walking diagonally", strafe = 1, ticks = 30),
            Course("walking backwards to the right", forward = -1, strafe = -1, ticks = 30),
            Course("sneaking", sneak = true, ticks = 20),
            Course("sneaking up to a ledge", sneak = true, ticks = 60, height = 3, build = ::platform),
            Course("sneaking into a corner", strafe = 1, sneak = true, ticks = 60, height = 3, build = ::platform),
            Course("walking off a ledge", height = 3, build = ::platform),
            Course("sprint jumping off a ledge", jump = true, sprint = true, height = 3, build = ::platform),
            Course("sprint jumping over a gap", jump = true, sprint = true, ticks = 30, height = 3) { y ->
                listOf("fill 0 $y -1 4 ${y + 2} 1 stone", "fill 8 $y -1 14 ${y + 2} 1 stone")
            },
            Course("sprint jumping on soul sand", jump = true, sprint = true, ticks = 60, build = floor("soul_sand")),
            Course("sprint jumping on ice", jump = true, sprint = true, ticks = 60, build = floor("packed_ice")),
            Course("sprint jumping on slime", jump = true, sprint = true, ticks = 60, build = floor("slime_block")),
            Course("sprinting on honey", sprint = true, build = floor("honey_block")),
            Course("sprinting up slabs and into a wall", sprint = true, ticks = 80) { y ->
                listOf(
                    "fill 5 $y -1 7 $y 1 stone_slab",
                    "fill 8 $y -1 10 $y 1 stone",
                    "fill 16 $y -1 16 ${y + 3} 1 stone",
                )
            },
            Course("sprint jumping up a block", jump = true, sprint = true) { y ->
                listOf("fill 6 $y -1 20 $y 1 stone")
            },
            Course("sprint jumping under a ceiling", jump = true, sprint = true) { y ->
                listOf("fill 0 ${y + 2} -1 30 ${y + 2} 1 stone")
            },
            Course("sprinting diagonally past a fence", strafe = 1, sprint = true) { y ->
                listOf("setblock 5 $y 2 oak_fence")
            },
            Course("sprinting over carpet and stairs", sprint = true) { y ->
                listOf(
                    "fill 4 $y -1 5 $y 1 white_carpet",
                    "fill 7 $y -1 7 $y 1 oak_stairs[facing=east]",
                    "fill 8 $y -1 12 $y 1 stone",
                )
            },
        )
    }

}
