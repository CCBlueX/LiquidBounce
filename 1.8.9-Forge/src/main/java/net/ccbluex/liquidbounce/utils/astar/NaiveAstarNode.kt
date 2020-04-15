/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.astar

import jdk.nashorn.internal.ir.Block
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.minecraft.block.material.Material
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import kotlin.math.pow
import kotlin.math.sqrt

typealias AstarNode = Astar.AstarNode

abstract class NaiveAstarNode(open val x: Int, open val y: Int, open val z: Int, open var parentNode: NaiveAstarNode? = null) : AstarNode
{

    override fun f(begin: AstarNode, end: AstarNode): Double
    {
        return g(begin) + h(end)
    }

    override fun parent(): AstarNode?
    {
        return parentNode
    }

    override fun setParent(p: AstarNode?)
    {
        parentNode = (p as NaiveAstarNode?)!!
    }

    override fun g(begin: AstarNode): Double
    {
        val node = begin as NaiveAstarNode
        return sqrt((node.x - x).toDouble().pow(2)
                + (node.y - y).toDouble().pow(2)
                + (node.z - z).toDouble().pow(2))
    }

    override fun h(end: AstarNode): Double
    {
        val node = end as NaiveAstarNode
        return sqrt((node.x - x).toDouble().pow(2) +
                (node.y - y).toDouble().pow(2) +
                (node.z - z).toDouble().pow(2))
    }

    override fun costToParent(): Double
    {
        return sqrt((parentNode!!.x - x).toDouble().pow(2) +
                (parentNode!!.y - y).toDouble().pow(2) + (parentNode!!.z - z)
                .toDouble().pow(2))
    }

    fun getPos(): Vec3
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