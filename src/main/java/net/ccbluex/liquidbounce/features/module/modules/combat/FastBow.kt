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
import net.minecraft.item.BowItem
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookOnly
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.PlayerActionC2SPacket.Action.RELEASE_USE_ITEM
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.Direction

object FastBow : Module("FastBow", Category.COMBAT, hideModule = false) {

    private val packets by IntegerValue("Packets", 20, 3..20)

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.player ?: return

        if (!thePlayer.isUsingItem)
            return

        val selectedSlot = thePlayer.inventory.getselectedSlot()

        if (selectedSlot != null && selectedSlot.item is BowItem) {
            sendPacket(
                PlayerInteractBlockC2SPacket(
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
                sendPacket(LookOnly(yaw, pitch, true))
            }

            sendPacket(PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN))
            thePlayer.itemInUseCount = selectedSlot.maxItemUseDuration - 1
        }
    }
}