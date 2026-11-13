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
import net.ccbluex.gametest.harness.action.awaitServer
import net.ccbluex.gametest.harness.action.fill
import net.ccbluex.gametest.harness.action.setBlock
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleParkour
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleSprint
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.math.yaw
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3

private const val HEIGHT = 24
private const val TIMEOUT = 600
private const val CARPET_HEIGHT = 1.0 / 16

/** How far off the next block a player looks, and how far they turn the mouse in a tick. */
private const val LOOK_OFF = 12f
private const val MAX_TURN = 18f

/** Along +X: a gap of two, three, a jump up, one down, and two blocks off to the side. */
private val COURSE = listOf(
    BlockPos(0, 0, 0), BlockPos(1, 0, 0), BlockPos(4, 0, 0), BlockPos(8, 0, 0), BlockPos(11, 1, 0),
    BlockPos(15, 0, 0), BlockPos(18, 0, 1), BlockPos(21, 0, 0), BlockPos(22, 0, 0),
)

/** Along +X: a drop of six onto slime, whose bounce gets up to a block three higher, out of reach of a jump. */
private val SLIME_COURSE = listOf(
    BlockPos(-3, 6, 0), BlockPos(-2, 6, 0), BlockPos(-1, 6, 0), BlockPos(0, 6, 0), BlockPos(4, 0, 0),
    BlockPos(8, 3, 0),
)
private val SLIME = BlockPos(4, 0, 0)

/**
 * Along +X: from the middle of a carpeted 1x1 pillar to the next one, one lower and one to the side, past a pot on a
 * post that stands on the straight line between them at feet height, looking at the next pillar.
 */
private val AROUND_COURSE = listOf(BlockPos(0, 0, 0), BlockPos(4, -1, 1))
private val POT = BlockPos(2, 1, 0)

/** Along +X: a drop of eight onto a bed, whose bounce gets up to a block two higher, out of reach of a jump. */
private val BED_COURSE = listOf(
    BlockPos(-3, 8, 0), BlockPos(-2, 8, 0), BlockPos(-1, 8, 0), BlockPos(0, 8, 0), BlockPos(4, 0, 0),
    BlockPos(9, 2, 0),
)
private val BED = BlockPos(4, 0, 0)

/**
 * A run-up along a wall at +Z and a gap past its end, so the way there slides along the wall. Then jumps up and down,
 * two turns and a jump into a corner.
 */
private val WALL_COURSE = listOf(
    BlockPos(0, 0, 0), BlockPos(1, 0, 0), BlockPos(2, 0, 0), BlockPos(3, 0, 0), BlockPos(5, 0, 1),
    BlockPos(8, 1, 1), BlockPos(9, 1, 1), BlockPos(9, 1, -2), BlockPos(9, 2, -5), BlockPos(9, 1, -8),
    BlockPos(9, 1, -9), BlockPos(9, 1, -10), BlockPos(9, 1, -12), BlockPos(11, 1, -12), BlockPos(12, 1, -12),
)
private val WALL = (0..3).flatMap { x -> (1..3).map { y -> BlockPos(x, y, 1) } } +
    listOf(BlockPos(9, 2, -13), BlockPos(9, 3, -13))

private const val WALL_OFFSET = 40

/**
 * Holding forward and sprint along a Jump 'n' Run course over a lake makes it to the end with Parkour on and falls in
 * without it, also when sprinting along a wall with the Sprint module and looking a little off the next block. With
 * Parkour, a drop onto slime or a bed bounces up to the next block too, and a pot in the way is jumped past.
 */
