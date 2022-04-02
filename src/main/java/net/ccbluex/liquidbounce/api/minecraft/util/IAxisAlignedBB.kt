/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.util

interface IAxisAlignedBB {
    fun addCoord(x: Double, y: Double, z: Double): IAxisAlignedBB
    fun expand(x: Double, y: Double, z: Double): IAxisAlignedBB
    fun calculateIntercept(from: WVec3, to: WVec3): IMovingObjectPosition?

    fun isVecInside(vec: WVec3): Boolean
    fun offset(sx: Double, sy: Double, sz: Double): IAxisAlignedBB
    fun intersectsWith(boundingBox: IAxisAlignedBB): Boolean

    val minX: Double
    val minY: Double
    val minZ: Double

    val maxX: Double
    val maxY: Double
    val maxZ: Double
}