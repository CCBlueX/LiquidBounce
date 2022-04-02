/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.tileentity.ITileEntity
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.injection.backend.utils.wrap
import net.minecraft.tileentity.TileEntity

class TileEntityImpl(val wrapped: TileEntity) : ITileEntity {
    override val pos: WBlockPos
        get() = wrapped.pos.wrap()

    override fun equals(other: Any?): Boolean {
        return other is TileEntityImpl && other.wrapped == this.wrapped
    }
}

inline fun ITileEntity.unwrap(): TileEntity = (this as TileEntityImpl).wrapped
inline fun TileEntity.wrap(): ITileEntity = TileEntityImpl(this)