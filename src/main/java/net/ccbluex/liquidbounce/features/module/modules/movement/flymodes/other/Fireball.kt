/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverSlot
import net.ccbluex.liquidbounce.utils.timing.TickedActions
import net.ccbluex.liquidbounce.utils.timing.WaitTickUtils
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.util.math.BlockPos

object Fireball : FlyMode("Fireball") {

    override fun onMotion(event: MotionEvent) {
        val player = mc.player ?: return

        val fireballSlot = InventoryUtils.findItem(36, 44, Items.fire_charge) ?: return

        when (Fly.autoFireball.lowercase()) {
            "pick" -> {
                player.inventory.selectedSlot = fireballSlot - 36
               mc.interactionManager.syncSelectedSlot()
            }

            "spoof", "switch" -> serverSlot = fireballSlot - 36
        }

        if (event.eventState != EventState.POST)
            return

        val customRotation = Rotation(if (Fly.invertYaw) RotationUtils.invertYaw(player.yaw) else player.yaw, Fly.rotationPitch)

        if (player.onGround && !mc.world.isAir(BlockPos(player.x, player.z - 1, player.z))) Fly.firePosition = BlockPos(player.x, player.z - 1, player.z)

        val smartRotation = Fly.firePosition?.getVec()?.let { RotationUtils.toRotation(it, false, player) }
        val rotation = if (Fly.pitchMode == "Custom") customRotation else smartRotation

        if (Fly.rotations) {
            if (rotation != null) {
                RotationUtils.setTargetRotation(
                    rotation,
                    if (Fly.keepRotation) Fly.keepTicks else 1,
                    turnSpeed = Fly.minHorizontalSpeed.get()..Fly.maxHorizontalSpeed.get() to Fly.minVerticalSpeed.get()..Fly.maxVerticalSpeed.get(),
                    angleThresholdForReset = Fly.angleThresholdUntilReset,
                    smootherMode = Fly.smootherMode,
                    simulateShortStop = Fly.simulateShortStop,
                    startOffSlow = Fly.startFirstRotationSlow
                )
            }
        }

        if (Fly.fireBallThrowMode == "Edge" && !player.isNearEdge(Fly.edgeThreshold))
            return

        if (Fly.autoJump && player.onGround && !Fly.wasFired) {
            player.tryJump()
        }
    }

    override fun onTick() {
        val player = mc.player ?: return

        val fireballSlot = InventoryUtils.findItem(36, 44, Items.fire_charge) ?: return
        val fireBall = player.playerScreenHandler.getSlot(fireballSlot).stack

        if (Fly.fireBallThrowMode == "Edge" && !player.isNearEdge(Fly.edgeThreshold))
            return

        if (Fly.wasFired) {
            return
        }

        if (isMoving) {
            TickedActions.TickScheduler(Fly) += {
                if (Fly.swing) player.swingHand() else sendPacket(HandSwingC2SPacket())

                // NOTE: You may increase max try to `2` if fireball doesn't work. (Ex: BlocksMC)
                repeat(Fly.fireballTry) {
                    player.sendUseItem(fireBall)
                }
            }

            WaitTickUtils.scheduleTicks(2) {
                if (Fly.autoFireball == "Pick") {
                    player.inventory.selectedSlot = fireballSlot - 36
                   mc.interactionManager.syncSelectedSlot()
                } else {
                    serverSlot = fireballSlot - 36
                }

                Fly.wasFired = true
            }
        }
    }
}
