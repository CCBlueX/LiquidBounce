package net.ccbluex.liquidbounce.utils.astar

import net.minecraft.block.material.Material
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3i
import net.ccbluex.liquidbounce.utils.block.BlockUtils

import java.util.ArrayList
import kotlin.math.abs

class NaiveAstarGroundNode(val x: Int, val y: Int, val z:Int, var parent_node: NaiveAstarGroundNode? = null) : AstarNode
{
    override fun get_neighbors(): ArrayList<AstarNode>
    {
        var neibor_poss = mutableListOf<Vec3i>(
                Vec3i(x + 1,y,z)
                , Vec3i(x-1,y,z)
                , Vec3i(x,y+1,z)
                , Vec3i(x,y-1,z)
                , Vec3i(x,y,z-1)
        )

        if(BlockUtils.getBlock(BlockPos(x,y,z))?.getMaterial() !== Material.air)
        {
            neibor_poss.add(Vec3i(x,y,z+1))
        }

        neibor_poss.filter { it -> BlockUtils.getBlock(BlockPos(it))?.getMaterial() == Material.air }
        neibor_poss.filter { it -> BlockUtils.getBlock(BlockPos(it.x,it.y + 1,it.z))?.getMaterial() == Material.air }
        neibor_poss.filter { it -> BlockUtils.getBlock(BlockPos(it.x,it.y - 1,it.z))?.getMaterial() !== Material.air }

        var arrayList = ArrayList<AstarNode>()

        neibor_poss.forEach { arrayList.add(NaiveAstarGroundNode(it.x,it.y,it.z,this)) }

        return arrayList
    }

    override fun set_parent(p: AstarNode?)
    {
        parent_node = p as NaiveAstarGroundNode?
    }

    /*override fun equals(p: AstarNode?): Boolean
    {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }*/

    override fun get_parent(): AstarNode?
    {
        return parent_node
    }

    override fun cauculate_g(begin: AstarNode): Double
    {
        val node = begin as NaiveAstarGroundNode
        return (abs(node.x - x) + abs(node.y - y) + abs(node.z - z)).toDouble()
    }

    override fun cauculate_f(begin: AstarNode,end : AstarNode): Double
    {
        return cauculate_g(begin) + cauculate_h(end)
    }

    override fun cauculate_h(end: AstarNode): Double
    {
        val node = end as NaiveAstarGroundNode
        return (abs(node.x - x) + abs(node.y - y) + abs(node.z - z)).toDouble()
    }

    override fun cauculate_cost_to_parent(): Double
    {
        return (abs(parent_node!!.x - x) + abs(parent_node!!.y - y) + abs(parent_node!!.z - z)).toDouble()
    }

}