/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.util

interface IEnumFacing {
    fun isNorth(): Boolean
    fun isSouth(): Boolean
    fun isEast(): Boolean
    fun isWest(): Boolean
    fun isUp(): Boolean

    val opposite: IEnumFacing
    val directionVec: WVec3i
    val axisOrdinal: Int
}