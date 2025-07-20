package net.ccbluex.liquidbounce.utils.render.placement

import net.ccbluex.liquidbounce.render.*
import net.minecraft.util.math.BlockPos

// TODO check whether the Boxes actually touch
class BlockCuller(
    val parent: PlacementRenderHandler
) {
    /**
     * Returns a long that stores in the first 32 bits what vertices are to be rendered for the faces and
     * in the other half what vertices are to be rendered for the outline.
     */
    fun getCullData(pos: BlockPos): Long {
        var faces = 1 shl 30
        var edges = 1 shl 30

        val eastPos = pos.east()
        val westPos = pos.west()
        val upPos = pos.up()
        val downPos = pos.down()
        val southPos = pos.south()
        val northPos = pos.north()

        val east = parent.contains(eastPos)
        val west = parent.contains(westPos)
        val up = parent.contains(upPos)
        val down = parent.contains(downPos)
        val south = parent.contains(southPos)
        val north = parent.contains(northPos)

        faces = cullSide(faces, east, FACE_EAST)
        faces = cullSide(faces, west, FACE_WEST)
        faces = cullSide(faces, up, FACE_UP)
        faces = cullSide(faces, down, FACE_DOWN)
        faces = cullSide(faces, south, FACE_SOUTH)
        faces = cullSide(faces, north, FACE_NORTH)

        edges = cullEdge(edges, north, down, parent.contains(northPos.down()), EDGE_NORTH_DOWN)
        edges = cullEdge(edges, east, down, parent.contains(eastPos.down()), EDGE_EAST_DOWN)
        edges = cullEdge(edges, south, down, parent.contains(southPos.down()), EDGE_SOUTH_DOWN)
        edges = cullEdge(edges, west, down, parent.contains(westPos.down()), EDGE_WEST_DOWN)
        edges = cullEdge(edges, north, west, parent.contains(northPos.west()), EDGE_NORTH_WEST)
        edges = cullEdge(edges, north, east, parent.contains(northPos.east()), EDGE_NORTH_EAST)
        edges = cullEdge(edges, south, east, parent.contains(southPos.east()), EDGE_SOUTH_EAST)
        edges = cullEdge(edges, south, west, parent.contains(westPos.south()), EDGE_SOUTH_WEST)
        edges = cullEdge(edges, north, up, parent.contains(northPos.up()), EDGE_NORTH_UP)
        edges = cullEdge(edges, east, up, parent.contains(eastPos.up()), EDGE_EAST_UP)
        edges = cullEdge(edges, south, up, parent.contains(southPos.up()), EDGE_SOUTH_UP)
        edges = cullEdge(edges, west, up, parent.contains(westPos.up()), EDGE_WEST_UP)

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
