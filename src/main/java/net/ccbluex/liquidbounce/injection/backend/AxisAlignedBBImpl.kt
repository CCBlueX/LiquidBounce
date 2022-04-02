/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.util.IAxisAlignedBB
import net.ccbluex.liquidbounce.api.minecraft.util.IMovingObjectPosition
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.injection.backend.utils.unwrap
import net.minecraft.util.AxisAlignedBB

class AxisAlignedBBImpl(val wrapped: AxisAlignedBB) : IAxisAlignedBB {
    override fun addCoord(x: Double, y: Double, z: Double): IAxisAlignedBB = wrapped.addCoord(x, y, z).wrap()

    override fun expand(x: Double, y: Double, z: Double): IAxisAlignedBB = wrapped.expand(x, y, z).wrap()

    override fun calculateIntercept(from: WVec3, to: WVec3): IMovingObjectPosition? = wrapped.calculateIntercept(from.unwrap(), to.unwrap())?.wrap()

    override fun isVecInside(vec: WVec3): Boolean = wrapped.isVecInside(vec.unwrap())

    override fun offset(sx: Double, sy: Double, sz: Double): IAxisAlignedBB = wrapped.offset(sx, sy, sz).wrap()

    override fun intersectsWith(boundingBox: IAxisAlignedBB): Boolean = wrapped.intersectsWith(boundingBox.unwrap())

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

    override fun equals(other: Any?): Boolean {
        return other is AxisAlignedBBImpl && other.wrapped == this.wrapped
    }
}

inline fun IAxisAlignedBB.unwrap(): AxisAlignedBB = (this as AxisAlignedBBImpl).wrapped
inline fun AxisAlignedBB.wrap(): IAxisAlignedBB = AxisAlignedBBImpl(this)