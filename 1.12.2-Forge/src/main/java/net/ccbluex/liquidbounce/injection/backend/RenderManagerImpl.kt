/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.api.minecraft.renderer.entity.IRenderManager
import net.ccbluex.liquidbounce.api.minecraft.tileentity.ITileEntity
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher

class RenderManagerImpl(val wrapped: RenderManager) : IRenderManager {
    override var isRenderShadow: Boolean
        get() = wrapped.isRenderShadow
        set(value) {
            wrapped.isRenderShadow = value
        }
    override val viewerPosX: Double
        get() = wrapped.viewerPosX
    override val viewerPosY: Double
        get() = wrapped.viewerPosY
    override val viewerPosZ: Double
        get() = wrapped.viewerPosZ
    override val playerViewX: Float
        get() = wrapped.playerViewX
    override var playerViewY: Float
        get() = wrapped.playerViewY
        set(value) {
            wrapped.setPlayerViewY(value)
        }
    override val renderPosX: Double
        get() = wrapped.renderPosX
    override val renderPosY: Double
        get() = wrapped.renderPosY
    override val renderPosZ: Double
        get() = wrapped.renderPosZ

    override fun renderEntityStatic(entity: IEntity, renderPartialTicks: Float, hideDebugBox: Boolean): Boolean {
        wrapped.renderEntityStatic(entity.unwrap(), renderPartialTicks, hideDebugBox)

        return true
    }

    override fun renderEntityAt(entity: ITileEntity, x: Double, y: Double, z: Double, partialTicks: Float) = TileEntityRendererDispatcher.instance.render(entity.unwrap(), x, y, z, partialTicks)

    override fun renderEntityWithPosYaw(entityLivingBase: IEntityLivingBase, d: Double, d1: Double, d2: Double, fl: Float, fl1: Float): Boolean {
        wrapped.renderEntity(entityLivingBase.unwrap(), d, d1, d2, fl, fl1, true)

        return true
    }


    override fun equals(other: Any?): Boolean {
        return other is RenderManagerImpl && other.wrapped == this.wrapped
    }
}

inline fun IRenderManager.unwrap(): RenderManager = (this as RenderManagerImpl).wrapped
inline fun RenderManager.wrap(): IRenderManager = RenderManagerImpl(this)