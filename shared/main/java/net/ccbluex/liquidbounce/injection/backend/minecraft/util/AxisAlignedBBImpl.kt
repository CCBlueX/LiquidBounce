/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.util

import net.ccbluex.liquidbounce.api.minecraft.util.IAxisAlignedBB
import net.ccbluex.liquidbounce.api.minecraft.util.IMovingObjectPosition
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.injection.backend.utils.unwrap
import net.minecraft.util.AxisAlignedBB

class AxisAlignedBBImpl(val wrapped: AxisAlignedBB) : IAxisAlignedBB
{
    override val minX: Double
        get() = wrapped.minX
    override val minY: Double
        get() = wrapped.minY
    override val minZ: Double
        get() = wrapped.minZ

    override val maxX: Double
        get() = wrapped.maxX
    override val maxY: Double
        get() = wrapped.maxY
    override val maxZ: Double
        get() = wrapped.maxZ

    // <editor-fold desc="Calculations">
    override fun addCoord(x: Double, y: Double, z: Double): IAxisAlignedBB = wrapped.addCoord(x, y, z).wrap()

    override fun expand(x: Double, y: Double, z: Double): IAxisAlignedBB = wrapped.expand(x, y, z).wrap()

    override fun offset(x: Double, y: Double, z: Double): IAxisAlignedBB = wrapped.offset(x, y, z).wrap()
    // </editor-fold>

    // <editor-fold desc="Intercept checks">
    override fun calculateIntercept(from: WVec3, to: WVec3): IMovingObjectPosition? = wrapped.calculateIntercept(from.unwrap(), to.unwrap())?.wrap()

    override fun isVecInside(vec: WVec3): Boolean = wrapped.isVecInside(vec.unwrap())

    override fun intersectsWith(boundingBox: IAxisAlignedBB): Boolean = wrapped.intersectsWith(boundingBox.unwrap())
    // </editor-fold>

    override fun toString(): String = "$wrapped"

    override fun equals(other: Any?): Boolean = other is AxisAlignedBBImpl && other.wrapped == wrapped
}

fun IAxisAlignedBB.unwrap(): AxisAlignedBB = (this as AxisAlignedBBImpl).wrapped
fun AxisAlignedBB.wrap(): IAxisAlignedBB = AxisAlignedBBImpl(this)
