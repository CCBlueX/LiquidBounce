/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.util

interface IAxisAlignedBB
{
    val minX: Double
    val minY: Double
    val minZ: Double

    val maxX: Double
    val maxY: Double
    val maxZ: Double

    // <editor-fold desc="Calculations">
    fun addCoord(x: Double, y: Double, z: Double): IAxisAlignedBB
    fun expand(x: Double, y: Double, z: Double): IAxisAlignedBB
    fun offset(x: Double, y: Double, z: Double): IAxisAlignedBB
    // </editor-fold>

    // <editor-fold desc="Intercept checks">
    fun isVecInside(vec: WVec3): Boolean
    fun intersectsWith(boundingBox: IAxisAlignedBB): Boolean
    fun calculateIntercept(from: WVec3, to: WVec3): IMovingObjectPosition?
    // </editor-fold>

    override fun toString(): String
}
