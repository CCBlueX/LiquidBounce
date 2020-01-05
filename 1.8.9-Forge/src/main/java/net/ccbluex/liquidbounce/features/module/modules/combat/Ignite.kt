package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.canBeClicked
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isReplaceable
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.minecraft.block.BlockAir
import net.minecraft.init.Items
import net.minecraft.item.ItemBucket
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.util.EnumFacing
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * Zündet einfach alle zuverlässig auf Gomme an.
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ModuleInfo(name = "Ignite", description = "Automatically sets targets aroung you on fire.", category = ModuleCategory.COMBAT)
class Ignite : Module() {
    private val lighterValue = BoolValue("Lighter", true)
    private val lavaBucketValue = BoolValue("Lava", true)
    private val msTimer = MSTimer()

    @EventTarget
    fun onUpdate(event: UpdateEvent?) {
        if (!msTimer.hasTimePassed(500L))
            return

        val lighterInHotbar = if (lighterValue.get()) InventoryUtils.findItem(36, 45, Items.flint_and_steel) else -1
        val lavaInHotbar = if (lavaBucketValue.get()) InventoryUtils.findItem(26, 45, Items.lava_bucket) else -1

        if (lighterInHotbar == -1 && lavaInHotbar == -1)
            return

        val fireInHotbar = if (lighterInHotbar != -1) lighterInHotbar else lavaInHotbar

        for (entity in mc.theWorld.loadedEntityList) {
            if (EntityUtils.isSelected(entity, true) && !entity.isBurning) {
                val blockPos = entity.position

                if (mc.thePlayer.getDistanceSq(blockPos) >= 22.3 || !isReplaceable(blockPos) || getBlock(blockPos) !is BlockAir)
                    continue

                RotationUtils.keepCurrentRotation = true

                mc.netHandler.addToSendQueue(C09PacketHeldItemChange(fireInHotbar - 36))

                val itemStack = mc.thePlayer.inventoryContainer.getSlot(fireInHotbar).stack

                if (itemStack.item is ItemBucket) {
                    val diffX = blockPos.x + 0.5 - mc.thePlayer.posX
                    val diffY = blockPos.y + 0.5 - (mc.thePlayer.entityBoundingBox.minY + mc.thePlayer.getEyeHeight())
                    val diffZ = blockPos.z + 0.5 - mc.thePlayer.posZ

                    val dist = sqrt(diffX * diffX + diffZ * diffZ)
                    val yaw = (atan2(diffZ, diffX) * 180.0 / Math.PI).toFloat() - 90f
                    val pitch = (-(atan2(diffY, dist) * 180.0 / Math.PI)).toFloat()

                    mc.netHandler.addToSendQueue(C05PacketPlayerLook(mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw), mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - mc.thePlayer.rotationPitch), mc.thePlayer.onGround))
                    mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, itemStack)
                } else {
                    for (side in EnumFacing.values()) {
                        val neighbor = blockPos.offset(side)

                        if (!canBeClicked(neighbor))
                            continue

                        val diffX = neighbor.x + 0.5 - mc.thePlayer.posX
                        val diffY = neighbor.y + 0.5 - (mc.thePlayer.entityBoundingBox.minY + mc.thePlayer.getEyeHeight())
                        val diffZ = neighbor.z + 0.5 - mc.thePlayer.posZ

                        val dist = sqrt(diffX * diffX + diffZ * diffZ)
                        val yaw = (atan2(diffZ, diffX) * 180.0 / Math.PI).toFloat() - 90f
                        val pitch = (-(atan2(diffY, dist) * 180.0 / Math.PI)).toFloat()

                        mc.netHandler.addToSendQueue(C05PacketPlayerLook(
                                mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw),
                                mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - mc.thePlayer.rotationPitch),
                                mc.thePlayer.onGround
                        ))

                        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, itemStack, neighbor, side.opposite, Vec3(side.directionVec))) {
                            mc.thePlayer.swingItem()
                            break
                        }

                    }
                }
                mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                RotationUtils.keepCurrentRotation = false
                mc.netHandler.addToSendQueue(C05PacketPlayerLook(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, mc.thePlayer.onGround))

                msTimer.reset()
                break
            }
        }
    }
}