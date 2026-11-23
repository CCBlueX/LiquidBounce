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

import net.ccbluex.gametest.harness.SingleplayerGameTest
import net.ccbluex.gametest.harness.TestWorld
import net.ccbluex.gametest.harness.action.awaitServer
import net.ccbluex.gametest.harness.action.fill
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoWalk
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.restoring
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

private const val GOTO_TICKS = 600

// AutoWalk stops within a block of the destination; Fly hops once more before the position is read
private const val ARRIVAL = 1.5

/**
 * `.goto` with the CubeCraft-2024-03-10 mode of Fly, which hops by itself, gets down to a platform three blocks lower
 * and twenty blocks away over the void.
 */
class FlyLandingGameTest : SingleplayerGameTest(TestWorld.VOID, {
    context.restoring(ModuleAutoWalk, ModuleFly) {
        val y = origin.y
        run("gamemode survival $playerName")
        fill(BlockPos(19, y - 4, -1), BlockPos(21, y - 4, 1), "minecraft:stone")
        awaitServer("block 21 ${y - 4} 1 minecraft:stone")
        moveTo(Vec3(0.5, y.toDouble(), 0.5), yaw = -90f)

        context.onClient {
            ModuleFly.containedValues.first { it.name == "Mode" }.setByString("CubeCraft-2024-03-10")
            ModuleFly.enabled = true
        }
        goto(context, Vec3(20.5, y - 3.0, 0.5), "down to the platform", GOTO_TICKS, ARRIVAL) { !it.onGround() }
    }
})
