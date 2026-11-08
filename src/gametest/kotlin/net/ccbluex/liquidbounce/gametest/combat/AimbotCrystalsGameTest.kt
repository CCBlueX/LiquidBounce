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
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import kotlin.math.abs

/** Every point of the crystal lies beyond this yaw, every point of the husk before it. */
private const val CRYSTAL_YAW = -10f

/** Looks past the husk, on the side away from the crystal. */
private const val AWAY_YAW = 20f
private const val ANGLE_TOLERANCE = 0.5f

/**
 * With a husk straight ahead and a crystal to the left of it, the aimbot turns to the crystal first, to the husk
 * while crystals wait for the use key, and to nothing when crystals are all it may aim at.
 */
class AimbotCrystalsGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.combatTest(ModuleAimbot) { server, y ->
        summonCrystal(context, server, y, 2.5, 3.5)
        summonHusk(context, server, y, 0.5, 3.0)

        context.onClient {
            ModuleAimbot.set("Crystals/Enabled", "true")
            ModuleAimbot.enabled = true
        }
        context.waitFor({ client -> checkNotNull(client.player).yRot < CRYSTAL_YAW }, WAIT_TICKS)

        checkOnlyWhileUsing(context, server, y)
        checkOnly(context, server, y)
    }

    private fun checkOnlyWhileUsing(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        lookAway(context, server, y)
        context.onClient {
            ModuleAimbot.set("Crystals/OnlyWhileUsing", "true")
            ModuleAimbot.enabled = true
        }
        context.waitFor({ client -> checkNotNull(client.player).yRot < AWAY_YAW / 2 }, WAIT_TICKS)
        context.waitTicks(MEASURE_TICKS / 2)
        val yaw = context.fromClient { checkNotNull(it.player).yRot }
        check(yaw > CRYSTAL_YAW) { "Turned to the crystal at $yaw while use was not held" }

        context.input.holdKey { it.keyUse }
        try {
            context.waitFor({ client -> checkNotNull(client.player).yRot < CRYSTAL_YAW }, WAIT_TICKS)
        } finally {
            context.input.releaseKey { it.keyUse }
        }
    }

    /**
     * Use is not held, so there is no crystal to aim at either.
     */
    private fun checkOnly(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        lookAway(context, server, y)
        context.onClient {
            ModuleAimbot.set("Crystals/Only", "true")
            ModuleAimbot.enabled = true
        }
        context.waitTicks(MEASURE_TICKS / 2)
        val yaw = context.fromClient { checkNotNull(it.player).yRot }
        check(abs(yaw - AWAY_YAW) < ANGLE_TOLERANCE) { "Turned from $AWAY_YAW to $yaw with only crystals to aim at" }
    }

    private fun lookAway(context: ClientGameTestContext, server: TestServerContext, y: Int) {
        context.onClient { ModuleAimbot.enabled = false }
        face(context, server, y, AWAY_YAW)
    }

}
