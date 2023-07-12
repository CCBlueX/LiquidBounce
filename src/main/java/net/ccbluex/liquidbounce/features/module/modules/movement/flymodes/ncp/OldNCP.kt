package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.ncp

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.startY
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.extensions.component3
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

object OldNCP : FlyMode("OldNCP") {
	override fun onEnable() {
		if (!mc.thePlayer.onGround) return

		val (x, y, z) = mc.thePlayer

		repeat(4) {
			sendPackets(
				C04PacketPlayerPosition(x, y + 1.01, z, false),
				C04PacketPlayerPosition(x, y, z, false)
			)
		}

		mc.thePlayer.jump()
		mc.thePlayer.swingItem()
	}

	override fun onUpdate() {
		if (startY > mc.thePlayer.posY)
			mc.thePlayer.motionY = -0.000000000000000000000000000000001

		if (mc.gameSettings.keyBindSneak.isKeyDown)
			mc.thePlayer.motionY = -0.2

		if (mc.gameSettings.keyBindJump.isKeyDown && mc.thePlayer.posY < startY - 0.1)
			mc.thePlayer.motionY = 0.2

		strafe()
	}
}