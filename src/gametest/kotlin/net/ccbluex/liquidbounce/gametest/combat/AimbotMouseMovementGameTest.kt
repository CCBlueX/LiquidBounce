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
import kotlin.math.abs

private const val OFF_YAW = 20f
private const val ANGLE_TOLERANCE = 0.5f

/**
 * With the MouseMovement requirement the view stays where it is until the mouse moves, then the aimbot turns it
 * to the husk.
 */
class AimbotMouseMovementGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.combatTest(ModuleAimbot) { server, y ->
        summonHusk(context, server, y, 0.5, 3.0)
        face(context, server, y, OFF_YAW)
        context.onClient {
            ModuleAimbot.set("Requires", "MouseMovement")
            ModuleAimbot.enabled = true
        }
        context.waitTicks(MEASURE_TICKS / 2)
        val idleYaw = context.fromClient { checkNotNull(it.player).yRot }
        check(abs(idleYaw - OFF_YAW) < ANGLE_TOLERANCE) { "Turned from $OFF_YAW to $idleYaw without mouse movement" }

        // The mouse turns away from the husk, so only the aimbot brings the view onto it
        repeat(MEASURE_TICKS) {
            context.input.moveCursor(1.0, 0.0)
            context.waitTick()
        }
        val yaw = context.fromClient { checkNotNull(it.player).yRot }
        check(abs(yaw) < OFF_YAW / 2) { "Moving the mouse only turned from $OFF_YAW to $yaw" }

        context.onClient { ModuleAimbot.enabled = false }
        server.runCommand("kill @e[type=husk]")
    }

}
