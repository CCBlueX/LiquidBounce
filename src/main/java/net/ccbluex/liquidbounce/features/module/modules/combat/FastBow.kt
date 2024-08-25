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
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.extensions.rotation
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.item.ItemBow
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook
import net.minecraft.network.play.client.PlayerActionC2SPacket
import net.minecraft.network.play.client.PlayerActionC2SPacket.Action.RELEASE_USE_ITEM
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.math.BlockPos
import net.minecraft.util.Direction

object FastBow : Module("FastBow", Category.COMBAT, hideModule = false) {

    private val packets by IntegerValue("Packets", 20, 3..20)

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.player ?: return

        if (!thePlayer.isUsingItem)
            return

        val currentItem = thePlayer.inventory.getCurrentItem()

        if (currentItem != null && currentItem.item is ItemBow) {
            sendPacket(
                C08PacketPlayerBlockPlacement(
                    BlockPos.ORIGIN,
                    255,
                    mc.player.currentEquippedItem,
                    0F,
                    0F,
                    0F
                )
            )

            val (yaw, pitch) = currentRotation ?: thePlayer.rotation

            repeat(packets) {
                sendPacket(C05PacketPlayerLook(yaw, pitch, true))
            }

            sendPacket(PlayerActionC2SPacket(RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN))
            thePlayer.itemInUseCount = currentItem.maxItemUseDuration - 1
        }
    }
}