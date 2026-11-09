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

import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAimbot
import net.ccbluex.liquidbounce.gametest.onClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.util.Mth
import kotlin.math.abs

private const val SIDE_YAW = 90f
private const val AIMED_YAW = 10f

/**
 * Without a wall range the aimbot leaves a husk behind a wall alone, with one it turns towards it.
 */
class AimbotWallRangeGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.combatTest(ModuleAimbot) { server, y ->
        server.runCommand("fill -2 $y 2 2 ${y + 3} 2 stone")
        val husk = summonHusk(context, server, y, 0.5, 3.5)
        face(context, server, y, SIDE_YAW)

        context.onClient {
            ModuleAimbot.set("WallRange", "0")
            ModuleAimbot.enabled = true
        }
        context.waitTicks(MEASURE_TICKS)
        context.onClient { client ->
            check(ModuleAimbot.targetTracker.target == null) { "Wall range 0: aimed at the husk behind the wall" }
            val yaw = Mth.wrapDegrees(checkNotNull(client.player).yRot)
            check(abs(yaw - SIDE_YAW) < 1f) { "Wall range 0: turned to $yaw" }

            ModuleAimbot.set("WallRange", "4")
        }
        context.waitFor({ client ->
            ModuleAimbot.targetTracker.target?.id == husk &&
                abs(Mth.wrapDegrees(checkNotNull(client.player).yRot)) < AIMED_YAW
        }, WAIT_TICKS)
    }

}
