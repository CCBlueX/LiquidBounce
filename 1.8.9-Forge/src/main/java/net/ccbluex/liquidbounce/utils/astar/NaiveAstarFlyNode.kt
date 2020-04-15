/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.astar

import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.minecraft.block.BlockAir
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3i

class NaiveAstarFlyNode(override val x: Int, override val y: Int, override val z: Int, override var parentNode: NaiveAstarNode? = null) : NaiveAstarNode(x, y, z, parentNode)
{
    override fun neighbors(): ArrayList<AstarNode>
    {
        var neiborPoss = mutableListOf<Vec3i>(
                Vec3i(x + 1, y, z)
                , Vec3i(x - 1, y, z)
                , Vec3i(x, y + 1, z)
                , Vec3i(x, y - 1, z)
                , Vec3i(x, y, z + 1)
                , Vec3i(x, y, z - 1)
        )

        neiborPoss = neiborPoss.filter { it -> BlockUtils.getBlock(BlockPos(it)) is BlockAir } as MutableList<Vec3i>
        neiborPoss = neiborPoss.filter { it -> BlockUtils.getBlock(BlockPos(it.x, it.y + 1, it.z)) is BlockAir } as MutableList<Vec3i>

        var arrayList = ArrayList<AstarNode>()

        neiborPoss.forEach { arrayList.add(NaiveAstarFlyNode(it.x, it.y, it.z, this)) }

        return arrayList
    }


    override fun equals(p: AstarNode?): Boolean
    {
        return super.equals(p)
    }

}