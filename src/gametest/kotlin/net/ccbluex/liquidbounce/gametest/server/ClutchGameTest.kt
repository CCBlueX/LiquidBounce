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
package net.ccbluex.liquidbounce.gametest.server

import net.ccbluex.gametest.harness.GameTestScope
import net.ccbluex.gametest.harness.TestWorld
import net.ccbluex.gametest.harness.action.awaitServer
import net.ccbluex.gametest.harness.action.fill
import net.ccbluex.gametest.harness.action.give
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleClutch
import net.ccbluex.liquidbounce.gametest.awaitServerAt
import net.ccbluex.liquidbounce.gametest.carried
import net.ccbluex.liquidbounce.gametest.harness.LiquidBounceGameTest
import net.ccbluex.liquidbounce.gametest.harness.enable
import net.minecraft.core.BlockPos
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3

// Not the platform's stone, so that the blocks of the catch can be told apart from it
private const val BLOCK = "minecraft:oak_planks"
private const val STONE = "minecraft:stone"
private const val ICE = "minecraft:packed_ice"
private const val BLOCKS = 16
private const val EMPTY_SLOT = 8

// A block the server refused is gone again well within this
private const val SETTLE_TICKS = 20

// The platform of the void world is 9 by 9 blocks with its top at y 64. Its edges are at 5, and a player further out
// than 5.3 has none of it below
private const val PLATFORM_Y = 64.0
private const val EDGE = 5.0
private const val OFF_PLATFORM = 5.3

/**
 * [ModuleClutch] has to keep a player on a platform over the void from falling, with blocks the server accepts and
 * without a move the anticheat objects to. The player leaves the platform in three ways: a hit throws it over the
 * edge, it slides over the edge on the ground, and it steps off where only a wall below is left to build against.
 */
class ClutchGameTest : LiquidBounceGameTest(TestWorld.VOID, {
    give(BLOCK, BLOCKS)
    awaitClient("the blocks") { checkNotNull(it.player).inventory.countItem(Items.OAK_PLANKS) == BLOCKS }
    // The blocks are not in the hand, so the catch has to take them out of the hotbar itself
    client { checkNotNull(it.player).inventory.selectedSlot = EMPTY_SLOT }
    enable(ModuleClutch)

    var blocks = BLOCKS
    fun placed() = carried(BLOCK).let { left -> (left < blocks).also { blocks = left } }

    travel(BlockPos(4, 64, 0))
    hitFromWest()
    val thrown = awaitStop()
    check(thrown.x >= OFF_PLATFORM && thrown.y == PLATFORM_Y) { "thrown off the platform, the player is at $thrown" }
    check(placed()) { "the server placed no block under the player that was thrown off" }

    // Here the same hit only throws the player up to the edge, but on ice it slides on and over it
    fill(BlockPos(0, 63, -3), BlockPos(4, 63, -3), ICE)
    awaitServer("block 4 63 -3 $ICE")
    travel(BlockPos(1, 64, -3))
    hitFromWest()
    val slid = awaitStop()
    check(slid.x > EDGE && slid.y == PLATFORM_Y) { "sliding off the platform, the player is at $slid" }
    check(placed()) { "the server placed no block under the player that slid off" }

    // A wall of three blocks goes down from the south edge, and that far a block can still go against it
    fill(BlockPos(-4, 60, 4), BlockPos(4, 62, 4), STONE)
    awaitServer("block 0 60 4 $STONE")
    travel(BlockPos(0, 64, 3))
    input.holdKey { it.keyUp }
    awaitClient("the step off the platform") { !checkNotNull(it.player).onGround() }
    input.releaseKey { it.keyUp }
    val stepped = awaitStop()
    check(stepped.z >= OFF_PLATFORM && stepped.y in PLATFORM_Y - 3..PLATFORM_Y - 1) {
        "stepping off the platform, the player is at $stepped"
    }
    check(placed()) { "the server placed no block under the player that stepped off" }
})

/** A hit like a player's, from a block to the west. */
private fun GameTestScope.hitFromWest() {
    val at = client { checkNotNull(it.player).position() }
    run("damage $playerName 1 minecraft:player_attack at ${at.x - 1} ${at.y} ${at.z}")
    awaitClient("the hit") { !checkNotNull(it.player).onGround() }
}

/** Waits for the player to stand still, and returns where once the server has it standing there as well. */
private fun GameTestScope.awaitStop(): Vec3 {
    awaitClient("the player to stand still") {
        val player = checkNotNull(it.player)
        player.onGround() && player.deltaMovement.horizontalDistanceSqr() == 0.0
    }
    ticks(SETTLE_TICKS)

    val position = client { checkNotNull(it.player).position() }
    awaitServerAt(position)
    awaitServer("entity @a[name=$playerName,nbt={OnGround:1b}]")
    return position
}
