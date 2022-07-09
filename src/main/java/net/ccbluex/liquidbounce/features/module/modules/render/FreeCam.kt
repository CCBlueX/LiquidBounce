/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.FakePlayer
import net.ccbluex.liquidbounce.utils.extensions.strafe
import net.ccbluex.liquidbounce.utils.extensions.zeroXYZ
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue

@ModuleInfo(name = "FreeCam", description = "Allows you to move out of your body.", category = ModuleCategory.RENDER)
class FreeCam : Module()
{
	private val speedValue = FloatValue("Speed", 0.8f, 0.1f, 2f)
	private val flyValue = BoolValue("Fly", true)
	private val noClipValue = BoolValue("NoClip", true)

	private var fakePlayer: FakePlayer? = null

	private var oldX = 0.0
	private var oldY = 0.0
	private var oldZ = 0.0
	private var oldYaw = 0.0f
	private var oldPitch = 0.0f
	private var oldGround = false

	override fun onEnable()
	{
		val thePlayer = mc.thePlayer ?: return
		val theWorld = mc.theWorld ?: return

		oldX = thePlayer.posX
		oldY = thePlayer.posY
		oldZ = thePlayer.posZ
		oldYaw = thePlayer.rotationYaw
		oldPitch = thePlayer.rotationPitch
		oldGround = thePlayer.onGround

		fakePlayer = FakePlayer(theWorld, thePlayer, -13370)

		if (noClipValue.get()) thePlayer.noClip = true
	}

	override fun onDisable()
	{
		val thePlayer = mc.thePlayer ?: return

		fakePlayer?.destroy()

		thePlayer.setPositionAndRotation(oldX, oldY, oldZ, oldYaw, oldPitch)

		thePlayer.zeroXYZ()
	}

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		if (noClipValue.get()) thePlayer.noClip = true

		thePlayer.fallDistance = 0.0f

		if (flyValue.get())
		{
			val value = speedValue.get()

			thePlayer.zeroXYZ()

			val gameSettings = mc.gameSettings
			if (gameSettings.keyBindJump.isKeyDown) thePlayer.motionY += value
			if (gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY -= value

			thePlayer.strafe(value)
		}
	}

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		val packet = event.packet

		val provider = classProvider

		if (provider.isCPacketPlayer(packet)) // To bypass FreeCam checks, we need to keep sending normal packets.
		{
			val movePacket = packet.asCPacketPlayer()

			if (movePacket.moving)
			{
				movePacket.x = oldX
				movePacket.y = oldY
				movePacket.z = oldZ
			}

			movePacket.onGround = oldGround

			if (movePacket.rotating)
			{
				movePacket.yaw = oldYaw
				movePacket.pitch = oldPitch
			}
		}

		if (provider.isCPacketEntityAction(packet)) event.cancelEvent()
	}

	override val tag: String
		get() = "$oldX, $oldY, $oldZ, $oldGround, $oldYaw, $oldPitch"
}
