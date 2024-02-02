/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.toBlockPos
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.combat.ClickScheduler
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.combat.attack
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.blockVecPosition
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.ccbluex.liquidbounce.utils.math.toVec3
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.ccbluex.liquidbounce.utils.math.toVec3i
import net.minecraft.entity.LivingEntity
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.registry.tag.BlockTags
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import net.minecraft.world.RaycastContext
import kotlin.concurrent.thread
import kotlin.math.roundToInt

object ModuleTpAura : Module("TpAura", Category.COMBAT) {

    private val clickScheduler = tree(ClickScheduler(this, true))
    private val attackRange by float("AttackRange", 4.2f, 3f..5f)

    private val maximumDistance by int("MaximumDistance", 95, 50..250)
    private val maximumCost by int("MaximumCost", 250, 50..500)
    private val tickDistance by int("TickDistance", 5, 1..7)

    private val targetTracker = tree(TargetTracker())

    private var pathCache: PathCache? = null
    private var pathFinderThread: Thread? = null
    private var playerPosition: Vec3d? = null

    private val stuckChronometer = Chronometer()

    override fun enable() {
        pathFinderThread = thread {
            while (enabled) {
                runCatching {
                    val playerPosition = player.pos

                    val enemies = targetTracker.enemies()
                        .sortedBy { it.squaredBoxedDistanceTo(playerPosition) }

                    for (enemy in enemies) {
                        if (player.distanceTo(enemy) > maximumDistance) {
                            continue
                        }

                        val path = AStarPathFinder.findPath(playerPosition.toVec3i(),
                            enemy.blockVecPosition, maximumCost)

                        // Skip if the path is empty
                        if (path.isEmpty()) {
                            continue
                        }

                        pathCache = PathCache(enemy, path)
                    }
                }

                Thread.sleep(50)
            }
        }

        super.enable()
    }

    override fun disable() {
        pathFinderThread = null
        super.disable()
    }

    val attackRepeatable = repeatable {
        val position = playerPosition ?: player.pos

        clickScheduler.clicks {
            val enemy = targetTracker.enemies()
                .filter { it.squaredBoxedDistanceTo(position) <= attackRange * attackRange }
                .minByOrNull { it.hurtTime } ?: return@clicks false

            enemy.attack(true, keepSprint = true)
            true
        }
    }

    private var isCurrentlyTeleporting = false

    val repeatable = repeatable {
        val (_, path) = pathCache ?: return@repeatable

        if (!stuckChronometer.hasElapsed(1000) || isCurrentlyTeleporting) {
            return@repeatable
        }

        // If the scheduler is not going to click, we should not teleport
        if (!clickScheduler.goingToClick) {
            pathCache = null
            return@repeatable
        }

        isCurrentlyTeleporting = true

        travel(path)
        waitUntil { !clickScheduler.goingToClick }
        travel(path.reversed())

        isCurrentlyTeleporting = false
    }

    val packetHandler = handler<PacketEvent> {
        val packet = it.packet

        if (packet is PlayerMoveC2SPacket) {
            val position = playerPosition ?: return@handler

            // Set the packet position to the player position
            packet.x = position.x
            packet.y = position.y + (0.0f..0.2f).random()
            packet.z = position.z
            packet.changePosition = true

        } else if (packet is PlayerPositionLookS2CPacket) {
            chat("TpAura: Detected a position packet, we are stuck. Resetting..")
            stuckChronometer.reset()
            pathCache = null
            playerPosition = null
        }
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack
        val (_, path) = pathCache ?: return@handler

        renderEnvironmentForWorld(matrixStack) {
            val start = path.first().toVec3d().add(0.5, 0.5, 0.5)
            val end = path.last().toVec3d().add(0.5, 0.5, 0.5)

            withColor(Color4b.WHITE) {
                drawLineStrip(*path.map { it.toVec3d().add(0.5, 0.5, 0.5).toVec3() }.toTypedArray())
            }

            withColor(Color4b.RED) {
                withPosition(start.toVec3()) {
                    drawSolidBox(Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0))
                }
            }

            withColor(Color4b.GREEN) {
                withPosition(end.toVec3()) {
                    drawSolidBox(Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0))
                }
            }

