package net.ccbluex.liquidbounce.utils.pathfinding

import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.extensions.floor
import net.ccbluex.liquidbounce.utils.extensions.plus
import net.minecraft.block.BlockCactus
import net.minecraft.block.BlockChest
import net.minecraft.block.BlockEnderChest
import net.minecraft.block.BlockFence
import net.minecraft.block.BlockGlass
import net.minecraft.block.BlockPane
import net.minecraft.block.BlockPistonBase
import net.minecraft.block.BlockPistonExtension
import net.minecraft.block.BlockPistonMoving
import net.minecraft.block.BlockSkull
import net.minecraft.block.BlockSlab
import net.minecraft.block.BlockStainedGlass
import net.minecraft.block.BlockStairs
import net.minecraft.block.BlockTrapDoor
import net.minecraft.block.BlockWall
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraft.world.World
import java.io.Serializable

/**
 * Author: Sigma devs
 */
class PathFinder(startVec: Vec3, endVec: Vec3) : MinecraftInstance()
{
    private val startVec: Vec3 = startVec.floor()
    private val endVec: Vec3 = endVec.floor()

    private val hubs: MutableList<Hub> = ArrayList()
    private val hubsToWork: MutableList<Hub> = ArrayList()
    var path: MutableList<Vec3> = ArrayList()
        private set

    fun compute(theWorld: World, loops: Int = 1000, depth: Int = 4)
    {
        path.clear()
        hubsToWork.clear()

        val initPath = ArrayList<Vec3>()

        initPath.add(startVec)
        hubsToWork.add(Hub(startVec, null, initPath, startVec.squareDistanceTo(endVec), 0.0, 0.0))

        run findLoop@{
            repeat(loops) { _ ->
                hubsToWork.sortWith(HubComparator())

                if (hubsToWork.isEmpty()) return@findLoop

                run hubLoop@{
                    ArrayList(hubsToWork).forEachIndexed { index, hub ->
                        if (index + 1 > depth) return@hubLoop

                        hubsToWork.remove(hub)
                        hubs.add(hub)

                        flatCardinalDirections.map { (hub.position + it).floor() }.forEach { if (checkPositionValidity(theWorld, it) && addHub(hub, it)) return@findLoop }

                        val up = hub.position.plus(0.0, 1.0, 0.0).floor()
                        if (checkPositionValidity(theWorld, up) && addHub(hub, up)) return@findLoop

                        val down = hub.position.plus(0.0, -1.0, 0.0).floor()
                        if (checkPositionValidity(theWorld, down) && addHub(hub, down)) return@findLoop
                    }
                }
            }
        }
        hubs.sortWith(HubComparator())
        path = hubs[0].path
    }

    private fun isHubExisting(pos: Vec3): Hub? = hubs.firstOrNull { it.position == pos } ?: hubsToWork.firstOrNull { hub -> hub.position == pos }

    private fun addHub(parent: Hub?, loc: Vec3, cost: Double = 0.0): Boolean
    {
        val existingHub = isHubExisting(loc)
        var totalCost = cost

        if (parent != null) totalCost += parent.totalCost

        if (existingHub == null)
        {
            val minDistanceSquared = 9.0

            if (loc.xCoord == endVec.xCoord && loc.yCoord == endVec.yCoord && loc.zCoord == endVec.zCoord || loc.squareDistanceTo(endVec) <= minDistanceSquared)
            {
                path.clear()
                path = parent!!.path
                path.add(loc)

                return true
            }

            val path = ArrayList(parent!!.path)

            path.add(loc)

            hubsToWork.add(Hub(loc, parent, path, loc.squareDistanceTo(endVec), cost, totalCost))
        }
        else if (existingHub.cost > cost)
        {
            val path = ArrayList(parent!!.path)

            path.add(loc)

            existingHub.position = loc
            existingHub.path = path
            existingHub.squareDistanceToFromTarget = loc.squareDistanceTo(endVec)
            existingHub.cost = cost
            existingHub.totalCost = totalCost
        }

        return false
    }

    class Hub internal constructor(var position: Vec3, @Suppress("UNUSED_PARAMETER") parent: Hub?, var path: ArrayList<Vec3>, var squareDistanceToFromTarget: Double, var cost: Double, var totalCost: Double)

    class HubComparator : Comparator<Hub>, Serializable
    {
        override fun compare(hub: Hub, otherHub: Hub): Int = (hub.squareDistanceToFromTarget + hub.totalCost - (otherHub.squareDistanceToFromTarget + otherHub.totalCost)).toInt()

        companion object
        {
            private const val serialVersionUID = -6706152803040254364L
        }
    }

    companion object
    {
        private val flatCardinalDirections = arrayOf(Vec3(1.0, 0.0, 0.0), Vec3(-1.0, 0.0, 0.0), Vec3(0.0, 0.0, 1.0), Vec3(0.0, 0.0, -1.0))

        private fun checkPositionValidity(theWorld: World, loc: Vec3, checkGround: Boolean = false): Boolean = checkPositionValidity(theWorld, loc.xCoord.toInt(), loc.yCoord.toInt(), loc.zCoord.toInt(), checkGround)

        fun checkPositionValidity(theWorld: World, x: Int, y: Int, z: Int, checkGround: Boolean): Boolean
        {
            val block1 = BlockPos(x, y, z)
            val block2 = BlockPos(x, y + 1, z)
            val block3 = BlockPos(x, y - 1, z)
            return !isBlockSolid(theWorld, block1) && !isBlockSolid(theWorld, block2) && (isBlockSolid(theWorld, block3) || !checkGround) && isSafeToWalkOn(theWorld, block3)
        }

        private fun isBlockSolid(theWorld: World, blockpos: BlockPos): Boolean
        {
            val block = theWorld.getBlockState(blockpos).block
            return (block.material?.blocksMovement() ?: true) && block.isFullCube || block is BlockSlab || block is BlockStairs || block is BlockCactus || block is BlockChest || block is BlockEnderChest || block is BlockSkull || block is BlockPane || block is BlockFence || block is BlockWall || block is BlockGlass || block is BlockPistonBase || block is BlockPistonExtension || block is BlockPistonMoving || block is BlockStainedGlass || block is BlockTrapDoor
        }

        private fun isSafeToWalkOn(theWorld: World, blockpos: BlockPos): Boolean
        {
            val block = theWorld.getBlockState(blockpos).block
            return block !is BlockFence && block !is BlockWall
        }
    }
}
