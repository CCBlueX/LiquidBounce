package net.ccbluex.liquidbounce.features.module.modules.movement.parkour

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.math.Vec2i
import net.ccbluex.liquidbounce.utils.math.geometry.LineSegment
import net.ccbluex.liquidbounce.utils.math.toVec3
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import java.text.DecimalFormat

class Platform(
    val platformBlocks: List<BlockPos>,
    val edges: List<LineSegment>,
    var reachable: Boolean? = null
) {

}

private fun canBeInside(blockPos: BlockPos): Boolean {
    return blockPos.getState()?.getCollisionShape(world, blockPos)?.isEmpty ?: true
}

private fun canRouteThrough(blockPos: BlockPos): Boolean {
    return blockPos.getState()?.isFullCube(world, blockPos) == true
}

fun findHighestPlatformBlock(blockPos: BlockPos, maxDepth: Int = 3): BlockPos? {
    // How many air blocks have we seen?
    var airBlocks = 0

    val openBlocks = ArrayDeque<BlockPos>()

    for (y in blockPos.y + 2 downTo blockPos.y - maxDepth) {
        val currentPos = BlockPos(blockPos.x, y, blockPos.z)

        when {
            canBeInside(currentPos) -> {
                airBlocks += 1
            }

            canRouteThrough(currentPos) && airBlocks >= 2 -> {
                return currentPos
            }

            else -> {
                airBlocks = 0
            }
        }
    }

    return null
}

fun discoverPlatforms(
    interestingPoints: List<Vec2i>,
    y: Int,
    lineFrom: Vec2i,
    lineTo: Vec2i,
    maxLineDistance: Double = 3.0
): List<Platform> {
    val currentTime = System.nanoTime()
    val blocks = ArrayDeque<BlockPos>()

    for (point in interestingPoints) {
        val block = findHighestPlatformBlock(BlockPos(point.x, y, point.y), maxDepth = 3)

        if (block != null) {
            blocks.add(block)
        }
    }

    val basePos = Vec3d(lineFrom.x.toDouble(), y.toDouble(), lineFrom.y.toDouble())
    val lineDirection = lineTo - lineFrom

    val line = LineSegment(basePos, Vec3d(lineDirection.x.toDouble(), 0.0, lineDirection.y.toDouble()), 0.0..1.0)

    val cca = CCA(blocks) {
        val canStandOn = canRouteThrough(it) && canBeInside(it.up(1)) && canBeInside(it.up(2))

        canStandOn && line.squaredDistanceTo(Vec3d.of(it)) < 4.0 * 4.0
    }

    cca.run()

    val deltaT = System.nanoTime() - currentTime

    val platforms = cca.findGroups().map {
        val edges = findEdges(it)

        Platform(it.components, edges)
    }

    ModuleDebug.debugParameter(ModuleParkour, "t_PA", DecimalFormat("0.000").format(deltaT / 1000.0) + " us")

    return platforms
}

fun debugPlatforms(platforms: List<Platform>, currentPlatform: Platform?) {
    val edgeGeomety = ArrayList<ModuleDebug.DebuggedGeometry>()
    val groups = ArrayList<ModuleDebug.DebuggedGeometry>()

    val sortedPlatforms = platforms.sortedBy { platform ->
        platform.platformBlocks.minOfOrNull { it.getSquaredDistance(player.x, player.y, player.z) }
    }

    sortedPlatforms.forEachIndexed { index, platform ->
        val color = ModuleDebug.getArrayEntryColor(index, sortedPlatforms.size.coerceAtLeast(6))

        val alpha = if (platform.reachable == true) 255 else 64

        val modColor = if (platform == currentPlatform) Color4b.BLACK else color.alpha(alpha)

        val group = platform.platformBlocks.map { ModuleDebug.DebuggedBox(Box.from(Vec3d.of(it)), modColor) }

        groups.add(ModuleDebug.DebugCollection(group))

        for (edge in platform.edges) {
            edgeGeomety.add(
                ModuleDebug.DebuggedLineSegment(
                    edge.getPosition(0.0).toVec3(),
                    edge.getPosition(1.0).toVec3(),
                    Color4b.WHITE
                )
            )
        }
    }


    ModuleDebug.debugGeometry(ModuleParkour, "groups", ModuleDebug.DebugCollection(groups))
    ModuleDebug.debugGeometry(ModuleParkour, "edgesS", ModuleDebug.DebugCollection(edgeGeomety))
}

private val EDGE_DIRECTIONS: Array<Pair<Vec3i, Int>> = arrayOf(
    Vec3i(-1, 0, 0) to EdgeDetectionComponent.DOWN,
    Vec3i(1, 0, 0) to EdgeDetectionComponent.UP,
    Vec3i(0, 0, 1) to EdgeDetectionComponent.RIGHT,
    Vec3i(0, 0, -1) to EdgeDetectionComponent.LEFT,
)

private class EdgeDetectionComponent(
    val pos: BlockPos,
    var directionsLeft: Int = ALL
) {
    fun clearDirection(direction: Int) {
        this.directionsLeft = (this.directionsLeft and (direction.inv()))
    }

    fun isDirectionDone(directionBit: Int): Boolean {
        return (this.directionsLeft and directionBit) == 0
    }

    companion object {
        const val ALL: Int = 0xF

        const val UP: Int = 1
        const val DOWN: Int = 2
        const val LEFT: Int = 4
        const val RIGHT: Int = 8
    }
}

