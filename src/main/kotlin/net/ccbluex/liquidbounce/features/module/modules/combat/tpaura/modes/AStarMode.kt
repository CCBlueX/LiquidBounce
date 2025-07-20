package net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.modes

import kotlinx.coroutines.Dispatchers
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.ModuleTpAura.clicker
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.ModuleTpAura.desyncPlayerPosition
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.ModuleTpAura.stuckChronometer
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.ModuleTpAura.targetSelector
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.TpAuraChoice
import net.ccbluex.liquidbounce.render.drawLineStrip
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withColor
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.entity.blockVecPosition
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.kotlin.mapArray
import net.ccbluex.liquidbounce.utils.math.*
import net.minecraft.entity.LivingEntity
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3i
import java.util.*
import kotlin.math.roundToInt

private class Node(val position: Vec3i, var parent: Node? = null) {
    var g = 0
    var h = 0
    var f = 0

    override fun hashCode(): Int = position.hashCode()
    override fun equals(other: Any?): Boolean = other is Node && other.position == this.position
}

object AStarMode : TpAuraChoice("AStar") {

    private val maximumDistance by int("MaximumDistance", 95, 50..250)
    private val maximumCost by int("MaximumCost", 250, 50..500)
    private val tickDistance by int("TickDistance", 3, 1..7)
    private val allowDiagonal by boolean("AllowDiagonal", false)

    private val stickAt by int("Stick", 5, 1..10, "ticks")

    private var pathCache: PathCache? = null

    @Suppress("unused")
    private val tickHandler = tickHandler {
        val (_, path) = pathCache ?: return@tickHandler

        if (!clicker.isClickTick) {
            return@tickHandler
        }

        travel(path)
        waitTicks(stickAt)
        travel(path.reversed())
        desyncPlayerPosition = null
        pathCache = null
    }

    @Suppress("unused")
    private val pathFinder = tickHandler {
        waitTicks(1)

        pathCache = waitFor(Dispatchers.Default) {
            val playerPosition = player.pos

            val maximumDistanceSq = maximumDistance.sq()

            targetSelector.targets().filter {
                it.squaredDistanceTo(playerPosition) <= maximumDistanceSq
            }.sortedBy {
                it.squaredBoxedDistanceTo(playerPosition)
            }.firstNotNullOfOrNull { enemy ->
                val path = findPath(playerPosition.toVec3i(), enemy.blockVecPosition, maximumCost)

                // Skip if the path is empty
                if (path.isNotEmpty()) {
                    // Stop searching when the pathCache is ready
                    PathCache(enemy, path)
                } else {
                    null
                }
            }
        }
    }

