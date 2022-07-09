/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.network.IPacket
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.render.Breadcrumbs
import net.ccbluex.liquidbounce.utils.FakePlayer
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbowRGB
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerRangeValue
import net.ccbluex.liquidbounce.value.ValueGroup
import org.lwjgl.opengl.GL11.*
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

@ModuleInfo(name = "Blink", description = "Suspends all movement packets. (If you enable Pulse option, you can use this module as FakeLag)", category = ModuleCategory.PLAYER)
class Blink : Module()
{
    private var packetGroup = ValueGroup("Packets")
    private val packetBlockPlaceValue = BoolValue("BlockPlace", true, "BlockPlace-Packets")
    private val packetSwingValue = BoolValue("Swing", true, "Swing-Packets")
    private val packetEntityActionValue = BoolValue("EntityAction", true, "EntityAction-Packets")
    private val packetUseEntityValue = BoolValue("UseEntity", true, "UseEntity-Packets")

    private val pulseGroup = ValueGroup("Pulse")
    private val pulseEnabledValue = BoolValue("Enabled", false, "Pulse")
    private val pulseDelayValue = object : IntegerRangeValue("PulseDelay", 500, 1000, 10, 10000, "MaxPulseDelay" to "MinPulseDelay")
    {
        override fun showCondition() = pulseEnabledValue.get()
    }
    private val displayPreviousPos = BoolValue("DisplayPreviousPos", false)

    /**
     * Variables
     */
    private val packets = LinkedBlockingQueue<IPacket>()
    private val positions = LinkedList<DoubleArray>()

    private val pulseTimer = MSTimer()
    private var pulseDelay = pulseDelayValue.getRandomLong()

    private var fakePlayer: FakePlayer? = null

    init
    {
        packetGroup.addAll(packetBlockPlaceValue, packetSwingValue, packetEntityActionValue, packetUseEntityValue)
        pulseGroup.addAll(pulseEnabledValue, pulseDelayValue)
    }

    override fun onEnable()
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        if (displayPreviousPos.get()) fakePlayer = FakePlayer(theWorld, thePlayer, -13371)

        synchronized(positions) {
            positions.add(doubleArrayOf(thePlayer.posX, thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight * 0.5, thePlayer.posZ))
            positions.add(doubleArrayOf(thePlayer.posX, thePlayer.entityBoundingBox.minY, thePlayer.posZ))
        }

        pulseTimer.reset()
    }

    override fun onDisable()
    {
        blink()
        fakePlayer?.destroy()
    }

    @EventTarget
    fun onPacket(event: PacketEvent)
    {
        val packet: IPacket = event.packet

        mc.thePlayer ?: return

        val provider = classProvider

        if (provider.isCPacketPlayer(packet) || provider.isCPacketPlayerPosition(packet) || provider.isCPacketPlayerPosLook(packet) || packetBlockPlaceValue.get() && provider.isCPacketPlayerBlockPlacement(packet) || packetSwingValue.get() && provider.isCPacketAnimation(packet) || packetEntityActionValue.get() && provider.isCPacketEntityAction(packet) || packetUseEntityValue.get() && provider.isCPacketUseEntity(packet))
        {
            event.cancelEvent()
            packets.add(packet)
        }
    }

    @EventTarget
    fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
    {
        val thePlayer = mc.thePlayer ?: return

        synchronized(positions) { positions.add(doubleArrayOf(thePlayer.posX, thePlayer.entityBoundingBox.minY, thePlayer.posZ)) }

        if (pulseEnabledValue.get() && pulseTimer.hasTimePassed(pulseDelay))
        {
            blink()
            fakePlayer?.updatePositionAndRotation(thePlayer)

            pulseTimer.reset()
            pulseDelay = pulseDelayValue.getRandomLong()
        }
    }

    @EventTarget
    fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
    {
        // The color settings are depended on BreadCrumb's
        val breadcrumbs = LiquidBounce.moduleManager[Breadcrumbs::class.java] as Breadcrumbs
        val color = if (breadcrumbs.colorRainbowEnabledValue.get()) rainbowRGB(breadcrumbs.colorValue.getAlpha(), speed = breadcrumbs.colorRainbowSpeedValue.get(), saturation = breadcrumbs.colorRainbowSaturationValue.get(), brightness = breadcrumbs.colorRainbowBrightnessValue.get()) else breadcrumbs.colorValue.get()

        // Draw the positions
        val renderManager = mc.renderManager
        val viewerPosX = renderManager.viewerPosX
        val viewerPosY = renderManager.viewerPosY
        val viewerPosZ = renderManager.viewerPosZ

        val entityRenderer = mc.entityRenderer

        synchronized(positions) {
            glPushMatrix()
            glDisable(GL_TEXTURE_2D)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            glEnable(GL_LINE_SMOOTH)
            glEnable(GL_BLEND)
            glDisable(GL_DEPTH_TEST)
            entityRenderer.disableLightmap()

            glBegin(GL_LINE_STRIP)
            RenderUtils.glColor(color)

            for (pos in positions) glVertex3d(pos[0] - viewerPosX, pos[1] - viewerPosY, pos[2] - viewerPosZ)

            RenderUtils.resetColor()
            glEnd()

            glEnable(GL_DEPTH_TEST)
            glDisable(GL_LINE_SMOOTH)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            glPopMatrix()
        }
    }

    override val tag: String
        get() = packets.size.toString()

    /**
     * @param blinkMode
     * 0 - Just create the faker
     * 1 - Send all queued packets and update the faker
     * 2 - Send all queued packets and remove the faker from the world
     */
    private fun blink()
    {
        try
        {
            while (packets.isNotEmpty()) mc.netHandler.networkManager.sendPacketWithoutEvent(packets.take())
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }

        synchronized(positions, positions::clear)
    }
}