fun isEdge(pos: BlockPos): Boolean {
    return canBeInside(pos.up(1)) && canBeInside(pos.up(2))
}

private fun calculateEdgeLength(components: Map<BlockPos, EdgeDetectionComponent>, pos: BlockPos, edgeDirection: Vec3i, direction: Int, sign: Int): Vec3d? {
    val walkDirection = if (edgeDirection.x == 0) {
        Vec3i(sign, 0, 0)
    } else {
        Vec3i(0, 0, sign)
    }

    // Fail safe
    for (i in 1..500) {
        val currPos = pos.add(walkDirection.multiply(i))
        val component = components[currPos] ?: return Vec3d.of(walkDirection).multiply(i - 0.5)

        val edgePos = currPos.add(edgeDirection)

        if (edgePos in components || !isEdge(edgePos)) {
            return Vec3d.of(walkDirection).multiply(i - 0.5)
        }

        component.clearDirection(direction)
    }

    throw IllegalStateException("NO!!!")
}

fun findEdges(components: ConnectedComponents): List<LineSegment> {
    val edges = ArrayList<LineSegment>()

    val componentsLeft = components.components.associateWith { EdgeDetectionComponent(it) }

    for (edgeDetectionComponent in componentsLeft.values) {
        val pos = edgeDetectionComponent.pos

        for ((directionVector, directionBit) in EDGE_DIRECTIONS) {
            if (edgeDetectionComponent.isDirectionDone(directionBit)) {
                continue
            }
            val edgePos = pos.add(directionVector)

            if (edgePos in componentsLeft || !isEdge(edgePos)) {
                continue
            }

            val edgeLenRight = calculateEdgeLength(componentsLeft, pos, directionVector, directionBit, 1)
            val edgeLenLeft = calculateEdgeLength(componentsLeft, pos, directionVector, directionBit, -1)

            val lineCenter = Vec3d.ofCenter(pos, 1.0).add(Vec3d.of(directionVector).multiply(0.5))

            edges.add(LineSegment.from(lineCenter.add(edgeLenLeft), lineCenter.add(edgeLenRight)))
        }
    }

    return edges
}

class ConnectedComponents(val components: ArrayList<BlockPos> = ArrayList())

class CCA(
    blocks: ArrayDeque<BlockPos>,
    val relevancePredicate: (BlockPos) -> Boolean
) {
    private val componentConnections = HashMap<BlockPos, ConnectedComponents>()

    private val visitedBlocks = HashSet<BlockPos>()
    private val openBlocks: ArrayDeque<BlockPos> = blocks

    fun run() {
        while (!openBlocks.isEmpty()) {
            val currentPos = openBlocks.removeFirst()

            // Did we process this location before?
            if (!visitedBlocks.add(currentPos)) {
                continue
            }

            val currentComponents = this.componentConnections.computeIfAbsent(currentPos) {
                ConnectedComponents(arrayListOf(currentPos))
            }

            val neighbors = arrayOf(
                currentPos.add(1, 0, 0),
                currentPos.add(-1, 0, 0),
                currentPos.add(0, 0, 1),
                currentPos.add(0, 0, -1),
            )

            for (neighbor in neighbors) {
                if (visitedBlocks.contains(neighbor)) {
                    continue
                }

                // Check if this position is relevant for us
                if (!relevancePredicate(neighbor)) {
                    // Prevent further processing
                    visitedBlocks.add(neighbor)

                    continue
                }

                processNeighbor(currentComponents, neighbor)

                openBlocks.addFirst(neighbor)
            }
        }
    }

    fun findGroups(): Set<ConnectedComponents> {
        return this.componentConnections.values.toHashSet()
    }

    private fun processNeighbor(currentComponents: ConnectedComponents, neighbor: BlockPos): ConnectedComponents {
        val currentOtherComponents = this.componentConnections[neighbor]

        // The neighbor wasn't processed yet.
        when {
            currentOtherComponents == null -> {
                addToConnection(neighbor, currentComponents)

                return currentComponents
            }
            // Both are already linked as neighbors
            currentOtherComponents === currentComponents -> {
                return currentComponents
            }
            // We need to merge
            else -> {
                return mergeConnections(currentComponents, currentOtherComponents)
            }
        }
    }

    private fun mergeConnections(
        currentComponents: ConnectedComponents,
        currentOtherComponents: ConnectedComponents
    ): ConnectedComponents {
        val listWithBothComponents = run {
            val components = currentComponents.components

            components.addAll(currentOtherComponents.components)

            components
        }

        val newComponents = ConnectedComponents(listWithBothComponents)

        // Remapping
        for (pos in newComponents.components) {
            this.componentConnections[pos] = newComponents
        }

        return newComponents
    }

    private fun addToConnection(neighbor: BlockPos, currentComponents: ConnectedComponents) {
        currentComponents.components.add(neighbor)

        this.componentConnections[neighbor] = currentComponents
    }
}

