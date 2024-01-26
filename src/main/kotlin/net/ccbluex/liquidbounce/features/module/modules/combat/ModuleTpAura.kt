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

import net.ccbluex.liquidbounce.event.events.NotificationEvent
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
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.combat.ClickScheduler
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.combat.attack
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.blockVecPosition
import net.ccbluex.liquidbounce.utils.math.toVec3
import net.minecraft.entity.Entity
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.registry.tag.BlockTags
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3i
import kotlin.concurrent.thread
import kotlin.math.roundToInt

object ModuleTpAura : Module("TpAura", Category.COMBAT) {

    val clickScheduler = tree(ClickScheduler(this, true))
    val teleportCooldown by int("Delay", 4, 1..20, "ticks")
    val range by float("Range", 4.2f, 3f..5f)
    val atOnce by int("AtOnce", 5, 0..10)

    val targetTracker = tree(TargetTracker())

    private val pathCache = mutableMapOf<Entity, List<Vec3i>>()
    private var pathFinderThread: Thread? = null
    private var spoofPosition: Vec3i? = null

    override fun enable() {
        pathFinderThread = thread {
            while (enabled) {
                if (spoofPosition != null) {
                    Thread.sleep(50)
                    continue
                }

                runCatching {
                    pathCache.clear()

                    var playerPosition = player.blockVecPosition

                    val enemies = targetTracker.enemies().take(atOnce).toMutableList()

                    // Sort them by the distance to each other, so go with the entity that is the closest to the player
                    // but after that from this enemy find the closest to the next one
                    while (enemies.isNotEmpty()) {
                        val enemy = enemies.removeAt(0)

                        val path = AStarPathFinder.findPath(playerPosition, enemy.blockVecPosition, 250)
                        pathCache[enemy] = path

                        if (path.isEmpty()) {
                            continue
                        }

                        enemies.sortBy { it.squaredBoxedDistanceTo(enemy) }

                        // Update to the newest player position
                        playerPosition = path.last()
                    }

                    pathCache[player] = AStarPathFinder.findPath(playerPosition, player.blockVecPosition, 500)
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
        val position = spoofPosition ?: player.blockVecPosition

        clickScheduler.clicks {
            val enemy = targetTracker.enemies()
                .filter { it.blockVecPosition.isWithinDistance(position, range.toDouble()) }
                .minByOrNull { it.hurtTime } ?: return@clicks false

            spoofPosition = enemy.blockVecPosition
            network.sendPacket(PositionAndOnGround(enemy.x, enemy.y, enemy.z, false))

            chat("Hit ${enemy.nameForScoreboard} ${enemy.hurtTime} ${enemy.blockVecPosition.getSquaredDistance(position)}")
            enemy.attack(true, keepSprint = true)

            true
        }

    }

    val repeatable = repeatable {
        for ((entity, path) in pathCache.iterator()) {
            for (position in path) {
                spoofPosition = position
                network.sendPacket(PositionAndOnGround(position.x + 0.5, position.y + 0.01, position.z + 0.5, false))
            }

            chat("Sitting at ${entity.nameForScoreboard}")
            waitTicks(teleportCooldown)
        }

        spoofPosition = null
    }

    val packetHandler = handler<PacketEvent> {
        val packet = it.packet

        if (packet is PlayerMoveC2SPacket) {
            val position = spoofPosition ?: return@handler

            packet.x = position.x + 0.5
            packet.y = position.y + 0.01
            packet.z = position.z + 0.5
            packet.changePosition = true
        } else if (packet is PlayerPositionLookS2CPacket) {
            it.cancelEvent()
        }
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack


        renderEnvironmentForWorld(matrixStack) {
            for ((entity, path) in pathCache) {
                val color = if (entity == player) {
                    Color4b.GREEN
                } else {
                    Color4b.WHITE
                }

                withColor(color) {
                    drawLineStrip(*path.map { it.toBlockPos().toCenterPos().toVec3() }.toTypedArray())
                    spoofPosition?.let {
                        withPosition(it.toBlockPos().toCenterPos().toVec3()) {
                            drawSolidBox(Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0))
                        }
                    }
                }

            }

        }
    }

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
                val adjacentPosition = Vec3i(node.position.x + direction.x, node.position.y + direction.y, node.position.z + direction.z)
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
                val adjacentPosition = Vec3i(node.position.x + direction.x, node.position.y + direction.y, node.position.z + direction.z)
                val intermediatePosition1 = Vec3i(node.position.x + direction.x, node.position.y, node.position.z)
                val intermediatePosition2 = Vec3i(node.position.x, node.position.y, node.position.z + direction.z)
                if (isPassable(adjacentPosition) && isPassable(intermediatePosition1) && isPassable(intermediatePosition2)) {
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

            return blockStates.all { it?.isAir == true || it?.isIn(BlockTags.LEAVES) == true || it?.isIn(BlockTags.FLOWERS) == true || it?.isIn(BlockTags.CROPS) == true || it?.isIn(BlockTags.SAPLINGS) == true || it?.isIn(BlockTags.PORTALS) == true || it?.isIn(BlockTags.FIRE) == true }
        }

        private fun distanceBetween(a: Vec3i, b: Vec3i) = a.getSquaredDistance(b).roundToInt()
    }


}
