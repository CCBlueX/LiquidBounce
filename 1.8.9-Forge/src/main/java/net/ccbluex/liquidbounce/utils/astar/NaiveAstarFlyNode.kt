package net.ccbluex.liquidbounce.utils.astar

import net.ccbluex.liquidbounce.utils.ClientUtils
import net.minecraft.block.material.Material
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3i
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.minecraft.block.BlockAir

import kotlin.collections.ArrayList

class NaiveAstarFlyNode(override val x: Int, override val y: Int, override val z: Int, override var parent_node: NaiveAstarNode? = null) : NaiveAstarNode(x, y, z, parent_node)
{
    override fun get_neighbors(): ArrayList<AstarNode>
    {
        var neibor_poss = mutableListOf<Vec3i>(
                Vec3i(x + 1, y, z)
                , Vec3i(x - 1, y, z)
                , Vec3i(x, y + 1, z)
                , Vec3i(x, y - 1, z)
                , Vec3i(x, y, z + 1)
                , Vec3i(x, y, z - 1)
        )

        neibor_poss = neibor_poss.filter { it -> BlockUtils.getBlock(BlockPos(it)) is BlockAir } as MutableList<Vec3i>
        neibor_poss = neibor_poss.filter { it -> BlockUtils.getBlock(BlockPos(it.x, it.y + 1, it.z)) is BlockAir } as MutableList<Vec3i>

        var arrayList = ArrayList<AstarNode>()

        neibor_poss.forEach { arrayList.add(NaiveAstarFlyNode(it.x, it.y, it.z, this)) }

        return arrayList
    }


    /*override fun equals(p: AstarNode?): Boolean
    {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }*/

}