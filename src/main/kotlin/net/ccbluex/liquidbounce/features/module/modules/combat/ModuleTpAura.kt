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
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.combat.ClickScheduler
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.combat.attack
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.blockVecPosition
import net.ccbluex.liquidbounce.utils.math.toBlockPos
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

    val clickScheduler = tree(ClickScheduler(this, true))
    val teleportCooldown by int("Delay", 4, 1..20, "ticks")
    val range by float("Range", 4.2f, 3f..5f)

    /**
     * Maximum distance we allow our TP Aura to travel out of our player position.
     * This is to prevent the player from teleporting too far away from the player and not being able to go back.
     */
    val maximumDistance by int("MaximumDistance", 95, 50..250, "blocks")

    /**
     * Maximum cost
     *
     * If the cost of the path exceeds this value, the path will be skipped.
     */
    val maximumCost by int("MaximumCost", 250, 50..500)

    /**
     * Maximum distance per TP
     */
    val maximumDistancePerTp by int("MaximumDistancePerTp", 3, 1..5, "blocks")

    val targetTracker = tree(TargetTracker())

    private var pathCache: PathCache? = null
    private var pathFinderThread: Thread? = null

    private var playerPosition: Vec3d? = null

    override fun enable() {
        pathFinderThread = thread {
            while (enabled) {
                runCatching {
                    val playerPosition = playerPosition ?: player.pos
                    val enemies = targetTracker.enemies()
                        .sortedBy { it.squaredBoxedDistanceTo(playerPosition) }

                    for (enemy in enemies) {
                        if (player.distanceTo(enemy) > maximumDistance) {
                            continue
                        }

                        val path = AStarPathFinder.findPath(playerPosition.toVec3i(),
                            enemy.blockVecPosition, maximumCost)
                        val playerPath = AStarPathFinder.findPath(enemy.blockVecPosition,
                            player.blockVecPosition, maximumCost)

                        // Skip if the path is empty
                        if (path.isEmpty()) {
                            continue
                        }

                        pathCache = PathCache(enemy, path, playerPath)
                    }
                }

                Thread.sleep(50)
            }
        }

        super.enable()
    }

    override fun disable() {
        pathFinderThread = null

        // TODO: Teleport back to the player
        player.setPosition(playerPosition ?: return)
        super.disable()
    }

    /**
     * Our attack repeatable acts as very simplified KillAura.
     *
     * It will attack the closest enemy within the range of the player position.
     * This allows to attack enemies during our stay at the position and not just when we are teleporting.
     */
    val attackRepeatable = repeatable {
        val position = playerPosition ?: player.pos

        clickScheduler.clicks {
            val enemy = targetTracker.enemies()
                .filter { it.squaredBoxedDistanceTo(position) <= range * range }
                .minByOrNull { it.hurtTime } ?: return@clicks false

            enemy.attack(true, keepSprint = true)
            true
        }
    }

    val repeatable = repeatable {
        val (_, path, playerPath) = pathCache ?: return@repeatable

        // If the scheduler is not going to click, we should not teleport and wait for the next tick.
        if (!clickScheduler.goingToClick) {
            // TODO: Teleport back to the player
            return@repeatable
        }

        // Currently path is a list of positions we need to go one by one, however we can split it into chunks
        // to use less packets and teleport more efficiently.
        // However, we cannot teleport if there are blocks in the way, so we need to check if the path is clear.
        val pathChunks = path.chunked(maximumDistancePerTp)

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
                continue
            }

            // If the path is clear, we can teleport to the last position of the chunk.
            network.sendPacket(PositionAndOnGround(end.x, end.y, end.z, false))
            playerPosition = end
        }
        pathCache = null

        waitTicks(teleportCooldown)
    }

    val packetHandler = handler<PacketEvent> {
        val packet = it.packet

        if (packet is PlayerMoveC2SPacket) {
            val position = playerPosition ?: return@handler

            packet.x = position.x + 0.5
            packet.y = position.y + 0.01
            packet.z = position.z + 0.5
            packet.changePosition = true
        } else if (packet is PlayerPositionLookS2CPacket) {
            chat("TpAura: Detected server position update packet, updating player position.")
            playerPosition = Vec3d(packet.x, packet.y, packet.z)
            it.cancelEvent()
        }
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack
        val (entity, path) = pathCache ?: return@handler

        renderEnvironmentForWorld(matrixStack) {
            val color = if (entity == player) {
                Color4b.GREEN
            } else {
                Color4b.WHITE
            }

            withColor(color) {
                drawLineStrip(*path.map { it.toBlockPos().toCenterPos().toVec3() }.toTypedArray())
                playerPosition?.let {
                    withPosition(it.toBlockPos().toCenterPos().toVec3()) {
                        drawSolidBox(Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0))
                    }
                }
            }
        }
    }

    data class PathCache(val enemy: LivingEntity, val pathToEnemy: List<Vec3i>,
                         val pathFromEnemyToPlayer: List<Vec3i>)

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

                if (currentNode.position.isWithinDistance(endNode.position, range.toDouble())) {
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

            val diagonalDirections = listOf(
                Vec3i(-1, 0, -1), // left front
                Vec3i(1, 0, -1), // right front
                Vec3i(-1, 0, 1), // left back
                Vec3i(1, 0, 1) // right back
            )

            for (direction in diagonalDirections) {
                val adjacentPosition = Vec3i(node.position.x + direction.x, node.position.y + direction.y,
                    node.position.z + direction.z)
                val intermediatePosition1 = Vec3i(node.position.x + direction.x, node.position.y, node.position.z)
                val intermediatePosition2 = Vec3i(node.position.x, node.position.y, node.position.z + direction.z)
                if (isPassable(adjacentPosition) && isPassable(intermediatePosition1)
                    && isPassable(intermediatePosition2)) {
                    adjacentNodes.add(Node(adjacentPosition, node))
                }
            }

            return adjacentNodes
        }

        private fun isPassable(position: Vec3i): Boolean {
            val blockPos = position.toBlockPos()

            // Because the player box is 2 blocks height, we need both of them.
            val blockStates = arrayOf(
                blockPos.getState(),
                blockPos.up().getState()
            )

            return blockStates.all { it?.isAir == true || it?.isIn(BlockTags.LEAVES) == true ||
                it?.isIn(BlockTags.FLOWERS) == true || it?.isIn(BlockTags.CROPS) == true ||
                it?.isIn(BlockTags.SAPLINGS) == true || it?.isIn(BlockTags.PORTALS) == true ||
                it?.isIn(BlockTags.FIRE) == true
            }
        }

        private fun distanceBetween(a: Vec3i, b: Vec3i) = a.getSquaredDistance(b).roundToInt()
    }


}
