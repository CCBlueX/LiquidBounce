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

import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.player
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext
import kotlin.math.abs
import kotlin.math.hypot

private const val HEAD_CENTER = 5.0 / 6.0
private const val FEET_CENTER = 1.0 / 6.0
private const val HEIGHT_TOLERANCE = 0.05
private const val AIM_TICKS = 20

/**
 * With the center aim point mode, the server sees the player look at the center of a box part that is not exempt:
 * the one that takes the least rotation.
 */
class AimPointCenterGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.combatTest(ModuleKillAura) { server, y ->
        context.onClient {
            ModuleKillAura.set("AimPoint/Mode", "Center")
            ModuleKillAura.set("AimPoint/ExemptBoxParts", "Body,Feet")
        }
        val husk = summonHusk(context, server, y, 0.5, 3.0)
        context.onClient { ModuleKillAura.enabled = true }
        context.waitFor({ ModuleKillAura.targetTracker.target != null }, WAIT_TICKS)

        assertAimHeight(context, server, husk, HEAD_CENTER, "Head")

        context.onClient { ModuleKillAura.set("AimPoint/ExemptBoxParts", "Head,Body") }
        assertAimHeight(context, server, husk, FEET_CENTER, "Feet")

        // Looking at the feet, they take less rotation than the head
        context.onClient { ModuleKillAura.set("AimPoint/ExemptBoxParts", "Body") }
        assertAimHeight(context, server, husk, FEET_CENTER, "Head and feet from the feet")

        context.onClient { ModuleKillAura.enabled = false }
        server.runCommand("kill @e[type=husk]")
    }

    /**
     * Checks at which share of the height of the husk the look of the server-side player passes its center.
     */
    private fun assertAimHeight(
        context: ClientGameTestContext,
        server: TestServerContext,
        husk: Int,
        expected: Double,
        case: String,
    ) {
        context.waitTicks(AIM_TICKS)
        val share = server.player { player ->
            val box = checkNotNull(player.level().getEntity(husk)).boundingBox
            val eyes = player.eyePosition
            val look = player.lookAngle
            val distance = hypot(box.center.x - eyes.x, box.center.z - eyes.z) / hypot(look.x, look.z)
            (eyes.y + look.y * distance - box.minY) / box.ysize
        }
        check(abs(share - expected) < HEIGHT_TOLERANCE) { "$case: aimed at $share of the height, not $expected" }
    }

}
