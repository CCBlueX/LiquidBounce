/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.util.IEnumFacing
import net.ccbluex.liquidbounce.api.minecraft.util.IMovingObjectPosition
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.injection.backend.utils.wrap
import net.minecraft.util.MovingObjectPosition

class MovingObjectPositionImpl(val wrapped: MovingObjectPosition) : IMovingObjectPosition {
    override val entityHit: IEntity?
        get() = wrapped.entityHit?.wrap()
    override val blockPos: WBlockPos?
        get() = wrapped.blockPos?.wrap()
    override val sideHit: IEnumFacing?
        get() = wrapped.sideHit?.wrap()
    override val hitVec: WVec3
        get() = wrapped.hitVec.wrap()
    override val typeOfHit: IMovingObjectPosition.WMovingObjectType
        get() = wrapped.typeOfHit.wrap()


    override fun equals(other: Any?): Boolean {
        return other is MovingObjectPositionImpl && other.wrapped == this.wrapped
    }
}

inline fun IMovingObjectPosition.unwrap(): MovingObjectPosition = (this as MovingObjectPositionImpl).wrapped
inline fun MovingObjectPosition.wrap(): IMovingObjectPosition = MovingObjectPositionImpl(this)