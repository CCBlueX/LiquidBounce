/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.hypixel

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.ClientUtils.displayChatMessage
import net.ccbluex.liquidbounce.utils.MovementUtils.direction
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.extensions.component3
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import net.minecraft.init.Blocks.air
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.server.PlayerPositionLookS2CPacket
import net.minecraft.potion.Potion
import net.minecraft.util.AxisAlignedBB
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object BoostHypixel : FlyMode("BoostHypixel") {
	private var state = 1
	private var moveSpeed = 0.0
	private var lastDistance = 0.0
	private val tickTimer = TickTimer()
	
	override fun onEnable() {
		if (!mc.player.onGround) return
		
		tickTimer.reset()
		
		val (x, y, z) = mc.player

		repeat(10) {
			//Imagine flagging to NCP.
			sendPacket(C04PacketPlayerPosition(x, y, z, true))
		}

		var fallDistance = 3.0125 //add 0.0125 to ensure we get the fall dmg

		while (fallDistance > 0) {
			sendPackets(
				C04PacketPlayerPosition(x, y + 0.0624986421, z, false),
				C04PacketPlayerPosition(x, y + 0.0625, z, false),
				C04PacketPlayerPosition(x, y + 0.0624986421, z, false),
				C04PacketPlayerPosition(x, y + 0.0000013579, z, false)
			)
			fallDistance -= 0.0624986421
		}

		sendPacket(C04PacketPlayerPosition(x, y, z, true))

		mc.player.tryJump()

		mc.player.posY += 0.42f // Visual
		
		state = 1
		moveSpeed = 0.1
		lastDistance = 0.0
	}

	override fun onMotion(event: MotionEvent) {
		when (event.eventState) {
			EventState.PRE -> {
				tickTimer.update()
				
				if (tickTimer.hasTimePassed(2)) {
					mc.player.setPosition(mc.player.posX, mc.player.posY + 1.0E-5, mc.player.posZ)
					
					tickTimer.reset()
				}

				mc.player.velocityY = 0.0
			}
			EventState.POST -> {
				val xDist = mc.player.posX - mc.player.prevPosX
				val zDist = mc.player.posZ - mc.player.prevZ
				lastDistance = sqrt(xDist * xDist + zDist * zDist)
			}
		}
	}

	override fun onMove(event: MoveEvent) {
		if (!isMoving) {
			event.zeroXZ()
			return
		}

		val amplifier =
			1 + (if (mc.player.isPotionActive(Potion.moveSpeed)) 0.2 * (mc.player.getActivePotionEffect(Potion.moveSpeed).amplifier + 1.0) else 0.0)

		val baseSpeed = 0.29 * amplifier

		when (state) {
			1 -> {
				moveSpeed = (if (mc.player.isPotionActive(Potion.moveSpeed)) 1.56 else 2.034) * baseSpeed
				state++
			}
			2 -> {
				moveSpeed *= 2.16
				state++
			}
			3 -> {
				moveSpeed = lastDistance - (if (mc.player.ticksExisted % 2 == 0) 0.0103 else 0.0123) * (lastDistance - baseSpeed)
				state++
			}
			else -> moveSpeed = lastDistance - lastDistance / 159.8
		}

		moveSpeed = moveSpeed.coerceAtMost(0.3)

		val yaw = direction

		event.x = -sin(yaw) * moveSpeed
		event.z = cos(yaw) * moveSpeed

		mc.player.velocityX = event.x
		mc.player.velocityZ = event.z
	}

	override fun onPacket(event: PacketEvent) {
		when (val packet = event.packet) {
			is PlayerPositionLookS2CPacket -> {
				Fly.state = false
				displayChatMessage("§8[§c§lBoostHypixel-§a§lFly§8] §cSetback detected.")
			}
			is C03PacketPlayer -> packet.onGround = false
		}
	}

	override fun onBB(event: BlockBBEvent) {
		if (event.block == air && event.y < mc.player.posY)
			event.boundingBox = AxisAlignedBB.fromBounds(
				event.x.toDouble(),
				event.y.toDouble(),
				event.z.toDouble(),
				event.x + 1.0,
				mc.player.posY,
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
