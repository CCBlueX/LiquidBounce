/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.IMinecraft
import net.ccbluex.liquidbounce.api.minecraft.client.audio.ISoundHandler
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IFontRenderer
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiScreen
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IPlayerControllerMP
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IServerData
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.client.network.IINetHandlerPlayClient
import net.ccbluex.liquidbounce.api.minecraft.client.render.entity.IRenderItem
import net.ccbluex.liquidbounce.api.minecraft.client.render.texture.ITextureManager
import net.ccbluex.liquidbounce.api.minecraft.client.renderer.IEntityRenderer
import net.ccbluex.liquidbounce.api.minecraft.client.renderer.IRenderGlobal
import net.ccbluex.liquidbounce.api.minecraft.client.settings.IGameSettings
import net.ccbluex.liquidbounce.api.minecraft.renderer.entity.IRenderManager
import net.ccbluex.liquidbounce.api.minecraft.util.IMovingObjectPosition
import net.ccbluex.liquidbounce.api.minecraft.util.ISession
import net.ccbluex.liquidbounce.api.minecraft.util.ITimer
import net.ccbluex.liquidbounce.injection.backend.utils.unwrap
import net.ccbluex.liquidbounce.injection.backend.utils.wrap
import net.minecraft.client.Minecraft

class MinecraftImpl(val wrapped: Minecraft) : IMinecraft {
    override val debugFPS: Int
        get() = Minecraft.getDebugFPS()
    override val renderGlobal: IRenderGlobal
        get() = wrapped.renderGlobal.wrap()
    override val renderItem: IRenderItem
        get() = wrapped.renderItem.wrap()
    override val displayWidth: Int
        get() = wrapped.displayWidth
    override val displayHeight: Int
        get() = wrapped.displayHeight
    override val entityRenderer: IEntityRenderer
        get() = wrapped.entityRenderer.wrap()
    override var rightClickDelayTimer: Int
        get() = wrapped.rightClickDelayTimer
        set(value) {
            wrapped.rightClickDelayTimer = value
        }
    override var session: ISession
        get() = wrapped.session.wrap()
        set(value) {
            wrapped.session = value.unwrap()
        }
    override val soundHandler: ISoundHandler
        get() = wrapped.soundHandler.wrap()
    override val objectMouseOver: IMovingObjectPosition?
        get() = wrapped.objectMouseOver?.wrap()
    override val timer: ITimer
        get() = wrapped.timer.wrap()
    override val renderManager: IRenderManager
        get() = wrapped.renderManager.wrap()
    override val playerController: IPlayerControllerMP
        get() = wrapped.playerController.wrap
    override val currentScreen: IGuiScreen?
        get() = TODO("Not yet implemented")
    override var renderViewEntity: IEntity?
        get() = wrapped.renderViewEntity.wrap()
        set(value) {
            wrapped.renderViewEntity = value?.unwrap()
        }
    override val netHandler: IINetHandlerPlayClient
        get() = TODO("Not yet implemented")
    override val theWorld: IWorldClient?
        get() = TODO("Not yet implemented")
    override val thePlayer: IEntityPlayerSP?
        get() = wrapped.thePlayer?.wrap()
    override val textureManager: ITextureManager
        get() = TODO("Not yet implemented")
    override val isIntegratedServerRunning: Boolean
        get() = wrapped.isIntegratedServerRunning
    override val currentServerData: IServerData?
        get() = TODO("Not yet implemented")
    override val gameSettings: IGameSettings
        get() = GameSettingsImpl(wrapped.gameSettings)
    override val fontRendererObj: IFontRenderer
        get() = TODO("Not yet implemented")

    override fun displayGuiScreen(screen: IGuiScreen?) {
        TODO("Not yet implemented")
    }

    override fun rightClickMouse() = wrapped.rightClickMouse()
    override fun shutdown() = wrapped.shutdown()
}