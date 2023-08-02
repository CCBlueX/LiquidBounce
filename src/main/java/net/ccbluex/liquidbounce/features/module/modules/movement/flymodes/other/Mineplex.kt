package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.StepEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.mineplexSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.ClientUtils.displayChatMessage
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.init.Blocks
import net.minecraft.init.Blocks.air
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3

object Mineplex : FlyMode("Mineplex") {
	private val timer = MSTimer()


	override fun onUpdate() {
		if (mc.thePlayer.heldItem != null) {
			mc.timer.timerSpeed = 1f
			Fly.state = false
			displayChatMessage("§8[§c§lMineplex-§a§lFly§8] §aSelect an empty slot to fly.")
			return
		}
		
		val (x, y, z) = mc.thePlayer

		if (timer.hasTimePassed(100)) {
			if (mc.gameSettings.keyBindJump.isKeyDown) {
				mc.thePlayer.setPosition(x, y + 0.6, z)
				timer.reset()
			}

			if (mc.gameSettings.keyBindSneak.isKeyDown) {
				mc.thePlayer.setPosition(x, y - 0.6, z)
				timer.reset()
			}
		}

		val blockPos = BlockPos(mc.thePlayer).down()
		val vec = (Vec3(blockPos) + Vec3(0.4, 0.4, 0.4) + Vec3(EnumFacing.UP.directionVec)) * 0.4

		mc.playerController.onPlayerRightClick(
			mc.thePlayer,
			mc.theWorld,
			mc.thePlayer.heldItem,
			blockPos,
			EnumFacing.UP,
			vec
		)

		strafe(0.27f)
		mc.timer.timerSpeed = 1 + mineplexSpeed
	}

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