    override fun disable() {
        desyncPlayerPosition = null
        pathCache = null
        super.disable()
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack
        val (_, path) = pathCache ?: return@handler

        renderEnvironmentForWorld(matrixStack) {
            withColor(Color4b.WHITE) {
                drawLineStrip(positions = path.mapArray {
                    relativeToCamera(it.toVec3d(0.5, 0.5, 0.5)).toVec3()
                })
            }
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> {
        val packet = it.packet

        if (packet is PlayerMoveC2SPacket) {
            val position = desyncPlayerPosition ?: return@handler

            // Set the packet position to the player position
            packet.x = position.x
            packet.y = position.y
            packet.z = position.z
            packet.changePosition = true
        } else if (packet is PlayerPositionLookS2CPacket) {
            val change = packet.change.position
            chat(markAsError("Server setback detected - teleport failed at ${change.x} ${change.y} ${change.z}!"))
            stuckChronometer.reset()
            pathCache = null
            desyncPlayerPosition = null
        }
    }

    private fun travel(path: List<Vec3i>) {
        // Currently path is a list of positions we need to go one by one, however we can split it into chunks
        // to use less packets and teleport more efficiently.
        // However, we cannot teleport if there are blocks in the way, so we need to check if the path is clear.
        val pathChunks = path.chunked(tickDistance)

        for (chunk in pathChunks) {
            // Check if the path is clear, this can be done by raycasting the start and end position of the chunk.
            val start = chunk.first().toVec3d(0.5, 0.5, 0.5)
            val end = chunk.last().toVec3d(0.5, 0.5, 0.5)

            if (world.getBlockCollisions(player, Box(start, end)).any()) {
                // If the path is not clear, we need to go one by one.
                for (position in chunk) {
                    network.sendPacket(
                        PositionAndOnGround(
                            position.x + 0.5, position.y.toDouble(), position.z + 0.5, false, false
                        )
                    )
                    desyncPlayerPosition = position.toVec3d()
                }
                continue
            } else {
                // If the path is clear, we can teleport to the last position of the chunk.
                network.sendPacket(PositionAndOnGround(end.x, end.y, end.z, false, false))
                desyncPlayerPosition = end
            }
        }
    }

    data class PathCache(val enemy: LivingEntity, val path: List<Vec3i>)

    private fun findPath(start: Vec3i, end: Vec3i, maxCost: Int, maxIterations: Int = 500): List<Vec3i> {
        if (start == end) return listOf(end)

        val startNode = Node(start)
        val endNode = Node(end)

        // Node::f won't be modified after added
        val openList = TreeSet(Comparator.comparingInt(Node::f).thenComparing(Node::position)).apply { add(startNode) }
        val closedList = hashSetOf<Node>()

        var iterations = 0
        while (openList.isNotEmpty()) {
            iterations++
            if (iterations > maxIterations) {
                break
            }

            val currentNode = openList.removeFirst()
            closedList.add(currentNode)

            if (currentNode.position.isWithinDistance(endNode.position, 2.0)) {
                return constructPath(currentNode)
            }

            for (node in getAdjacentNodes(currentNode)) {
                if (node in closedList || !isPassable(node.position)) continue

                val tentativeG = currentNode.g + distanceBetween(currentNode.position, node.position)
                if (tentativeG < node.g || node !in openList) {
                    if (tentativeG > maxCost) continue // Skip this node if the cost exceeds the maximum

                    node.parent = currentNode
                    node.g = tentativeG
                    node.h = distanceBetween(node.position, endNode.position)
                    node.f = node.g + node.h

                    openList.add(node)
                }
            }
        }

        return emptyList() // Return an empty list if no path was found
    }

    private fun constructPath(node: Node): List<Vec3i> {
        val path = mutableListOf<Vec3i>()
        var currentNode = node
        while (currentNode.parent != null) {
            path.add(currentNode.position)
            currentNode = currentNode.parent!!
        }
        path.reverse()
        return path
    }

    private val directions = buildList(22) {
        add(Vec3i(-1, 0, 0)) // left
        add(Vec3i(1, 0, 0)) // right
        (-9..-1).mapTo(this) { Vec3i(0, it, 0) } // down
        (1..9).mapTo(this) { Vec3i(0, it, 0) } // up
        add(Vec3i(0, 0, -1)) // front
        add(Vec3i(0, 0, 1)) // back
    }

    private val diagonalDirections = arrayOf(
        Vec3i(-1, 0, -1), // left front
        Vec3i(1, 0, -1), // right front
        Vec3i(-1, 0, 1), // left back
        Vec3i(1, 0, 1) // right back
    )

    @Suppress("detekt:CognitiveComplexMethod")
    private fun getAdjacentNodes(node: Node): List<Node> = buildList {
        for (direction in directions) {
            val adjacentPosition = node.position + direction
            if (isPassable(adjacentPosition)) {
                add(Node(adjacentPosition, node))
            }
        }

        if (!allowDiagonal) {
            return@buildList
        }

        for (direction in diagonalDirections) {
            val adjacentPosition = node.position + direction
            if (!isPassable(adjacentPosition)) {
                continue
            }

            if (isPassable(node.position.add(direction.x, 0, 0)) && isPassable(node.position.add(0, 0, direction.z))) {
                add(Node(adjacentPosition, node))
            }
        }
    }

    private fun isPassable(position: Vec3i): Boolean {
        val start = position.toVec3d()
        val end = start.add(1.0, 2.0, 1.0)

        val collisions = world.getBlockCollisions(player, Box(start, end))

        return collisions.none()
    }

    private fun distanceBetween(a: Vec3i, b: Vec3i) = a.getSquaredDistance(b).roundToInt()

}
