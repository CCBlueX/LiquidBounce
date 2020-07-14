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
import net.ccbluex.liquidbounce.api.minecraft.client.shader.IFramebuffer
import net.ccbluex.liquidbounce.api.minecraft.renderer.entity.IRenderManager
import net.ccbluex.liquidbounce.api.minecraft.util.IMovingObjectPosition
import net.ccbluex.liquidbounce.api.minecraft.util.ISession
import net.ccbluex.liquidbounce.api.minecraft.util.ITimer
import net.minecraft.client.Minecraft
import java.io.File

class MinecraftImpl(val wrapped: Minecraft) : IMinecraft {
    override val framebuffer: IFramebuffer
        get() = wrapped.framebuffer.wrap()
    override val isFullScreen: Boolean
        get() = wrapped.isFullScreen
    override val dataDir: File
        get() = wrapped.mcDataDir
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
        get() = wrapped.playerController.wrap()
    override val currentScreen: IGuiScreen?
        get() = wrapped.currentScreen?.wrap()
    override var renderViewEntity: IEntity?
        get() = wrapped.renderViewEntity?.wrap()
        set(value) {
            wrapped.renderViewEntity = value?.unwrap()
        }
    override val netHandler: IINetHandlerPlayClient
        get() = wrapped.connection!!.wrap()
    override val theWorld: IWorldClient?
        get() = wrapped.world?.wrap()
    override val thePlayer: IEntityPlayerSP?
        get() = wrapped.player?.wrap()
    override val textureManager: ITextureManager
        get() = wrapped.textureManager.wrap()
    override val isIntegratedServerRunning: Boolean
        get() = wrapped.isIntegratedServerRunning
    override val currentServerData: IServerData?
        get() = wrapped.currentServerData?.wrap()
    override val gameSettings: IGameSettings
        get() = GameSettingsImpl(wrapped.gameSettings)
    override val fontRendererObj: IFontRenderer
        get() = wrapped.fontRenderer.wrap()

    override fun displayGuiScreen(screen: IGuiScreen?) = wrapped.displayGuiScreen(screen?.unwrap())

    override fun rightClickMouse() = wrapped.rightClickMouse()
    override fun shutdown() = wrapped.shutdown()
    override fun toggleFullscreen() = wrapped.toggleFullscreen()

    override fun equals(other: Any?): Boolean {
        return other is MinecraftImpl && other.wrapped == this.wrapped
    }
}

inline fun IMinecraft.unwrap(): Minecraft = (this as MinecraftImpl).wrapped
inline fun Minecraft.wrap(): IMinecraft = MinecraftImpl(this)