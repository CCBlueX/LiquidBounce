/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.astar

import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3i
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.minecraft.block.BlockAir

import java.util.ArrayList


class NaiveAstarGroundNode(override val x: Int, override val y: Int, override val z: Int, override var parentNode: NaiveAstarNode? = null) : NaiveAstarNode(x, y, z, parentNode)
{
    override fun neighbors(): ArrayList<AstarNode>
    {
        var neighborPoss = mutableListOf<Vec3i>()

        if (parentNode == null ||BlockUtils.getBlock(BlockPos(parentNode!!.x,parentNode!!.y - 1,parentNode!!.z)) !is BlockAir)
        {
            neighborPoss.add(Vec3i(x + 1, y, z))
            neighborPoss.add(Vec3i(x - 1, y, z))
            neighborPoss.add(Vec3i(x, y, z - 1))
            neighborPoss.add(Vec3i(x, y , z + 1))
        }
        else
            neighborPoss.add(Vec3i(x, y - 1, z))

        if (BlockUtils.getBlock(BlockPos(x,y - 1,z)) !is BlockAir)
            neighborPoss.add(Vec3i(x,y+1,z))

        neighborPoss = neighborPoss.filter { it -> BlockUtils.getBlock(BlockPos(it)) is BlockAir } as MutableList<Vec3i>
        neighborPoss = neighborPoss.filter { it -> BlockUtils.getBlock(BlockPos(it.x, it.y + 1, it.z)) is BlockAir } as MutableList<Vec3i>

        var arrayList = ArrayList<AstarNode>()

        neighborPoss.forEach { arrayList.add(NaiveAstarGroundNode(it.x, it.y, it.z, this)) }

        return arrayList
    }

    override fun equals(p: AstarNode?): Boolean
    {
        return super.equals(p)
    }

}