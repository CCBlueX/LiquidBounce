package net.ccbluex.liquidbounce.utils.pathfinding

import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import java.io.Serializable

/**
 * Author: Sigma devs
 */
class PathFinder(startVec: WVec3, endVec: WVec3) : MinecraftInstance()
{
    private val startVec: WVec3 = startVec.floor()
    private val endVec: WVec3 = endVec.floor()

    private val hubs: MutableList<Hub> = ArrayList()
    private val hubsToWork: MutableList<Hub> = ArrayList()
    var path: MutableList<WVec3> = ArrayList()
        private set

    fun compute(theWorld: IWorld, loops: Int = 1000, depth: Int = 4)
    {
        path.clear()
        hubsToWork.clear()

        val initPath = ArrayList<WVec3>()

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

    private fun isHubExisting(pos: WVec3): Hub? = hubs.firstOrNull { it.position == pos } ?: hubsToWork.firstOrNull { hub -> hub.position == pos }

    private fun addHub(parent: Hub?, loc: WVec3, cost: Double = 0.0): Boolean
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

    class Hub internal constructor(var position: WVec3, @Suppress("UNUSED_PARAMETER") parent: Hub?, var path: ArrayList<WVec3>, var squareDistanceToFromTarget: Double, var cost: Double, var totalCost: Double)

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
        private val flatCardinalDirections = arrayOf(WVec3(1.0, 0.0, 0.0), WVec3(-1.0, 0.0, 0.0), WVec3(0.0, 0.0, 1.0), WVec3(0.0, 0.0, -1.0))

        private fun checkPositionValidity(theWorld: IWorld, loc: WVec3, checkGround: Boolean = false): Boolean = checkPositionValidity(theWorld, loc.xCoord.toInt(), loc.yCoord.toInt(), loc.zCoord.toInt(), checkGround)

        fun checkPositionValidity(theWorld: IWorld, x: Int, y: Int, z: Int, checkGround: Boolean): Boolean
        {
            val block1 = WBlockPos(x, y, z)
            val block2 = WBlockPos(x, y + 1, z)
            val block3 = WBlockPos(x, y - 1, z)

            return !isBlockSolid(theWorld, block1) && !isBlockSolid(theWorld, block2) && (isBlockSolid(theWorld, block3) || !checkGround) && isSafeToWalkOn(theWorld, block3)
        }

        private fun isBlockSolid(theWorld: IWorld, blockpos: WBlockPos): Boolean
        {
            val state = theWorld.getBlockState(blockpos)
            val block = state.block

            val provider = classProvider

            return (block.getMaterial(state)?.blocksMovement() ?: true) && block.isFullCube(state) || provider.isBlockSlab(block) || provider.isBlockStairs(block) || provider.isBlockCactus(block) || provider.isBlockChest(block) || provider.isBlockEnderChest(block) || provider.isBlockSkull(block) || provider.isBlockPane(block) || provider.isBlockFence(block) || provider.isBlockWall(block) || provider.isBlockGlass(block) || provider.isBlockPistonBase(block) || provider.isBlockPistonExtension(block) || provider.isBlockPistonMoving(block) || provider.isBlockStainedGlass(block) || provider.isBlockTrapDoor(block)
        }

        private fun isSafeToWalkOn(theWorld: IWorld, blockpos: WBlockPos): Boolean
        {
            val block = theWorld.getBlockState(blockpos).block

            val provider = classProvider

            return !provider.isBlockFence(block) && !provider.isBlockWall(block)
        }
    }
}
