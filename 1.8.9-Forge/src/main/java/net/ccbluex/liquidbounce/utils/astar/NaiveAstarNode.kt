package net.ccbluex.liquidbounce.utils.astar

import net.minecraft.util.Vec3
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

abstract class NaiveAstarNode(open val x: Int, open val y: Int, open val z: Int, open var parent_node: NaiveAstarNode? = null) : AstarNode
{

    override fun cauculate_f(begin: AstarNode, end: AstarNode): Double
    {
        return cauculate_g(begin) + cauculate_h(end)
    }

    override fun get_parent(): AstarNode?
    {
        return parent_node
    }

    override fun set_parent(p: AstarNode?)
    {
        parent_node = (p as NaiveAstarNode?)!!
    }

    override fun cauculate_g(begin: AstarNode): Double
    {
        val node = begin as NaiveAstarNode
        return sqrt((node.x - x).toDouble().pow(2)
                + (node.y - y).toDouble().pow(2)
                + (node.z - z).toDouble().pow(2))
    }

    override fun cauculate_h(end: AstarNode): Double
    {
        val node = end as NaiveAstarNode
        return sqrt((node.x - x).toDouble().pow(2) +
                (node.y - y).toDouble().pow(2) +
                (node.z - z).toDouble().pow(2))
    }

    override fun cauculate_cost_to_parent(): Double
    {
        return sqrt((parent_node!!.x - x).toDouble().pow(2) +
                (parent_node!!.y - y).toDouble().pow(2) + (parent_node!!.z - z)
                .toDouble().pow(2))
    }

    fun get_pos(): Vec3
    {
        return Vec3(x + 0.5, y + 0.0, z + 0.5)
    }

    override fun equals(other: Any?): Boolean
    {
        return if (other is NaiveAstarNode)
            other.x == x && other.y == y && other.z == z
        else
            false
    }

}