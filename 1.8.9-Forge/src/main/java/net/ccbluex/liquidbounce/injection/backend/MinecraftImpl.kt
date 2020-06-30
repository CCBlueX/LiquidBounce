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
import net.ccbluex.liquidbounce.api.minecraft.client.render.texture.ITextureManager
import net.ccbluex.liquidbounce.api.minecraft.client.renderer.IEntityRenderer
import net.ccbluex.liquidbounce.api.minecraft.client.settings.IGameSettings
import net.ccbluex.liquidbounce.api.minecraft.renderer.entity.IRenderManager
import net.ccbluex.liquidbounce.api.minecraft.util.IMovingObjectPosition
import net.ccbluex.liquidbounce.api.minecraft.util.ISession
import net.ccbluex.liquidbounce.api.minecraft.util.ITimer
import net.ccbluex.liquidbounce.injection.backend.utils.wrap
import net.minecraft.client.Minecraft

class MinecraftImpl(val wrapped: Minecraft) : IMinecraft {
    override val entityRenderer: IEntityRenderer
        get() = TODO("Not yet implemented")
    override var rightClickDelayTimer: Int
        get() = wrapped.rightClickDelayTimer
        set(value) {
            wrapped.rightClickDelayTimer = value
        }
    override val session: ISession
        get() = TODO("Not yet implemented")
    override val soundHandler: ISoundHandler
        get() = TODO("Not yet implemented")
    override val objectMouseOver: IMovingObjectPosition?
        get() = TODO("Not yet implemented")
    override val timer: ITimer
        get() = TimerImpl(wrapped.timer)
    override val renderManager: IRenderManager
        get() = TODO("Not yet implemented")
    override val playerController: IPlayerControllerMP
        get() = TODO("Not yet implemented")
    override val currentScreen: IGuiScreen?
        get() = TODO("Not yet implemented")
    override val renderViewEntity: IEntity?
        get() = wrapped.renderViewEntity.wrap()
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
}