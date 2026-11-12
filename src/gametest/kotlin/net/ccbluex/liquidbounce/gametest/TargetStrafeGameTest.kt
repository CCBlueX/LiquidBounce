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
package net.ccbluex.liquidbounce.gametest

import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleTargetStrafe
import net.ccbluex.liquidbounce.gametest.harness.LocalGameTest
import net.ccbluex.liquidbounce.utils.aiming.utils.canSeeBox
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import net.minecraft.world.entity.EntityTypes
import kotlin.math.abs
import kotlin.math.min

/**
 * [ModuleTargetStrafe] has to strafe out from behind a wall that ends on one side until it sees the target,
 * and must leave a target alone that stands above its range sphere, even though the horizontal circle reaches it.
 */
class TargetStrafeGameTest : LocalGameTest() {

    override fun test(context: ClientGameTestContext) {
        context.restoring(ModuleTargetStrafe) {
            context.worldBuilder().create().use { world ->
                val server = world.server
                val y = server.groundY()
                server.runCommand("gamerule advance_time false")
                server.runCommand("gamerule spawn_mobs false")
                server.runCommand("time set noon")
                server.runCommand("difficulty easy")
                server.runCommand("gamemode creative @a")

                context.onClient {
                    ModuleTargetStrafe.settings.getValue("Range").setByString("3.5")
                    ModuleTargetStrafe.settings.getValue("FollowRange").setByString("6")
                    ModuleTargetStrafe.enabled = true
                }

                strafesToTheOpenSide(context, server, y)
                skipsTargetAbove(context, server, y)
            }
        }
    }

    /**
     * The wall covers x -7 to 1 between the player and the husk. Westwards the orbit runs into the wall,
     * eastwards it passes the end of the wall.
     */
    private fun strafesToTheOpenSide(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        server.runCommand("fill -7 $y -2 1 ${y + 2} -2 stone")
        server.runCommand("summon husk 0.5 $y 0.5 $STILL")
        server.runCommand("tp @a 1.0 $y -3.5 0 0")
        context.waitTicks(SETTLE_TICKS)

        val start = context.playerPosition()
        check(!context.seesHusk()) { "the husk is visible from the start at $start" }

        var westmost = start.x
        var seenAfter = -1
        context.input.holdKey { it.keyUp }
        try {
            for (tick in 1..SIGHT_TICKS) {
                context.waitTick()
                westmost = min(westmost, context.playerPosition().x)
                if (context.seesHusk()) {
                    seenAfter = tick
                    break
                }
            }
        } finally {
            context.input.releaseKey { it.keyUp }
        }

        val end = context.playerPosition()
        check(seenAfter > 0) { "no line of sight after $SIGHT_TICKS ticks, the player ended at $end" }
        check(westmost > start.x - DRIFT) { "the player strafed west into the wall, to x $westmost" }
        check(end.x > WALL_END_X) { "the player sees the husk from $end, not past the end of the wall" }
    }

    /** The husk stands 4 blocks up, 2 blocks away: inside the horizontal circle, outside the sphere. */
    private fun skipsTargetAbove(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        server.runCommand("kill @e[type=husk]")
        server.runCommand("setblock 20 ${y + 3} 0 stone")
        server.runCommand("summon husk 20.5 ${y + 4} 0.5 $STILL")
        server.runCommand("tp @a 22.5 $y 0.5 -90 0")
        context.waitTicks(SETTLE_TICKS)

        val start = context.playerPosition()
        context.input.holdKey { it.keyUp }
        try {
            context.waitTicks(WALK_TICKS)
        } finally {
            context.input.releaseKey { it.keyUp }
        }

        val end = context.playerPosition()
        check(abs(end.z - start.z) < DRIFT) { "the player strafed around the husk above, from $start to $end" }
        check(end.x - start.x > 1.0) { "the player did not walk east, from $start to $end" }
    }

    private fun ClientGameTestContext.seesHusk() = fromClient { client ->
        val player = checkNotNull(client.player)
        val husk = checkNotNull(client.level).entitiesForRendering().first { it.type == EntityTypes.HUSK }
        canSeeBox(player.eyePosition, husk.boundingBox, range = Double.MAX_VALUE, wallsRange = 0.0)
    }

    private companion object {
        const val STILL = "{NoAI:1b,Silent:1b,PersistenceRequired:1b}"
        const val SETTLE_TICKS = 20
        const val SIGHT_TICKS = 80
        const val WALK_TICKS = 20
        const val DRIFT = 0.25
        const val WALL_END_X = 2.0
    }

}
