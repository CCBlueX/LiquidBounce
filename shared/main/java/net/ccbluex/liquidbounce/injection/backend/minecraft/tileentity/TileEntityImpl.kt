/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.tileentity

import net.ccbluex.liquidbounce.api.minecraft.tileentity.ITileEntity
import net.ccbluex.liquidbounce.api.minecraft.tileentity.ITileEntityChest
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.injection.backend.utils.wrap
import net.minecraft.tileentity.TileEntity
import net.minecraft.tileentity.TileEntityChest

open class TileEntityImpl(open val wrapped: TileEntity) : ITileEntity
{
    override val pos: WBlockPos
        get() = wrapped.pos.wrap()

    override fun asTileEntityChest(): ITileEntityChest = TileEntityChestImpl(wrapped as TileEntityChest)

    override fun equals(other: Any?): Boolean = other is TileEntityImpl && other.wrapped == wrapped
}

fun ITileEntity.unwrap(): TileEntity = (this as TileEntityImpl).wrapped
fun TileEntity.wrap(): ITileEntity = TileEntityImpl(this)
