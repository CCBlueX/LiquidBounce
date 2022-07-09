/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.renderer.entity

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.api.minecraft.renderer.entity.IRenderManager
import net.ccbluex.liquidbounce.api.minecraft.tileentity.ITileEntity
import net.ccbluex.liquidbounce.injection.backend.minecraft.entity.unwrap
import net.ccbluex.liquidbounce.injection.backend.minecraft.tileentity.unwrap
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher

class RenderManagerImpl(val wrapped: RenderManager) : IRenderManager
{
    override var isRenderShadow: Boolean
        get() = wrapped.isRenderShadow
        set(value)
        {
            wrapped.isRenderShadow = value
        }

    override val viewerPosX: Double
        get() = wrapped.viewerPosX
    override val viewerPosY: Double
        get() = wrapped.viewerPosY
    override val viewerPosZ: Double
        get() = wrapped.viewerPosZ

    override val renderPosX: Double
        get() = wrapped.renderPosX
    override val renderPosY: Double
        get() = wrapped.renderPosY
    override val renderPosZ: Double
        get() = wrapped.renderPosZ

    override val playerViewX: Float
        get() = wrapped.playerViewX
    override var playerViewY: Float
        get() = wrapped.playerViewY
        set(value)
        {
            wrapped.setPlayerViewY(value)
        }

    override fun renderEntityStatic(entity: IEntity, renderPartialTicks: Float, hideDebugBox: Boolean) = wrapped.renderEntityStatic(entity.unwrap(), renderPartialTicks, hideDebugBox)

    override fun renderEntityAt(entity: ITileEntity, x: Double, y: Double, z: Double, partialTicks: Float) = TileEntityRendererDispatcher.instance.renderTileEntityAt(entity.unwrap(), x, y, z, partialTicks)

    override fun renderEntityWithPosYaw(entityLivingBase: IEntityLivingBase, d: Double, d1: Double, d2: Double, fl: Float, fl1: Float) = wrapped.renderEntityWithPosYaw(entityLivingBase.unwrap(), d, d1, d2, fl, fl1)

    override fun equals(other: Any?): Boolean = other is RenderManagerImpl && other.wrapped == wrapped
}

fun IRenderManager.unwrap(): RenderManager = (this as RenderManagerImpl).wrapped
fun RenderManager.wrap(): IRenderManager = RenderManagerImpl(this)
