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
import net.ccbluex.gametest.harness.ServerGameTest
import net.ccbluex.gametest.harness.TestWorld
import net.ccbluex.gametest.harness.action.awaitServer
import net.ccbluex.gametest.harness.action.fill
import net.ccbluex.gametest.harness.action.give
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.ModuleAntiVoid
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.mode.AntiVoidGhostBlockMode
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleClutch
import net.ccbluex.liquidbounce.gametest.awaitServerAt
import net.ccbluex.liquidbounce.gametest.carried
import net.ccbluex.liquidbounce.gametest.harness.enable
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3

// Not the platform's stone, so that the blocks of the catch can be told apart from it
private const val BLOCK = "minecraft:oak_planks"
private const val STONE = "minecraft:stone"
private const val ICE = "minecraft:packed_ice"
private const val BLOCKS = 32
private const val EMPTY_SLOT = 8

// No command hits like a Knockback sword does. An arrow out of this bow throws as far as a sprinting hit with one
private const val PUNCH_BOW =
    """{id:"minecraft:bow",count:1,components:{"minecraft:enchantments":{"minecraft:punch":3}}}"""
private const val ARROW_SPEED = 1.5
private const val COMBO = 3

// A block the server refused is gone again well within this
private const val SETTLE_TICKS = 20

// The platform of the void world is 9 by 9 blocks with its top at y 64. It reaches from -4 to 5, and a player more
// than 0.3 further out has none of it below
private const val PLATFORM_Y = 64.0
private const val EDGE = 5.0
private const val OFF_PLATFORM = 5.3
private const val OPPOSITE_EDGE = -4.0

// A plain hit throws the player not even two blocks past the edge
private const val FAR = 4.0

/**
 * [ModuleClutch] has to keep a player on a platform over the void from falling, with blocks the server accepts and
 * without a move the anticheat objects to. The player leaves the platform in five ways: a hit throws it over the
 * edge, an arrow throws it blocks further than that, a hit follows every time it comes down, it slides over the edge
 * on the ground, and it steps off where only a wall below is left to build against. [ModuleAntiVoid] runs along with
 * its ghost blocks, which would make the fall look like a landing and leave the player standing on nothing.
 */
class ClutchGameTest : ServerGameTest(TestWorld.VOID, {
    give(BLOCK, BLOCKS)
    awaitClient("the blocks") { checkNotNull(it.player).inventory.countItem(Items.OAK_PLANKS) == BLOCKS }
    // The blocks are not in the hand, so the catch has to take them out of the hotbar itself
    client { checkNotNull(it.player).inventory.selectedSlot = EMPTY_SLOT }
    enable(ModuleClutch)
    client { ModuleAntiVoid.mode.setByString(AntiVoidGhostBlockMode.tag) }
    enable(ModuleAntiVoid)

    var blocks = BLOCKS
    fun placed() = carried(BLOCK).let { left -> (left < blocks).also { blocks = left } }

    travel(BlockPos(4, 64, 0))
    hit(Direction.EAST)
    val thrown = awaitStop()
    check(thrown.x >= OFF_PLATFORM && thrown.y == PLATFORM_Y) { "thrown off the platform, the player is at $thrown" }
    check(placed()) { "the server placed no block under the player that was thrown off" }

    // Only a row of blocks that keeps up with the player gets under where this comes down
    travel(BlockPos(0, 64, -4))
    shoot(Direction.NORTH)
    val shot = awaitStop()
    check(shot.z <= OPPOSITE_EDGE - FAR && shot.y == PLATFORM_Y) { "shot off the platform, the player is at $shot" }
    check(placed()) { "the server placed no block under the player that was shot off" }

    // From the second hit on the player is thrown from the blocks it was caught with
    travel(BlockPos(-4, 64, 0))
    repeat(COMBO) {
        hit(Direction.WEST)
        awaitClient("the player to come down") { checkNotNull(it.player).onGround() }
    }
    val comboed = awaitStop()
    check(comboed.x <= OPPOSITE_EDGE - FAR && comboed.y == PLATFORM_Y) {
        "hit $COMBO times in a row, the player is at $comboed"
    }
    check(placed()) { "the server placed no block under the player that was hit $COMBO times" }

    // Here the same hit only throws the player up to the edge, but on ice it slides on and over it
    fill(BlockPos(0, 63, -3), BlockPos(4, 63, -3), ICE)
    awaitServer("block 4 63 -3 $ICE")
    travel(BlockPos(1, 64, -3))
    hit(Direction.EAST)
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

/** A hit like a player's that throws the player [towards]. */
private fun GameTestScope.hit(towards: Direction) {
    val from = client { checkNotNull(it.player).position() }.relative(towards.opposite, 1.0)
    run("damage $playerName 1 minecraft:player_attack at ${from.x} ${from.y} ${from.z}")
    awaitClient("the hit") { !checkNotNull(it.player).onGround() }
}

/** An arrow out of the [PUNCH_BOW] that throws the player [towards]. */
private fun GameTestScope.shoot(towards: Direction) {
    val at = client { checkNotNull(it.player).position() }
    val from = at.relative(towards.opposite, ARROW_SPEED).add(0.0, 1.0, 0.0)
    val motion = "[${towards.stepX * ARROW_SPEED}d,0.0d,${towards.stepZ * ARROW_SPEED}d]"
    run("summon minecraft:arrow ${from.x} ${from.y} ${from.z} {Motion:$motion,pickup:0b,weapon:$PUNCH_BOW}")
    awaitClient("the arrow") { !checkNotNull(it.player).onGround() }
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
