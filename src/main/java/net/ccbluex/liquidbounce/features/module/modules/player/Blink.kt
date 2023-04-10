/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.render.Breadcrumbs
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.network.Packet
import net.minecraft.network.play.client.*
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

object Blink : Module("Blink", "Suspends all movement packets.", ModuleCategory.PLAYER) {

    private val packets = LinkedBlockingQueue<Packet<*>>()
    private var fakePlayer: EntityOtherPlayerMP? = null
    private var disableLogger = false
    private val positions = LinkedList<DoubleArray>()
    private val pulseValue = BoolValue("Pulse", false)
    private val pulseDelayValue = object : IntegerValue("PulseDelay", 1000, 500, 5000) {
        override fun isSupported() = pulseValue.get()
    }
    private val pulseTimer = MSTimer()

    override fun onEnable() {
        val thePlayer = mc.thePlayer ?: return

        if (!pulseValue.get()) {
            val faker = EntityOtherPlayerMP(mc.theWorld, thePlayer.gameProfile)

            faker.rotationYawHead = thePlayer.rotationYawHead
            faker.renderYawOffset = thePlayer.renderYawOffset
            faker.copyLocationAndAnglesFrom(thePlayer)
            faker.rotationYawHead = thePlayer.rotationYawHead
            mc.theWorld.addEntityToWorld(-1337, faker)

            fakePlayer = faker
        }
        synchronized(positions) {
            positions.add(
                doubleArrayOf(
                    thePlayer.posX,
                    thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight / 2,
                    thePlayer.posZ
                )
            )
            positions.add(doubleArrayOf(thePlayer.posX, thePlayer.entityBoundingBox.minY, thePlayer.posZ))
        }
        pulseTimer.reset()
    }

    override fun onDisable() {
        if (mc.thePlayer == null)
            return

        blink()

        val faker = fakePlayer

        if (faker != null) {
            mc.theWorld?.removeEntityFromWorld(faker.entityId)
            fakePlayer = null
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (mc.thePlayer == null || disableLogger)
            return

        if (packet is C03PacketPlayer) // Cancel all movement stuff
            event.cancelEvent()

        if (packet is C03PacketPlayer.C04PacketPlayerPosition || packet is C03PacketPlayer.C06PacketPlayerPosLook ||
            packet is C08PacketPlayerBlockPlacement ||
            packet is C0APacketAnimation ||
            packet is C0BPacketEntityAction || packet is C02PacketUseEntity
        ) {
            event.cancelEvent()
            packets.add(packet)
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.thePlayer ?: return

        synchronized(positions) {
            positions.add(
                doubleArrayOf(
                    thePlayer.posX,
                    thePlayer.entityBoundingBox.minY,
                    thePlayer.posZ
                )
            )
        }
        if (pulseValue.get() && pulseTimer.hasTimePassed(pulseDelayValue.get())) {
            blink()
            pulseTimer.reset()
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val color = if (Breadcrumbs.colorRainbow.get()) rainbow() else Color(
            Breadcrumbs.colorRedValue.get(),
            Breadcrumbs.colorGreenValue.get(),
            Breadcrumbs.colorBlueValue.get()
        )
        synchronized(positions) {
            glPushMatrix()
            glDisable(GL_TEXTURE_2D)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            glEnable(GL_LINE_SMOOTH)
            glEnable(GL_BLEND)
            glDisable(GL_DEPTH_TEST)
            mc.entityRenderer.disableLightmap()
            glBegin(GL_LINE_STRIP)
            glColor(color)
            val renderPosX: Double = mc.renderManager.viewerPosX
            val renderPosY: Double = mc.renderManager.viewerPosY
            val renderPosZ: Double = mc.renderManager.viewerPosZ
            for (pos in positions) glVertex3d(pos[0] - renderPosX, pos[1] - renderPosY, pos[2] - renderPosZ)
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
        get() = packets.size.toString()

    private fun blink() {
        try {
            disableLogger = true

            while (!packets.isEmpty()) {
                mc.netHandler.networkManager.sendPacket(packets.take())
            }

            disableLogger = false
        } catch (e: Exception) {
            e.printStackTrace()
            disableLogger = false
        }
        synchronized(positions) { positions.clear() }
    }
}