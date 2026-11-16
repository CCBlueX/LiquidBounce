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
import net.ccbluex.gametest.harness.action.give
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoWalk
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.gametest.command
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.playerPosition
import net.ccbluex.liquidbounce.gametest.restoring
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

private const val CROSS_TICKS = 400
private const val ARRIVAL = 1.1

/**
 * `.goto` crosses 8 blocks of void from the start platform to one at its own height while Scaffold places the
 * bridge, with AutoWalk not allowed to place blocks itself.
 */
class AutoWalkBridgeGameTest : SingleplayerGameTest(TestWorld.VOID, {
    context.restoring(ModuleAutoWalk, ModuleScaffold) {
        val y = origin.y
        fill(BlockPos(13, y - 1, -1), BlockPos(15, y - 1, 1), "minecraft:stone")
        awaitServer("block 15 ${y - 1} 1 minecraft:stone")
        give("minecraft:cobblestone", 64)
        moveTo(Vec3(0.5, y.toDouble(), 0.5), yaw = -90f)

        context.onClient {
            ModuleAutoWalk.containedValues.first { it.name == "AllowPlace" }.setByString("false")
            ModuleScaffold.enabled = true
        }
        val destination = Vec3(14.5, y.toDouble(), 0.5)
        unhurt("Bridging to the platform") {
            context.command("goto 14 $y 0")
            awaitPlayer(context, CROSS_TICKS, "Did not arrive on the platform") { !ModuleAutoWalk.enabled }
        }
        val position = context.playerPosition()
        check(position.distanceTo(destination) <= ARRIVAL) { "AutoWalk turned off at $position, not at $destination" }
    }
})
