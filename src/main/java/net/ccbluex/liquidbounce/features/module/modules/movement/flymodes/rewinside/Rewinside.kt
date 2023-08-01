package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.rewinside

import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.StepEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.minecraft.init.Blocks.air
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.AxisAlignedBB

object Rewinside : FlyMode("Rewinside") {
	override fun onPacket(event: PacketEvent) {
		val packet = event.packet

		if (packet is C03PacketPlayer)
			packet.onGround = true
	}

	override fun onBB(event: BlockBBEvent) {
		if (event.block == air && event.y < mc.thePlayer.posY)
			event.boundingBox = AxisAlignedBB.fromBounds(
				event.x.toDouble(),
				event.y.toDouble(),
				event.z.toDouble(),
				event.x + 1.0,
				mc.thePlayer.posY,
				event.z + 1.0
			)
	}

	override fun onJump(event: JumpEvent) {
		event.cancelEvent()
	}

	override fun onStep(event: StepEvent) {
		event.stepHeight = 0f
	}
}