            playerPosition?.let { playerPosition ->
                withColor(Color4b.BLUE) {
                    withPosition(playerPosition.toVec3()) {
                        drawSolidBox(Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0))
                    }
                }

            }
        }
    }

    private suspend fun Sequence<*>.travel(path: List<Vec3i>) {
        // Currently path is a list of positions we need to go one by one, however we can split it into chunks
        // to use less packets and teleport more efficiently.
        // However, we cannot teleport if there are blocks in the way, so we need to check if the path is clear.
        val pathChunks = path.chunked(tickDistance)

        for (chunk in pathChunks) {
            // Check if the path is clear, this can be done by raycasting the start and end position of the chunk.
            val start = chunk.first().toVec3d().add(0.5, 0.5, 0.5)
            val end = chunk.last().toVec3d().add(0.5, 0.5, 0.5)

            if (world.raycast(RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE, player)).type != HitResult.Type.MISS) {
                // If the path is not clear, we need to go one by one.
                for (position in chunk) {
                    network.sendPacket(PositionAndOnGround(position.x + 0.5,
                        position.y + 0.01, position.z + 0.5, false))
                    playerPosition = position.toVec3d()
                }
                waitTicks(1)
                continue
            }

            // If the path is clear, we can teleport to the last position of the chunk.
            network.sendPacket(PositionAndOnGround(end.x, end.y, end.z, false))
            playerPosition = end

            waitTicks(1)
        }
    }

    data class PathCache(val enemy: LivingEntity, val path: List<Vec3i>)

    data class Node(val position: Vec3i, var parent: Node? = null) {
        var g = 0
        var h = 0
        var f = 0
    }

    object AStarPathFinder {
        fun findPath(start: Vec3i, end: Vec3i, maxCost: Int, maxIterations: Int = 500): List<Vec3i> {
            val openList = mutableListOf<Node>()
            val closedList = mutableListOf<Node>()
            val startNode = Node(start)
            val endNode = Node(end)

            openList.add(startNode)

            var iterations = 0
            while (openList.isNotEmpty()) {
                iterations++
                if (iterations > maxIterations) {
                    break
                }

                val currentNode = openList.minByOrNull { it.f } ?: break
                openList.remove(currentNode)
                closedList.add(currentNode)

                if (currentNode.position.isWithinDistance(endNode.position, 2.0)) {
                    return constructPath(currentNode)
                }

                val adjacentNodes = getAdjacentNodes(currentNode)
                for (node in adjacentNodes) {
                    if (node in closedList || !isPassable(node.position)) continue

                    val tentativeG = currentNode.g + distanceBetween(currentNode.position, node.position)
                    if (tentativeG < node.g || node !in openList) {
                        if (tentativeG > maxCost) continue // Skip this node if the cost exceeds the maximum

                        node.parent = currentNode
                        node.g = tentativeG
                        node.h = distanceBetween(node.position, endNode.position)
                        node.f = node.g + node.h

                        if (node !in openList) {
                            openList.add(node)
                        }
                    }
                }
            }

            return emptyList() // Return an empty list if no path was found
        }

        private fun constructPath(node: Node): List<Vec3i> {
            val path = mutableListOf<Vec3i>()
            var currentNode = node
            while (currentNode.parent != null) {
                path.add(0, currentNode.position)
                currentNode = currentNode.parent!!
            }
            return path
        }

        private fun getAdjacentNodes(node: Node): List<Node> {
            val adjacentNodes = mutableListOf<Node>()

            val directions = listOf(
                Vec3i(-1, 0, 0), // left
                Vec3i(1, 0, 0), // right
                Vec3i(0, -1, 0), // down
                Vec3i(0, 1, 0), // up
                Vec3i(0, 0, -1), // front
                Vec3i(0, 0, 1) // back
            )

            for (direction in directions) {
                val adjacentPosition = Vec3i(node.position.x + direction.x, node.position.y + direction.y,
                    node.position.z + direction.z)
                if (isPassable(adjacentPosition)) {
                    adjacentNodes.add(Node(adjacentPosition, node))
                }
            }

//            val diagonalDirections = listOf(
//                Vec3i(-1, 0, -1), // left front
//                Vec3i(1, 0, -1), // right front
//                Vec3i(-1, 0, 1), // left back
//                Vec3i(1, 0, 1) // right back
//            )
//
//            for (direction in diagonalDirections) {
//                val adjacentPosition = Vec3i(node.position.x + direction.x, node.position.y + direction.y,
//                    node.position.z + direction.z)
//                val intermediatePosition1 = Vec3i(node.position.x + direction.x, node.position.y, node.position.z)
//                val intermediatePosition2 = Vec3i(node.position.x, node.position.y, node.position.z + direction.z)
//                if (isPassable(adjacentPosition) && isPassable(intermediatePosition1)
//                    && isPassable(intermediatePosition2)) {
//                    adjacentNodes.add(Node(adjacentPosition, node))
//                }
//            }

            return adjacentNodes
        }

        private fun isPassable(position: Vec3i): Boolean {
            val blockPos = position.toBlockPos()

            val blockStates = arrayOf(
                blockPos.getState(),
                blockPos.up().getState(),
            )

            return blockStates.all { it == null || it.isAir || it.isIn(BlockTags.FIRE) || it.isIn(BlockTags.CLIMBABLE) }
        }

        private fun distanceBetween(a: Vec3i, b: Vec3i) = a.getSquaredDistance(b).roundToInt()
    }


}
