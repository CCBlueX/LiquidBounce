/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.options
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.getVec
import net.ccbluex.liquidbounce.utils.extensions.isNearEdge
import net.ccbluex.liquidbounce.utils.extensions.sendUseItem
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverSlot
import net.ccbluex.liquidbounce.utils.timing.TickedActions
import net.ccbluex.liquidbounce.utils.timing.WaitTickUtils
import net.minecraft.init.Items
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.util.BlockPos

object Fireball : FlyMode("Fireball") {

    override fun onMotion(event: MotionEvent) {
        val player = mc.thePlayer ?: return

        val fireballSlot = InventoryUtils.findItem(36, 44, Items.fire_charge) ?: return

        when (Fly.autoFireball.lowercase()) {
            "pick" -> {
                player.inventory.currentItem = fireballSlot - 36
                mc.playerController.updateController()
            }

            "spoof", "switch" -> serverSlot = fireballSlot - 36
        }

        if (event.eventState != EventState.POST)
            return

        val customRotation = Rotation(if (Fly.invertYaw) RotationUtils.invertYaw(player.rotationYaw) else player.rotationYaw,
            Fly.rotationPitch
        )

        if (player.onGround && !mc.theWorld.isAirBlock(BlockPos(player.posX, player.posY - 1, player.posZ))) {
            Fly.firePosition = BlockPos(player.posX, player.posY - 1, player.posZ)
        }

        val smartRotation = Fly.firePosition?.getVec()?.let { RotationUtils.toRotation(it, false, player) }
        val rotation = if (Fly.pitchMode == "Custom") customRotation else smartRotation

        if (options.rotationsActive && rotation != null) {
            RotationUtils.setTargetRotation(rotation, options, if (options.keepRotation) options.resetTicks else 1)
        }

        if (Fly.fireBallThrowMode == "Edge" && !player.isNearEdge(Fly.edgeThreshold))
            return

        if (Fly.autoJump && player.onGround && !Fly.wasFired) {
            player.tryJump()
        }
    }

    override fun onTick() {
        val player = mc.thePlayer ?: return

        val fireballSlot = InventoryUtils.findItem(36, 44, Items.fire_charge) ?: return
        val fireBall = player.inventoryContainer.getSlot(fireballSlot).stack

        if (Fly.fireBallThrowMode == "Edge" && !player.isNearEdge(Fly.edgeThreshold))
            return

        if (Fly.wasFired) {
            return
        }

        if (isMoving) {
            TickedActions.TickScheduler(Fly) += {
                if (Fly.swing) player.swingItem() else sendPacket(C0APacketAnimation())

                // NOTE: You may increase max try to `2` if fireball doesn't work. (Ex: BlocksMC)
                repeat(Fly.fireballTry) {
                    player.sendUseItem(fireBall)
                }
            }

            WaitTickUtils.scheduleTicks(2) {
                if (Fly.autoFireball == "Pick") {
                    player.inventory.currentItem = fireballSlot - 36
                    mc.playerController.updateController()
                } else {
                    serverSlot = fireballSlot - 36
                }

                Fly.wasFired = true
            }
        }
    }
}
