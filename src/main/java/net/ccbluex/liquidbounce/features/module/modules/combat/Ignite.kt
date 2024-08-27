/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.minecraft.block.BlockAir
import net.minecraft.item.Items
import net.minecraft.item.ItemBucket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookOnly
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.util.Direction
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.atan2
import kotlin.math.sqrt

object Ignite : Module("Ignite", Category.COMBAT, hideModule = false) {

    private val lighter by BoolValue("Lighter", true)
    private val lavaBucket by BoolValue("Lava", true)

    private val msTimer = MSTimer()

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (!msTimer.hasTimePassed(500))
            return

        val player = mc.player ?: return
        val theWorld = mc.world ?: return

        val lighterInHotbar = if (lighter) InventoryUtils.findItem(36, 44, Items.flint_and_steel) else -1
        val lavaInHotbar = if (lavaBucket) InventoryUtils.findItem(26, 44, Items.lava_bucket) else -1

        if (lighterInHotbar == -1 && lavaInHotbar == -1)
            return

        val fireInHotbar = if (lighterInHotbar != -1) lighterInHotbar else lavaInHotbar

        for (entity in theWorld.entities) {
            if (EntityUtils.isSelected(entity, true) && !entity.isBurning) {
                val blockPos = entity.position

                if (player.squaredDistanceTo(blockPos) >= 22.3 || !blockPos.isReplaceable() || blockPos.getBlock() !is BlockAir)
                    continue

                RotationUtils.resetTicks += 1

                InventoryUtils.serverSlot = fireInHotbar!! - 36

                val itemStack = player.inventoryContainer.getSlot(fireInHotbar).stack

                if (itemStack.item is ItemBucket) {
                    val diffX = blockPos.x + 0.5 - player.x
                    val diffY = blockPos.y + 0.5 - (player.boundingBox.minY + player.eyeHeight)
                    val diffZ = blockPos.z + 0.5 - player.z
                    val sqrt = sqrt(diffX * diffX + diffZ * diffZ)
                    val yaw = (atan2(diffZ, diffX)).toDegreesF() - 90F
                    val pitch = -(atan2(diffY, sqrt)).toDegreesF()

                    sendPacket(LookOnly(
                            player.yaw +
                                    MathHelper.wrapDegrees(yaw - player.yaw),
                            player.pitch +
                                    MathHelper.wrapDegrees(pitch - player.pitch),
                            player.onGround)
                    )

                    player.sendUseItem(itemStack)
                } else {
                    for (side in Direction.values()) {
                        val neighbor = blockPos.offset(side)

                        if (!neighbor.canBeClicked())
                            continue

                        val diffX = neighbor.x + 0.5 - player.x
                        val diffY = neighbor.y + 0.5 - (player.boundingBox.minY + player.eyeHeight)
                        val diffZ = neighbor.z + 0.5 - player.z
                        val sqrt = sqrt(diffX * diffX + diffZ * diffZ)
                        val yaw = (atan2(diffZ, diffX)).toDegreesF() - 90F
                        val pitch = -(atan2(diffY, sqrt)).toDegreesF()

                        sendPacket(LookOnly(
                                player.yaw +
                                        MathHelper.wrapDegrees(yaw - player.yaw),
                                player.pitch +
                                        MathHelper.wrapDegrees(pitch - player.pitch),
                                player.onGround)
                        )

                        if (player.onPlayerRightClick(neighbor, side.opposite, Vec3d(side.directionVec), itemStack)) {
                            player.swingItem()
                            break
                        }
                    }
                }

                sendPackets(
                    UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot),
                    LookOnly(
                        player.yaw,
                        player.pitch,
                        player.onGround
                    )
                )

                msTimer.reset()
                break
            }
        }
    }
}
