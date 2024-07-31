package net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.modes

import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.ModuleTpAura
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.ModuleTpAura.clickScheduler
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.ModuleTpAura.desyncPlayerPosition
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.ModuleTpAura.stuckChronometer
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.ModuleTpAura.targetTracker
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.TpAuraChoice
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.toBlockPos
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.entity.blockVecPosition
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
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
import net.minecraft.util.math.Vec3i
import net.minecraft.world.RaycastContext
import kotlin.concurrent.thread
import kotlin.math.roundToInt

data class Node(val position: Vec3i, var parent: Node? = null) {
    var g = 0
    var h = 0
    var f = 0
}

object AStarMode : TpAuraChoice("AStar") {

    private val maximumDistance by int("MaximumDistance", 95, 50..250)
    private val maximumCost by int("MaximumCost", 250, 50..500)
    private val tickDistance by int("TickDistance", 3, 1..7)
    private val allowDiagonal by boolean("AllowDiagonal", false)

    private var pathCache: PathCache? = null
    private var pathFinderThread: Thread? = null

    val repeatable = repeatable {
        val (_, path) = pathCache ?: return@repeatable

        if (!clickScheduler.goingToClick) {
            return@repeatable
        }

        travel(path)
        waitTicks(20)
        travel(path.reversed())
        desyncPlayerPosition = null
        pathCache = null
    }

    override fun enable() {
        pathFinderThread = thread {
            while (ModuleTpAura.enabled) {
                runCatching {
                    val playerPosition = player.pos

                    val enemies = targetTracker.enemies().sortedBy { it.squaredBoxedDistanceTo(playerPosition) }

                    for (enemy in enemies) {
                        if (player.distanceTo(enemy) > maximumDistance) {
                            continue
                        }

                        val path = findPath(
                            playerPosition.toVec3i(), enemy.blockVecPosition, maximumCost
                        )

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
        pathFinderThread?.interrupt()
        pathFinderThread = null
        pathCache = null
        desyncPlayerPosition = null
        super.disable()
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack
        val (_, path) = pathCache ?: return@handler

        renderEnvironmentForWorld(matrixStack) {
            withColor(Color4b.WHITE) {
                drawLineStrip(path.map { relativeToCamera(it.toVec3d().add(0.5, 0.5, 0.5)).toVec3() })
            }

            desyncPlayerPosition?.let { playerPosition ->
                withColor(Color4b.BLUE) {
                    withPositionRelativeToCamera(playerPosition) {
                        drawSolidBox(Box(0.4, 0.4, 0.4, 0.6, 0.6, 0.6))
                    }
                }
            }
        }
    }

    val packetHandler = handler<PacketEvent> {
        val packet = it.packet

        if (packet is PlayerMoveC2SPacket) {
            val position = desyncPlayerPosition ?: return@handler

            // Set the packet position to the player position
            packet.x = position.x
            packet.y = position.y
            packet.z = position.z
            packet.changePosition = true
        } else if (packet is PlayerPositionLookS2CPacket) {
            chat(markAsError("Server setback detected - teleport failed!"))
            stuckChronometer.reset()
            pathCache = null
            desyncPlayerPosition = null
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

            if (world.raycast(
                    RaycastContext(
                        start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player
                    )
                ).type != HitResult.Type.MISS
            ) {
                // If the path is not clear, we need to go one by one.
                for (position in chunk) {
                    network.sendPacket(
                        PositionAndOnGround(
                            position.x + 0.5, position.y + 0.01, position.z + 0.5, false
                        )
                    )
                    desyncPlayerPosition = position.toVec3d()
                }
                waitTicks(1)
                continue
            }

            // If the path is clear, we can teleport to the last position of the chunk.
            network.sendPacket(PositionAndOnGround(end.x, end.y, end.z, false))
            desyncPlayerPosition = end

            waitTicks(1)
        }
    }

    data class PathCache(val enemy: LivingEntity, val path: List<Vec3i>)

    fun findPath(start: Vec3i, end: Vec3i, maxCost: Int, maxIterations: Int = 500): List<Vec3i> {
        if (start == end) return listOf(end)

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
            val adjacentPosition = Vec3i(
                node.position.x + direction.x, node.position.y + direction.y, node.position.z + direction.z
            )
            if (isPassable(adjacentPosition)) {
                adjacentNodes.add(Node(adjacentPosition, node))
            }
        }

        if (allowDiagonal) {
            val diagonalDirections = listOf(
                Vec3i(-1, 0, -1), // left front
                Vec3i(1, 0, -1), // right front
                Vec3i(-1, 0, 1), // left back
                Vec3i(1, 0, 1) // right back
            )

            for (direction in diagonalDirections) {
                val adjacentPosition = Vec3i(
                    node.position.x + direction.x, node.position.y + direction.y, node.position.z + direction.z
                )
                val intermediatePosition1 = Vec3i(node.position.x + direction.x, node.position.y, node.position.z)
                val intermediatePosition2 = Vec3i(node.position.x, node.position.y, node.position.z + direction.z)
                if (isPassable(adjacentPosition) && isPassable(intermediatePosition1) && isPassable(
                        intermediatePosition2
                    )
                ) {
                    adjacentNodes.add(Node(adjacentPosition, node))
                }
            }
        }

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
