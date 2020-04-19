/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.astar

import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import java.util.*

object Astar : MinecraftInstance()
{
    fun findPath(begin: AstarNode, end: AstarNode,
                 op: (AstarNode,AstarNode) -> Boolean, timeout_ms: Int): MutableList<AstarNode>
    {
        val openList = mutableListOf<AstarNode?>()
        val closedList = mutableListOf<AstarNode?>()

        val timer = MSTimer()
        timer.reset()


        openList.add(begin)


        var currentNode: AstarNode? = null
        do
        {
            if (openList.size == 0) break

            currentNode = openList.minBy { it!!.f(begin, end) }

            closedList.add(currentNode)
            openList.remove(currentNode)

            val neighbors = currentNode!!.neighbors()

            for (element in neighbors)
            {
                if (closedList.contains(element)) continue

                if (!openList.contains(element)) openList.add(element) else
                {
                    val actualElement = openList[openList.indexOf(element)]!!

                    val parent = actualElement.parent()
                    val g = parent!!.g(begin) + actualElement.costToParent()
                    val tentativeG = actualElement.g(begin)
                    if (tentativeG < g) actualElement.setParent(currentNode)
                }
            }

            if (openList.size > 10000) return ArrayList()
            if (timer.hasTimePassed(timeout_ms.toLong())) return ArrayList()
        } while (!op(currentNode!!, end))


        val list = mutableListOf<AstarNode>()
        while (currentNode!!.parent() != null)
        {
            list.add(0, currentNode)
            currentNode = currentNode.parent()
        }
        list.add(0, currentNode)
        return list
    }

    interface AstarNode
    {
        fun f(begin: AstarNode, end: AstarNode): Double
        fun g(begin: AstarNode): Double
        fun h(end: AstarNode): Double
        fun parent(): AstarNode?
        fun costToParent(): Double
        fun setParent(p: AstarNode?)
        fun neighbors(): ArrayList<AstarNode>
        fun equals(p: AstarNode): Boolean
    }
}
