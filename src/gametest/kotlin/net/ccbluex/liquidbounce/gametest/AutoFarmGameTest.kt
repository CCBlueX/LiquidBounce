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

import net.ccbluex.gametest.harness.SingleplayerGameTest
import net.ccbluex.gametest.harness.action.awaitServer
import net.ccbluex.gametest.harness.action.fill
import net.ccbluex.gametest.harness.action.give
import net.ccbluex.gametest.harness.action.setBlock
import net.ccbluex.liquidbounce.features.module.modules.world.autofarm.AutoFarmAutoWalk
import net.ccbluex.liquidbounce.features.module.modules.world.autofarm.ModuleAutoFarm
import net.minecraft.core.BlockPos

/**
 * AutoFarm walks to a row of ripe wheat and carrots, harvests it, sows it again and picks up what it dropped.
 */
class AutoFarmGameTest : SingleplayerGameTest({
    val y = origin.y
    context.restoring(ModuleAutoFarm) {
        run("gamemode survival $playerName")
        run("clear $playerName")
        // Nothing grows while the test looks for the new crops
        run("gamerule random_tick_speed 0")
        fillArea(BlockPos(-8, y, -8), BlockPos(8, y + 4, 8), "minecraft:air")
        fill(BlockPos(-8, y - 1, -8), BlockPos(8, y - 1, 8), "minecraft:grass_block")
        setBlock(BlockPos(3, y - 1, 6), "minecraft:water")
        fill(BlockPos(-2, y - 1, 6), BlockPos(2, y - 1, 6), "minecraft:farmland")
        fill(BlockPos(-2, y, 6), BlockPos(0, y, 6), "minecraft:wheat[age=7]")
        fill(BlockPos(1, y, 6), BlockPos(2, y, 6), "minecraft:carrots[age=7]")
        awaitServer("block 2 $y 6 minecraft:carrots[age=7]")
        give("minecraft:wheat_seeds")
        travel(BlockPos(0, y, -5))
        context.onClient {
            AutoFarmAutoWalk.enabled = true
            ModuleAutoFarm.enabled = true
        }
        try {
            for (x in -2..2) awaitServer("block $x $y 6 #minecraft:crops[age=0]", 1200)
            var waited = 0
            while (carried("minecraft:wheat") < 3 && waited++ < 600) ticks(1)
            check(carried("minecraft:wheat") == 3) { "Picked up ${carried("minecraft:wheat")} wheat, not 3" }
            check(carried("minecraft:carrot") > 0) { "Picked up no carrots" }
        } finally {
            context.onClient { ModuleAutoFarm.enabled = false }
            run("gamerule random_tick_speed 3")
            fillArea(BlockPos(-8, y, -8), BlockPos(8, y + 4, 8), "minecraft:air")
        }
    }
})
