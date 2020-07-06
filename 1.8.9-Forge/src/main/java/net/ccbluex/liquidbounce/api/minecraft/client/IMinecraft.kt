/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.client

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
import java.io.File

interface IMinecraft {
    val framebuffer: IFramebuffer
    val isFullScreen: Boolean
    val dataDir: File
    val debugFPS: Int
    val renderGlobal: IRenderGlobal
    val renderItem: IRenderItem
    val displayWidth: Int
    val displayHeight: Int
    val entityRenderer: IEntityRenderer
    var rightClickDelayTimer: Int
    var session: ISession
    val soundHandler: ISoundHandler
    val objectMouseOver: IMovingObjectPosition?
    val timer: ITimer
    val renderManager: IRenderManager
    val playerController: IPlayerControllerMP
    val currentScreen: IGuiScreen?
    var renderViewEntity: IEntity?
    val netHandler: IINetHandlerPlayClient
    val theWorld: IWorldClient?
    val thePlayer: IEntityPlayerSP?
    val textureManager: ITextureManager
    val isIntegratedServerRunning: Boolean
    val currentServerData: IServerData?
    val gameSettings: IGameSettings
    val fontRendererObj: IFontRenderer

    fun displayGuiScreen(screen: IGuiScreen?)
    fun rightClickMouse()
    fun shutdown()
    fun toggleFullscreen()
}