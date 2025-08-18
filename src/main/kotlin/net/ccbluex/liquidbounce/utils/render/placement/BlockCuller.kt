package net.ccbluex.liquidbounce.utils.render.placement

import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Direction.*

private const val FACE_DOWN = (1 shl 0) or (1 shl 1) or (1 shl 2) or (1 shl 3)
private const val FACE_UP = (1 shl 4) or (1 shl 5) or (1 shl 6) or (1 shl 7)
private const val FACE_NORTH = (1 shl 8) or (1 shl 9) or (1 shl 10) or (1 shl 11)
private const val FACE_EAST = (1 shl 12) or (1 shl 13) or (1 shl 14) or (1 shl 15)
private const val FACE_SOUTH = (1 shl 16) or (1 shl 17) or (1 shl 18) or (1 shl 19)
private const val FACE_WEST = (1 shl 20) or (1 shl 21) or (1 shl 22) or (1 shl 23)

private const val EDGE_NORTH_DOWN = ((1 shl 0) or (1 shl (1)))
private const val EDGE_EAST_DOWN = ((1 shl 2) or (1 shl (3)))
private const val EDGE_SOUTH_DOWN = ((1 shl 4) or (1 shl (5)))
private const val EDGE_WEST_DOWN = ((1 shl 6) or (1 shl (7)))

private const val EDGE_NORTH_WEST = ((1 shl 8) or (1 shl (9)))
private const val EDGE_NORTH_EAST = ((1 shl 10) or (1 shl (11)))
private const val EDGE_SOUTH_EAST = ((1 shl 12) or (1 shl (13)))
private const val EDGE_SOUTH_WEST = ((1 shl 14) or (1 shl (15)))

private const val EDGE_NORTH_UP = ((1 shl 16) or (1 shl (17)))
private const val EDGE_EAST_UP = ((1 shl 18) or (1 shl (19)))
private const val EDGE_SOUTH_UP = ((1 shl 20) or (1 shl (21)))
private const val EDGE_WEST_UP = ((1 shl 22) or (1 shl (23)))

// TODO check whether the Boxes actually touch
internal class BlockCuller(
    val parent: PlacementRenderHandler
) {

    private fun contains(pos: Long, direction: Direction) =
        BlockPos.offset(pos, direction) in parent

    private fun contains(pos: Long, direction1: Direction, direction2: Direction) =
        BlockPos.offset(BlockPos.offset(pos, direction1), direction2) in parent

    /**
     * Returns a long that stores in the first 32 bits what vertices are to be rendered for the faces and
     * in the other half what vertices are to be rendered for the outline.
     *
     * @param pos The position of the block, in long value.
     */
    fun getCullData(pos: Long): Long {
        var faces = 1 shl 30
        var edges = 1 shl 30

        val east = contains(pos, EAST)
        val west = contains(pos, WEST)
        val up = contains(pos, UP)
        val down = contains(pos, DOWN)
        val south = contains(pos, SOUTH)
        val north = contains(pos, NORTH)

        faces = cullSide(faces, east, FACE_EAST)
        faces = cullSide(faces, west, FACE_WEST)
        faces = cullSide(faces, up, FACE_UP)
        faces = cullSide(faces, down, FACE_DOWN)
        faces = cullSide(faces, south, FACE_SOUTH)
        faces = cullSide(faces, north, FACE_NORTH)

        edges = cullEdge(edges, north, down, contains(pos, NORTH, DOWN), EDGE_NORTH_DOWN)
        edges = cullEdge(edges, east, down, contains(pos, EAST, DOWN), EDGE_EAST_DOWN)
        edges = cullEdge(edges, south, down, contains(pos, SOUTH, DOWN), EDGE_SOUTH_DOWN)
        edges = cullEdge(edges, west, down, contains(pos, WEST, DOWN), EDGE_WEST_DOWN)
        edges = cullEdge(edges, north, west, contains(pos, NORTH, WEST), EDGE_NORTH_WEST)
        edges = cullEdge(edges, north, east, contains(pos, NORTH, EAST), EDGE_NORTH_EAST)
        edges = cullEdge(edges, south, east, contains(pos, SOUTH, EAST), EDGE_SOUTH_EAST)
        edges = cullEdge(edges, south, west, contains(pos, SOUTH, WEST), EDGE_SOUTH_WEST)
        edges = cullEdge(edges, north, up, contains(pos, NORTH, UP), EDGE_NORTH_UP)
        edges = cullEdge(edges, east, up, contains(pos, EAST, UP), EDGE_EAST_UP)
        edges = cullEdge(edges, south, up, contains(pos, SOUTH, UP), EDGE_SOUTH_UP)
        edges = cullEdge(edges, west, up, contains(pos, WEST, UP), EDGE_WEST_UP)

        // combines the data in a single long and inverts it, so that all vertices that are to be rendered are
        // represented by 1s
        return ((faces.toLong() shl 32) or edges.toLong()).inv()
    }

    /**
     * Applies a mask to the current data if either [direction1Present] and [direction2Present] are `false` or
     * [direction1Present] and [direction2Present] are `true` but [diagonalPresent] is `false`.
     *
     * This will result in the edge only being rendered if it's not surrounded by blocks and is on an actual
     * edge from multiple blocks seen as one entity.
     *
     * @return The updated [currentData]
     */
    private fun cullEdge(
        currentData: Int,
        direction1Present: Boolean,
        direction2Present: Boolean,
        diagonalPresent: Boolean,
        mask: Int
    ): Int {
        val neither1Nor2 = !direction1Present && !direction2Present
        val both1And2 = direction1Present && direction2Present

        return if (neither1Nor2 || (both1And2 && !diagonalPresent)) {
            currentData or mask
        } else {
            currentData
        }
    }

    /**
     * Applies a mask to the current data if either [directionPresent] is `false`.
     *
     * This will result in the face only being visible if it's on the outside of multiple blocks.
     *
     * @return The updated [currentData]
     */
    private fun cullSide(currentData: Int, directionPresent: Boolean, mask: Int): Int {
        return if (!directionPresent) {
            currentData or mask
        } else {
            currentData
        }
    }
}