class ParkourAssistGameTest : ServerGameTest({
    try {
        val ground = origin.y
        val start = BlockPos(0, ground + HEIGHT, 0)
        val wallStart = start.offset(WALL_OFFSET, 0, 0)
        val slimeStart = BlockPos(0, ground + HEIGHT, 8)
        val aroundStart = BlockPos(0, ground + HEIGHT, -8)
        val bedStart = BlockPos(0, ground + HEIGHT, 16)
        run("effect give $playerName minecraft:saturation infinite 0 true")
        fill(BlockPos(-10, ground - 2, -20), BlockPos(WALL_OFFSET + 20, ground - 1, 20), "minecraft:water")
        for (block in COURSE) {
            setBlock(start.offset(block), "minecraft:stone")
        }
        for (block in WALL_COURSE + WALL) {
            setBlock(wallStart.offset(block), "minecraft:stone")
        }
        for (block in SLIME_COURSE) {
            setBlock(slimeStart.offset(block), if (block == SLIME) "minecraft:slime_block" else "minecraft:stone")
        }
        for (block in AROUND_COURSE) {
            fill(aroundStart.offset(block).below(4), aroundStart.offset(block), "minecraft:stone")
            setBlock(aroundStart.offset(block).above(), "minecraft:red_carpet")
        }
        fill(aroundStart.offset(POT).below(5), aroundStart.offset(POT).below(), "minecraft:stripped_oak_log")
        setBlock(aroundStart.offset(POT), "minecraft:mud_bricks")
        setBlock(aroundStart.offset(POT).above(), "minecraft:potted_poppy")
        for (block in BED_COURSE) {
            if (block != BED) {
                setBlock(bedStart.offset(block), "minecraft:stone")
            }
        }
        setBlock(bedStart.offset(BED), "minecraft:red_bed[part=foot,facing=east]")
        setBlock(bedStart.offset(BED).east(), "minecraft:red_bed[part=head,facing=east]")
        val last = bedStart.offset(BED_COURSE.last())
        awaitServer("block ${last.x} ${last.y} ${last.z} minecraft:stone")

        val unassisted = runCourse(start, COURSE, assist = false)
        check(unassisted != null) { "Without Parkour the course was run to the end" }

        val failure = runCourse(start, COURSE, assist = true)
        check(failure == null) { "With Parkour the run failed: $failure" }

        val wallFailure = runAlongWall(wallStart)
        check(wallFailure == null) { "With Parkour the run along the wall failed: $wallFailure" }

        val slimeFailure = runCourse(slimeStart, SLIME_COURSE, assist = true)
        check(slimeFailure == null) { "With Parkour the run over slime failed: $slimeFailure" }

        val aroundFailure = runCourse(aroundStart, AROUND_COURSE, assist = true, carpet = true, facesGoal = true)
        check(aroundFailure == null) { "With Parkour the run past the pot failed: $aroundFailure" }

        val bedFailure = runCourse(bedStart, BED_COURSE, assist = true)
        check(bedFailure == null) { "With Parkour the run over the bed failed: $bedFailure" }
    } finally {
        context.onClient { ModuleParkour.enabled = false }
    }
})

/**
 * Runs [course] from [origin] holding forward and sprint, looking towards its last block or along +X, on [carpet] on
 * every block of it; why it failed, or null.
 */
private fun GameTestScope.runCourse(
    origin: BlockPos,
    course: List<BlockPos>,
    assist: Boolean,
    carpet: Boolean = false,
    facesGoal: Boolean = false,
): String? {
    val goal = origin.offset(course.last()).above()
    val lowest = origin.y + course.minOf { it.y }

    val first = Vec3.atBottomCenterOf(origin.offset(course.first()).above())
    val yaw = if (facesGoal) Vec3.atBottomCenterOf(goal).subtract(first).yaw else -90f
    moveTo(if (carpet) first.add(0.0, CARPET_HEIGHT, 0.0) else first, yaw = yaw)
    context.onClient { ModuleParkour.enabled = assist }

    input.holdKey { it.keyUp }
    input.holdKey { it.keySprint }
    try {
        val title = if (assist) "With Parkour" else "Without Parkour"
        return runCourse(context, title, "ASSIST", lowest, TIMEOUT) { BlockPos.containing(it) == goal }
    } finally {
        input.releaseKey { it.keyUp }
        input.releaseKey { it.keySprint }
        context.onClient { ModuleParkour.enabled = false }
    }
}

/** Runs [WALL_COURSE] with Parkour and Sprint, facing the block after the one below; why it failed, or null. */
private fun GameTestScope.runAlongWall(origin: BlockPos): String? {
    val path = WALL_COURSE.map(origin::offset)
    val lowest = path.minOf { it.y }

    moveTo(Vec3.atBottomCenterOf(path.first().above()), yaw = -90f + LOOK_OFF)
    context.onClient {
        ModuleParkour.enabled = true
        ModuleSprint.enabled = true
    }

    var reached = 0
    input.holdKey { it.keyUp }
    try {
        for (tick in 0 until TIMEOUT) {
            val (position, onGround) = context.fromClient { mc ->
                val player = checkNotNull(mc.player)
                player.position() to player.onGround()
            }
            if (position.y < lowest) {
                return "fell at $position after $tick ticks"
            }

            val over = path.indexOfFirst {
                it.x == Mth.floor(position.x) && it.z == Mth.floor(position.z) && position.y > it.y
            }
            reached = maxOf(reached, over)
            if (onGround && reached == path.lastIndex) {
                logger.info("PARKOUR WALL: done in $tick ticks")
                return null
            }

            val next = path[minOf(reached + 1, path.lastIndex)]
            val look = Vec3.atBottomCenterOf(next).subtract(position).yaw + LOOK_OFF
            context.onClient { mc ->
                val player = checkNotNull(mc.player)
                val turn = Mth.clamp(Mth.wrapDegrees(look - player.yRot), -MAX_TURN, MAX_TURN)
                player.yRot += turn
            }
            context.waitTick()
        }
        return "not done within $TIMEOUT ticks, stopped at ${context.fromClient { checkNotNull(it.player).position() }}"
    } finally {
        input.releaseKey { it.keyUp }
        context.onClient {
            ModuleParkour.enabled = false
            ModuleSprint.enabled = false
        }
    }
}
