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

import net.ccbluex.gametest.harness.GameTestScope
import net.ccbluex.gametest.harness.ServerGameTest
import net.ccbluex.gametest.harness.SingleplayerGameTest
import net.ccbluex.gametest.harness.TestWorld
import net.ccbluex.gametest.harness.action.awaitServer
import net.ccbluex.gametest.harness.action.fill
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoWalk
import net.ccbluex.liquidbounce.gametest.command
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.playerPosition
import net.ccbluex.liquidbounce.gametest.restoring
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.vehicle.boat.AbstractBoat
import net.minecraft.world.item.Items
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

/**
 * `.goto` across a lake that falls off into the void on both sides: hungry, AutoWalk puts the boat it carries on the
 * water, rows across, takes the boat along again at the far shore and eats its steak on the way.
 */
class AutoWalkTravelGameTest : SingleplayerGameTest(TestWorld.VOID, travel)

/** The same crossing on the game test server, under its anticheats. */
class AutoWalkTravelServerGameTest : ServerGameTest(TestWorld.VOID, travel)

private val travel: GameTestScope.() -> Unit = {
    context.restoring(ModuleAutoWalk) {
        val y = origin.y
        fill(BlockPos(-4, y - 4, -8), BlockPos(40, y - 1, 8), "minecraft:stone")
        fill(BlockPos(6, y - 3, -8), BlockPos(26, y - 1, 8), "minecraft:water")
        awaitServer("block 26 ${y - 1} 8 minecraft:water")
        // Peaceful refills the bar
        run("difficulty easy")
        run("gamemode survival $playerName")
        run("clear $playerName")
        run("item replace entity $playerName inventory.0 with minecraft:oak_boat")
        run("item replace entity $playerName inventory.1 with minecraft:cooked_beef 2")
        moveTo(Vec3(1.5, y.toDouble(), 0.5), yaw = -90f)
        // Hunger at its strongest empties the bar within a few seconds, cleared once it is low enough
        run("effect give $playerName minecraft:hunger 10 255")
        awaitClient("the player to get hungry") { checkNotNull(it.player).foodData.foodLevel <= 6 }
        run("effect clear $playerName minecraft:hunger")
        val hunger = context.fromClient { checkNotNull(it.player).foodData.foodLevel }

        context.onClient {
            ModuleAutoWalk.containedValues.first { it.name == "AllowBoat" }.setByString("true")
            ModuleAutoWalk.containedValues.first { it.name == "AllowEat" }.setByString("true")
        }
        val destination = Vec3(34.5, y.toDouble(), 0.5)
        var rode = false
        context.command("goto 34 $y 0")
        await(context, 2400, "Did not get across the lake") {
            context.fromClient { mc ->
                rode = rode || checkNotNull(mc.player).isPassenger
                !ModuleAutoWalk.enabled
            }
        }
        val position = context.playerPosition()
        check(position.distanceTo(destination) <= 1.1) { "AutoWalk turned off at $position, not at $destination" }
        check(rode) { "Swam across the lake, never got into a boat" }
        val (boats, left, fed) = context.fromClient { mc ->
            val player = checkNotNull(mc.player)
            val area = AABB.ofSize(player.position(), 80.0, 20.0, 40.0)
            Triple(
                player.inventory.countItem(Items.OAK_BOAT),
                checkNotNull(mc.level).getEntitiesOfClass(AbstractBoat::class.java, area).size,
                player.foodData.foodLevel,
            )
        }
        check(left == 0) { "Left $left boats behind" }
        check(boats == 1) { "Carries $boats boats after the crossing, not the one it brought" }
        check(fed > hunger) { "Hunger went from $hunger to $fed on the way" }
    }
}
