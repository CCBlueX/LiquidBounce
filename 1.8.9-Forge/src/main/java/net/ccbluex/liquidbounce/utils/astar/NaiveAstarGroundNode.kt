package net.ccbluex.liquidbounce.utils.astar

import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3i
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.minecraft.block.BlockAir

import java.util.ArrayList

class NaiveAstarGroundNode(override val x: Int, override val y: Int, override val z: Int, override var parent_node: NaiveAstarNode? = null) : NaiveAstarNode(x, y, z, parent_node)
{
    override fun get_neighbors(): ArrayList<AstarNode>
    {
        var neibor_poss = mutableListOf<Vec3i>()

        if (BlockUtils.getBlock(BlockPos(x, y - 1, z)) !is BlockAir)
        {
            neibor_poss.add(Vec3i(x, y + 1, z))
        }
        if (parent_node == null ||BlockUtils.getBlock(BlockPos(parent_node!!.x,parent_node!!.y - 1,parent_node!!.z)) !is BlockAir)
        {
            neibor_poss.add(Vec3i(x + 1, y, z))
            neibor_poss.add(Vec3i(x - 1, y, z))
            neibor_poss.add(Vec3i(x, y, z - 1))
            neibor_poss.add(Vec3i(x, y , z + 1))
        }

        neibor_poss.add(Vec3i(x, y - 1, z))

        neibor_poss = neibor_poss.filter { it -> BlockUtils.getBlock(BlockPos(it)) is BlockAir } as MutableList<Vec3i>
        neibor_poss = neibor_poss.filter { it -> BlockUtils.getBlock(BlockPos(it.x, it.y + 1, it.z)) is BlockAir } as MutableList<Vec3i>
        neibor_poss = neibor_poss.filter { it -> BlockUtils.getBlock(BlockPos(it.x, it.y - 2, it.z)) !is BlockAir } as MutableList<Vec3i>

        var arrayList = ArrayList<AstarNode>()

        neibor_poss.forEach { arrayList.add(NaiveAstarGroundNode(it.x, it.y, it.z, this)) }

        return arrayList
    }

    /*override fun equals(p: AstarNode?): Boolean
    {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }*/


}