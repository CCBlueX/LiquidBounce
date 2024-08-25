/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.modules.render.Breadcrumbs
import net.ccbluex.liquidbounce.utils.BlinkUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket
import org.lwjgl.opengl.GL11.*
import java.awt.Color

object Blink : Module("Blink", Category.PLAYER, gameDetecting = false, hideModule = false) {

	private val mode by ListValue("Mode", arrayOf("Sent", "Received", "Both"), "Sent")

    private val pulse by BoolValue("Pulse", false)
		private val pulseDelay by IntegerValue("PulseDelay", 1000, 500..5000) { pulse }

    private val fakePlayerMenu by BoolValue("FakePlayer", true)

    private val pulseTimer = MSTimer()

    override fun onEnable() {
        pulseTimer.reset()

        if (fakePlayerMenu)
            BlinkUtils.addFakePlayer()
    }

    override fun onDisable() {
        if (mc.player == null)
            return

        BlinkUtils.unblink()
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (mc.player == null || mc.player.isDead)
            return

        when (mode.lowercase()) {
            "sent" -> {
                BlinkUtils.blink(packet, event, sent = true, receive = false)
            }
            "received" -> {
                BlinkUtils.blink(packet, event, sent = false, receive = true)
            }
            "both" -> {
                BlinkUtils.blink(packet, event)
            }
        }
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState == EventState.POST) {
            val thePlayer = mc.player ?: return

            if (thePlayer.isDead || mc.player.ticksAlive <= 10) {
                BlinkUtils.unblink()
            }

            when (mode.lowercase()) {
                "sent" -> {
                    BlinkUtils.syncSent()
                }

                "received" -> {
                    BlinkUtils.syncReceived()
                }
            }

            if (pulse && pulseTimer.hasTimePassed(pulseDelay)) {
                BlinkUtils.unblink()
                if (fakePlayerMenu) {
                    BlinkUtils.addFakePlayer()
                }
                pulseTimer.reset()
            }
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val color =
            if (Breadcrumbs.colorRainbow) rainbow()
            else Color(Breadcrumbs.colorRed, Breadcrumbs.colorGreen, Breadcrumbs.colorBlue)

        synchronized(BlinkUtils.positions) {
            glPushMatrix()
            glDisable(GL_TEXTURE_2D)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            glEnable(GL_LINE_SMOOTH)
            glEnable(GL_BLEND)
            glDisable(GL_DEPTH_TEST)
            mc.entityRenderer.disableLightmap()
            glBegin(GL_LINE_STRIP)
            glColor(color)

            val renderPosX = mc.renderManager.viewerPosX
            val renderPosY = mc.renderManager.viewerPosY
            val renderPosZ = mc.renderManager.viewerPosZ

            for (pos in BlinkUtils.positions)
                glVertex3d(pos.xCoord - renderPosX, pos.yCoord - renderPosY, pos.zCoord - renderPosZ)

            glColor4d(1.0, 1.0, 1.0, 1.0)
            glEnd()
            glEnable(GL_DEPTH_TEST)
            glDisable(GL_LINE_SMOOTH)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            glPopMatrix()
        }
    }

    override val tag
        get() = (BlinkUtils.packets.size + BlinkUtils.packetsReceived.size).toString()

    fun blinkingSend() = handleEvents() && (mode == "Sent" || mode == "Both")
    fun blinkingReceive() = handleEvents() && (mode == "Received" || mode == "Both")
}
