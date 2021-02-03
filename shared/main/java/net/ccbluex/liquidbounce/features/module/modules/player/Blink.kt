/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityOtherPlayerMP
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.network.IPacket
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.render.Breadcrumbs
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

@ModuleInfo(name = "Blink", description = "Suspends all movement packets. (If you enable Pulse option, you can use this module as FakeLag)", category = ModuleCategory.PLAYER)
class Blink : Module()
{
	/**
	 * Options
	 */
	private val pulseValue = BoolValue("Pulse", false)
	private val minPulseDelayValue = IntegerValue("MaxPulseDelay", 1000, 500, 5000)
	private val maxPulseDelayValue = IntegerValue("MinPulseDelay", 500, 500, 5000)
	private val displayPreviousPos = BoolValue("DisplayPreviousPos", false)
	private val blockPlacePackets = BoolValue("BlockPlace-Packets", true)
	private val swingPackets = BoolValue("Swing-Packets", true)
	private val entityActionPackets = BoolValue("EntityAction-Packets", true)
	private val useEntityPackets = BoolValue("UseEntity-Packets", true)

	/**
	 * Variables
	 */
	private val packets = LinkedBlockingQueue<IPacket>()
	private val positions = LinkedList<DoubleArray>()

	private val pulseTimer = MSTimer()
	private var pulseDelay = TimeUtils.randomDelay(minPulseDelayValue.get(), maxPulseDelayValue.get())

	private var fakePlayer: IEntityOtherPlayerMP? = null

	override fun onEnable()
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		if (displayPreviousPos.get())
		{
			val faker: IEntityOtherPlayerMP = classProvider.createEntityOtherPlayerMP(theWorld, thePlayer.gameProfile)


			faker.rotationYawHead = thePlayer.rotationYawHead
			faker.renderYawOffset = thePlayer.renderYawOffset
			faker.copyLocationAndAnglesFrom(thePlayer)
			faker.rotationYawHead = thePlayer.rotationYawHead
			theWorld.addEntityToWorld(-1337, faker)


			fakePlayer = faker
		}

		synchronized(positions) {
			positions.add(doubleArrayOf(thePlayer.posX, thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight / 2, thePlayer.posZ))
			positions.add(doubleArrayOf(thePlayer.posX, thePlayer.entityBoundingBox.minY, thePlayer.posZ))
		}

		pulseTimer.reset()
	}

	override fun onDisable()
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		blink(theWorld, thePlayer, displayPreviousPos.get(), false)
	}

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		val packet: IPacket = event.packet

		mc.thePlayer ?: return

		//		if (classProvider.isCPacketPlayer(packet)) // Cancel all movement stuff
		//			event.cancelEvent()

		if (classProvider.isCPacketPlayer(packet) || classProvider.isCPacketPlayerPosition(packet) || classProvider.isCPacketPlayerPosLook(packet) || blockPlacePackets.get() && classProvider.isCPacketPlayerBlockPlacement(packet) || swingPackets.get() && classProvider.isCPacketAnimation(packet) || entityActionPackets.get() && classProvider.isCPacketEntityAction(packet) || useEntityPackets.get() && classProvider.isCPacketUseEntity(packet))
		{
			event.cancelEvent()
			packets.add(packet)
		}
	}

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent?)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		synchronized(positions) { positions.add(doubleArrayOf(thePlayer.posX, thePlayer.entityBoundingBox.minY, thePlayer.posZ)) }

		if (pulseValue.get() && pulseTimer.hasTimePassed(pulseDelay))
		{
			blink(theWorld, thePlayer, displayPreviousPos.get(), true)
			pulseTimer.reset()
			pulseDelay = TimeUtils.randomDelay(minPulseDelayValue.get(), maxPulseDelayValue.get())
		}
	}

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent?)
	{
		// The color settings are depended on BreadCrumb's
		val breadcrumbs = LiquidBounce.moduleManager[Breadcrumbs::class.java] as Breadcrumbs
		val color = if (breadcrumbs.colorRainbow.get()) rainbow(saturation = breadcrumbs.saturationValue.get(), brightness = breadcrumbs.brightnessValue.get()) else Color(breadcrumbs.colorRedValue.get(), breadcrumbs.colorGreenValue.get(), breadcrumbs.colorBlueValue.get())

		// Draw the positions
		val renderManager = mc.renderManager
		val viewerPosX = renderManager.viewerPosX
		val viewerPosY = renderManager.viewerPosY
		val viewerPosZ = renderManager.viewerPosZ

		val entityRenderer = mc.entityRenderer

		synchronized(positions) {
			GL11.glPushMatrix()
			GL11.glDisable(GL11.GL_TEXTURE_2D)
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
			GL11.glEnable(GL11.GL_LINE_SMOOTH)
			GL11.glEnable(GL11.GL_BLEND)
			GL11.glDisable(GL11.GL_DEPTH_TEST)
			entityRenderer.disableLightmap()

			GL11.glBegin(GL11.GL_LINE_STRIP)
			RenderUtils.glColor(color)

			for (pos in positions) GL11.glVertex3d(pos[0] - viewerPosX, pos[1] - viewerPosY, pos[2] - viewerPosZ)

			GL11.glColor4d(1.0, 1.0, 1.0, 1.0)
			GL11.glEnd()

			GL11.glEnable(GL11.GL_DEPTH_TEST)
			GL11.glDisable(GL11.GL_LINE_SMOOTH)
			GL11.glDisable(GL11.GL_BLEND)
			GL11.glEnable(GL11.GL_TEXTURE_2D)
			GL11.glPopMatrix()
		}
	}

	override val tag: String
		get() = packets.size.toString()

	private fun blink(theWorld: IWorldClient, thePlayer: IEntityPlayerSP, displayPrevPos: Boolean, canCreateFaker: Boolean)
	{
		if (displayPrevPos || !canCreateFaker)
		{
			var faker = fakePlayer
			if (faker != null)
			{
				theWorld.removeEntityFromWorld(faker.entityId)
				fakePlayer = null
			}

			if (canCreateFaker)
			{
				faker = classProvider.createEntityOtherPlayerMP(theWorld, thePlayer.gameProfile)

				faker.rotationYawHead = thePlayer.rotationYawHead
				faker.renderYawOffset = thePlayer.renderYawOffset
				faker.copyLocationAndAnglesFrom(thePlayer)
				faker.rotationYawHead = thePlayer.rotationYawHead
				theWorld.addEntityToWorld(-1337, faker)

				fakePlayer = faker
			}
		}

		try
		{
			while (!packets.isEmpty()) mc.netHandler.networkManager.sendPacketWithoutEvent(packets.take())
		}
		catch (e: Exception)
		{
			e.printStackTrace()
		}

		synchronized(positions, positions::clear)
	}
}
